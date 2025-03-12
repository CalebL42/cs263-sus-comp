// translated from canonical java version for a more direct comparison
#include <iostream>
#include <string>

struct Tree {
    Tree* left;
    Tree* right;

    Tree(Tree* left, Tree* right) : left(left), right(right) {}

    static Tree* with(int depth) {
        return (depth == 0) 
            ? new Tree(nullptr, nullptr)
            : new Tree(with(depth - 1), with(depth - 1));
    }

    int nodeCount() const {
        return (left == nullptr) ? 1 : 1 + left->nodeCount() + right->nodeCount();
    }

    void clear() {
        if (left) {
            left->clear();
            delete left;
            left = nullptr;
            right->clear();
            delete right;
            right = nullptr;
        }
    }
};

void stretch(int depth);
int count(int depth);

int main(int argc, char* argv[]) {
    int n = 10;
    if (argc > 1) n = std::stoi(argv[1]);

    int minDepth = 4;
    int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
    int stretchDepth = maxDepth + 1;

    stretch(stretchDepth);
    Tree* longLivedTree = Tree::with(maxDepth);

    for (int depth = minDepth; depth <= maxDepth; depth += 2) {
        int iterations = 1 << (maxDepth - depth + minDepth);
        int sum = 0;
        for (int i = 1; i <= iterations; i++) {
            sum += count(depth);
        }
        std::cout << iterations << "\t trees of depth "
                  << depth << "\t check: " << sum << std::endl;
    }

    int count = longLivedTree->nodeCount();
    longLivedTree->clear();
    delete longLivedTree;
    std::cout << "long lived tree of depth "
              << maxDepth << "\t check: " << count << std::endl;

    return 0;
}

void stretch(int depth) {
    std::cout << "stretch tree of depth "
              << depth << "\t check: " << count(depth) << std::endl;
}

int count(int depth) {
    Tree* t = Tree::with(depth);
    int c = t->nodeCount();
    t->clear();
    delete t;
    return c;
}
