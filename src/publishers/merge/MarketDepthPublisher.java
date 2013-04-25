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
 */package publishers.merge;

import publishers.BroadcastConfiguration;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.TimeoutException;
import com.pushtechnology.diffusion.api.config.ConfigManager;
import com.pushtechnology.diffusion.api.config.ConflationPolicyConfig;
import com.pushtechnology.diffusion.api.config.ConflationPolicyConfig.Mode;
import com.pushtechnology.diffusion.api.data.TopicData;
import com.pushtechnology.diffusion.api.data.TopicDataType;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.publisher.Client;
import com.pushtechnology.diffusion.api.publisher.Publisher;
import com.pushtechnology.diffusion.api.topic.Topic;
import com.pushtechnology.diffusion.api.topic.TopicClient;
import com.pushtechnology.diffusion.api.topic.TopicTreeNode;
import com.pushtechnology.diffusion.data.TopicDataImpl;

public class MarketDepthPublisher extends Publisher {

    protected static final String ZERO = "0";

    private boolean conflation;

    private String conflationMode;

    //

    // Initialize a new Simulator

    //

    public MarketDepthPublisher() throws APIException {

    }

    @Override
    protected void initialLoad() throws APIException

    {

        TopicData data = new TopicDataImpl() {
            @Override
            public TopicDataType getType() {
                return TopicDataType.CUSTOM;
            }

            @Override
            public TopicMessage getLoadMessage(TopicClient client)
                    throws TimeoutException, APIException {
                return getLoadMessage();
            }

            @Override
            public TopicMessage getLoadMessage() throws TimeoutException,
                    APIException {
                final TopicMessage loadMessage = getTopic().createLoadMessage(
                        20);
                loadMessage.put("ROOT");
                return loadMessage;

            }

            @Override
            protected void attachedToTopic(String topicName,
                    TopicTreeNode parent) throws APIException {
            }
        };

        // Create root topic

        Topic rootTopic = addTopic(getProperty("topic"), data);

        rootTopic.setAutoSubscribe(true);

        conflationMode = getProperty("conflationMode", "NONE");

        // message size is set per experiment

        int messageSize = getProperty("messageSize", 100);

        // the interval drives both publications and ramping changes

        long intervalPauseNanos =

        (long) (1000000000L * getProperty("intervalPauseSeconds", 0.1));

        // message publications, all topics are updated

        int initialMessages = getProperty("initialMessages", 100);

        long messageIncrementIntervalInPauses =

        getProperty("messageIncrementIntervalInPauses", 10L);

        int messageIncrement = getProperty("messageIncrement", 100);

        // topics

        int initialTopicNum = getProperty("initialTopicNum", 100);

        long topicIncrementIntervalInPauses =

        getProperty("topicIncrementIntervalInPauses", 10);

        int topicIncrement = getProperty("topicIncrement", 10);

        BroadcastConfiguration config =

        new BroadcastConfiguration(messageSize, intervalPauseNanos,

        initialMessages,

        messageIncrementIntervalInPauses, messageIncrement,

        initialTopicNum, topicIncrementIntervalInPauses,

        topicIncrement);

        setupDefaultPolicy(ConflationPolicyConfig.Mode.REPLACE);

        ConflationPolicyConfig policy = null;

        if (!conflationMode.equals("NONE")) {

            conflation = true;

            // Setting up conflation by topic, one message for topic

            if (conflationMode.equals("REPLACE")) {

                policy = createReplacePolicy();

            }

            else if (conflationMode.equals("MERGE")) {

                policy = createMergePolicy();

            }

            else {

                conflation = false;

            }

        }

        for (int i = 0; i < config.getInitialTopicNum(); i++) {

            // Add in the topic

            OrderBookTopicData tdata = new OrderBookTopicData(conflationMode);

            final Topic topic =

            rootTopic.addTopic(String.valueOf(i), tdata);

            if (conflation) {

                ConfigManager.getConfig().getConflation()

                .setTopicPolicy(topic.getName(), "Depth");

            }

            // Start the simulator

            (new OrderSimulator(tdata.om,
                    Math.round(Math.random() * 1000) + 500))

            .setOrdersPerSecond(10.0);

        }

    }

    private ConflationPolicyConfig createReplacePolicy() throws APIException {

        return ConfigManager.getConfig().getConflation()

        .addPolicy("Depth", ConflationPolicyConfig.Mode.REPLACE);

    }

    protected static ConflationPolicyConfig createMergePolicy()

    throws APIException {

        return ConfigManager.getConfig().getConflation().addPolicy("Depth",

        ConflationPolicyConfig.Mode.REPLACE, null, new OrderBookDeltaMerger());

    }

    protected static void setupDefaultPolicy(Mode mode) throws APIException {

        ConflationPolicyConfig policy =

        ConfigManager.getConfig().getConflation().addPolicy("Default", mode);

        ConfigManager.getConfig().getConflation()

        .setDefaultPolicy(policy.getName());

    }

    @Override
    protected void subscription(Client client, Topic topic, boolean loaded)

    throws APIException {

        if (conflation != client.isConflating()) {

            client.setConflation(conflation);

        }

    }

    //

    // Populate the depth deltas from the order book

    //

    private double getProperty(String prop, double defaultVal) {

        try {

            return Double.valueOf(getProperty(prop));

        }

        catch (Exception e) {

        }

        return defaultVal;

    }

    private String getProperty(String prop, String defaultVal) {

        if (getProperty(prop) != null) {

            return getProperty(prop);

        }

        return defaultVal;

    }

    private int getProperty(String prop, int defaultVal) {

        try {

            return getIntegerProperty(prop);

        }

        catch (Exception e) {

        }

        return defaultVal;

    }

    private long getProperty(String prop, long defaultVal) {

        try {

            return Long.valueOf(getProperty(prop));

        }

        catch (Exception e) {

        }

        return defaultVal;

    }

}
