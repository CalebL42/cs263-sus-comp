# Helper to run perf commands multiple times and record output
import os
import subprocess
import time
import statistics
import json

cpp_comp_flags = ["-O0", "-O1", "-O2", "-O3"]
java_exec_flags = [["-Xint"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=2"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=3"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=4"]]

cpp_bin = {"gpp": "g++",
           "clang": "clang++"}

java_bin = {"openjdk": "/usr/bin/",
            "graal": os.path.expanduser("~/.sdkman/candidates/java/21.0.6-graal/bin/")}

testbench_log = open("testbench_log.txt", "w")


# get the joules consumed for the specified event and command, as well as the time elapsed in seconds
def run_perf(event, command, timeout=0):
    if timeout == 0:
        print(command)
    else:
        print(command, f"timeout={timeout}")
    base = ["perf", "stat", "-a", "-e", event]
    if timeout > 0:
        base.append(f"--timeout={int(1000*timeout)}")
    result = subprocess.run(base + command, stderr=subprocess.PIPE, text=True)
    result = result.stderr # not sure why it prints to stderr and not stdout
    print(result)

    result_list = result.split()
    trimmed_list = []
    for res in result_list:
        try:
            trimmed_list.append(float(res.replace(",", "")))
        except ValueError:
            continue
    print(trimmed_list)
    if len(trimmed_list) != 2:
        testbench_log.write(f"Wrong amount of numbers for perf to return. Data likely recorded incorrectly. (result was {trimmed_list})")

    return {"joules": trimmed_list[0], 
            "seconds": trimmed_list[1]}

# get the joules and duration for the specified event and command. run multiple times, with a delay between
# trials to let the cpu slow down again before the next test.
def run_perfs(event, command, times, delay, timeout=0):
    d = {}
    for i in range(times):
        print(i+1)
        d[i] = run_perf(event, command, timeout)
        time.sleep(delay)
    return d

# given a dictionary of trials, each with joules and duration, return a new dictionary that includes 
# the average and standard deviation of joules and duration
def get_stats(d):
    joules = []
    seconds = []
    print(d)
    for entry in d:
        print(d[entry])
        joules.append(d[entry]["joules"])
        seconds.append(d[entry]["seconds"])
    
    stat_dict = {"avg": {
                    "joules": statistics.mean(joules),
                    "seconds": statistics.mean(seconds)}, 
                 "stddev": {
                    "joules": statistics.stdev(joules),
                    "seconds": statistics.stdev(seconds)}
                }
    return {**d, **stat_dict}

# write data to file
def write_json(d, filename: str):
    os.makedirs(filename[:filename.rfind("/")], exist_ok=True)
    json_str = json.dumps(d, indent=4)
    with open(filename, 'w') as f:
        f.write(json_str) 

# given a test and g++ optimization flag, compile a c++ program `times` times
def compile_cpp(test_name, times=10, delay=0, flag="", comp="gpp", timeout=0):
    comp = cpp_bin[comp]
    cpp_comp_cmd = [comp, f"tests/{test_name}/{test_name}.cpp", "-o", f"tests/{test_name}/{test_name}{flag}", flag]
    
    res = get_stats(run_perfs("power/energy-pkg/", cpp_comp_cmd, times, delay, timeout))
    if times == 1:
        return res
    else:
        return get_stats(res)

# given a test, compile a java program `times` times
def compile_java(test_name, times=10, delay=0, jit="openjdk", extra_flags=[], timeout=0):
    javac = java_bin[jit] + "javac"
    java_comp_cmd = [javac] + extra_flags + [f"tests/{test_name}/{test_name}.java"]

    res = get_stats(run_perfs("power/energy-pkg/", java_comp_cmd, times, delay, timeout))
    if times == 1:
        return res
    else:
        return get_stats(res)

# given a test, a g++ optimization flag, and an optional test parameter, run a c++ program `times` times
def run_test_cpp(test_name, flag, times, delay, size=0, timeout=0):
    command = [f"./tests/{test_name}/{test_name}{flag}"]
    if size > 0:
        command.append(str(size))
    res = get_stats(run_perfs("power/energy-pkg/", command, times, delay, timeout))
    if times == 1:
        return res
    else:
        return get_stats(res)

# given a test, a set of java optimization flags, and an optional test parameter, run a java program `times` times
def run_test_java(test_name, flag_number, times, delay, size, jit, extra_flags=[], timeout=0):
    java = java_bin[jit] + "java"
    command = [java] + extra_flags + java_exec_flags[flag_number] + [f"tests.{test_name}.{test_name}"]
    if size > 0:
        command.append(str(size))
    
    res = run_perfs("power/energy-pkg/", command, times, delay, timeout)
    if times == 1:
        return res
    else:
        return get_stats(res)

# given a test, a list of g++ optimization flags, a starting size, and a desired number of iterations,
# run the test `times` times for every flag value at `iters` different sizes that double each time.
# (starting_size, 2*starting_size, ..., 2^(iters-1)*starting_size)
def run_full_cpp(test_name, starting_size, iters, iter_type, iter_factor, flags, times, delay, timeout=0):
    cpp_executions = {}
    for flag in flags:
        size = starting_size
        cpp_executions[flag] = {}
        for _ in range(iters):
            cpp_executions[flag][size] = run_test_cpp(test_name, flag, times, delay, size, timeout)
            if iter_type == "add":
                size += iter_factor
            elif iter_type == "mult":
                size *= iter_factor
    return cpp_executions

# given a test, a list of java optimization flag sets, a starting size, and a desired number of iterations,
# run the test `times` times for every flag value at `iters` different sizes that double each time.
# (starting_size, 2*starting_size, ..., 2^(iters-1)*starting_size)
def run_full_java(test_name, starting_size, iters, iter_type, iter_factor, flags, times, delay, jit, extra_flags, timeout=0):
    java_executions = {}
    for flag_index in range(len(flags)):
        size = starting_size
        java_executions["flag" + str(flag_index)] = {}
        for _ in range(iters):
            java_executions["flag" + str(flag_index)][size] = run_test_java(test_name, flag_index, times, delay, size, jit, extra_flags, timeout)
            if iter_type == "add":
                size += iter_factor
            elif iter_type == "mult":
                size *= iter_factor
    return java_executions

# template function for testing c++/java programs that take a single command line argument, 
# usually a size of some kind. record the duration and energy usage of the test over several trials
# for c++ and java )openjdk and graal) for a range of compilation levels and argument values
def test_command_with_arg(test_name, starting_size, iters, iter_type, iter_factor, times, delay, extra_java_comp_flags=[], extra_java_exec_flags=[], timeout=0):

    gpp_compilations = {}
    for flag in cpp_comp_flags:
        gpp_compilations[flag] = compile_cpp(test_name, times, delay, flag, "gpp")
    gpp_executions = run_full_cpp(test_name, starting_size, iters, iter_type, iter_factor, cpp_comp_flags, times, delay, timeout)

    clang_compilations = {}
    for flag in cpp_comp_flags:
        clang_compilations[flag] = compile_cpp(test_name, times, delay, flag, "clang")
    clang_executions = run_full_cpp(test_name, starting_size, iters, iter_type, iter_factor, cpp_comp_flags, times, delay, timeout)

    openjdk_compilations = compile_java(test_name, times, delay, "openjdk", extra_java_comp_flags)
    openjdk_executions = run_full_java(test_name, starting_size, iters, iter_type, iter_factor, java_exec_flags, times, delay, "openjdk", extra_java_exec_flags, timeout)

    graal_compilations = compile_java(test_name, times, delay, "graal", extra_java_comp_flags)
    graal_executions = run_full_java(test_name, starting_size, iters, iter_type, iter_factor, java_exec_flags, times, delay, "graal", extra_java_exec_flags, timeout)

    # save results
    base_filepath = f"testbench_outputs/{test_name}/start{starting_size}_iters{iters}_times{times}_delay{delay}_"
    write_json(gpp_compilations, base_filepath + "gpp_comp.json")
    write_json(gpp_executions, base_filepath + "gpp_exec.json")
    write_json(clang_compilations, base_filepath + "clang_comp.json")
    write_json(clang_executions, base_filepath + "clang_exec.json")
    write_json(openjdk_compilations, base_filepath + "openjdk_comp.json")
    write_json(openjdk_executions, base_filepath + "openjdk_exec.json")
    write_json(graal_compilations, base_filepath + "graal_comp.json")
    write_json(graal_executions, base_filepath + "graal_exec.json")


# run all the benchmarks
delay = 0
times = 10
test_command_with_arg("matmul", starting_size=128, iters=4, iter_type="mult", 
                      iter_factor=2, times=times, delay=delay)
test_command_with_arg("shootout_binary_trees", starting_size=18, iters=5, iter_type="add", 
                      iter_factor=1, times=times, delay=delay)
test_command_with_arg("shootout_fannkuch_redux", starting_size=9, iters=4, iter_type="add", 
                      iter_factor=1, times=times, delay=delay)
test_command_with_arg("shootout_spectral_norm", starting_size=4096, iters=3, iter_type="mult",
                      iter_factor=2, times=times, delay=delay)
test_command_with_arg("shootout_mandelbrot", starting_size=2048, iters=4, iter_type="mult",
                      iter_factor=2, times=times, delay=delay)
test_command_with_arg("shootout_n_body", starting_size=20000000, iters=5, iter_type="add",
                      iter_factor=20000000, times=times, delay=delay)
test_command_with_arg("text_io", starting_size=128, iters=4, iter_type="mult",
                      iter_factor=2, times=times, delay=delay)
test_command_with_arg("json_io", starting_size=128, iters=4, iter_type="mult",
                      iter_factor=2, times=times, delay=delay, 
                      extra_java_comp_flags=["-cp", "libs/*:."], extra_java_exec_flags=["-cp", "libs/*:."])
test_command_with_arg("multithread_primes", starting_size=0, iters=1, iter_type="add",
                      iter_factor=0, times=times, delay=delay)
test_command_with_arg("server", starting_size=0, iters=1, iter_type="add", 
                      iter_factor=0, times=times, delay=delay, timeout=15)

testbench_log.close()
# # output_file = "testbench_outputs/" + sys.argv[1] + ".json"
# res = run_perfs("power/energy-pkg/", ["sleep", "1"], times=5, delay=1)
# res = get_stats(res)
# print(res)
# # write_json(res, output_file, "basic_test")
