// translated from canonical java version for a more direct comparison
#include <iostream>
#include <iomanip>
#include <string>

class SpectralNorm {
public:
    double eval_A(int i, int j) {
        return 1.0 / ((i + j) * (i + j + 1) / 2 + i + 1);
    }

    void eval_A_times_u(int N, const double* u, double* Au) {
        int i, j;
        for (i = 0; i < N; i++) {
            Au[i] = 0;
            for (j = 0; j < N; j++) {
                Au[i] += eval_A(i, j) * u[j];
            }
        }
    }

    void eval_At_times_u(int N, const double* u, double* Au) {
        int i, j;
        for (int i = 0; i < N; i++) {
            Au[i] = 0;
            for (j = 0; j < N; j++) {
                Au[i] += eval_A(j, i) * u[j];
            }
        }
    }

    void eval_AtA_times_u(int N, const double* u, double* AtAu) {
        double* v = new double[N]; 
        eval_A_times_u(N, u, v);
        eval_At_times_u(N, v, AtAu);
    }
};

int main(int argc, char* argv[]) {
    int i;
    static int N = (argc > 1) ? std::stoi(argv[1]) : 100;
    SpectralNorm nonStatic;
    double* u = new double[N];
    double* v = new double[N];
    double vBv, vv;
    // std::vector<double> u(N, 1.0);
    // std::vector<double> v(N);
    for (i = 0; i < N; i++) {u[i] = 1; v[i] = 0;}
    for (i = 0; i < 10; i++) {
        nonStatic.eval_AtA_times_u(N, u, v);
        nonStatic.eval_AtA_times_u(N, v, u);
    }
    vBv = vv = 0;
    
    for (i = 0; i < N; i++) {
        vBv += u[i] * v[i];
        vv += v[i] * v[i];
    }
    
    std::cout << std::fixed << std::setprecision(9) << std::sqrt(vBv / vv) << std::endl;
    return 0;
}
