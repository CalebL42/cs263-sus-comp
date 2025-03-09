#include <fstream>
#include <sstream>
#include <iostream>
using namespace std;

int main() {
    string file_data;

    // load data
    ifstream read_file("timeseries.json");
    stringstream sstr;
    read_file >> sstr.rdbuf();
    file_data = sstr.str();
    read_file.close();

    // cout << file_data << endl;

    // save data
    ofstream write_file("cpp_out.txt");
    write_file << file_data;
    write_file.close();

    return 0;
}