package edu.tudelft.serg.fault;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class ProcessFault {
    public final long round;
    public final List<Integer> receivers;
    public final int corruptionType;

    public ProcessFault(long r, List<Integer> recv, int t) {
        round = r;
        receivers = recv;
        corruptionType = t;
    }

    public static ProcessFault readJson(String jsonStr) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(jsonStr, JsonObject.class);
        JsonArray array = root.get("receivers").getAsJsonArray();
        List<Integer> receivers = new ArrayList<>();
        for(JsonElement e: array)
            receivers.add(e.getAsInt());
        return new ProcessFault(root.get("round").getAsLong(),receivers, root.get("corruptionType").getAsInt());
    }

    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        root.addProperty("round", round);
        JsonArray recvs = new JsonArray();
        for(int r: receivers)
            recvs.add(r);

        root.add("receivers", recvs);
        root.addProperty("corruptionType", corruptionType);
        return root;
    }

    @Override
    public String toString() {
        return "Corruption{" +
                "round=" + round +
                ", recv=" + Arrays.toString(receivers.toArray()) +
                ", type=" + corruptionType +
                '}';
    }
}
