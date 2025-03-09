# Helper to run perf commands multiple times and record output
import subprocess
import re
import time
import statistics
import json


# get the joules consumed for the specified event and command, as well as the time elapsed in seconds
def run_perf(event, command):
    result = subprocess.run(["perf", "stat", "-a", "-e", event] + command, capture_output=True, text=True)
    result = result.stderr # not sure why it prints to stderr and not stdout
    print(result)

    [joules, seconds] = [float(val) for val in re.findall(r'\-?[0-9.]+', result)]

    return {"joules": joules, 
            "seconds": seconds}

# get the joules and duration for the specified event and command. run multiple times, with a delay between
# trials to let the cpu slow down again before the next test.
def run_perfs(event, command, times, delay):
    d = {}
    for i in range(times):
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

def write_json(d, filename):
    json_str = json.dumps(d)
    with open(filename, 'w') as f:
        f.write(json_str)


res = run_perfs("power/energy-pkg/", ["sleep", "1"], times=5, delay=1)
res = get_stats(res)
print(res)
write_json(res, "temp.txt")
