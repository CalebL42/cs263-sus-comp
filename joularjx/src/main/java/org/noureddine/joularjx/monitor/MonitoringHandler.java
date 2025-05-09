/*
 * Copyright (c) 2021-2024, Adel Noureddine, Université de Pau et des Pays de l'Adour.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the
 * GNU General Public License v3.0 only (GPL-3.0-only)
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 */

package org.noureddine.joularjx.monitor;

import java.io.IOException;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.noureddine.joularjx.Agent;
import org.noureddine.joularjx.cpu.Cpu;
import org.noureddine.joularjx.result.ResultTreeManager;
import org.noureddine.joularjx.result.ResultWriter;
import org.noureddine.joularjx.utils.AgentProperties;
import org.noureddine.joularjx.utils.CallTree;
import org.noureddine.joularjx.utils.JoularJXLogging;
import org.noureddine.joularjx.utils.Scope;
import org.noureddine.joularjx.utils.StackTraceFilter;

import com.sun.management.OperatingSystemMXBean;

/**
 * The MonitoringHandler performs all the sampling and energy computation step, and stores the data in dedicated MonitoringStatus structures or in files.
 */
public class MonitoringHandler implements Runnable {

    private static final String DESTROY_THREAD_NAME = "DestroyJavaVM";
    private static final Logger logger = JoularJXLogging.getLogger();

    private final long appPid;
    private final AgentProperties properties;
    private final ResultWriter resultWriter;
    private final Cpu cpu;
    private final MonitoringStatus status;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;
    private final ResultTreeManager resultTreeManager;
    private final long sampleTimeMilliseconds = 1000;
    private final long sampleRateMilliseconds;
    private final int sampleIterations;

    /**
     * Creates a new MonitoringHandler.
     * @param appPid the PID of the monitored application
     * @param properties the agent's configuration properties
     * @param resultWriter the writer that will be used to save data in files
     * @param cpu an implementation of the CPU interface, depending on the OS and hardware
     * @param status where all the runtime data will be saved
     * @param osBean the OperatingSystemMXBean, used to collect CPU and process loads
     * @param threadBean the ThreadMXBean, used to collect thread CPU time
     * @param resultTreeManager the ResultTreeManager, used to provide filepaths for runtime generated files
     */
    public MonitoringHandler(long appPid, AgentProperties properties, ResultWriter resultWriter, Cpu cpu,
                             MonitoringStatus status, OperatingSystemMXBean osBean, ThreadMXBean threadBean, ResultTreeManager resultTreeManager) {
        this.appPid = appPid;
        this.properties = properties;
        this.resultWriter = resultWriter;
        this.cpu = cpu;
        this.status = status;
        this.osBean = osBean;
        this.threadBean = threadBean;
        this.resultTreeManager = resultTreeManager;
        this.sampleRateMilliseconds = properties.stackMonitoringSampleRate();
        this.sampleIterations = (int) (sampleTimeMilliseconds / sampleRateMilliseconds);
    }

