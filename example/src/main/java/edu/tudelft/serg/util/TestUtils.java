package edu.tudelft.serg.util;

import edu.tudelft.serg.TestConf;
import edu.tudelft.serg.TesterTransport;

public class TestUtils {

    public static Thread setTimer(String timerInfo, long durationMillis1, long durationMillis2) {
        Thread timerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(durationMillis1);
                    System.out.println("Reached test duration: " + durationMillis1 + " ms");
                    System.out.println("TEST MODE OFF - Delivering all messages... ");
                    TestConf.getInstance().setTestMode(false);

                    // Run on non-faulty network
                    Thread.sleep(2000 + durationMillis2);

                    System.out.println("\n" + timerInfo + " - Test duration + sync execution timeout. " + "\nTimer terminates the execution.");
                    System.exit(-1); // shut down cluster, etc

                } catch (InterruptedException e) {
                    // System.out.println("Task completed.");
                    //FileUtils.logToFile("Timer thread interrupted - main task finished.");
                    //e.printStackTrace();
                }
            }
        });
        timerThread.start();
        return timerThread;
    }
}
