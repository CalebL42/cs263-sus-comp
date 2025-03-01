// translated from canonical java version for a more direct comparison
#include <iostream>
#include <string>

static const int IM = 139968;
static const int IA = 3877;
static const int IC = 29573;
static const int SEED = 42;

static int seed = SEED;
static double fastaRand(double max) {
    seed = (seed * IA + IC) % IM;
    return max * seed / IM;
}

static const std::string ALU =
    "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
    "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
    "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
    "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
    "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
    "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
    "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA";

static const std::string iub = "acgtBDHKMNRSVWY";
static const double iubP[] = {
    0.27, 0.12, 0.12, 0.27,
    0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02};

static const std::string homosapiens = "acgt";
static const double homosapiensP[] = {
    0.3029549426680,
    0.1979883004921,
    0.1975473066391,
    0.3015094502008};

static const int LINELEN = 60;

static void repeatFasta(const std::string& seq, int n) {
    int len = seq.length();
    std::string b;
    for (int i = 0; i < n; i++) {
        b += seq[i % len];
        if (i % LINELEN == LINELEN - 1) {
            std::cout << b << std::endl;
            b.clear();
        }
    }
    if (!b.empty()) std::cout << b << std::endl;
}

static void randomFasta(const std::string& seq, const double* probability, int n) {
    int len = seq.length();
    int i, j;
    std::string b;
    for (i = 0; i < n; i++) {
        double v = fastaRand(1.0);
        for (j = 0; j < len - 1; j++) {
            v -= probability[j];
            if (v < 0) break;
        }
        b += seq[j];
        if (i % LINELEN == LINELEN - 1) {
            std::cout << b << std::endl;
            b.clear();
        }
    }
    if (!b.empty()) std::cout << b << std::endl;
}

int main(int argc, char* argv[]) {
    const int n = (argc > 1) ? std::stoi(argv[1]) : 1000;
    
    std::cout << ">ONE Homo sapiens alu" << std::endl;
    repeatFasta(ALU, n * 2);
    
    std::cout << ">TWO IUB ambiguity codes" << std::endl;
    randomFasta(iub, iubP, n * 3);
    
    std::cout << ">THREE Homo sapiens frequency" << std::endl;
    randomFasta(homosapiens, homosapiensP, n * 5);
    
    return 0;
}
