package edu.tudelft.serg.fault;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class NetworkLinkFault {
    public final long round;
    public final int sender;
    public final int receiver;

    public NetworkLinkFault(long r, int s, int recv) {
        round = r;
        sender = s;
        receiver = recv;
    }

    public static NetworkLinkFault readJson(String jsonStr) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(jsonStr, JsonObject.class);
        return new NetworkLinkFault(root.get("round").getAsLong(), root.get("sender").getAsInt(), root.get("receiver").getAsInt());
    }

    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        root.addProperty("round", round);
        root.addProperty("sender", sender);
        root.addProperty("receiver", receiver);
        return root;
    }

    @Override
    public String toString() {
        return "NetworkFault{" +
                "round=" + round +
                ", sender=" + sender +
                ", recv=" + receiver +
                '}';
    }
}
