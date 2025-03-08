#include "httplib.h"
#include <iostream>
#include <vector>
#include <string>
#include <chrono>
#include <thread>
#include <random>
#include <atomic>

// Statistics tracking
struct Stats {
    std::atomic<int> total_requests{0};
    std::atomic<int> successful_requests{0};
    std::atomic<int> failed_requests{0};
    std::atomic<long long> total_response_time{0};  // in milliseconds
};

// Function to simulate a single client making requests
void client_simulator(const std::string& host, int port, 
                     const std::vector<std::string>& paths,
                     int num_requests, int delay_ms, Stats& stats) {
    
    // Create client
    httplib::Client cli(host, port);
    
    // Set timeout
    cli.set_connection_timeout(5, 0); 
    cli.set_read_timeout(10, 0);

    //reuse HTTP connections
    cli.set_keep_alive(true); 
    
    for (int i = 0; i < num_requests; i++) {
        // Select a random path
        std::string path = paths[i%paths.size()];
        
        // Record start time
        auto start = std::chrono::high_resolution_clock::now();
        
        // Make the request
        auto res = cli.Get(path);
        
        // Record end time and calculate duration
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
        
        // Update statistics
        stats.total_requests++;
        stats.total_response_time += duration;
        
        if (res && res->status == 200) {
            stats.successful_requests++;
            std::cout << "Request to " << path << " succeeded in " << duration << "ms" << std::endl;
        } else {
            stats.failed_requests++;
            std::cout << "Request to " << path << " failed with ";
            if (res) {
                std::cout << "status " << res->status << std::endl;
            } else {
                std::cout << "error: " << httplib::to_string(res.error()) << std::endl;
            }
        }
        
        // Add delay between requests
        if (delay_ms > 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
        }
    }
}

int main(int argc, char* argv[]) {
    // Default parameters
    //std::string host = "localhost";
    std::string host = "169.231.165.102"; // IP address of the server
    int port = 8080;
    int num_clients = 1;
    int requests_per_client = 200;
    int delay_ms = 0;
    
    // Parse command line arguments if provided
    if (argc > 1) host = argv[1];
    if (argc > 2) port = std::stoi(argv[2]);
    if (argc > 3) num_clients = std::stoi(argv[3]);
    if (argc > 4) requests_per_client = std::stoi(argv[4]);
    if (argc > 5) delay_ms = std::stoi(argv[5]);
    
    // List of paths to request (add your own paths based on your test files)
    std::vector<std::string> paths = {
        "/index.html",
        "/images/amogus.png",
        "/images/pumpkin.jpg", 
        "/js/script.js",
        "/css/style.css"
    };
    
    // Statistics
    Stats stats;
    
    // Create and start client threads
    std::vector<std::thread> threads;
    std::cout << "Starting " << num_clients << " client(s), each making " 
              << requests_per_client << " request(s)..." << std::endl;
    
    auto start_time = std::chrono::high_resolution_clock::now();
    
    for (int i = 0; i < num_clients; i++) {
        threads.emplace_back(client_simulator, host, port, std::ref(paths), 
                            requests_per_client, delay_ms, std::ref(stats));
    }
    
    // Wait for all threads to complete
    for (auto& t : threads) {
        t.join();
    }
    
    auto end_time = std::chrono::high_resolution_clock::now();
    auto total_duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();
    
    // Print results
    std::cout << "\n----- Simulation Results -----" << std::endl;
    std::cout << "Total requests: " << stats.total_requests << std::endl;
    std::cout << "Successful requests: " << stats.successful_requests << std::endl;
    std::cout << "Failed requests: " << stats.failed_requests << std::endl;
    
    if (stats.total_requests > 0) {
        double avg_response_time = static_cast<double>(stats.total_response_time) / stats.total_requests;
        std::cout << "Average response time: " << avg_response_time << " ms" << std::endl;
        
        double requests_per_second = static_cast<double>(stats.total_requests) * 1000 / total_duration;
        std::cout << "Throughput: " << requests_per_second << " requests/second" << std::endl;
    }
    
    std::cout << "Total simulation time: " << total_duration << " ms" << std::endl;
    
    return 0;
}