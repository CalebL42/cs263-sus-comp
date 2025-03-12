import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CpuStressTest {
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int MAX_NUMBER = 100000000;  
    private static final AtomicInteger primeCount = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("Starting CPU Stress Test using " + NUM_THREADS + " threads...");

        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            int start = i * (MAX_NUMBER / NUM_THREADS);
            int end = (i == NUM_THREADS - 1) ? MAX_NUMBER : (i + 1) * (MAX_NUMBER / NUM_THREADS);
            executor.execute(() -> findPrimes(start, end));
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            // Wait for all threads to finish
        }

        long endTime = System.currentTimeMillis();
        System.out.println("CPU Stress Test Completed in " + (endTime - startTime) + " ms");
        System.out.println("Total Primes Found: " + primeCount.get());
    }

    private static void findPrimes(int start, int end) {
        int localCount = 0;
        for (int num = start; num < end; num++) {
            if (isPrime(num)) {
                localCount++;
            }
        }
        primeCount.addAndGet(localCount);
    }

    private static boolean isPrime(int num) {
        if (num < 2) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }
}
