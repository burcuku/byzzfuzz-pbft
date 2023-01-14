package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.ReplicaTransport;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaTransport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.tudelft.serg.message.Messages;

import java.util.*;
import java.util.stream.IntStream;

/**
 * TesterServer that injects network and process faults in the transport layer
 * TODO: Rename TesterTransport with the tool name
 */
public class TesterTransport implements ReplicaTransport<String> {
    private final AdditionReplicaTransport replicaTransport; // Intercepts communication
    private final FaultInjector faultInjector; // Injects network and process faults communication

    /**
     * Some fault settings
     */
    private static final String noFaults = "{\"byzantineReplicaId\":0,\"partition\":[],\"msgCorruptions\":[], \"smallScope\":true}";

    // The fault setting that detected known liveness violation bug presented in [Berger, Reiser, Bessani. 2021]:
    private static final String detectsKnownLivenessViolation = "{\"byzantineReplicaId\":3,\"networkFaults\":[{\"round\":5,\"partition\":[[2],[0,1,3]]},{\"round\":8,\"partition\":[[0],[1],[2],[3]]}],\"msgCorruptions\":[{\"round\":8,\"receivers\":[2,1,4],\"corruptionType\":-637998034},{\"round\":5,\"receivers\":[0,4,1,2],\"corruptionType\":1107758960}],\"smallScope\":true}\n";

    private static final String detectsKnownLivenessViolationSimplified = "{\"byzantineReplicaId\":3,\"networkFaults\":[{\"round\":5,\"partition\":[[2],[0,1,3]]},{\"round\":8,\"partition\":[[0],[1],[2],[3]]}],\"msgCorruptions\":[{\"round\":8,\"receivers\":[2,1,4],\"corruptionType\":-637998034},{\"round\":5,\"receivers\":[0,4,1,2],\"corruptionType\":1107758960}],\"smallScope\":true}\n";

    // An example fault configuration detects an agreement violation
    private static final String detectsAgreementViolation = "{\"byzantineReplicaId\":0,\"networkFaults\":[],\"msgCorruptions\":[{\"round\":1,\"receivers\":[3],\"corruptionType\":6}], \"smallScope\":true}";

    public TesterTransport(AdditionReplicaTransport replicaTransport, TestConf conf) {
        this.replicaTransport = replicaTransport;

        // Inject faults to test the system with a randomly generated execution using the TestConf configuration:
        faultInjector = new FaultInjector(conf);

        // Inject faults reproduce the detected bugs with the given configurations:
        // (Uncomment the next two lines)
        // FaultSetting faultSetting = FaultSetting.readJson(detectsAgreementViolation);
        // faultInjector = new FaultInjector(faultSetting);

        PropertyChecker.initialize(conf.REPLICA_COUNT, faultInjector.getFaultSetting().getByzantineReplicaId());

        // Print fault setting:
        System.out.println(faultInjector.getFaultSetting().toJSON() + "\n");
    }

    @Override
    public synchronized void sendMessage(int replicaId, String data) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String type = root.get("type").getAsString();

        // Do not mutate request or checkpoint messages
        if ("REQUEST".equals(type) || "CHECKPOINT".equals(type)){
            replicaTransport.sendMessage(replicaId, data);
            return;
        }

        int sender = Messages.getSender(root);

        if(TestConf.getInstance().LOG_MSGS) {
                System.out.println("Sent: " + sender + " -> " + replicaId + " " + data + " ##" + faultInjector.getReplicaRound(sender));
        }

        // check if message will be dropped
        if(TestConf.INSTANCE.TEST_MODE && faultInjector.toDrop(root, replicaId)) {
            if(TestConf.getInstance().LOG_FAULTS) {
                System.out.println("     - Dropped: " + Messages.getSender(root) + " -> " + replicaId + data + " ##" + faultInjector.getReplicaRound(sender));
            }
            return;
        }

        faultInjector.updateRoundOnMessageReceive(replicaId, data); // if not dropped, update receiver's round

        // TODO Revise for efficiency - check if message will be corrupted - take only the round number, sender, receiver

        int corType = faultInjector.toCorrupt(root, replicaId);
        String mutatedData = data;
        if(corType != -1) {
            mutatedData = faultInjector.corruptMessage(root, corType);
            if(mutatedData.isEmpty()) { // process omission fault - message omitted
                if(TestConf.getInstance().LOG_FAULTS) {
                    System.out.println("     - Omitted: " + Messages.getSender(root) + " -> " + replicaId + data + " #" + faultInjector.getReplicaRound(sender));
                    return;
                }
            }
            if(TestConf.getInstance().LOG_FAULTS) {
                System.out.println("     - Mutated: " + Messages.getSender(root) + " -> " + replicaId + mutatedData + " #" + faultInjector.getReplicaRound(sender));
            }
            //FileUtils.saveCorruptedMessage(replicaId, data, mutatedData);
        }

        replicaTransport.sendMessage(replicaId, mutatedData);
    }

    @Override
    public synchronized void multicast(String data, int... ignoredReplicas) {
        Set<Integer> ignored = new HashSet<>(ignoredReplicas.length);
        for (int id : ignoredReplicas) {
            ignored.add(id);
        }

        assert(ignored.size() == 1); // We assume only sender is ignored in multicast and increase its round

        int sender = (int)ignored.toArray()[0];
        faultInjector.updateRoundOnMulticast(sender, data);

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
        // replicaTransport.sendReply(clientId, reply);

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(reply, JsonObject.class);
        String type = root.get("type").getAsString();

        int sender = Messages.getSender(root);
        faultInjector.updateRoundOnReplyToClient(sender, reply);

        if(TestConf.getInstance().LOG_MSGS) {
            System.out.println("Sent reply: " + Messages.getSender(root) + " -> Client " + reply + " ##" + faultInjector.getReplicaRound(sender));
        }

        // TODO Revise for efficiency - check if message will be corrupted/omitted - take only the round number, sender, receiver
        int corType = faultInjector.toCorrupt(root, TestConf.INSTANCE.REPLICA_COUNT); // client-id = TestConf.INSTANCE.REPLICA_COUNT (id's start with 0)
        String mutatedData = reply;
        if(corType != -1) {
            mutatedData = faultInjector.corruptMessage(root, corType);
            if(mutatedData.isEmpty()) {
                if(TestConf.getInstance().LOG_FAULTS) {
                    System.out.println("     - Omitted: " + Messages.getSender(root) + " -> " + sender + reply + " #" + faultInjector.getReplicaRound(sender));
                    return;
                }
            }
            if(TestConf.getInstance().LOG_FAULTS)
                System.out.println("     - Mutated: " + Messages.getSender(root) + " -> client " + mutatedData);
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
