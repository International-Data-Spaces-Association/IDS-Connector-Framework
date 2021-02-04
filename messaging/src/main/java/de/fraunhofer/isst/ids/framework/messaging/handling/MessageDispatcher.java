package de.fraunhofer.isst.ids.framework.messaging.handling;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ConnectorDeployMode;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.ClaimsException;
import de.fraunhofer.isst.ids.framework.daps.DapsPublicKeyProvider;
import de.fraunhofer.isst.ids.framework.daps.DapsValidator;
import de.fraunhofer.isst.ids.framework.daps.DapsVerifier;
import de.fraunhofer.isst.ids.framework.messaging.model.filters.PreDispatchingFilter;
import de.fraunhofer.isst.ids.framework.messaging.model.filters.PreDispatchingFilterResult;
import de.fraunhofer.isst.ids.framework.messaging.model.filters.PreProcessingException;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessageHandler;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessageHandlingException;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessagePayloadImpl;
import de.fraunhofer.isst.ids.framework.messaging.model.responses.ErrorResponse;
import de.fraunhofer.isst.ids.framework.messaging.model.responses.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * The MessageDispatcher takes all incoming Messages, applies all defined PreDispatchingFilters onto them,
 * checks the DAPS token, gives Messages to the specified MessageHandlers depending on their type and returns
 * the results returned by the MessageHandlers.
 */
public class MessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    private final ObjectMapper objectMapper;
    private final List<PreDispatchingFilter> preDispatchingFilters;
    private final RequestHandlerResolver requestHandlerResolver;
    private final ConfigurationContainer configurationContainer;

    /**
     * Create a MessageDispatcher
     *
     * @param objectMapper a jackson objectmapper for (de)serializing objects
     * @param requestHandlerResolver resolver for finding the fitting {@link MessageHandler} for the incoming Message
     * @param provider a provider that can access the public key of the DAPS
     * @param configurationContainer the connector configuration
     */
    public MessageDispatcher(final ObjectMapper objectMapper,
                             final RequestHandlerResolver requestHandlerResolver,
                             final DapsPublicKeyProvider provider,
                             final ConfigurationContainer configurationContainer) {
        this.objectMapper = objectMapper;
        this.requestHandlerResolver = requestHandlerResolver;
        this.configurationContainer = configurationContainer;
        preDispatchingFilters = new LinkedList<>();

        //add DAT verification as PreDispatchingFilter
        registerPreDispatchingAction(in -> {
            if(configurationContainer.getConfigModel().getConnectorDeployMode() == ConnectorDeployMode.TEST_DEPLOYMENT)
                return PreDispatchingFilterResult.successResult("ConnectorDeployMode is Test. Skipping Token verification!");
            try {
                var verified = DapsVerifier.verify(DapsValidator.getClaims(in, provider.providePublicKey()));
                return PreDispatchingFilterResult.builder()
                        .withSuccess(verified)
                        .withMessage(String.format("Token verification result is: %s", verified))
                        .build();
            } catch (ClaimsException e) {
                return PreDispatchingFilterResult.builder()
                        .withSuccess(false)
                        .withMessage("Token could not be parsed!" + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Register a new PreDispatchingFilter which will be used to filter incoming messages.
     *
     * @param preDispatchingFilter a new {@link PreDispatchingFilter} that should be added to the list of filters
     */
    public void registerPreDispatchingAction(final PreDispatchingFilter preDispatchingFilter) {
        this.preDispatchingFilters.add(preDispatchingFilter);
    }

    /**
     * Apply the preDispatchingFilters to the message. If it wasn't filtered: find the {@link MessageHandler} for its type.
     * Let the handler handle the Message and return the {@link MessageResponse}.
     *
     * @param header header of the incoming Message (RequestMessage implementation)
     * @param payload payload of the incoming Message
     * @param <R> a subtype of RequestMessage
     * @return the {@link MessageResponse} that is returned by the specified {@link MessageHandler} for the type of the incoming Message
     * @throws PreProcessingException if an error occurs in a PreDispatchingFilter
     */
    @SuppressWarnings("unchecked")
    public <R extends Message> MessageResponse process(final R header, final InputStream payload) throws PreProcessingException {
        var connectorId = configurationContainer.getConnector().getId();
        var modelVersion = configurationContainer.getConnector().getOutboundModelVersion();
        //apply all preDispatchingFilters to the message
        for (PreDispatchingFilter preDispatchingFilter : this.preDispatchingFilters) {
            logger.debug("Applying a preDispatchingFilter");
            try {
                final var result = preDispatchingFilter.process(header);
                if (!result.isSuccess()) {
                    logger.debug("A preDispatchingFilter failed!");
                    logger.error(result.getMessage(), result.getError());

                    return ErrorResponse.withDefaultHeader(RejectionReason.MALFORMED_MESSAGE, result.getMessage(), connectorId, modelVersion, header.getId());
                }
            } catch (Exception e) {
                logger.debug("A preDispatchingFilter threw an exception!");
                logger.debug(e.getMessage(), e);
                throw new PreProcessingException(e);
            }
        }

        // Returns the MessageHandler of a given MessageType of the header-part.
        // The MessageType is a subtype of RequestMessage.class from Infomodel.
        var resolvedHandler = requestHandlerResolver.resolveHandler(header.getClass());

        // Checks if revolvedHandler is not null
        if (resolvedHandler.isPresent()) {
            //if an handler exists, let the handle handle the message and return its response
            try {
                MessageHandler<R> handler = (MessageHandler<R>) resolvedHandler.get();
                return handler.handleMessage(header, new MessagePayloadImpl(payload, objectMapper));
            } catch (MessageHandlingException e) {
                logger.debug("The message handler threw an exception!");

                return ErrorResponse.withDefaultHeader(RejectionReason.INTERNAL_RECIPIENT_ERROR,"Error while handling the request!", connectorId, modelVersion, header.getId());
            }
        } else {
            logger.debug(String.format("No message handler exists for %s", header.getClass()));

            //If no handler for the type exists, the message type isn't supported
            return ErrorResponse.withDefaultHeader(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED, "No handler for provided message type was found!", connectorId, modelVersion, header.getId());
        }
    }

}
