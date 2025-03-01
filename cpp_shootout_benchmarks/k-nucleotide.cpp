// translated from canonical java version for a more direct comparison
#include <iostream>
#include <unordered_map>
#include <vector>
#include <algorithm>
#include <iomanip>
#include <sstream>

std::vector<std::string> seqLines() {
    std::string line;    
    while (std::getline(std::cin, line)) {
        if (line.find(">THREE") == 0) break;
    }
    std::vector<std::string> lines;
    while (std::getline(std::cin, line)) {
        if (line.find(">")) break;
        lines.push_back(line);
    }
    return lines;
}

std::unordered_map<std::string, int> baseCounts(int bases, const std::string& seq) {
    std::unordered_map<std::string, int> counts;
    const int size = seq.length() + 1 - bases;
    for (int i = 0; i < size; i++) {
        std::string nucleo = seq.substr(i, bases);
        counts[nucleo]++;
    }
    return counts;
}

// this method is a little different but the java version uses a lot of 
// language-specific data structures and functions
std::vector<std::string> sortedFreq(int bases, const std::string& seq) {
    int size = seq.length() + 1 - bases;
    auto counts = baseCounts(bases, seq);
    
    std::vector<std::pair<std::string, int>> entries(counts.begin(), counts.end());
    
    std::sort(entries.begin(), entries.end(), [](const auto& a, const auto& b) {
        return a.second > b.second;
    });
    
    std::vector<std::string> result;
    std::transform(entries.begin(), entries.end(), std::back_inserter(result),
        [size](const std::pair<std::string, int>& e) {
            std::ostringstream stream;
            stream << e.first << " " << std::fixed << std::setprecision(3) << (100.0 * e.second / size);
            return stream.str();
        });
    
    return result;
}

int specificCount(const std::string& code, const std::string& seq) {
    auto counts = baseCounts(code.length(), seq);
    return counts.count(code) ? counts[code] : 0;
}

int main() {
    std::vector<std::string> lines = seqLines();
    std::string seq;
    for (const auto& line : lines) {
        for (char c : line) {
            seq += std::toupper(c);
        }
    }
    
    for (int i : {1, 2}) {
        for (const std::string& s : sortedFreq(i, seq)) {
            std::cout << s << std::endl;
        }
        std::cout << std::endl;
    }
    
    for (const std::string& code : {"GGT", "GGTA", "GGTATT", "GGTATTTTAATT", "GGTATTTTAATTTATAGT"}) {
        std::cout << specificCount(code, seq) << "\t" << code << std::endl;
    }
    
    return 0;
}
