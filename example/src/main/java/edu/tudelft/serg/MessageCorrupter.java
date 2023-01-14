package edu.tudelft.serg;

import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.JsonObject;

public abstract class MessageCorrupter {

    public abstract ReplicaPrePrepare<AdditionOperation> corruptPrePrepare(JsonObject root, int randomNumber);

    public abstract ReplicaPrepare corruptPrepare(JsonObject root, int randomNumber);

    public abstract ReplicaCommit corruptCommit(JsonObject root, int randomNumber);

    public abstract ReplicaReply<AdditionResult> corruptReply(JsonObject root, int randomNumber);

    public abstract ReplicaCheckpoint corruptCheckpoint(JsonObject root, int randomNumber);

    public abstract ReplicaViewChange corruptViewChange(JsonObject root, int randomNumber);

    public abstract ReplicaNewView corruptNewView(JsonObject root, int randomNumber);

    public abstract ReplicaRequest<AdditionOperation> corruptRequest(JsonObject root, int randomNumber);

}
