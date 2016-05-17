/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dlmu.agent.client;


import com.dlmu.agent.client.receiver.SpanReceiver;
import com.dlmu.agent.common.conf.DTraceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

/**
 * A pool of Tracer objects.
 * <p>
 * There may be more than one {@link DTraceClient} running inside a single 'process'; for example,
 * unit tests may spin up a DataNode, a NameNode, and HDFS clients all running in a single JVM
 * instance, each with its own Tracer. TracerPool is where all Tracer instances register
 * on creation so Tracers can coordinate around shared resources such as {@link SpanReceiver}
 * instances. TracerPool takes care of properly cleaning up registered Tracer instances on shutdown.
 */
public class TracerPool {
    private static final Logger logger = LoggerFactory.getLogger(TracerPool.class);

    /**
     * The global pool of dTraceClient objects.
     * <p>
     * This is the pool that new tracers get put into by default.
     */
    static final TracerPool GLOBAL = new TracerPool("Global");

    /**
     * The shutdown hook which closes the Tracers in this pool when the process is
     * shutting down.
     */
    private class SpanReceiverShutdownHook extends Thread {
        SpanReceiverShutdownHook() {
            setName("SpanReceiverShutdownHook");
            setDaemon(false);
        }

        @Override
        public void run() {
            removeAndCloseAllSpanReceivers();
        }
    }

    /**
     * The name of this TracerPool.
     */
    private final String name;

    /**
     * The current span receivers which these tracers are using.
     * <p>
     * Can be read locklessly.  Must be written under the lock.
     * The array itself should never be modified.
     */
    private volatile SpanReceiver[] curReceivers;

    /**
     * The currently installed shutdown hook, or null if no hook has been
     * installed.
     */
    private SpanReceiverShutdownHook shutdownHook;

    /**
     * The current Tracers.
     */
    private final HashSet<DTraceClient> curTracers;

    /**
     * Get the global dTraceClient pool.
     *
     * @return The dTraceClient pool.
     */
    public static TracerPool getGlobalTracerPool() {
        return GLOBAL;
    }

    public TracerPool(String name) {
        this.name = name;
        this.shutdownHook = null;
        this.curTracers = new HashSet<DTraceClient>();
        this.curReceivers = new SpanReceiver[0];
    }

    /**
     * Return the name of this TracerPool.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an array of all the current span receivers.
     * <p>
     * Note that if the current span receivers change, those changes will not be
     * reflected in this array.  In other words, this array may be stale.
     *
     * @return An array of the current span receivers.
     */
    public SpanReceiver[] getReceivers() {
        return curReceivers;
    }

    /**
     * Add a new span receiver.
     *
     * @param receiver The new receiver to add.
     * @return True if the new receiver was added; false if it
     * already was there.
     */
    public synchronized boolean addReceiver(SpanReceiver receiver) {
        SpanReceiver[] receivers = curReceivers;
        for (int i = 0; i < receivers.length; i++) {
            if (receivers[i] == receiver) {
                logger.trace(toString() + ": can't add receiver " + receiver.toString() +
                        " since it is already in this pool.");
                return false;
            }
        }
        SpanReceiver[] newReceivers =
                Arrays.copyOf(receivers, receivers.length + 1);
        newReceivers[receivers.length] = receiver;
        registerShutdownHookIfNeeded();
        curReceivers = newReceivers;
        logger.trace(toString() + ": added receiver " + receiver.toString());
        return true;
    }

    /**
     * Register the shutdown hook if needed.
     */
    private synchronized void registerShutdownHookIfNeeded() {
        if (shutdownHook != null) {
            return;
        }
        shutdownHook = new SpanReceiverShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        logger.trace(toString() + ": registered shutdown hook.");
    }

