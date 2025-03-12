#include <iostream>
#include <fstream>
#include <nlohmann/json.hpp>

using json = nlohmann::ordered_json;
using namespace std;

int main(int argc, char* argv[]) {
    json json_data;
    string load_path = "../../json/random" + string(argv[1]) + ".json";
    string store_path = "../../json/cpp_out_" + string(argv[1]) + ".json";
    
    // load data
    ifstream read_file(load_path);
    read_file >> json_data; 
    read_file.close();

    // save data
    ofstream write_file(store_path);
    write_file << json_data.dump(4);
    write_file.close();

    return 0;
}