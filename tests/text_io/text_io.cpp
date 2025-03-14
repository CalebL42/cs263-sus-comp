#include <fstream>
#include <sstream>
#include <iostream>
using namespace std;

int main(int argc, char* argv[]) {
    string file_data;
    string load_path = "json/random" + string(argv[1]) + ".json";
    string store_path = "json/cpp_out_" + string(argv[1]) + ".txt";

    // load data
    ifstream read_file(load_path);
    stringstream sstr;
    read_file >> sstr.rdbuf();
    file_data = sstr.str();
    read_file.close();

    // cout << file_data << endl;

    // save data
    ofstream write_file(store_path);
    write_file << file_data;
    write_file.close();

    return 0;
}