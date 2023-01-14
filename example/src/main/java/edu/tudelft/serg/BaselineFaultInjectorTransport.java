package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.ReplicaTransport;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaTransport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.tudelft.serg.message.Messages;

import java.util.*;
import java.util.stream.IntStream;

/**
 * BaselineFaultInjectorTransport - randomly injects arbitrary network and process faults in the transport layer
 * Crashes the system with parse errors!
 */
public class BaselineFaultInjectorTransport implements ReplicaTransport<String> {
    private final AdditionReplicaTransport replicaTransport; // Intercepts communication

    private final TestConf testConf;
    private final Random random = new Random();
    private final int byzantineReplicaId;
    private final double dropRate;
    private final double corruptionRate;

    public BaselineFaultInjectorTransport(AdditionReplicaTransport replicaTransport, TestConf conf) {
        this.replicaTransport = replicaTransport;
        testConf = conf; // To check when test mode is off - due to timeout

        // select random Byzantine node
        byzantineReplicaId = random.nextInt(conf.REPLICA_COUNT);
        dropRate = conf.RANDOM_DROP_RATE;
        corruptionRate = conf.RANDOM_CORRUPTION_RATE;

        PropertyChecker.initialize(conf.REPLICA_COUNT, byzantineReplicaId);

        // Print fault setting:
        System.out.println("Naive random fault injector started with Byzantine process: " + byzantineReplicaId
                + " message drop rate: " + dropRate + " message corruption rate: " + corruptionRate + "\n");
    }

    @Override
    public synchronized void sendMessage(int replicaId, String data) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String type = root.get("type").getAsString();
        String mutatedData = data;

        // If the message is not REQUEST sent by a client, do not mutate it
        // Client request type ex: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
        if ("REQUEST".equals(type) || "CHECKPOINT".equals(type)){
            replicaTransport.sendMessage(replicaId, data);
            return;
        }

        // Deliver or Drop or Corrupt randomly
        double drop = random.nextDouble();
        if(drop < dropRate && testConf.TEST_MODE) {
            System.out.println("\tDropping: " + root);
            return;
        }

        double corrupt = random.nextDouble();
        if(corrupt < corruptionRate) {
            System.out.println("\tCorrupting: " + data);
            mutatedData = flipRandomCharacter(data);
            System.out.println("\t\t To: " + mutatedData);
        }

        replicaTransport.sendMessage(replicaId, mutatedData);
    }

    // Random mutator ( in a similar fashion to AFL Fuzzer string mutator, but does not use bitwise ops)
    // Mutates a single char (depending on the char, can modify the view number, seq number, proposal, request, etc)
    private String flipRandomCharacter(String s) {
        if(s.isEmpty()) return s;

        StringBuilder corrupted = new StringBuilder(s);
        int pos = random.nextInt(s.length());
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ=\"";
        char randomChar = chars.charAt(random.nextInt(chars.length()));
        corrupted.setCharAt(pos, randomChar);
        return corrupted.toString();
    }

    @Override
    public synchronized void multicast(String data, int... ignoredReplicas) {
        Set<Integer> ignored = new HashSet<>(ignoredReplicas.length);
        for (int id : ignoredReplicas) {
            ignored.add(id);
        }

        assert(ignored.size() == 1); // We assume only sender is ignored in multicast and increase its round

        for (int i = 0; i < countKnownReplicas(); i++) {
            if (!ignored.contains(i)) {
                this.sendMessage(i, data);
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void sendReply(String clientId, String reply) {  // Reply sent to the client
        //replicaTransport.sendReply(clientId, reply);

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(reply, JsonObject.class);
        String type = root.get("type").getAsString();
        String mutatedData = reply;

        int sender = Messages.getSender(root);

        switch (random.nextInt(3)) {
            case 0: // drop
                System.out.println("Dropping: " + root);
                return;
            case 1:
                if(sender == byzantineReplicaId) {
                    System.out.println("Corrupting: " + reply);
                    mutatedData = flipRandomCharacter(reply);
                    System.out.println("\t\t To: " + mutatedData); // generate random string?
                }
                break;
            case 2:
                break;
        }

        replicaTransport.sendReply(clientId, mutatedData);
    }

    @Override
    public int countKnownReplicas() {
        return replicaTransport.countKnownReplicas();
    }

    @Override
    public IntStream knownReplicaIds() {
        return replicaTransport.knownReplicaIds();
    }

}
