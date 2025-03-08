# cs263-sus-comp
Comparing the sustainability of large software workloads in Java vs C++

We are now moving away from using CPPJoules, and will primarily rely on intel RAPL, specifically the perf linux tool: https://perfwiki.github.io/main/.

JoularJX at https://github.com/joular/joularjx

### Running tests with JoularJX:
1. `javac .\<test_folder>\<test_name>.java` 
   1. e.g. `javac .\math_benchmarks\Base64Java.java`
2. `java -javaagent:joularjx\target\joularjx-3.0.1.jar <test_folder>.<ClassName>` 
   1. e.g. `java -javaagent:joularjx\target\joularjx-3.0.1.jar math-benchmarks.Base64Java`

Test results are generated in the folder joularjx-result

### Computer Languages Benchmarks Game 
A large portion of the tests we use came from this project. See https://benchmarksgame-team.pages.debian.net/benchmarksgame/index.html for details

### Tests thus far:
As our first real test, we ran each viable Java test from the shootout benchmark suite 10 times. Raw results are in `joularjx-result/` and summarized in `java_shootout_benchmarks/benchmark_shootout_test1_java.csv`. 

We found that some tests have such short execution times that they range between 0 and 2 joules of power consumption. At that low power consumption, the variance between trials is relatively high, and profiling data is poor. Some tests take a parameter by command line argument, and usually setting those to a large number makes the test take longer. This results in better profiling data and the variance in power consumption is smaller relative to the average. So going forward we'll focus on longer running programs.

### Comparing JVMs
We compare the power efficiency/memory consumption of different implementations of the JVM, namely:
- OpenJDK HotSpot VM 	
- GraalVM CE 	21.0.1 	
- Oracle GraalVM 	
- Azul Prime VM 	
- Eclipse OpenJ9 VM

### Comparing performance on Datacenter common tasks
We compare the power efficiency/memory consumption of Java and C++ on web servicing tasks, namely http requests/responses.
For C++ we are using Drogon to create a basic web server program: https://github.com/drogonframework/drogon
For Java we are using the built in HTTP Server Class.