    /**
     * Remove a span receiver.
     *
     * @param receiver The receiver to remove.
     * @return True if the receiver was removed; false if it
     * did not exist in this pool.
     */
    public synchronized boolean removeReceiver(SpanReceiver receiver) {
        SpanReceiver[] receivers = curReceivers;
        for (int i = 0; i < receivers.length; i++) {
            if (receivers[i] == receiver) {
                SpanReceiver[] newReceivers = new SpanReceiver[receivers.length - 1];
                System.arraycopy(receivers, 0, newReceivers, 0, i);
                System.arraycopy(receivers, i + 1, newReceivers, i,
                        receivers.length - i - 1);
                curReceivers = newReceivers;
                logger.trace(toString() + ": removed receiver " + receiver.toString());
                return true;
            }
        }
        logger.trace(toString() + ": can't remove receiver " + receiver.toString() +
                " since it's not currently in this pool.");
        return false;
    }

    /**
     * Remove and close a span receiver.
     *
     * @param receiver The receiver to remove.
     * @return True if the receiver was removed; false if it
     * did not exist in this pool.
     */
    public boolean removeAndCloseReceiver(SpanReceiver receiver) {
        if (!removeReceiver(receiver)) {
            return false;
        }
        try {
            logger.trace(toString() + ": closing receiver " + receiver.toString());
            receiver.close();
        } catch (Throwable t) {
            logger.error(toString() + ": error closing " + receiver.toString(), t);
        }
        return true;
    }

    /**
     * Remove and close all of the span receivers.
     */
    private synchronized void removeAndCloseAllSpanReceivers() {
        SpanReceiver[] receivers = curReceivers;
        curReceivers = new SpanReceiver[0];
        for (SpanReceiver receiver : receivers) {
            try {
                logger.trace(toString() + ": closing receiver " + receiver.toString());
                receiver.close();
            } catch (Throwable t) {
                logger.error(toString() + ": error closing " + receiver.toString(), t);
            }
        }
    }

    /**
     * Given a SpanReceiver class name, return the existing instance of that span
     * receiver, if possible; otherwise, invoke the callable to create a new
     * instance.
     *
     * @param className   The span receiver class name.
     * @param conf        The HTrace configuration.
     * @param classLoader The class loader to use.
     * @return The SpanReceiver.
     */
    public synchronized SpanReceiver loadReceiverType(String className,
                                                      DTraceConfiguration conf, ClassLoader classLoader) {
        String receiverClass = className.contains(".") ?
                className : SpanReceiver.Builder.DEFAULT_PACKAGE + "." + className;
        SpanReceiver[] receivers = curReceivers;
        for (SpanReceiver receiver : receivers) {
            if (receiver.getClass().getName().equals(receiverClass)) {
                logger.trace(toString() + ": returning a reference to receiver " +
                        receiver.toString());
                return receiver;
            }
        }
        logger.trace(toString() + ": creating a new SpanReceiver of type " +
                className);
        SpanReceiver receiver = new SpanReceiver.Builder(conf).
                className(className).
                classLoader(classLoader).
                build();
        addReceiver(receiver);
        return receiver;
    }

    /**
     * Returns an array of all the current Tracers.
     * <p>
     * Note that if the current Tracers change, those changes will not be
     * reflected in this array.  In other words, this array may be stale.
     *
     * @return The current array of tracers.
     */
    public synchronized DTraceClient[] getTracers() {
        return curTracers.toArray(new DTraceClient[curTracers.size()]);
    }

    /**
     * Add a new Tracer.
     */
    synchronized void addTracer(DTraceClient dTraceClient) {
        if (curTracers.add(dTraceClient)) {
            logger.trace(toString() + ": adding dTraceClient " + dTraceClient.toString());
        }
    }

    /**
     * Remove a Tracer.
     * <p>
     * If the Tracer removed was the last one, we will close all the SpanReceiver
     * objects that we're managing.
     */
    synchronized void removeTracer(DTraceClient dTraceClient) {
        if (curTracers.remove(dTraceClient)) {
            logger.trace(toString() + ": removing dTraceClient " + dTraceClient.toString());
            if (curTracers.size() == 0) {
                removeAndCloseAllSpanReceivers();
            }
        }
    }

    @Override
    public String toString() {
        return "TracerPool(" + name + ")";
    }
}
