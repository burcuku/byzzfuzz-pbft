package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaEncoder;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.tudelft.serg.fault.NetworkPartitionFault;
import edu.tudelft.serg.fault.ProcessFault;
import edu.tudelft.serg.message.Messages;
import edu.tudelft.serg.util.MathUtils;

import java.util.*;

/**
 * Introduces structure-aware faults into the execution of the protocol rounds
 * Process faults can cause small-scope or any-scope message corruptions based on the configuration
 */
public class FaultInjector {
    // Encoder used to encode the corrupted message into String - make it ready to be transmitted
    private final AdditionReplicaEncoder encoder = new AdditionReplicaEncoder();

    // Supports SmallScope or AnyScope corruption of messages
    private final MessageCorrupter corrupter;

    private final FaultSetting faultSetting;
    private final int byzantineReplicaId;

    private final RoundInfo initialRoundValue = new RoundInfo(0, 0 , Messages.Verb.NONE);
    private final RoundInfo[] infoOfReplicas;
    private final int[] replicaRounds;

    private class RoundInfo {
        int viewNo;
        int seqNo;
        Messages.Verb verb;

        RoundInfo(int viewNo, int seqNo, Messages.Verb verb) {
            this.viewNo = viewNo;
            this.seqNo = seqNo;
            this.verb = verb;
        }
    }

    // create FaultInjector for injecting the given faults in an execution
    public FaultInjector(FaultSetting faultSetting) {
        this.faultSetting = faultSetting;
        System.out.println("Test generation using the given fault setting.");

        if(faultSetting.smallScope)
            corrupter = new SmallScopeCorrupter();
        else
            corrupter = new AnyScopeCorrupter();

        byzantineReplicaId = faultSetting.byzantineReplicaId;

        infoOfReplicas = new RoundInfo[TestConf.getInstance().REPLICA_COUNT];
        Arrays.fill(infoOfReplicas, initialRoundValue);

        replicaRounds = new int[TestConf.INSTANCE.REPLICA_COUNT];
        Arrays.fill(replicaRounds, 0);
    }

    // create FaultInjector with random faults from TestConf configuration parameters
    public FaultInjector(TestConf conf) {
        // Random number generation for randomized fault injection
        Random random = new Random(conf.RANDOM_SEED);

        if(conf.SMALL_SCOPE_CORRUPTION)
            corrupter = new SmallScopeCorrupter();
        else
            corrupter = new AnyScopeCorrupter();

        if(conf.BYZANTINE != -1) byzantineReplicaId = conf.BYZANTINE;
        else byzantineReplicaId =  random.nextInt(10000) % conf.REPLICA_COUNT; // conf.REPLICA_COUNT as max value mostly returns the same value

        List<Integer> allProcs = new ArrayList<>();
        for(int i = 0; i < conf.REPLICA_COUNT; i++)
           allProcs.add(i);

        List<Integer> allRounds = new ArrayList<>();
        for(int i = 1; i <= conf.NUM_ROUNDS; i++) // no round 0
            allRounds.add(i);

        // sample network faults:
        Set<NetworkPartitionFault> networkFaults = new HashSet<>();
        List<Integer> rounds = MathUtils.Subset.randomSubsetOfSize(random, allRounds, conf.NUM_NETWORK_FAULTS);
        //for each round, sample a network partition
        for(int round: rounds) {
            List<List<Integer>> partition = MathUtils.SetPartition.randomPartition(random, allProcs);
            while(partition.size() == 1) partition = MathUtils.SetPartition.randomPartition(random, allProcs);
            networkFaults.add(new NetworkPartitionFault(round, partition));
        }

        // sample process faults:
        Set<ProcessFault> processFaults = new HashSet<>();
        allProcs.remove(byzantineReplicaId);
        allProcs.add(conf.REPLICA_COUNT); // add the client process (Byzantine process can possibly corrupt its reply)
        rounds = MathUtils.Subset.randomSubsetOfSize(random, allRounds, conf.NUM_PROCESS_FAULTS);
        for(int round: rounds) {
            // sample a subset of processes uniformly at random
            List<Integer> procs = MathUtils.Subset.randomSubset(random, allProcs);

            if(TestConf.INSTANCE.CONSISTENT_CORRUPTIONS) {
                // apply the same corruption to all selected processes in that round
                int c = random.nextInt();
                processFaults.add(new ProcessFault(round, procs, c));
            } else {
                // separate corruption type for each process - Currently not used!
                for(int p: procs) {
                    int c = random.nextInt();
                    List<Integer> ps = new ArrayList<>();
                    ps.add(p);
                    processFaults.add(new ProcessFault(round, ps, c));
                }
            }
        }

        System.out.println("Random test generation using the configuration in test.conf");
        System.out.println("Test inputs: d = " + conf.NUM_NETWORK_FAULTS + ", c = " + conf.NUM_PROCESS_FAULTS + ", r = " + conf.NUM_ROUNDS);
        System.out.println("Network and process faults are generated by using random seed: " + conf.RANDOM_SEED);

        if(!TestConf.INSTANCE.CONSISTENT_CORRUPTIONS) System.out.println("Possibly inconsistent corruptions to different processes." );
        faultSetting = new FaultSetting(byzantineReplicaId, networkFaults, processFaults, conf.SMALL_SCOPE_CORRUPTION);

        infoOfReplicas = new RoundInfo[TestConf.getInstance().REPLICA_COUNT];
        Arrays.fill(infoOfReplicas, initialRoundValue);

        replicaRounds = new int[TestConf.INSTANCE.REPLICA_COUNT];
        Arrays.fill(replicaRounds, 0);
    }

