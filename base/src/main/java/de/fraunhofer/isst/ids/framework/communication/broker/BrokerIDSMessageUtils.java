package de.fraunhofer.isst.ids.framework.communication.broker;

import java.io.IOException;
import java.net.URI;

import de.fraunhofer.iais.eis.ConnectorUnavailableMessage;
import de.fraunhofer.iais.eis.ConnectorUnavailableMessageBuilder;
import de.fraunhofer.iais.eis.ConnectorUpdateMessage;
import de.fraunhofer.iais.eis.ConnectorUpdateMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.QueryLanguage;
import de.fraunhofer.iais.eis.QueryMessage;
import de.fraunhofer.iais.eis.QueryMessageBuilder;
import de.fraunhofer.iais.eis.QueryScope;
import de.fraunhofer.iais.eis.QueryTarget;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceUnavailableMessage;
import de.fraunhofer.iais.eis.ResourceUnavailableMessageBuilder;
import de.fraunhofer.iais.eis.ResourceUpdateMessage;
import de.fraunhofer.iais.eis.ResourceUpdateMessageBuilder;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import okhttp3.MultipartBody;

/**
 * The MessageUtils class contains utility methods for building Infomodel Messages (used by the {@link IDSBrokerServiceImpl} class).
 */
@UtilityClass
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BrokerIDSMessageUtils {

    static Serializer SERIALIZER = new Serializer();

    /**
     * Create a ResourceUnavailableMessage used for unregistering the given resource at a broker.
     *
     * @param securityToken the DAT Token used for this request
     * @param infoModelVersion the Infomodel Version of the connector
     * @param connectorID the ID of the connector
     * @param resource the resource that is marked as unavailable at the broker
     * @return the {@link ResourceUnavailableMessage} as JSONLD
     * @throws IOException when the message cannot be serialized properly
     */
    public static String buildResourceUnavailableMessage(final DynamicAttributeToken securityToken,
                                                         final String infoModelVersion,
                                                         final URI connectorID,
                                                         final Resource resource) throws IOException {
        final var msg = new ResourceUnavailableMessageBuilder()
                ._affectedResource_(resource.getId())
                ._securityToken_(securityToken)
                ._issued_(IDSUtils.getGregorianNow())
                ._senderAgent_(connectorID)
                ._issuerConnector_(connectorID)
                ._modelVersion_(infoModelVersion)
                .build();
        return SERIALIZER.serialize(msg);
    }

    /**
     * Create a ResourceUpdateMessage used for registering and updating the given resource at a broker.
     *
     * @param securityToken the DAT Token used for this request
     * @param infoModelVersion the Infomodel Version of the connector
     * @param connectorID the ID of the connector
     * @param resource the resource that is updated at the broker
     * @return the {@link ResourceUpdateMessage} as JSONLD
     * @throws IOException when the message cannot be serialized properly
     */
    public static String buildResourceUpdateMessage(final DynamicAttributeToken securityToken,
                                                    final String infoModelVersion,
                                                    final URI connectorID,
                                                    final Resource resource) throws IOException {
        final var msg = new ResourceUpdateMessageBuilder()
                ._affectedResource_(resource.getId())
                ._securityToken_(securityToken)
                ._issued_(IDSUtils.getGregorianNow())
                ._senderAgent_(connectorID)
                ._issuerConnector_(connectorID)
                ._modelVersion_(infoModelVersion)
                .build();
        return SERIALIZER.serialize(msg);
    }

    /**
     * Create a ConnectorUnavailableMessage used for unregistering the connector at a broker.
     *
     * @param securityToken the DAT Token used for this request
     * @param infoModelVersion the Infomodel Version of the connector
     * @param connectorID the ID of the connector
     * @return the {@link ConnectorUnavailableMessage} as JSONLD
     * @throws IOException when the message cannot be serialized properly
     */
    public static String buildUnavailableMessage(final DynamicAttributeToken securityToken,
                                                 final String infoModelVersion,
                                                 final URI connectorID) throws IOException {
        final var msg = new ConnectorUnavailableMessageBuilder()
                ._securityToken_(securityToken)
                ._issued_(IDSUtils.getGregorianNow())
                ._modelVersion_(infoModelVersion)
                ._issuerConnector_(connectorID)
                ._senderAgent_(connectorID)
                ._affectedConnector_(connectorID)
                .build();
        return SERIALIZER.serialize(msg);
    }

    /**
     * Create a ConnectorUpdateMessage used for registering the connector at a broker.
     *
     * @param securityToken the DAT Token used for this request
     * @param infoModelVersion the Infomodel Version of the connector
     * @param connectorID the ID of the connector
     * @return the {@link ConnectorUpdateMessage} as JSONLD
     * @throws IOException when the message cannot be serialized properly
     */
    public static String buildUpdateMessage(final DynamicAttributeToken securityToken,
                                            final String infoModelVersion,
                                            final URI connectorID) throws IOException {
        final var msg = new ConnectorUpdateMessageBuilder()
                ._securityToken_(securityToken)
                ._issued_(IDSUtils.getGregorianNow())
                ._modelVersion_(infoModelVersion)
                ._issuerConnector_(connectorID)
                ._senderAgent_(connectorID)
                ._affectedConnector_(connectorID)
                .build();
        return SERIALIZER.serialize(msg);
    }

    /**
     * Create a QueryMessage used for querying the broker.
     *
     * @param securityToken the DAT Token used for this request
     * @param infoModelVersion the Infomodel Version of the connector
     * @param connectorID the ID of the connector
     * @param queryLanguage the Language of the Query (e.g. SPARQL, SQL, XQUERY)
     * @param queryScope the Scope of the Query (ALL connectors, ACTIVE connectors, INACTIVE connectors) {@link QueryScope}
     * @param queryTarget the type of IDS Components that are queried {@link QueryTarget}
     * @return the {@link QueryMessage} as JSONLD
     * @throws IOException when the message cannot be serialized properly
     */
    public static String buildQueryMessage(final DynamicAttributeToken securityToken,
                                           final String infoModelVersion,
                                           final URI connectorID,
                                           final QueryLanguage queryLanguage,
                                           final QueryScope queryScope,
                                           final QueryTarget queryTarget) throws IOException {
        final var msg = new QueryMessageBuilder()
                ._securityToken_(securityToken)
                ._issued_(IDSUtils.getGregorianNow())
                ._modelVersion_(infoModelVersion)
                ._issuerConnector_(connectorID)
                ._senderAgent_(connectorID)
                ._queryLanguage_(queryLanguage)
                ._queryScope_(queryScope)
                ._recipientScope_(queryTarget)
                .build();
        return SERIALIZER.serialize(msg);
    }

    /**
     * Generates a two part multipart message. First part includes the message type (register, update, unavailable) and
     * second part contains the payload.
     *
     * @param header          String representation of the header message
     * @param payload String representation of the payload
     * @return Two part multipart message containing the message header and self declaration as body
     */
    public static MultipartBody buildRequestBody(final String header, final String payload) {
        final var builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("header", header);

        if (payload != null) {
            builder.addFormDataPart("payload", payload);
        }

        return builder.build();
    }

}
