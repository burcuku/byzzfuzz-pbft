#!/usr/bin/python3

import os
import sys
import time
from subprocess import call


def runByzzFuzzTests(numTests, numNetworkFaults, numProcessFaults, smallScopeCorruption):
    resultFile = "out/"  # + suffixStr
    if (not os.path.isdir(resultFile)):
        os.mkdir(resultFile)

    suffixStr = "-D" + str(numNetworkFaults) + "-C" + str(numProcessFaults)

    if (numProcessFaults > 0) and (smallScopeCorruption.lower() == "true"):
        suffixStr = suffixStr + "-ss"
    elif (numProcessFaults > 0) and (smallScopeCorruption.lower() == "false"):
        suffixStr = suffixStr + "-as"
    
    resultFolder = resultFile + "tests" + suffixStr

    if (not os.path.isdir(resultFolder)):
        os.mkdir(resultFolder)

    start_all = time.time()

    for i in range(1, int(numTests) + 1):
        print("Running test %s" % i)

        seed = (i - 1) + 1234567890
        fout = open(resultFolder + "/out" + str(i) + ".txt", "w")
        program_args = "TEST_ID={0} RANDOM_SEED={1} NUM_NETWORK_FAULTS={2} NUM_PROCESS_FAULTS={3} OUT_FOLDER={4} SMALL_SCOPE_CORRUPTION={5}".format(
            str(i), str(seed), str(numNetworkFaults), str(numProcessFaults), resultFolder, str(smallScopeCorruption))
        call("java -jar example/target/pbft-java-example-jar-with-dependencies.jar " + program_args, shell=True,
             stdout=fout, stderr=fout)

    endAll = time.time()
    elapsedSec = endAll - start_all
    mins = "#Minutes: {:.4f} \n".format(elapsedSec / 60)
    print(mins)

    f = open("out/tests-out.txt", 'a')
    f.write("Tests with " + suffixStr + "\n" + mins + "\n\n")
    f.close()

def runBaselineTests(numTests):
    print("Running tests using the baseline fault injector.")
    resultFolder = "out/baseline"

    if (not os.path.isdir(resultFolder)):
        os.mkdir(resultFolder)

    start_all = time.time()

    for i in range(1, int(numTests) + 1):
        seed = (i - 1) + 1234567890
        fout = open(resultFolder + "/out" + str(i) + ".txt", "w")
        program_args = "FAULT_INJECTOR={0}".format("Baseline") + " RANDOM_SEED={0}".format(str(seed))
        call("java -jar example/target/pbft-java-example-jar-with-dependencies.jar " + program_args, shell=True,
             stdout=fout, stderr=fout)

    endAll = time.time()
    elapsedSec = endAll - start_all
    mins = "#Minutes: {:.4f} \n".format(elapsedSec / 60)
    print(mins)


if __name__ == '__main__':
    os.chdir("..")

    # no arguments for running all tests
    if len(sys.argv) == 1:
        numIterations = 200

        print("Running Byzzfuzz tests:\n")
        ## run for different # of network and process faults
        for numNetworkFaults in [0, 1, 2]:
            for numProcessFaults in [0, 1, 2]:
                if (numNetworkFaults == 0) and (numProcessFaults == 0):
                    continue
                if(numProcessFaults == 0):    
                    print("Running tests for d=%s c=0" % (numNetworkFaults))
                    runByzzFuzzTests(numIterations, numNetworkFaults, numProcessFaults, "true")
                else: 
                    for smallScopeCorruption in ["true", "false"]:
                        print("Running tests for d=%s c=%s small-scope=%s:" % (numNetworkFaults, numProcessFaults, smallScopeCorruption))
                        runByzzFuzzTests(numIterations, numNetworkFaults, numProcessFaults, smallScopeCorruption)

        print("Running baseline tests:\n")
        runBaselineTests(numTests=numIterations)
    
    # run ByzzFuzz tests with the given arguments
    elif len(sys.argv) == 5:
        runByzzFuzzTests(int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3]), sys.argv[4])

    else:
        print("Please enter the parameters for: numTests: Integer, numNetworkFaults: Integer, numProcessFaults: Integer, smallScopeCorruption: Boolean")
        print("Example usage: ./runtests.py 5 1 1 true")
