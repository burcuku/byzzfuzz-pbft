package edu.tudelft.serg;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.tudelft.serg.fault.NetworkPartitionFault;
import edu.tudelft.serg.fault.ProcessFault;

import java.util.HashSet;
import java.util.Set;

public class FaultSetting {
    public final boolean smallScope;
    public final int byzantineReplicaId;

    //public final Set<NetworkLinkFault> networkFaults;
    public final Set<NetworkPartitionFault> networkFaults;
    public final Set<ProcessFault> processFaults;

    public FaultSetting(int byzantineReplicaId, Set<NetworkPartitionFault> networkFaults, Set<ProcessFault> processFaults, boolean smallScope) {
        this.byzantineReplicaId = byzantineReplicaId;
        this.networkFaults = networkFaults;
        this.processFaults = processFaults;
        this.smallScope = smallScope;
    }

    public int getByzantineReplicaId() {
        return byzantineReplicaId;
    }

    public static FaultSetting readJson(String jsonStr) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(jsonStr, JsonObject.class);
        JsonArray array1 = root.get("networkFaults").getAsJsonArray();
        Set<NetworkPartitionFault> networkFaults = new HashSet<>();
        for(JsonElement e: array1)
            networkFaults.add(NetworkPartitionFault.readJson(e.toString()));
        JsonArray array2 = root.get("msgCorruptions").getAsJsonArray();
        Set<ProcessFault> processFaults = new HashSet<>();
        for(JsonElement e: array2)
            processFaults.add(ProcessFault.readJson(e.toString()));
        return new FaultSetting(root.get("byzantineReplicaId").getAsInt(), networkFaults, processFaults, root.get("smallScope").getAsBoolean());
    }

    public String toJSON() {
        JsonObject root = new JsonObject();
        root.addProperty("byzantineReplicaId", byzantineReplicaId);

        JsonArray networkFaultList = new JsonArray();
        for(NetworkPartitionFault nf: networkFaults)
            networkFaultList.add(nf.toJSON());
        root.add("networkFaults", networkFaultList);

        JsonArray corruptionList = new JsonArray();
        for(ProcessFault mc: processFaults)
            corruptionList.add(mc.toJSON());
        root.add("msgCorruptions", corruptionList);

        root.addProperty("smallScope", smallScope);

        return root.toString();
    }
}
