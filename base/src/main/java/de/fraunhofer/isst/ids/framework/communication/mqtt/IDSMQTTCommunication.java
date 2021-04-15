package de.fraunhofer.isst.ids.framework.communication.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * This class implements IDSCommunication for the MQTT Protocol.
 */
@Slf4j
public class IDSMQTTCommunication {

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
    public void connectClient(final ProtocolMqtt mqtt) throws MqttException {
        opt.setCleanSession(true);
        opt.setUserName(mqtt.getBasicAuthentication().getAuthUsername());
        opt.setPassword(mqtt.getBasicAuthentication().getAuthPassword().toCharArray());

        if (mqtt.getLastWillMessage() != null && mqtt.getLastWillTopic() != null && !mqtt.getLastWillTopic().isEmpty()) {
            opt.setWill(mqtt.getLastWillTopic(), mqtt.getLastWillMessage().getBytes(), mqtt.getQos(), true);
        }
        if (mqtt.getSslProperties() != null) {
            opt.setSSLProperties(mqtt.getSslProperties());
        }

        final var serverURI = mqtt.getUri().toString();
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
    public boolean send(final String payload, final ProtocolMqtt mqtt) {
        try {
            if (client == null || !client.isConnected()) {
                connectClient(mqtt);
            }

            final var message = new MqttMessage(payload.getBytes());
            message.setQos(mqtt.getQos());

            client.publish(mqtt.getTopic(), message);
            return true;

        } catch (MqttException e) {
            log.error(
                    "Error on MQTT Communication" + "\n"
                            + "reason: " + e.getReasonCode() + "\n"
                            + "message: " + e.getMessage() + "\n"
                            + "loc: " + e.getLocalizedMessage() + "\n"
                            + "cause: " + e.getCause() + "\n", e);
            return false;
        }
    }

    /**
     * Subscribes to the topic with default callback.
     *
     * @param mqtt            Configuration for MQTT
     * @return true if the payload was successfully sent, else false.
     */
    public boolean subscribe(final ProtocolMqtt mqtt) {
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
    public boolean subscribe(final ProtocolMqtt mqtt, final IMqttMessageListener messageListener) {
        try {
            if (client == null || !client.isConnected()) {
                connectClient(mqtt);
            }

            client.subscribe(mqtt.getTopic(), messageListener);

            return true;
        } catch (MqttException e) {
            log.error(
                    "Error on MQTT Communication" + "\n"
                            + "reason: " + e.getReasonCode() + "\n"
                            + "message: " + e.getMessage() + "\n"
                            + "loc: " + e.getLocalizedMessage() + "\n"
                            + "cause: " + e.getCause() + "\n", e);
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
