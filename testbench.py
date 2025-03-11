# Helper to run perf commands multiple times and record output
import os
import subprocess
import re
import time
import statistics
import json
import sys

cpp_comp_flags = ["-O0", "-O1", "-O2", "-O3"]
java_exec_flags = [["-Xint"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=2"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=3"],
                   ["-XX:+TieredCompilation", "-XX:TieredStopAtLevel=4"]]

testbench_log = open("testbench_log.txt", "w")


# get the joules consumed for the specified event and command, as well as the time elapsed in seconds
def run_perf(event, command):
    print(command)
    result = subprocess.run(["perf", "stat", "-a", "-e", event] + command, capture_output=True, text=True)
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
def run_perfs(event, command, times, delay):
    d = {}
    for i in range(times):
        print(i+1)
        d[i] = run_perf(event, command)
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

def write_json(d, filename: str):
    os.makedirs(filename[:filename.rfind("/")], exist_ok=True)
    json_str = json.dumps(d, indent=4)
    with open(filename, 'w') as f:
        f.write(json_str) 

def compile_cpp(test_name, times=10, delay=0, flag=""):
    cpp_comp_cmd = ["g++", f"{test_name}/{test_name}.cpp", "-o", f"{test_name}/{test_name}{flag}", flag]
    return get_stats(run_perfs("power/energy-pkg/", cpp_comp_cmd, times, delay))

def compile_java(test_name, times=10, delay=0):
    java_comp_cmd = ["javac", f"{test_name}/{test_name}.java"]
    return get_stats(run_perfs("power/energy-pkg/", java_comp_cmd, times, delay))

def run_test_cpp(test_name, flag, times, delay, size=0):
    command = [f"./{test_name}/{test_name}{flag}"]
    if size > 0:
        command.append(str(size))
    return get_stats(run_perfs("power/energy-pkg/", command, times, delay))

def run_test_java(test_name, flag_number, times, delay, size):
    command = ["java"] + java_exec_flags[flag_number] + [f"{test_name}.{test_name}"]
    if size > 0:
        command.append(str(size))
    return get_stats(run_perfs("power/energy-pkg/", command, times, delay))

def run_full_cpp(test_name, starting_size, iters, flags, times, delay):
    cpp_executions = {}
    for flag in flags:
        size = starting_size
        cpp_executions[flag] = {}
        for _ in range(iters):
            cpp_executions[flag][size] = run_test_cpp(test_name, flag, times, delay, size)
            size *= 2
    return cpp_executions

def run_full_java(test_name, starting_size, iters, flags, times, delay):
    java_executions = {}
    for flag_index in range(len(flags)):
        size = starting_size
        java_executions["flag" + str(flag_index)] = {}
        for _ in range(iters):
            java_executions["flag" + str(flag_index)][size] = run_test_java(test_name, flag_index, times, delay, size)
            size *= 2
    return java_executions

def test_command_with_arg(test_name, starting_size, iters, times, delay):

    # c++ compilation time at all compiler levels
    cpp_compilations = {}
    for flag in cpp_comp_flags:
        cpp_compilations[flag] = compile_cpp(test_name, times, delay, flag)
    
    # c++ executions
    cpp_executions = run_full_cpp(test_name, starting_size, iters, cpp_comp_flags, times, delay)

    # java compilation time
    java_compilations = compile_java(test_name, times, delay)

    # java executions
    java_executions = run_full_java(test_name, starting_size, iters, java_exec_flags, times, delay)

    graal_compilations = compile_java(test_name, times, delay)

    # save results
    base_filepath = f"testbench_outputs/{test_name}/start{starting_size}_iters{iters}_times{times}_delay{delay}_"
    write_json(cpp_compilations, base_filepath + "cpp_comp.json")
    write_json(java_compilations, base_filepath + "java_comp.json")
    write_json(cpp_executions, base_filepath + "cpp_exec.json")
    write_json(java_executions, base_filepath + "java_exec.json")



test_command_with_arg("matmul", starting_size=128, iters=4, times=10, delay=0)


testbench_log.close()
# # output_file = "testbench_outputs/" + sys.argv[1] + ".json"
# res = run_perfs("power/energy-pkg/", ["sleep", "1"], times=5, delay=1)
# res = get_stats(res)
# print(res)
# # write_json(res, output_file, "basic_test")
