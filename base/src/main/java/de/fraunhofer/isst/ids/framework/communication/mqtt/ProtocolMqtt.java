package de.fraunhofer.isst.ids.framework.communication.mqtt;

import java.net.URI;
import java.util.Properties;

import de.fraunhofer.iais.eis.BasicAuthentication;
import lombok.Data;

/**
 * Utility class for handling Mqtt Connections.
 */
@Data
public class ProtocolMqtt {
    private BasicAuthentication basicAuthentication;
    private int qos;
    private URI uri;
    private String clientId;
    private String topic;
    private String lastWillTopic;
    private String lastWillMessage;
    private Properties sslProperties;
}
