package de.fraunhofer.isst.ids.framework.communication.broker;

import de.fraunhofer.iais.eis.QueryLanguage;
import de.fraunhofer.iais.eis.QueryScope;
import de.fraunhofer.iais.eis.QueryTarget;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.ConnectorMissingCertExtensionException;
import de.fraunhofer.isst.ids.framework.daps.DapsConnectionException;
import de.fraunhofer.isst.ids.framework.daps.DapsEmptyResponseException;
import de.fraunhofer.isst.ids.framework.daps.DapsTokenProvider;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Broker Communication Controller. Generates appropriate ids multipart messages and send them to the broker
 * infrastructure api.
 **/
@Service
public class IDSBrokerServiceImpl implements IDSBrokerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IDSBrokerServiceImpl.class);

    private static final String INFO_MODEL_VERSION = "4.0.0";
    private static final Serializer ser = new Serializer();

    private ConfigurationContainer container;
    private ClientProvider clientProvider;
    private DapsTokenProvider tokenProvider;

    /**
     * Creates the IDSBrokerCommunication controller.
     *
     * @param container Configuration container
     * @param provider providing underlying OkHttpClient
     * @param tokenProvider providing DAT Token for RequestMessage
     */
    public IDSBrokerServiceImpl(ConfigurationContainer container, ClientProvider provider, DapsTokenProvider tokenProvider){
        this.container = container;
        this.clientProvider = provider;
        this.tokenProvider = tokenProvider;
    }

    /** {@inheritDoc} */
    @Override
    public Response removeResourceFromBroker(String brokerURI, Resource resource) throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        LOGGER.debug("Building message header");
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildResourceUnavailableMessage(securityToken, INFO_MODEL_VERSION, connectorID, resource);
        var payload = ser.serialize(resource);
        var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        LOGGER.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response updateResourceAtBroker(String brokerURI, Resource resource) throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        LOGGER.debug("Building message header");
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildResourceUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID, resource);
        var payload = ser.serialize(resource);
        var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        LOGGER.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response unregisterAtBroker(String brokerURI) throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        LOGGER.debug("Building message header");
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildUnavailableMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        LOGGER.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response updateSelfDescriptionAtBroker(String brokerURI) throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        LOGGER.debug("Building message header");
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        LOGGER.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public java.util.List<Response> updateSelfDescriptionAtBrokers(java.util.List<String> brokerUris) throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        var result = new ArrayList<Response>();
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        for(String uri : brokerUris){
            LOGGER.debug(String.format("Sending message to %s", uri));
            clientProvider.getClient().newCall(new Request.Builder().url(uri).post(body).build()).enqueue(
                    new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            LOGGER.warn(String.format("Connection to Broker %s failed!", uri));
                            LOGGER.warn(e.getMessage(), e);
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                            LOGGER.info(String.format("Received response from %s", uri));
                            result.add(response);
                        }
                    }
            );
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Response queryBroker(String brokerURI, String query, QueryLanguage queryLanguage, QueryScope queryScope, QueryTarget queryTarget)
            throws
            IOException,
            ConnectorMissingCertExtensionException,
            DapsConnectionException,
            DapsEmptyResponseException {
        var securityToken = tokenProvider.getDAT();
        LOGGER.debug("Building message header");
        var connectorID = container.getConnector().getId();
        var header = BrokerIDSMessageUtils.buildQueryMessage(securityToken, INFO_MODEL_VERSION, connectorID, queryLanguage, queryScope, queryTarget);
        var body = BrokerIDSMessageUtils.buildRequestBody(header, query);
        LOGGER.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /**
     * Send the given RequestBody to the broker at the given URI and return the response
     *
     * @param brokerURI URI of the Broker the Message is sent to
     * @param requestBody requestBody that is sent
     * @return Response from the broker
     * @throws IOException if requestBody cannot be sent
     */
    private Response sendBrokerMessage(String brokerURI, RequestBody requestBody) throws IOException {
        var response = clientProvider.getClient().newCall(
                new Request.Builder().url(brokerURI).post(requestBody).build()
        ).execute();
        if(!response.isSuccessful()){
            LOGGER.warn("Response of the Broker wasn't successful!");
        }
        return response;
    }

}
