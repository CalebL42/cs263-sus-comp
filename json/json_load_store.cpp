#include <iostream>
#include <fstream>
#include <nlohmann/json.hpp>

using json = nlohmann::ordered_json;
using namespace std;

int main() {
    json json_data;
    
    // load data
    ifstream read_file("timeseries.json");
    read_file >> json_data; 
    read_file.close();

    // save data
    ofstream write_file("cpp_out.json");
    write_file << json_data.dump(4);
    write_file.close();

    return 0;
}