    public FaultSetting getFaultSetting() {
        return faultSetting;
    }

    public int getReplicaRound(int replicaId) {
        return replicaRounds[replicaId];
    }

    public void updateRoundInfo(int replicaId, String data) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        int viewNo = Messages.getViewNo(root);
        int seqNo = Messages.getSeqNo(root);
        Messages.Verb verb = Messages.getVerb(root);

        RoundInfo ri = infoOfReplicas[replicaId];

        if(viewNo > ri.viewNo) {
            // timeout/new-view triggered, possibly faulty leader, move to the next round
            infoOfReplicas[replicaId] = new RoundInfo(viewNo, seqNo, verb);
        } else if(viewNo == ri.viewNo && seqNo > ri.seqNo) {
            // seq No incremented (sth committed), increase the round until "REPLY" (possibly out-of-order PP/P/C messages)
            infoOfReplicas[replicaId] = new RoundInfo(viewNo, seqNo, verb);
        } else if(viewNo == ri.viewNo && seqNo == ri.seqNo && verb.getValue() > ri.verb.getValue()) {
            infoOfReplicas[replicaId] = new RoundInfo(viewNo, seqNo, verb);
        }

        // else, an older-round message, do not update the round of the replica
    }

    public void updateRoundInfoAfterReply(int replicaId) {
        infoOfReplicas[replicaId] = new RoundInfo(infoOfReplicas[replicaId].viewNo, infoOfReplicas[replicaId].seqNo, Messages.Verb.REPLY);
    }

    // returns =< 0 if  replicaId is at a higher round than data or at the same round
    // returns > 0 if  replicaId is at a smaller round than data
    int inSmallerRound(int replicaId, String data) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        int viewNo = Messages.getViewNo(root);
        int seqNo = Messages.getSeqNo(root);
        Messages.Verb verb = Messages.getVerb(root);

        RoundInfo replicaRoundInfo = infoOfReplicas[replicaId];

        if(viewNo > replicaRoundInfo.viewNo) {
            return 1; // timeout/new-view triggered, possibly faulty leader, move to the next round
        } else if(viewNo == replicaRoundInfo.viewNo && seqNo > replicaRoundInfo.seqNo) {
            // seq No incremented (sth committed), increase the round until "REPLY" (possibly out-of-order PP/P/C messages)
            infoOfReplicas[replicaId] = new RoundInfo(viewNo, seqNo, verb);
            // e.g. replica receives PREPARE before PRE_PREPARE while it is in REPLY of previous message!
            // PREPARE = 2, REPLY = 4, NUM_ROUNDS = 4 and hence TO_INCREASE = PREPARE + (NUMROUNDS - REPLY)
            return verb.getValue() + Messages.NUM_ROUNDS_TO_PROCESS_REQUEST - replicaRoundInfo.verb.getValue();
        } else if(viewNo == replicaRoundInfo.viewNo && seqNo == replicaRoundInfo.seqNo && verb.getValue() > replicaRoundInfo.verb.getValue()) {
            infoOfReplicas[replicaId] = new RoundInfo(viewNo, seqNo, verb);
            return verb.getValue() - replicaRoundInfo.verb.getValue();
        }

        return 0;
    }

    public synchronized void updateRoundOnMulticast(int senderReplicaId,  String data) {
        Messages.Verb verb = Messages.getVerb(data);
        if(verb.getValue() == 0) return; // CHECKPOINT message

        replicaRounds[senderReplicaId] += inSmallerRound(senderReplicaId, data);
        updateRoundInfo(senderReplicaId, data);  // update the view-id, seq-no, verb of the sender
    }

    public synchronized void updateRoundOnReplyToClient(int replicaId, String reply) {
        replicaRounds[replicaId] ++; // REPLY
        updateRoundInfoAfterReply(replicaId);
    }

    public synchronized void updateRoundOnMessageReceive(int receiverReplicaId, String data) {
        Messages.Verb verb = Messages.getVerb(data);
        if(verb.getValue() == 0) return; // CHECKPOINT message

        replicaRounds[receiverReplicaId] += inSmallerRound(receiverReplicaId, data);
        updateRoundInfo(receiverReplicaId, data); // update the view-id, seq-no, verb of the receiver
    }

    public boolean toDrop(JsonObject root, int receiver) {
        int sender = Messages.getSender(root);
        long round = getReplicaRound(sender);

        for(NetworkPartitionFault p: faultSetting.networkFaults) {
            // drop if sender and the receiver are not in the same partition
            if (p.round == round && !p.inSamePartition(sender, receiver)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns corruption type of the message (-1 for no corruption)
     * @param root JsonObject of the message
     * @param receiver receiver of the message
     * @return corruption type, -1 for no corruption
     */
    public int toCorrupt(JsonObject root, int receiver) {
        int sender = Messages.getSender(root);
        if( sender != faultSetting.byzantineReplicaId) return -1;

        //long round = Messages.getRoundNumber(root);
        long round = getReplicaRound(sender);

        for(ProcessFault c: faultSetting.processFaults) {
            if(c.round == round && c.receivers.contains(receiver)) {
                return c.corruptionType;
            }
        }
        return -1;
    }

    public String corruptMessage(JsonObject root, int corType) {
        String type = root.get("type").getAsString();
        String corrupted = "";

        switch (type) {
            case "VIEW-CHANGE":
                ReplicaViewChange viewChange = corrupter.corruptViewChange(root, corType);
                if(viewChange == null) return ""; // process omission fault - message omitted
                corrupted =  encoder.encodeViewChange(viewChange);
                break;
            case "NEW-VIEW":
                ReplicaNewView newView = corrupter.corruptNewView(root, corType);
                if(newView == null) return ""; // process omission fault - message omitted
                corrupted =  encoder.encodeNewView(newView);
                break;
            case "PRE-PREPARE":
                ReplicaPrePrepare<AdditionOperation> prePrepare = corrupter.corruptPrePrepare(root, corType);
                if(prePrepare == null) return ""; // process omission fault - message omitted
                corrupted = encoder.encodePrePrepare(prePrepare);
                break;
            case "PREPARE":
                ReplicaPrepare prepare = corrupter.corruptPrepare(root, corType);
                if(prepare == null) return ""; // process omission fault - message omitted
                corrupted =  encoder.encodePrepare(prepare);
                break;
            case "COMMIT":
                ReplicaCommit commit = corrupter.corruptCommit(root, corType);
                if(commit == null) return ""; // process omission fault - message omitted
                corrupted =  encoder.encodeCommit(commit);
                break;
            case "CHECKPOINT":
                ReplicaCheckpoint checkpoint = corrupter.corruptCheckpoint(root, corType);
                corrupted =  encoder.encodeCheckpoint(checkpoint);
            case "REPLY":
                ReplicaReply<AdditionResult> reply = corrupter.corruptReply(root, corType);
                if(reply == null) return ""; // process omission fault - message omitted
                corrupted =  encoder.encodeReply(reply);
        }

        //System.out.println("- Corrupting message ");
        //System.out.println("     From: " + root);
        //System.out.println("     To: " + corrupted);
        return corrupted;
    }
}