    @Override
    public void run() {
        logger.log(Level.INFO, String.format("Started monitoring application with ID %d", appPid));

        // CPU time for each thread
        Map<Long, Long> threadsCpuTime = new HashMap<>();

        while (!destroyingVM()) {
            try {
                double energyBefore = cpu.getInitialPower();

                var samples = sample();
                var methodsStats = extractStats(samples, methodName -> true);
                var methodsStatsFiltered = extractStats(samples, properties::filtersMethod);

                //Collecting call trees stats only if the option is enabled
                Map<Thread, Map<CallTree, Integer>> callTreesStats = null;
                Map<Thread, Map<CallTree, Integer>> filteredCallTreeStats = null;
                if (this.properties.callTreesConsumption()) {
                    callTreesStats = extractCallTreesStats(samples, methodName -> true);
                    filteredCallTreeStats = extractCallTreesStats(samples, properties::filtersMethod);
                }

                double cpuLoad = osBean.getSystemCpuLoad(); // In future when Java 17 becomes widely deployed, use getCpuLoad() instead
                double processCpuLoad = osBean.getProcessCpuLoad();

                double energyAfter = cpu.getCurrentPower(cpuLoad);
                double cpuEnergy = energyAfter - energyBefore;
                
                // Check if energy after is smaller than before
                // Meaning: RAPL energy has wrapped
                if (energyBefore > energyAfter) {
                    cpuEnergy += cpu.getMaxPower(cpuLoad);
                }

                // if cpuEnergy is negative, skip this cycle.
                // this happens when energy counter is reset during program execution
                if (cpuEnergy < 0) {
                	logger.info("Negative energy delta detected, skipping this cycle: " + cpuEnergy);
                	continue;
                }
                
                // Calculate CPU energy consumption of the process of the JVM all its apps
                double processEnergy = calculateProcessCpuEnergy(cpuLoad, processCpuLoad, cpuEnergy);

                // Adds current power to total energy
                status.addConsumedEnergy(processEnergy);

                // Now we have:
                // CPU energy for JVM process
                // CPU energy for all processes
                // We need to calculate energy for each thread
//                long totalThreadsCpuTime = updateThreadsCpuTime(methodsStats, threadsCpuTime);
//                var threadCpuTimePercentages = getThreadsCpuTimePercentage(threadsCpuTime, totalThreadsCpuTime, processEnergy);

                var threadCpuTimePercentages = getThreadsCpuTimePercentage(methodsStats, threadsCpuTime, processEnergy);
                
                updateMethodsConsumedEnergy(methodsStats, threadCpuTimePercentages, status::addMethodConsumedEnergy, Scope.ALL);
                updateMethodsConsumedEnergy(methodsStatsFiltered, threadCpuTimePercentages, status::addFilteredMethodConsumedEnergy, Scope.FILTERED);

                //Updating call trees consumption if option is enabled
                if (this.properties.callTreesConsumption()) {
                    updateCallTreesConsumedEnergy(callTreesStats, threadCpuTimePercentages, status::addCallTreeConsumedEnergy);
                    updateCallTreesConsumedEnergy(filteredCallTreeStats, threadCpuTimePercentages, status::addFilteredCallTreeConsumedEnergy);

                    //Writing runtime call trees power only if option is enabled
                    if (this.properties.saveCallTreesRuntimeData()) {
                        if (this.properties.overwriteCallTreesRuntimeData()) {
                            this.saveResults(callTreesStats, threadCpuTimePercentages, this.resultTreeManager.getAllRuntimeCallTreePath()+String.format("/joularJX-%d-all-call-trees-power", appPid));
                            this.saveResults(filteredCallTreeStats, threadCpuTimePercentages, this.resultTreeManager.getFilteredRuntimeCallTreePath()+String.format("/joularJX-%d-filtered-call-trees-power", appPid));
                        } else {
                            this.saveResults(callTreesStats, threadCpuTimePercentages, this.resultTreeManager.getAllRuntimeCallTreePath()+String.format("/joularJX-%d-%d-all-call-trees-power", appPid, System.currentTimeMillis()));
                            this.saveResults(filteredCallTreeStats, threadCpuTimePercentages, this.resultTreeManager.getFilteredRuntimeCallTreePath()+String.format("/joularJX-%d-%d-filtered-call-trees-power", appPid, System.currentTimeMillis()));
                        }
                    }
                }

                //Writing runtime method's power only if option is enabled
                if (this.properties.savesRuntimeData()) {
                    if (this.properties.overwritesRuntimeData()) {
                        this.saveResults(methodsStats, threadCpuTimePercentages, this.resultTreeManager.getAllRuntimeMethodsPath()+String.format("/joularJX-%d-all-methods-power", appPid));
                        this.saveResults(methodsStatsFiltered, threadCpuTimePercentages, this.resultTreeManager.getFilteredRuntimeMethodsPath()+String.format("/joularJX-%d-filtered-methods-power", appPid));
                    } else {
                        this.saveResults(methodsStats, threadCpuTimePercentages, this.resultTreeManager.getAllRuntimeMethodsPath()+String.format("/joularJX-%d-%d-all-methods-power", appPid, System.currentTimeMillis()));
                        this.saveResults(methodsStatsFiltered, threadCpuTimePercentages, this.resultTreeManager.getFilteredRuntimeMethodsPath()+String.format("/joularJX-%d-%d-filtered-methods-power", appPid, System.currentTimeMillis()));
                    }
                }

                Thread.sleep(sampleRateMilliseconds);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Cannot perform IO \"{0}\"", exception.getMessage());
                logger.throwing(getClass().getName(), "run", exception);
                System.exit(1);
            }
        }
    }

	/**
     * Performs the sampling step. Collects a set of stack traces for each thread.
     * The sampling step is performed multiple time at the frequecy of SAMPLE_RATE_MILLSECONDS, for the duration of SAMPLE_TIME_MILLISECONDS
     * @return for each Thread, a List of it's the stack traces
     */
    private Map<Thread, List<StackTraceElement[]>> sample() {
        Map<Thread, List<StackTraceElement[]>> result = new HashMap<>();
        try {
            for (int duration = 0; duration < sampleTimeMilliseconds; duration += sampleRateMilliseconds) {
                for (var entry : Thread.getAllStackTraces().entrySet()) {
                    String threadName = entry.getKey().getName();
                    //Ignoring agent related threads, if option is enabled
                    if(this.properties.hideAgentConsumption() && (threadName.equals(Agent.COMPUTATION_THREAD_NAME))) {
                        continue; //Ignoring the thread
                    }

                    // Only check runnable threads (not waiting or blocked)
                    if (entry.getKey().getState() == Thread.State.RUNNABLE) {
                        var target = result.computeIfAbsent(entry.getKey(),
                                t -> new ArrayList<>(sampleIterations));
                        target.add(entry.getValue());
                    }
                }

                Thread.sleep(sampleRateMilliseconds);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        return result;
    }

    /**
     * Return the occurences of each method call during monitoring loop, per thread.
     * @param samples the result of the sampking step. A List of StackTraces of each Thread
     * @param covers a Predicate, used to filter method names
     * @return for each Thread, a Map of each method and its occurences during the last monitoring loop
     */
    private Map<Thread, Map<String, Integer>> extractStats(Map<Thread, List<StackTraceElement[]>> samples,
                                                           Predicate<String> covers) {
        Map<Thread, Map<String, Integer>> stats = new HashMap<>();

        for (var entry : samples.entrySet()) {
            Map<String, Integer> target = new HashMap<>();
            stats.put(entry.getKey(), target);

            for (StackTraceElement[] stackTrace : entry.getValue()) {
                for (StackTraceElement stackTraceElement : stackTrace) {
                    String methodName = stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName();
                    if (covers.test(methodName)) {
                        target.merge(methodName, 1, Integer::sum);
                        break;
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Returns the occurences of each call tree during monitoring loop, per thread.
     * @param samples the result of the sampling step. A List of StackTraces of each Thread. 
     * @param filter a Predicate, used to filter method names within the call tree.
     * @return for each Thread, a Map of each CallTree and its occurences during the last monitoring loop.
     */
    private Map<Thread, Map<CallTree, Integer>> extractCallTreesStats(Map<Thread, List<StackTraceElement[]>> samples, Predicate<String> filter){
        Map<Thread, Map<CallTree, Integer>> stats = new HashMap<>();

        for (var entry : samples.entrySet()) {
            Map<CallTree, Integer> target = new HashMap<>();
            stats.put(entry.getKey(), target);

            for (var stackTraceEntry : entry.getValue()) {
                List<StackTraceElement> stackTrace = StackTraceFilter.filter(stackTraceEntry, filter);
                if (!stackTrace.isEmpty()) {
                    target.merge(new CallTree(stackTrace), 1, Integer::sum);
                }
            }
        }

        return stats;
    }

    
    /**
     * Updates the CPU times for each Thread.
     * Returns for each thread (PID) it's percentage of CPU time used
     * @param methodsStats a map of method occurrences for each thread
     * @param threadsCpuTime a map of CPU time per PID, contains the cpu time for each tread, resulting from the last call to getThreadCpuTime(threadId)
     * @param processEnergy the energy consumed by the process in the last monitoring period
     * @return for each PID, the percentage of energy used by the associated thread
     */
    private Map<Long, Double> getThreadsCpuTimePercentage(Map<Thread, Map<String, Integer>> methodsStats,
			Map<Long, Long> threadsCpuTime, double processEnergy) {
		Map<Long, Double> threadsCpuTimePercentage = new HashMap<Long, Double>();
    	
		Map<Long, Double> actualThreadsCpuTime = new HashMap<Long, Double>(); 
		double totalThreadsCpuTime = 0;
		// first compute the proportion of cpu time for each thread in the last sampling period
		for (Entry<Thread, Map<String, Integer>> threadEntry : methodsStats.entrySet()) {
			long threadId = threadEntry.getKey().getId();
			long currentThreadCpuTime = threadBean.getThreadCpuTime(threadId);
			long previousThreadCpuTime = threadsCpuTime.getOrDefault(threadId, 0l);
			if (currentThreadCpuTime < 0) { // thread has quit
				// TODO ignore last sampling period??
				long jump = this.sampleRateMilliseconds / 10;
				currentThreadCpuTime = previousThreadCpuTime + jump; // assume interval of 1 millisecond 
				logger.info("Thread CPU time negative, taking previous time + " + jump + " : " + currentThreadCpuTime + " for thread: " + threadId);
			}
			
			threadsCpuTime.put(threadId, currentThreadCpuTime);
			long delta = currentThreadCpuTime - previousThreadCpuTime;
			double adjustedThreadCpuTime = delta * threadEntry.getValue().values().stream().mapToDouble(i -> i).sum() / sampleIterations;
			totalThreadsCpuTime += adjustedThreadCpuTime;
			actualThreadsCpuTime.put(threadId, adjustedThreadCpuTime);
		}
		
		// compute the proportion of total energy consumed by the thread using its proportion of cpu time in the last sampling period
    	for (Entry<Long, Double> threadEntry : actualThreadsCpuTime.entrySet()) {
    		double threadEnergy = totalThreadsCpuTime > 0d?threadEntry.getValue() * processEnergy / totalThreadsCpuTime : 0d;
    		threadsCpuTimePercentage.put(threadEntry.getKey(), threadEnergy);
    	}
		
		return threadsCpuTimePercentage;
	}
    
    /**
     * Update method's consumed energy. 
     * @param methodsStats method's encounters statistics per Thread
     * @param threadCpuTimePercentages a map of CPU time usage per PID
     * @param updateMethodConsumedEnergy an object consumer, used to update all or only filtered methods
     * @param scope the scope (all methods or only filterd methods). Used for energy consumption tracking
     */
    private void updateMethodsConsumedEnergy(Map<Thread, Map<String, Integer>> methodsStats,
                                             Map<Long, Double> threadCpuTimePercentages,
                                             ObjDoubleConsumer<String> updateMethodConsumedEnergy,
                                             Scope scope) {
        for (var threadEntry : methodsStats.entrySet()) {
            double totalEncounters = threadEntry.getValue().values().stream().mapToDouble(i -> i).sum();
            for (var methodEntry : threadEntry.getValue().entrySet()) {
                double methodPower = 0.0;
                if(totalEncounters >= Double.MIN_VALUE) {
                    methodPower = threadCpuTimePercentages.get(threadEntry.getKey().getId()) * (methodEntry.getValue() / totalEncounters);
                }

                //Only of consumption evolution tracking is enabled
                if (this.properties.trackConsumptionEvolution()) {
                    //computing the UNIX EPOCH timestamp
                    long unixTimestamp = System.currentTimeMillis() / 1000L;

                    if (scope == Scope.ALL) {
                        this.status.trackMethodConsumption(methodEntry.getKey(), unixTimestamp, methodPower);
                    } else {
                        this.status.trackFilteredMethodConsumption(methodEntry.getKey(), unixTimestamp, methodPower);
                    }
                }

                updateMethodConsumedEnergy.accept(methodEntry.getKey(), methodPower);
            }
        }
    }

    /**
     * Update call trees consumed energy.
     * @param stats call trees encounters statistics per Thread
     * @param threadCpuTimePercentages map of CPU time usage per PID
     * @param callTreeConsumer the method used to update the energy consumption
     */
    private void updateCallTreesConsumedEnergy(Map<Thread, Map<CallTree, Integer>> stats, Map<Long, Double> threadCpuTimePercentages, ObjDoubleConsumer<CallTree> callTreeConsumer) {
        for (var entry : stats.entrySet()) {
            double totalEncounters = entry.getValue().values().stream().mapToDouble(i -> i).sum();

            for (var callTreeEntry : entry.getValue().entrySet()) {
                double stackTracePower = 0.0;
                if (totalEncounters >= Double.MIN_VALUE) {
                     stackTracePower = threadCpuTimePercentages.get(entry.getKey().getId()) * (callTreeEntry.getValue() / totalEncounters);
                }

                callTreeConsumer.accept(callTreeEntry.getKey(), stackTracePower);
            }
        }
    }

    /**
     * Calculate process energy consumption
     * @param totalCpuUsage Total CPU usage
     * @param processCpuUsage Process CPU usage
     * @param cpuEnergy CPU energy
     * @return Process energy consumption
     */
    private double calculateProcessCpuEnergy(double totalCpuUsage, double processCpuUsage, double cpuEnergy) {
        return (processCpuUsage * cpuEnergy) / totalCpuUsage;
    }

    /**
     * Writes the results in a file. The filename is partially defined by the given parameters.
     * @param <K> The type of key that will be written in the file. Must implement the toString() method.
     * @param stats the data to be written, given under the form of a Map<Thread, Map<K>, Double>> where the Double is the enrgy consumption.
     * @param threadCpuTimePercentages a map of CPU time usage per Thread (PID)
     * @param filePath the path of the file where the data will be written
     * @throws IOException if an I/O error occurs while writing the file
     */
    public <K> void saveResults(Map<Thread, Map<K,Integer>> stats,  Map<Long, Double> threadCpuTimePercentages, String filePath) throws IOException {
        resultWriter.setTarget(filePath, true);

            for (var statEntry : stats.entrySet()) {
                for (var entry : statEntry.getValue().entrySet()) {
                    double power = threadCpuTimePercentages.get(statEntry.getKey().getId()) * (entry.getValue() / 100.0);
                    resultWriter.write(entry.getKey().toString(), power);
                }
            }
        
            resultWriter.closeTarget();
    }

    /**
     * Indicate if the JVM is destroying
     * @return true if the JVM destroying thread is present, false otherwise
     */
    private boolean destroyingVM() {
        if (!this.properties.isApplicationServer()) {
            return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals(DESTROY_THREAD_NAME));
        } else {
            return false;
        }
    }
}