# Helper to run perf commands multiple times and record output
import subprocess
import re

# get the joules consumed for the specified event and command, as well as the time elapsed in seconds
def run_perf(event, command):
    result = subprocess.run(["perf", "stat", "-a", "-e", event] + command, capture_output=True, text=True)
    result = result.stderr # not sure why it prints to stderr and not stdout
    [joules, seconds] = [float(val) for val in re.findall(r'\-?[0-9.]+', result)]
    
    print(joules, type(joules))
    print(seconds, type(seconds))
    print(result)

    return {"joules": joules, 
            "seconds": seconds}



run_perf("power/energy-pkg/", ["sleep", "1"])