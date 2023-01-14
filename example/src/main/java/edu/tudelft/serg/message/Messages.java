package edu.tudelft.serg.message;

import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.tudelft.serg.TestConf;

public class Messages {

  public static final int NUM_ROUNDS_TO_PROCESS_REQUEST = 4; // num rounds to complete a client request

  public enum Verb {
    NONE(0), CHECKPOINT(0), PRE_PREPARE(1), PREPARE(2), COMMIT(3), REPLY(4), VIEW_CHANGE(5), NEW_VIEW(6), ;

    private final int value;

    Verb(int value)
    {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static int getSender(JsonObject root) {
    String type = root.get("type").getAsString();

    // NEW-VIEW and PRE-PREPARE are only sent by the primary node
    if ("NEW-VIEW".equals(type)) {
      int newViewNumber = root.get("new-view-number").getAsInt();
      return newViewNumber % TestConf.getInstance().REPLICA_COUNT;  // viewNumber % knownReplicas;
    } else if ("PRE-PREPARE".equals(type)) {
      int newViewNumber = root.get("view-number").getAsInt();
      return newViewNumber % TestConf.getInstance().REPLICA_COUNT;
    }

    return root.get("replica-id").getAsInt();
  }

  public static int getViewNo(JsonObject root) {
    String type = root.get("type").getAsString();
    int viewNumber = -1;
    // NEW-VIEW and PRE-PREPARE are only sent by the primary node
    if ("NEW-VIEW".equals(type) || "VIEW-CHANGE".equals(type) ) {
      viewNumber = root.get("new-view-number").getAsInt();
    } else if ("PRE-PREPARE".equals(type) || "PREPARE".equals(type) || "COMMIT".equals(type)) {
      viewNumber = root.get("view-number").getAsInt();
    }

    assert(viewNumber != -1);
    return viewNumber;
  }

  public static int getSeqNo(JsonObject root) {
    String type = root.get("type").getAsString();

    int seqNumber = -1;
    if ("PRE-PREPARE".equals(type) || "PREPARE".equals(type) || "COMMIT".equals(type)) {
      seqNumber = root.get("seq-number").getAsInt();
    }

    return seqNumber;
  }

  public static Verb getVerb(String data) {
    Gson gson = new Gson();
    JsonObject root = gson.fromJson(data, JsonObject.class);

    return getVerb(root);
  }

  public static Verb getVerb(JsonObject root) {
    String type = root.get("type").getAsString();
    switch (type) {
      case "VIEW-CHANGE":
        return Verb.VIEW_CHANGE;
      case "NEW-VIEW":
        return Verb.NEW_VIEW;
      case "PRE-PREPARE":
        return Verb.PRE_PREPARE;
      case "PREPARE":
        return Verb.PREPARE;
      case "COMMIT":
        return Verb.COMMIT;
      case "CHECKPOINT":
        return Verb.CHECKPOINT;
      case "REPLY":
        return Verb.REPLY;
    }
    return Verb.NONE;
  }

  public static ReplicaRequest<AdditionOperation> readRequest(JsonObject root) {
    //System.out.println("Not corrupted request" + root);
    JsonElement operation = root.get("operation");
    AdditionOperation additionOperation = null;
    if (!operation.isJsonNull()) {
      JsonObject operationObject = operation.getAsJsonObject();
      int first = operationObject.get("first").getAsInt();
      int second = operationObject.get("second").getAsInt();
      additionOperation = new AdditionOperation(first, second);
    }
    long timestamp = root.get("timestamp").getAsLong();
    String clientId = root.get("client").getAsString();

    return new DefaultReplicaRequest<>(additionOperation, timestamp, clientId);
  }

}
