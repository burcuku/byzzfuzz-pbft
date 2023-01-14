package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.ReplicaTicket;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;

import java.util.*;

public class PropertyChecker<O, R> {
    public static PropertyChecker INSTANCE;

    // maps each processId to the Integer[] of commits, indexed by sequence numbers
    private final Map<Integer, ReplicaRequest<O>[]> commits;
    private final int numProcesses;
    private final int byzantineId;
    private static final int MAX_SEQ_NO = 50;
    private static final int NO_VIOLATION = 0;
    private static final int VALIDITY_VIOLATION = 1;
    private static final int INTEGRITY_VIOLATION = 2;
    private static final int AGREEMENT_VIOLATION = 3;
    private static String[] violationStrs = {"NO_VIOLATION", "VALIDITY", "INTEGRITY", "AGREEMENT"};

    private PropertyChecker(int numProcesses, int byzantineId) {
        this.numProcesses = numProcesses;
        this.byzantineId = byzantineId;
        commits = new HashMap<>();
        for(int i = 0; i < numProcesses; i++) {
            ReplicaRequest<O>[] c = new ReplicaRequest[MAX_SEQ_NO];
            Arrays.fill(c, null);
            commits.put(i, c);
        }
    }

    public synchronized static PropertyChecker initialize(int numProcesses, int byzantineId) {
        INSTANCE = new PropertyChecker(numProcesses, byzantineId);
        return INSTANCE;
    }

    public synchronized static PropertyChecker getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("PropertyChecker not initialized");
        }
        return INSTANCE;
    }

    // add a decision by a replica
    public void addCommit(int replicaId, ReplicaTicket<O, R> ticket) {
        int violation = addCommit(replicaId, (int) ticket.seqNumber(), ticket.request());

        /*System.out.println("LOG-Replica-Commit:" + replicaId +
            " \tviewNo: " + ticket.viewNumber() +
            " \tseqNo: " + ticket.seqNumber() +
            " \trequest: " + ticket.request());*/

        System.out.println("LOG-Replica-Commit:" + replicaId +
                " \tviewNo: " + ticket.viewNumber() +
                " \tseqNo: " + ticket.seqNumber() +
                " \trequest: " + ticket.request());

        if(violation != 0)
            System.out.println("Violation of " + violationStrs[violation] + " at Replica: " + replicaId + " at: " +
                    "LOG-Replica-Commit:" + replicaId +
                    " \tviewNo: " + ticket.viewNumber() +
                    " \tseqNo: " + ticket.seqNumber() +
                    " \trequest: " + ticket.request());

    }

    /**
     *
     * @return NO_VIOLATION for all correct properties
     *  VALIDITY_VIOLATION for validity violation
     *  INTEGRITY_VIOLATION for integrity violation
     *  AGREEMENT_VIOLATION for agreement violation
     */
    private int addCommit(int process, int seqNo, ReplicaRequest<O> value) {
        assert(seqNo < MAX_SEQ_NO);
        ReplicaRequest<O>[] ps = commits.get(process);
        ps[seqNo] = value;

        return checkProperties(process, seqNo, value);
    }

    private int checkProperties(int process, int seqNo, ReplicaRequest<O> committedValue) {
       if(process == byzantineId) return NO_VIOLATION;

       // check properties of the correct replicas
        if(!checkValidity(process, seqNo, committedValue))
           return VALIDITY_VIOLATION;
       if(!checkIntegrity(process, seqNo, committedValue))
           return INTEGRITY_VIOLATION;
       if(!checkAgreement(process, seqNo, committedValue))
           return AGREEMENT_VIOLATION;
       return NO_VIOLATION;
    }

    // Valid operations
    private Set<String> validOperations = new HashSet<>(Arrays.asList(
        "DRRequest{operation=AddOp{first=1, second=1}, timestamp=0, clientId='client-0'}",
        "DRRequest{operation=AddOp{first=2, second=2}, timestamp=1, clientId='client-0'}",
        "DRRequest{operation=AddOp{first=3, second=3}, timestamp=2, clientId='client-0'}"));

    private boolean checkValidity(int process, int seqNo, ReplicaRequest<O> value) {
        // A correct process may only decide a value that was proposed by a correct process
        // Currently hardcoded with the client operations
        String valueStr = value.toString();
        return validOperations.contains(valueStr);
    }

    private boolean checkIntegrity(int process, int seqNo, ReplicaRequest<O> value) {
        // Check integrity - a correct process does not decide/commit twice
        ReplicaRequest<O>[] ps = commits.get(process);

        return ps[seqNo] == null || ps[seqNo].equals(value);
    }

    private boolean checkAgreement(int process, int seqNo, ReplicaRequest<O> value) {
        // Check agreement - No two correct processes decide differently
        String valueStr = value.toString();
        for(int processId = 0; processId < numProcesses; processId ++) {
            if(processId == byzantineId) continue;
            // check if there exist a process that commits a different value
            ReplicaRequest<O> committed = commits.get(processId)[seqNo];
            if(committed != null && !committed.toString().equals(valueStr)) {
                //System.out.println("------- Process  " + process + " does not agree with process: " + processId + "  at seq no: " + seqNo);
                //System.out.println("      ------- Process   " + process + " has: "+ valueStr);
                //System.out.println("      ------- Process   " + processId + " has: "+ committed.toString());
                return false;
            }
        }
        return true;
    }
}
