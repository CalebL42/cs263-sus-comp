#include "httplib.h"
#include <iostream>
#include <string>

int main() {
    // Create a server instance
    httplib::Server svr;
    
    // Set up a directory with your test files
    const std::string static_dir = "../www";  // Create this directory with test files
    
    // Mount the static directory to be served at the root URL
    auto success = svr.set_mount_point("/", static_dir);
    if (!success) {
        std::cerr << "Failed to mount directory: " << static_dir << std::endl;
        return 1;
    }
    
    // Start the server
    std::cout << "Server started at http://localhost:8080" << std::endl;
    svr.listen("0.0.0.0", 8080);
    
    return 0;
}