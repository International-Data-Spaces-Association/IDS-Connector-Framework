package de.fraunhofer.isst.ids.framework.communication.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class implements IDSCommunication for the MQTT Protocol.
 */
public class IDSMQTTCommunication {
    private static final Logger LOGGER = LoggerFactory.getLogger(IDSMQTTCommunication.class);
    private Queue<String> messageQueue;
    @Getter @Setter
    private MqttClient client = null;
    @Getter
    private final MqttConnectOptions opt;

    public IDSMQTTCommunication() {
        super();
        messageQueue = new LinkedList<>();
        opt = new MqttConnectOptions();
    }

    /**
     * Establishes connection between client and broker.
     *
     * @param mqtt Configuration for MQTT
     * @throws MqttException if mqtt client creation or connection establishing fail
     */
    public void connectClient(ProtocolMqtt mqtt) throws MqttException {
        opt.setCleanSession(true);
        opt.setUserName(mqtt.getBasicAuthentication().getAuthUsername());
        opt.setPassword(mqtt.getBasicAuthentication().getAuthPassword().toCharArray());

        if (mqtt.getLastWillMessage() != null && mqtt.getLastWillTopic() != null && !mqtt.getLastWillTopic().isEmpty()) {
            opt.setWill(mqtt.getLastWillTopic(), mqtt.getLastWillMessage().getBytes(), mqtt.getQos(), true);
        }
        if (mqtt.getSslProperties() != null) {
            opt.setSSLProperties(mqtt.getSslProperties());
        }

        String serverURI = mqtt.getUri().toString();
        client = new MqttClient(serverURI, mqtt.getClientId() != null ? mqtt.getClientId() : MqttClient.generateClientId());

        client.connect(opt);
    }

    /**
     * Sends message to the MQTT Broker.
     *
     * @param payload         IDSMessage to be sent
     * @param mqtt            Configuration for MQTT
     * @return true if the payload was successfully sent, else false.
     */
    public boolean send(String payload, ProtocolMqtt mqtt) {
        try {
            if (client == null || !client.isConnected()) {
                connectClient(mqtt);
            }

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(mqtt.getQos());

            client.publish(mqtt.getTopic(), message);
            return true;

        } catch (MqttException e) {
            LOGGER.error(
                    "Error on MQTT Communication" + "\n" +
                            "reason: " + e.getReasonCode() + "\n" +
                            "message: " + e.getMessage() + "\n" +
                            "loc: " + e.getLocalizedMessage() + "\n" +
                            "cause: " + e.getCause() + "\n", e);
            return false;
        }
    }

    /**
     * Subscribes to the topic with default callback.
     *
     * @param mqtt            Configuration for MQTT
     * @return true if the payload was successfully sent, else false.
     */
    public boolean subscribe(ProtocolMqtt mqtt) {
        return subscribe(mqtt, (topic, mqttMessage) ->
                messageQueue.add(String.format("Topic: %s. Payload: %s.", topic, new String(mqttMessage.getPayload(), StandardCharsets.UTF_8))));
    }

    /**
     * Subscribes to the topic and sets callback. Use {@link #messageQueue} to store the received messages.
     *
     * @param mqtt            Configuration for MQTT
     * @param messageListener Listener with callback for message arriving
     * @return true if the payload was successfully sent, else false.
     */
    public boolean subscribe(ProtocolMqtt mqtt, IMqttMessageListener messageListener) {
        try {
            if (client == null || !client.isConnected()) {
                connectClient(mqtt);
            }

            client.subscribe(mqtt.getTopic(), messageListener);

            return true;
        } catch (MqttException e) {
            LOGGER.error(
                    "Error on MQTT Communication" + "\n" +
                            "reason: " + e.getReasonCode() + "\n" +
                            "message: " + e.getMessage() + "\n" +
                            "loc: " + e.getLocalizedMessage() + "\n" +
                            "cause: " + e.getCause() + "\n", e);
            return false;
        }
    }

    /**
     * Gets message from message queue.
     *
     * @return received message or null if queue is empty.
     */
    public String getMessageFromQueue() {
        return messageQueue.poll();
    }
}
