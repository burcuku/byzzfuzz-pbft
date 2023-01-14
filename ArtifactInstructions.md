# Artifact Instructions 

This document provides the instructions to reproduce the experimental results in the paper "Randomized Testing of Byzantine Fault Tolerant Consensus Algorithms" for testing an implementation of the PBFT algorithm. 

The testing algorithm is implemented on top of the PBFT implementation under test which you can find at the GitHub repository [here](https://github.com/caojohnny/pbft-java).

 
## Running Quick Tests:


1. Install and start the redis server used by PBFT processes. 

	You can download and run the redis server using [redis github repository](https://github.com/redis/redis). We tested the system using redis version 6.2.1.

	```
	cd /path/to/redis/server/src
	./redis-server
    ```
    
2. Build the PBFT implementation instrumented with ByzzFuzz:

    ``` 
    mvn clean install
    ```

3. Run a single Byzzfuzz test using the following command:
    
    ```
    java -cp example/target/pbft-java-example-jar-with-dependencies.jar com.gmail.woodyc40.pbft.Main
    ```
    
     Running a test execution will print the messages exchanged between the processes together with the dropped/mutated messages on the console. 


4. Run a small batch of ByzzFuzz tests:
    
    The script `runTests.py` runs a batch of ByzzFuzz tests. As an example, you can run the script using the following command which runs 5 randomly generated test executions with $d=1$ round with a network fault, and $c=1$ round with a process fault using small-scope corruptions.
    
    ```
    cd scripts
    ./runTests.py 5 1 1 true
    ```

    Running the tests will create an output folder `out` that contains the output of each test execution.
    
## Step-by-Step Instructions


1. Install and start the redis server used by PBFT processes. 

	You can download and run the redis server using [redis github repository](https://github.com/redis/redis). We tested the system using redis version 6.2.1.

	```
	cd /path/to/redis/server/src
	./redis-server
    ```

2. Build the PBFT implementation instrumented with ByzzFuzz:

    ```
    mvn clean install
    ```
    
3. Clean the previously generated output files.

    ```
    rm -rf out
    ```

4. Run 200 tests for each of the test configurations using the baseline fault injecting algorithm and ByzzFuzz with $c=[0,2]$ and $d=[0,2]$ using small-scope or any-scope corruptions. 

    You can run all the tests presented in Table 2 by running the `runtests.py` script with no arguments:
    

    ```
    cd scripts
    ./runTests.py
    ```

    The execution of all tests completes in around ~9 hours. You will find the output files of the tests in the folder `out`. (Alternatively, you can run a smaller number of tests for each configuration by modifying the number of iterations in `runTests.py`.)

7. After the completion of the tests, run the test output analyzer to count the number of violations detected in the test executions.

    ```
    ./runStats.py 
    ```
 
    Running the script will process the `out` file that keeps the test outputs and it will print the number of detected termination, validity, integrity, and agreement violations in the tests for each of the test configurations. Below is an excerpt from the output of the `runStats.py` script for the tests with $d=0$ and $c=2$ with small-scope mutations:
 
    ```
    ... 
    Processing folder: ../out/tests-D0-C2-ss
    Termination violations in  ../out/tests-D0-C2-ss  :  3
    Validity violations in  ../out/tests-D0-C2-ss  :  6    
    Integrity violations in  ../out/tests-D0-C2-ss  :  0
    Agreement violations in  ../out/tests-D0-C2-ss  :  4
    All in  ../out/tests-D0-C2-ss  :  7
    ...
    ```
     
     Note that the number of detected violations might be slightly different than the numbers detected in the paper due to the concurrency nondeterminism in the timing and delivery order of exchanged messages.


    (Optional) For debugging purposes, the script additionally creates a folder `stats` that lists the test executions that violate the consensus properties. For example, the created `stats/D0-C2-ss` folder keeps the following five files: 
    - `D0-C2-ss.txt` which lists the test executions that violate consensus (any of termination (1), validity (2), integrity (3), or agreement (4))
    - `D0-C2-ss-1.txt` which lists the test executions that violate termination
    - `D0-C2-ss-2.txt` which lists the test executions that violate validity
    - `D0-C2-ss-3.txt` which lists the test executions that violate integrity 
    - `D0-C2-ss-4.txt` which lists the test executions that violate agreement
 
    The developers can analyze the corresponding log files to debug the detected violations. 
     

## (Optional) Configuring and Extending for reuse:

You can use the `test.conf` file configure the test parameters (the number of protocol rounds with network faults, the number of protocol rounds with process faults, the number of rounds among which to inject faults) and the options (e.g., the random seed to reproduce a test execution, the timeout for processing a client request, the timeout for bounded liveness detection, etc) for running a test execution.

The configuration in the `test.conf` file can be overwritten by providing arguments on the command line. 

As an example, you can run the following command after building ByzzFuzz to run a test with a given random seed, number of execution rounds, and given number of rounds with network and process faults: 
    
```
java -cp example/target/pbft-java-example-jar-with-dependencies.jar com.gmail.woodyc40.pbft.Main RANDOM_SEED=12345151 NUM_NETWORK_FAULTS=1 NUM_PROCESS_FAULTS=1 NUM_ROUNDS=8
``` 

The ByzzFuzz implementation can be reused by modifying/extending the message mutations by implementing the abstract methods in `/example/src/main/java/edu/tudelft/serg/MessageCorrupter.java`. 

The current implementations of small-scope and any-scope mutations are provided in `/example/src/main/java/edu/tudelft/serg/SmallScopeCorrupter.java` and `/example/src/main/java/edu/tudelft/serg/AnyScopeCorrupter.java`.

