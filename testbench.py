# Helper to run perf commands multiple times and record output
import os
import subprocess
import re
import time
import statistics
import json
import sys

testbench_log = open("testbench_log.txt", "w")

# get the joules consumed for the specified event and command, as well as the time elapsed in seconds
def run_perf(event, command):
    print(command)
    result = subprocess.run(["perf", "stat", "-a", "-e", event] + command, capture_output=True, text=True)
    result = result.stderr # not sure why it prints to stderr and not stdout
    print(result)

    result_list = result.split()
    trimmed_list = []
    for res in result_list:
        try:
            trimmed_list.append(float(res))
        except ValueError:
            continue
    print(trimmed_list)
    if len(trimmed_list) != 2:
        testbench_log.write(f"Wrong amount of numbers for perf to return. Data likely recorded incorrectly. (result was {trimmed_list})")

    return {"joules": trimmed_list[0], 
            "seconds": trimmed_list[1]}

# get the joules and duration for the specified event and command. run multiple times, with a delay between
# trials to let the cpu slow down again before the next test.
def run_perfs(event, command, times, delay):
    d = {}
    for i in range(times):
        print(i+1)
        d[i] = run_perf(event, command)
        time.sleep(delay)
    return d

# given a dictionary of trials, each with joules and duration, return a new dictionary that includes 
# the average and standard deviation of joules and duration
def get_stats(d):
    joules = []
    seconds = []
    print(d)
    for entry in d:
        print(d[entry])
        joules.append(d[entry]["joules"])
        seconds.append(d[entry]["seconds"])
    
    stat_dict = {"avg": {
                    "joules": statistics.mean(joules),
                    "seconds": statistics.mean(seconds)}, 
                 "stddev": {
                    "joules": statistics.stdev(joules),
                    "seconds": statistics.stdev(seconds)}
                }
    return {**d, **stat_dict}

def write_json(d, filename: str):
    os.makedirs(filename[:filename.rfind("/")], exist_ok=True)
    json_str = json.dumps(d, indent=4)
    with open(filename, 'w') as f:
        f.write(json_str)

### TESTS ###

# test matmuls. start from `starting_size`. run for `iters` 
# different sizes, doubling the size each time. for each size, 
# run `times`` times. wait `delay`` seconds between each invocation. 
def test_matmul(starting_size, iters, times, delay):
    cpp_comp_flags = ["-O0", "-O1", "-O2", "-O3"]
    java_exec_flags = [["-Xint"],
                       ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1"],
                       ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=2"],
                       ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=3"],
                       ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=4"],
                      ]

    # c++ compilation time at all compiler levels
    cpp_compilations = {}
    for flag in cpp_comp_flags:
        cpp_comp_cmd = ["g++", "matmul/matmul.cpp", "-o", f"matmul/matmul{flag}", flag]
        d = get_stats(run_perfs("power/energy-pkg/", cpp_comp_cmd, times, delay))
        cpp_compilations[flag] = d
    
    # java compilation time
    java_comp_cmd = ["javac", "matmul/matmul.java"]
    java_compilations = get_stats(run_perfs("power/energy-pkg/", java_comp_cmd, times, delay))
    
    # execution times
    cpp_executions = {}
    java_executions = {}
    size = starting_size
    for _ in range(iters):

        # c++ execution time at all compiler levels
        cpp_executions[size] = {}
        for flag in cpp_comp_flags:
            d = get_stats(run_perfs("power/energy-pkg/", [f"./matmul/matmul{flag}", str(size)], times, delay))
            cpp_executions[size][flag] = d
        
        # java execution time at all JIT levels
        java_executions[size] = {}
        for flag_num, flag_set in enumerate(java_exec_flags):
            d = get_stats(run_perfs("power/energy-pkg/", ["java"] + flag_set + ["matmul.matmul", str(size)], times, delay))
            java_executions[size][f"flag{flag_num}"] = d

        size *= 2

    # save results
    base_filepath = f"testbench_outputs/matmul/start{starting_size}_iters{iters}_times{times}_delay{delay}_"
    write_json(cpp_compilations, base_filepath + "cpp_comp.json")
    write_json(java_compilations, base_filepath + "java_comp.json")
    write_json(cpp_executions, base_filepath + "cpp_exec.json")
    write_json(java_executions, base_filepath + "java_exec.json")


test_matmul(2028, 5, 10, 0)
testbench_log.close()
# # output_file = "testbench_outputs/" + sys.argv[1] + ".json"
# res = run_perfs("power/energy-pkg/", ["sleep", "1"], times=5, delay=1)
# res = get_stats(res)
# print(res)
# # write_json(res, output_file, "basic_test")
