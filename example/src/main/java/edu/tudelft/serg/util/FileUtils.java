package edu.tudelft.serg.util;

import edu.tudelft.serg.TestConf;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is currently not in use - prints to console, results collected into files by test execution scripts
 */
public class FileUtils {

    private final static Object mutLock = new Object(); // for locking
    private final static Object dropLock = new Object(); // for locking

    private final static ConcurrentLinkedQueue<String> mutations = new ConcurrentLinkedQueue<>();
    private final static ConcurrentLinkedQueue<String> drops = new ConcurrentLinkedQueue<>();
    private static final int MAX_BUFFER_SIZE = 20;

    public static void createOutputFolder(String outFolderName) {
        File directory = new File(outFolderName);
        if (!directory.exists()){
            directory.mkdir();
        }
    }

    public static void writeToFile(String fileName, String content, boolean append) {
        try (FileWriter fw = new FileWriter(fileName, append); PrintWriter pw = new PrintWriter(fw)) {
            //File f = new File(fileName);
            //f.createNewFile(); // if file already exists will do nothing
            pw.println(content);
            pw.flush();
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveDroppedMessage(int receiver, String content) {
        synchronized (dropLock) {
            drops.add("TO: " + receiver + " " + content+ "\n");

            if(drops.size() == MAX_BUFFER_SIZE) {
                String toLog = "";
                for(String str: drops) {
                    toLog = toLog.concat(str);
                }
                writeToFile(TestConf.getInstance().OUT_FOLDER + File.separatorChar + TestConf.getInstance().OUT_FILE, toLog, true);
                drops.clear();
            }
        }
    }

    public static void saveCorruptedMessage(int receiver, String orig, String mutated) {
        synchronized (mutLock) {
            mutations.add("TO: " + receiver + "\n" + orig + "\n" + mutated + "\n");

            if(mutations.size() == MAX_BUFFER_SIZE) {
                String toLog = "";
                for(String str: mutations) {
                    toLog = toLog.concat(str);
                }
                writeToFile(TestConf.getInstance().OUT_FOLDER + File.separatorChar + TestConf.getInstance().OUT_FILE, toLog, true);
                mutations.clear();
            }
        }
    }

    public static void flushLogBuffers() {
        String toLog = "";
        for(String str: mutations) {
            toLog = toLog.concat(str);
        }
        if(!toLog.equals(""))
            writeToFile(TestConf.getInstance().OUT_FOLDER + File.separatorChar + TestConf.getInstance().OUT_FILE, toLog, true);

        toLog = "";
        for(String str: drops) {
            toLog = toLog.concat(str);
        }
        if(!toLog.equals(""))
            writeToFile(TestConf.getInstance().OUT_FOLDER + File.separatorChar + TestConf.getInstance().OUT_FILE, toLog, true);
    }

    public static void cleanFile(String fileName) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer.print("");
        writer.close();
    }

}
