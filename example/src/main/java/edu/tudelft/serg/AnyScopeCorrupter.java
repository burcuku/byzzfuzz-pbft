package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.replica.AdditionReplica;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Any-scope structure-aware message corruptions
 */
public class AnyScopeCorrupter extends MessageCorrupter {

  public ReplicaPrePrepare<AdditionOperation> corruptPrePrepare(JsonObject root, int randomNumber) {
    int viewNumber = root.get("view-number").getAsInt();
    long seqNumber = root.get("seq-number").getAsLong();
    byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
    ReplicaRequest<AdditionOperation> request = readRequest(root);

    switch (randomNumber % 4) {
      case 0: // corrupt request
        request = corruptRequest(root, randomNumber); break;
      case 1: // corrupt view #
        viewNumber = randomNumber; break;
      case 2: // corrupt seq #
        seqNumber = randomNumber; break;
      case 3: // omit message
        return null;
    }

    return new DefaultReplicaPrePrepare<>(viewNumber, seqNumber, digest, request);
  }

  public ReplicaPrepare corruptPrepare(JsonObject root, int randomNumber) {
    int viewNumber = root.get("view-number").getAsInt();
    long seqNumber = root.get("seq-number").getAsLong();
    byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
    int replicaId = root.get("replica-id").getAsInt();

    switch (randomNumber % 3) {
      case 0: // corrupt view #
        viewNumber = randomNumber; break;
      case 1: // corrupt seq #
        seqNumber = randomNumber; break;
      case 2: // omit message
        return null;
    }

    return new DefaultReplicaPrepare(viewNumber, seqNumber, digest, replicaId);
  }

  public ReplicaCommit corruptCommit(JsonObject root, int randomNumber) {
    int viewNumber = root.get("view-number").getAsInt();
    long seqNumber = root.get("seq-number").getAsLong();
    byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
    int replicaId = root.get("replica-id").getAsInt();

    switch (randomNumber % 3) {
      case 0: // corrupt view #
        viewNumber = randomNumber; break;
      case 1: // corrupt seq #
        seqNumber = randomNumber; break;
      case 2: // omit message
        return null;
    }

    return new DefaultReplicaCommit(viewNumber, seqNumber, digest, replicaId);
  }

  public ReplicaReply<AdditionResult> corruptReply(JsonObject root, int randomNumber) {
    int timestamp = root.get("timestamp").getAsInt();
    int result = root.get("result").getAsInt();

    switch (randomNumber % 2) {
      case 0: // corrupt result
        result = randomNumber; break;
      case 1: // omit
        return null;
    }

    //int viewNumber, long timestamp, String clientId, int replicaId, R result
    return new DefaultReplicaReply<AdditionResult>(root.get("view-number").getAsInt(),
            timestamp,
            root.get("client-id").getAsString(),
            root.get("replica-id").getAsInt(),
            new AdditionResult(result));
  }

  // not in use, currently we do not corrupt checkpoint messages
  public ReplicaCheckpoint corruptCheckpoint(JsonObject root, int randomNumber) {
    long lastSeqNumber = root.get("last-seq-number").getAsLong();
    byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
    int replicaId = root.get("replica-id").getAsInt();

    // return new DefaultReplicaCheckpoint(randomNumber, digest, replicaId);
    return new DefaultReplicaCheckpoint(lastSeqNumber, digest, replicaId);
  }

  public ReplicaViewChange corruptViewChange(JsonObject root, int randomNumber) {
    int newViewNumber = root.get("new-view-number").getAsInt();
    long lastSeqNumber = root.get("last-seq-number").getAsLong();

    Collection<ReplicaCheckpoint> checkpointProofs = new ArrayList<>();
    JsonArray checkpointProofsArray = root.get("checkpoint-proofs").getAsJsonArray();
    for (JsonElement checkpoint : checkpointProofsArray) {
      checkpointProofs.add(AdditionReplica.readCheckpoint(checkpoint.getAsJsonObject()));
    }

    Map<Long, Collection<ReplicaPhaseMessage>> preparedProofs = new HashMap<>();
    JsonArray preparedProofsArray = root.get("prepared-proofs").getAsJsonArray();
    for (JsonElement element : preparedProofsArray) {
      JsonObject proof = element.getAsJsonObject();
      long seqNumber = proof.get("seq-number").getAsLong();

      Collection<ReplicaPhaseMessage> messages = new ArrayList<>();
      JsonArray messagesArray = proof.get("messages").getAsJsonArray();
      for (JsonElement message : messagesArray) {
        String type = message.getAsJsonObject().get("type").getAsString();
        if ("PRE-PREPARE".equals(type)) {
          messages.add(AdditionReplica.readPrePrepare(message.getAsJsonObject()));
        } else if ("PREPARE".equals(type)) {
          messages.add(AdditionReplica.readPrepare(message.getAsJsonObject()));
        }
      }

      preparedProofs.put(seqNumber, messages);
    }
    int replicaId = root.get("replica-id").getAsInt();


    switch (randomNumber % 3) {
      case 0: // corrupt view #
        newViewNumber = randomNumber; break;
      case 1: // corrupt seq #
        lastSeqNumber = randomNumber; break;
      case 2: // omit message
        return null;
    }

    return new DefaultReplicaViewChange(
            newViewNumber,
            lastSeqNumber,
            checkpointProofs,
            preparedProofs,
            replicaId);
  }

  public ReplicaNewView corruptNewView(JsonObject root, int randomNumber) {
    int newViewNumber = root.get("new-view-number").getAsInt(); // currently not used

    Collection<ReplicaViewChange> viewChangeProofs = new ArrayList<>();
    JsonArray viewChangesArray = root.get("view-change-proofs").getAsJsonArray();
    for (JsonElement element : viewChangesArray) { // view changes from others
      viewChangeProofs.add(AdditionReplica.readViewChange(element.getAsJsonObject()));
    }

    Collection<ReplicaPrePrepare<?>> preparedProofs = new ArrayList<>();
    JsonArray preparedArray = root.get("prepared-proofs").getAsJsonArray(); // prepared proofs from others
    for (JsonElement element : preparedArray) {
      preparedProofs.add(AdditionReplica.readPrePrepare(element.getAsJsonObject()));
    }

    switch (randomNumber % 2) {
      case 0: // corrupt view #
        newViewNumber = randomNumber; break;
      case 1: // omit message
        return null;
    }

    return new DefaultReplicaNewView(
            newViewNumber,
            viewChangeProofs,
            preparedProofs);
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

  public ReplicaRequest<AdditionOperation> corruptRequest(JsonObject root, int randomNumber) {
    //System.out.println("Not corrupted request" + root);
    JsonElement operation = root.get("operation");
    AdditionOperation additionOperation = null;
    if (!operation.isJsonNull()) {
      JsonObject operationObject = operation.getAsJsonObject();
      int first = operationObject.get("first").getAsInt();
      int second = operationObject.get("second").getAsInt();

      switch (randomNumber % 2) {
        case 0: first = randomNumber; break;
        case 1: second = randomNumber; break;
      }

      additionOperation = new AdditionOperation(first, second);
    }
    long timestamp = root.get("timestamp").getAsLong();
    String clientId = root.get("client").getAsString();

    return new DefaultReplicaRequest<>(additionOperation, timestamp, clientId);
  }
}
