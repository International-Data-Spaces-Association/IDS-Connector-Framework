package de.fraunhofer.isst.ids.framework.communication.mqtt;

import java.net.URI;
import java.util.Properties;

import de.fraunhofer.iais.eis.BasicAuthentication;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Utility class for handling Mqtt Connections.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProtocolMqtt {
    BasicAuthentication basicAuthentication;
    int qos;
    URI uri;
    String clientId;
    String topic;
    String lastWillTopic;
    String lastWillMessage;
    Properties sslProperties;
}
