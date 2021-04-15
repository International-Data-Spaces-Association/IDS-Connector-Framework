package de.fraunhofer.isst.ids.framework.communication.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.fraunhofer.iais.eis.QueryLanguage;
import de.fraunhofer.iais.eis.QueryScope;
import de.fraunhofer.iais.eis.QueryTarget;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.DapsTokenProvider;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * Broker Communication Controller. Generates appropriate ids multipart messages and send them to the broker
 * infrastructure api.
 **/
@Slf4j
@Service
public class IDSBrokerServiceImpl implements IDSBrokerService {

    private static final String     INFO_MODEL_VERSION = "4.0.0";
    private static final Serializer SERIALIZER         = new Serializer();

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
    public IDSBrokerServiceImpl(final ConfigurationContainer container,
                                final ClientProvider provider,
                                final DapsTokenProvider tokenProvider) {
        this.container = container;
        this.clientProvider = provider;
        this.tokenProvider = tokenProvider;
    }

    /** {@inheritDoc} */
    @Override
    public Response removeResourceFromBroker(final String brokerURI, final Resource resource) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        log.debug("Building message header");
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildResourceUnavailableMessage(securityToken, INFO_MODEL_VERSION, connectorID, resource);
        final var payload = SERIALIZER.serialize(resource);
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        log.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response updateResourceAtBroker(final String brokerURI, final Resource resource) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        log.debug("Building message header");
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildResourceUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID, resource);
        final var payload = SERIALIZER.serialize(resource);
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        log.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response unregisterAtBroker(final String brokerURI) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        log.debug("Building message header");
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildUnavailableMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        final var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        log.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public Response updateSelfDescriptionAtBroker(final String brokerURI) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        log.debug("Building message header");
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        final var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);
        log.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /** {@inheritDoc} */
    @Override
    public List<Response> updateSelfDescriptionAtBrokers(final List<String> brokerUris) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        final var result = new ArrayList<Response>();
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildUpdateMessage(securityToken, INFO_MODEL_VERSION, connectorID);
        final var payload = IDSUtils.buildSelfDeclaration(container.getConnector());
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, payload);

        for (final var uri : brokerUris) {
            log.debug(String.format("Sending message to %s", uri));
            clientProvider.getClient().newCall(new Request.Builder().url(uri).post(body).build()).enqueue(
                    new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            log.warn(String.format("Connection to Broker %s failed!", uri));
                            log.warn(e.getMessage(), e);
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                            log.info(String.format("Received response from %s", uri));
                            result.add(response);
                        }
                    }
            );
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Response queryBroker(final String brokerURI,
                                final String query,
                                final QueryLanguage queryLanguage,
                                final QueryScope queryScope,
                                final QueryTarget queryTarget) throws IOException {
        final var securityToken = tokenProvider.getDAT();
        log.debug("Building message header");
        final var connectorID = container.getConnector().getId();
        final var header = BrokerIDSMessageUtils.buildQueryMessage(securityToken, INFO_MODEL_VERSION, connectorID, queryLanguage, queryScope, queryTarget);
        final var body = BrokerIDSMessageUtils.buildRequestBody(header, query);
        log.debug(String.format("Sending message to %s", brokerURI));
        return sendBrokerMessage(brokerURI, body);
    }

    /**
     * Send the given RequestBody to the broker at the given URI and return the response.
     *
     * @param brokerURI URI of the Broker the Message is sent to
     * @param requestBody requestBody that is sent
     * @return Response from the broker
     * @throws IOException if requestBody cannot be sent
     */
    private Response sendBrokerMessage(final String brokerURI, final RequestBody requestBody) throws IOException {
        final var response = clientProvider.getClient().newCall(
                new Request.Builder().url(brokerURI).post(requestBody).build()
        ).execute();
        if (!response.isSuccessful()) {
            log.warn("Response of the Broker wasn't successful!");
        }
        return response;
    }

}
