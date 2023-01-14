package com.gmail.woodyc40.pbft.message;

import java.util.Arrays;

public class DefaultReplicaCommit implements ReplicaCommit {
    private final int viewNumber;
    private final long seqNumber;
    private final byte[] digest;
    private final int replicaId;

    public DefaultReplicaCommit(int viewNumber, long seqNumber, byte[] digest, int replicaId) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.digest = digest;
        this.replicaId = replicaId;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public long seqNumber() {
        return this.seqNumber;
    }

    @Override
    public byte[] digest() {
        return this.digest;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }

    @Override
    public String toString() {
        return "DRCommit{" +
                "viewNumber=" + viewNumber +
                ", seqNumber=" + seqNumber +
                ", digest=" + Arrays.toString(digest) +
                ", replicaId=" + replicaId +
                '}';
    }
}
