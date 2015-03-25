/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.clients;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;


/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is
 * in nano seconds.
 * 
 * @author nitsanw
 *
 */
public abstract class LatencyMonitoringClient extends MessageCountingClient {
    // CHECKSTYLE:OFF
    private static final int WARMUP_MESSAGES = 20000;
    private final Histogram histogram;
    protected ServerConnection connection;
    private Object connectionLock = new Object();
    
    public static final long MAX_LATENCY_VALUE = TimeUnit.SECONDS.toNanos(1);
    
    public LatencyMonitoringClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, Histogram commonHistogram, String... initialTopicsP) {
        super(experimentCountersP, reconnectP, initialTopicsP);
        
        if(commonHistogram != null)
        	histogram = commonHistogram;
        else
        	histogram = new Histogram(MAX_LATENCY_VALUE, 3);
    }
    // CHECKSTYLE:ON
    @Override
    public final void onServerConnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = serverConnection;
        }
    }

    /**
     * Measures and records latency.
     * See also ControlClientTLExperiment HISTOGRAM_SCALING_RATIO
     */
    @SuppressWarnings("deprecation")
    @Override
    public final void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
    	long sent = -1;
        long arrived = getArrivedTimestamp();
    	long rtt=-1;
        if (experimentCounters.getMessageCounter() > WARMUP_MESSAGES
                && topicMessage.isDelta()) {
            try {
                sent = getSentTimestamp(topicMessage);
                rtt = arrived - sent;
                // avoid java.lang.ArrayIndexOutOfBoundsException: value outside of histogram covered range.
                // clearly if we observe MAX_LATENCY_VALUE then it is already a bad situation
                if(rtt >= MAX_LATENCY_VALUE){
                	rtt = MAX_LATENCY_VALUE;
                }
                getHistogram().recordValue(rtt);
            } catch (Exception e) {
                Logs.severe("Failed to capture rtt: "+rtt+" sent= "+sent+" arrived= "+arrived, e);
                return;
            }
        }
    }

    /**
     * @return ...
     */
    protected abstract long getArrivedTimestamp();

    /**
     * @param topicMessage ...
     * @return ...
     */
    protected abstract long getSentTimestamp(TopicMessage topicMessage);

    @Override
    public final void onServerDisconnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = null;
        }
    }

    /**
     * will disconnect if connected.
     */
    public final void disconnect() {
        synchronized (connectionLock) {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * @return my histogram.
     */
    public final Histogram getHistogram() {
        return histogram;
    }
}
