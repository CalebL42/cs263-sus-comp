#include <vector>
#include <iostream>
using namespace std;

// generate a square matrix of given size with values starting from 'start'
vector<vector<int>> generate(int size, int start) {
    vector<vector<int>> matrix(size, vector<int>(size, 0));
    int counter = start;
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            matrix[i][j] = counter;
            counter++;
        }
    }
    return matrix;
}

// perform matrix multiplication
vector<vector<int>> mat_mul(vector<vector<int>>& m1, vector<vector<int>>& m2, int size) {
    vector<vector<int>> prod(size, vector<int>(size, 0));
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            for (int k = 0; k < size; k++) {
                prod[i][j] += m1[i][k]*m2[k][j];
            }
        }
    }
    return prod;
}

// print the full matrix
void print_matrix(vector<vector<int>>& matrix, int size) {
    cout << "[\n";
    for (int i = 0; i < size; i++) {
        cout << " [";
        for (int j = 0; j < size; j++) {
            cout << matrix[i][j] << " ";
        }
        cout << "]\n";
    }
    cout << "]\n";
}

int main(int argc, char* argv[]) {
    int n = (argc > 1) ? std::stoi(argv[1]) : 10;
    vector<vector<int>> m1 = generate(n, 0);
    vector<vector<int>> m2 = generate(n, n);

    vector<vector<int>> prod = mat_mul(m1, m2, n);
    print_matrix(prod, n);
    
    return 0;
}
