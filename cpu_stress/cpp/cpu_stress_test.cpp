#include <iostream>
#include <vector>
#include <cmath>
#include <thread>
#include <atomic>
#include <chrono>

const int MAX_NUMBER = 100000000;
const int NUM_THREADS = std::thread::hardware_concurrency();
std::atomic_int primeCount(0);

bool isPrime(int num) {
    if (num < 2) return false;
    for (int i = 2; i <= std::sqrt(num); ++i) {
        if (num % i == 0) return false;
    }
    return true;
}

void findPrimes(int start, int end) {
    int localCount = 0;
    for (int num = start; num < end; ++num) {
        if (isPrime(num)) {
            localCount++;
        }
    }
    primeCount.fetch_add(localCount);
}

int main() {
    std::cout << "Starting CPU Stress Test using " << NUM_THREADS << " threads..." << std::endl;
    
    auto startTime = std::chrono::system_clock::now();
    
    std::vector<std::thread> threads;
    
    for (int i = 0; i < NUM_THREADS; ++i) {
        int start = i * (MAX_NUMBER / NUM_THREADS);
        int end = (i == NUM_THREADS - 1) ? MAX_NUMBER : (i + 1) * (MAX_NUMBER / NUM_THREADS);
        threads.emplace_back(findPrimes, start, end);
    }
    
    for (auto& th : threads) {
        th.join();
    }
    
    auto endTime = std::chrono::system_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    std::cout << "CPU Stress Test Completed in " << elapsed.count() << " ms" << std::endl;
    std::cout << "Total Primes Found: " << primeCount.load() << std::endl;
    
    return 0;
}