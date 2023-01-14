package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.client.AdditionClient;
import com.gmail.woodyc40.pbft.client.AdditionClientEncoder;
import com.gmail.woodyc40.pbft.client.AdditionClientTransport;
import com.gmail.woodyc40.pbft.replica.AdditionReplica;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaEncoder;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaTransport;
import com.gmail.woodyc40.pbft.replica.NoopDigester;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import edu.tudelft.serg.*;
import edu.tudelft.serg.util.TestUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final List<AdditionReplica> replicas = new ArrayList<>();
    private static TestConf conf;

    public static void main(String[] args) throws InterruptedException {
        conf = TestConf.initialize("test.conf", args);

        try (JedisPool pool = new JedisPool()) {
            setupReplicas(pool);

            // Wait for PubSub listeners to setup
            Thread.sleep(1000);

            Set<ClientTicket<AdditionOperation, AdditionResult>> tickets = new HashSet<>();

            Client<AdditionOperation, AdditionResult, String> client = setupClient(pool);

            Thread timerThread = TestUtils.setTimer("Timer for Request Timeout", conf.TEST_TIMEOUT_MS, conf.SYNC_TIMEOUT_MS);

            // Surround calls in try-catch to stop at exceptions instead of waiting for the test timeout
            // (Throws format/io/etc exceptions for incorrectly formatted messages corrupted by baseline fault injectors)
            try{
                for (int i = 1; i <= conf.NUM_CLIENT_REQUESTS; i++) {
                    AdditionOperation operation = new AdditionOperation(i, i);
                    ClientTicket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);
                    tickets.add(ticket);

                    ticket.result().thenAccept(result -> {
                        synchronized (System.out) {
                            //System.out.println("==========================");
                            System.out.println("=========== COMPLETED ===============");
                            System.out.println(operation.first() + " + " + operation.second() + " = " + result.result());
                            System.out.println("==========================");
                            //System.out.println("==========================");
                        }
                    }).exceptionally(t -> {
                        timerThread.interrupt();
                        throw new RuntimeException(t);
                    });
                    Thread.sleep(1000);
                }

                waitTimeouts(client, tickets);
                Thread.sleep(1000); // Wait for some time for all replicas to commit and check properties

                timerThread.interrupt();
                System.out.println("Task completed.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Terminating the test execution after exception");
                timerThread.interrupt();
                System.exit(-1);
            }
        }
    }

    private static long determineSleepTime(ClientTicket<?, ?> ticket) {
        long start = ticket.dispatchTime();
        long elapsed = System.currentTimeMillis() - start;
        return Math.max(0, conf.TIMEOUT_MS - elapsed);
    }

    private static <O, R, T> void waitTimeouts(Client<O, R, T> client, Collection<ClientTicket<O, R>> tickets) {
        Set<ClientTicket<O, R>> completed = new HashSet<>();

        while (true) {
            long minTime = conf.TIMEOUT_MS;
            for (ClientTicket<O, R> ticket : tickets) {
                long sleepTime = determineSleepTime(ticket);
                if (sleepTime > 0 && sleepTime < minTime) {
                    minTime = sleepTime;
                }

                if (ticket.result().isDone()) {
                    completed.add(ticket);
                    continue;
                }

                client.checkTimeout(ticket);
            }

            if (completed.size() == tickets.size()) {
                break;
            }

            try {
                Thread.sleep(minTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static <O, R, T> void waitTimeouts(Replica<O, R, T> replica) {
        while (true) {
            long minTime = conf.TIMEOUT_MS;
            for (ReplicaRequestKey key : replica.activeTimers()) {
                long waitTime = replica.checkTimeout(key);
                if (waitTime > 0 && waitTime < minTime) {
                    minTime = waitTime;
                }
            }

            try {
                Thread.sleep(minTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void setupReplicas(JedisPool pool) {
        CountDownLatch readyLatch = new CountDownLatch(conf.REPLICA_COUNT - 1);

        AdditionReplicaEncoder replicaEncoder = new AdditionReplicaEncoder();
        NoopDigester digester = new NoopDigester();
        AdditionReplicaTransport replicaTransport = new AdditionReplicaTransport(pool, conf.REPLICA_COUNT);

        ReplicaTransport transportToUse;

        if(conf.getTestMode()) {
            // The fault injectors are implemented on top of the replica transport
            if(conf.FAULT_INJECTOR.equalsIgnoreCase("TESTER")) // Our fault injection algorithm
                transportToUse = new TesterTransport(replicaTransport, conf);
            else if(conf.FAULT_INJECTOR.equalsIgnoreCase("BASELINE")) // // Baseline, naive random fault injector
                transportToUse = new BaselineFaultInjectorTransport(replicaTransport, conf);
            else {
                transportToUse = replicaTransport;
                System.out.println("Unrecognized type for the fault injector.");
                System.exit(-1);
            }
        } else {
            transportToUse = replicaTransport;
        }

        for (int i = 0; i < conf.REPLICA_COUNT; i++) {
            DefaultReplicaMessageLog log = new DefaultReplicaMessageLog(100, 100, 200);
            AdditionReplica replica = new AdditionReplica(
                    i,
                    conf.TOLERANCE,
                    conf.TIMEOUT_MS,
                    log,
                    replicaEncoder,
                    digester,
                    transportToUse,
                    false);
                    //i == 0);

            replicas.add(replica);

            Thread listenerThread = new Thread(() -> {
                try (Jedis jedis = pool.getResource()) {
                    String channel = "replica-" + replica.replicaId();
                    JedisPubSub listener = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            replica.handleIncomingMessage(message);
                        }
                    };

                    readyLatch.countDown();
                    jedis.subscribe(listener, channel);
                } catch(redis.clients.jedis.exceptions.JedisConnectionException e) {
                    // Connection fault if test completes before processing all inflight messages
                    // System.out.println(e.toString());
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            Thread timeoutThread = new Thread(() -> waitTimeouts(replica));
            timeoutThread.setDaemon(true);
            timeoutThread.start();

        }

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private static Client<AdditionOperation, AdditionResult, String> setupClient(JedisPool pool) {
        AdditionClientEncoder clientEncoder = new AdditionClientEncoder();
        AdditionClientTransport clientTransport = new AdditionClientTransport(pool, conf.REPLICA_COUNT);

        AdditionClient client = new AdditionClient(
                "client-0",
                conf.TOLERANCE,
                conf.TIMEOUT_MS,
                clientEncoder,
                clientTransport);

        CountDownLatch readyLatch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                String channel = client.clientId();
                JedisPubSub listener = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        client.handleIncomingMessage(message);
                    }
                };

                readyLatch.countDown();
                jedis.subscribe(listener, channel);
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }

        return client;
    }
}
