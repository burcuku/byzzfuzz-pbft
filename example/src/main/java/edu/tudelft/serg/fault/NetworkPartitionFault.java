package edu.tudelft.serg.fault;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class NetworkPartitionFault {
    public final long round;
    public final List<List<Integer>> partition;

    public NetworkPartitionFault(long round, List<List<Integer>> partition) {
        this.round = round;
        this.partition = partition;
    }

    public static NetworkPartitionFault readJson(String jsonStr) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(jsonStr, JsonObject.class);

        JsonArray partitionArray = root.get("partition").getAsJsonArray();
        List<List<Integer>> partition = new ArrayList<>();

        for(JsonElement list: partitionArray) {
            List<Integer> subset = new ArrayList<>();
            for(JsonElement e: list.getAsJsonArray()) {
                subset.add(e.getAsInt());
            }
            partition.add(subset);
        }


        return new NetworkPartitionFault(root.get("round").getAsLong(), partition);
    }

    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        root.addProperty("round", round);

        JsonArray partitionArray = new JsonArray();
        for(List<Integer> list: partition) {
            JsonArray subset = new JsonArray();
            for(Integer e: list) {
                subset.add(e);
            }
            partitionArray.add(subset);
        }
        root.add("partition", partitionArray);
        return root;
    }

    public boolean inSamePartition(int p1, int p2) {
        int partition1 = getPartitionOf(p1);
        int partition2 = getPartitionOf(p2);
        assert(partition1 != -1 && partition2 != -1);

        return partition1 == partition2;
    }

    private int getPartitionOf(int p) {
        for(int i = 0; i < partition.size(); i ++) {
            if(partition.get(i).contains(p)) return i;
        }
        return -1;
    }

    private String partitionAsString() {
        String str = new String("{");
        for(List<Integer> s: partition) {
            str = str.concat(s.toString());
        }
        return str.concat("}");
    }

    @Override
    public String toString() {
        return "NetworkFault{" +
                "round=" + round +
                ", partition=" + partitionAsString() +
                '}';
    }
}
