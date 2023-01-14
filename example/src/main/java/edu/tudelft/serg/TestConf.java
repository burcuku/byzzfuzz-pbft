package edu.tudelft.serg;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import edu.tudelft.serg.util.FileUtils;

public class TestConf {

    public static TestConf INSTANCE;

    public boolean TEST_MODE;
    public final boolean LOG_FAULTS;
    public final boolean LOG_MSGS;

    // parameters of the sampling algorithm
    public final int TOLERANCE;
    public final long TIMEOUT_MS;
    public final int REPLICA_COUNT;
    public final int NUM_CLIENT_REQUESTS;

    // parameters for the fault injector
    public final String FAULT_INJECTOR;
    public final int TEST_ID;
    public final int RANDOM_SEED;

    public final int NUM_ROUNDS;
    public final int NUM_NETWORK_FAULTS;
    public final int NUM_PROCESS_FAULTS;
    public final boolean SMALL_SCOPE_CORRUPTION;

    // If given as -1 in the configuration file, the Byzantine node is randomly selected among the nodes
    public final int BYZANTINE;
    public final boolean CONSISTENT_CORRUPTIONS;

    public final String OUT_FOLDER;
    public final String OUT_FILE;

    public final long TEST_TIMEOUT_MS;
    public final long SYNC_TIMEOUT_MS;

    public final int MAX_NUM_BROADCASTS;
    private static int numBroadcasts = 0;

    // Parameters for the baseline fault injectors:
    public final double RANDOM_DROP_RATE;
    public final double RANDOM_CORRUPTION_RATE;

    private TestConf(String configFile, String[] args) {

        Properties prop = loadProperties(configFile);
        Map<String, String> overrideArgs = new HashMap<>();

        if(args != null && args.length != 0) {
            overrideArgs = Arrays.stream(args)
                    .filter(s -> s.contains("="))
                    .map(s -> Arrays.asList(s.split("=")))
                    .collect(Collectors.toMap(kv -> kv.get(0), kv -> kv.get(1)));
        }

        TOLERANCE =  Integer.parseInt(overrideArgs.getOrDefault("TOLERANCE", prop.getProperty("TOLERANCE")));
        TIMEOUT_MS =  Integer.parseInt(overrideArgs.getOrDefault("TIMEOUT_MS", prop.getProperty("TIMEOUT_MS")));
        NUM_CLIENT_REQUESTS =  Integer.parseInt(overrideArgs.getOrDefault("NUM_CLIENT_REQUESTS", prop.getProperty("NUM_CLIENT_REQUESTS")));
        REPLICA_COUNT = 3 * TOLERANCE + 1;

        TEST_MODE = Boolean.parseBoolean(overrideArgs.getOrDefault("TEST_MODE", prop.getProperty("TEST_MODE")));
        LOG_FAULTS = Boolean.parseBoolean(overrideArgs.getOrDefault("LOG_FAULTS", prop.getProperty("LOG_FAULTS")));
        LOG_MSGS = Boolean.parseBoolean(overrideArgs.getOrDefault("LOG_MSGS", prop.getProperty("LOG_MSGS")));

        FAULT_INJECTOR = overrideArgs.getOrDefault("FAULT_INJECTOR", prop.getProperty("FAULT_INJECTOR"));
        TEST_ID =  Integer.parseInt(overrideArgs.getOrDefault("TEST_ID", prop.getProperty("TEST_ID")));

        RANDOM_SEED =  Integer.parseInt(overrideArgs.getOrDefault("RANDOM_SEED", prop.getProperty("RANDOM_SEED")));
        BYZANTINE =  Integer.parseInt(overrideArgs.getOrDefault("BYZANTINE", prop.getProperty("BYZANTINE")));

        NUM_ROUNDS =  Integer.parseInt(overrideArgs.getOrDefault("NUM_ROUNDS", prop.getProperty("NUM_ROUNDS")));
        NUM_NETWORK_FAULTS =  Integer.parseInt(overrideArgs.getOrDefault("NUM_NETWORK_FAULTS", prop.getProperty("NUM_NETWORK_FAULTS")));
        NUM_PROCESS_FAULTS =  Integer.parseInt(overrideArgs.getOrDefault("NUM_PROCESS_FAULTS", prop.getProperty("NUM_PROCESS_FAULTS")));
        CONSISTENT_CORRUPTIONS = Boolean.parseBoolean(overrideArgs.getOrDefault("CONSISTENT_CORRUPTIONS", prop.getProperty("CONSISTENT_CORRUPTIONS")));
        SMALL_SCOPE_CORRUPTION = Boolean.parseBoolean(overrideArgs.getOrDefault("SMALL_SCOPE_CORRUPTION", prop.getProperty("SMALL_SCOPE_CORRUPTION")));

        OUT_FOLDER = overrideArgs.getOrDefault("OUT_FOLDER", prop.getProperty("OUT_FOLDER"));
        OUT_FILE = overrideArgs.getOrDefault("OUT_FILE", prop.getProperty("OUT_FILE")).concat("-" + TEST_ID + ".txt");

        TEST_TIMEOUT_MS =  Integer.parseInt(overrideArgs.getOrDefault("TEST_TIMEOUT_MS", prop.getProperty("TEST_TIMEOUT_MS")));
        MAX_NUM_BROADCASTS =  Integer.parseInt(overrideArgs.getOrDefault("MAX_NUM_BROADCASTS", prop.getProperty("MAX_NUM_BROADCASTS")));
        SYNC_TIMEOUT_MS =  Integer.parseInt(overrideArgs.getOrDefault("SYNC_TIMEOUT_MS", prop.getProperty("SYNC_TIMEOUT_MS")));

        // Parameters for the baseline fault injectors:
        RANDOM_DROP_RATE = Double.parseDouble(overrideArgs.getOrDefault("RANDOM_DROP_RATE", prop.getProperty("RANDOM_DROP_RATE")));
        RANDOM_CORRUPTION_RATE = Double.parseDouble(overrideArgs.getOrDefault("RANDOM_CORRUPTION_RATE", prop.getProperty("RANDOM_CORRUPTION_RATE")));

        FileUtils.createOutputFolder(OUT_FOLDER);
    }

    public int getNumBroadcasts() {
        return numBroadcasts;
    }

    public int incNumBroadcasts() {
        return ++numBroadcasts;
    }

    public void setTestMode(boolean inTestMode) {
        TEST_MODE = inTestMode;
    }

    public boolean getTestMode() {
        return TEST_MODE;
    }

    private static Properties loadProperties(String configFile) {
        Properties prop = new Properties();
        try (FileInputStream ip = new FileInputStream(configFile)) {
            prop.load(ip);
        } catch (IOException e) {
            System.out.println("Can't load properties file:" + configFile);
        }
        return prop;
    }

    public synchronized static TestConf initialize(String configFile, String[] args) {
        INSTANCE = new TestConf(configFile, args);
        return INSTANCE;
    }

    public synchronized static TestConf getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Configuration not initialized");
        }
        return INSTANCE;
    }

}
