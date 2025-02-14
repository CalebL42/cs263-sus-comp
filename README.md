# cs263-sus-comp
Comparing the sustainability of large software workloads in Java vs C++

### CPPJoules
See https://rishalab.github.io/CPPJoules/ for a quick start guide on installing/using CPPJoules.

JoularJX at https://github.com/joular/joularjx

### Running tests with JoularJX:
1. `javac .\<test_folder>\<test_name>.java` 
   1. e.g. `javac .\math_benchmarks\Base64Java.java`
2. `java -javaagent:joularjx\target\joularjx-3.0.1.jar <test_folder>.<ClassName>` 
   1. e.g. `java -javaagent:joularjx\target\joularjx-3.0.1.jar math-benchmarks.Base64Java`

Test results are generated in the folder joularjx-result

### Computer Languages Benchmarks Game 
A large portion of the tests we use came from this project. See https://benchmarksgame-team.pages.debian.net/benchmarksgame/index.html for details
