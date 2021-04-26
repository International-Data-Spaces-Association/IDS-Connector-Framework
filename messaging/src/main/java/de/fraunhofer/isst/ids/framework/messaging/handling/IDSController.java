package de.fraunhofer.isst.ids.framework.messaging.handling;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.messaging.model.filters.PreProcessingException;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * REST controller for handling all incoming IDS multipart Messages.
 */
@Slf4j
@Controller
public class IDSController {

    private static final String HEADER_MULTIPART_NAME = "header";
    private static final String PAYLOAD_MULTIPART_NAME = "payload";

    private final MessageDispatcher messageDispatcher;
    private final ConfigurationContainer configurationContainer;
    private final Serializer serializer;

    @Autowired
    public IDSController(final MessageDispatcher messageDispatcher,
                         final Serializer serializer,
                         final ConfigurationContainer configurationContainer) {
        this.messageDispatcher = messageDispatcher;
        this.serializer = serializer;
        this.configurationContainer = configurationContainer;
    }

    /**
     * Generic method to handle all incoming ids messages. One Method to Rule them All.
     * Get header and payload from incoming message, let the MessageDispatcher and MessageHandler process it
     * and return the result as a Multipart response.
     *
     * @param request incoming http request
     * @return multipart MultivalueMap containing ResponseMessage header and some payload
     */
    public ResponseEntity<MultiValueMap<String, Object>> handleIDSMessage(final HttpServletRequest request) {
        try {
            final var headerPart = request.getPart(HEADER_MULTIPART_NAME);
            final var payloadPart = request.getPart(PAYLOAD_MULTIPART_NAME);

            if (headerPart == null) {
                if (log.isDebugEnabled()) {
                    log.debug("header or payload of incoming message were empty!");
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.MALFORMED_MESSAGE, "Header was missing!"));
            }

            String input;
            if (log.isDebugEnabled()) {
                log.debug("parsing header of incoming message");
            }
            try (var scanner = new Scanner(headerPart.getInputStream(), StandardCharsets.UTF_8.name())) {
                input = scanner.useDelimiter("\\A").next();
            }

            // Deserialize JSON-LD headerPart to its RequestMessage.class
            final var requestHeader = serializer.deserialize(input, Message.class);

            if (log.isDebugEnabled()) {
                log.debug("hand the incoming message to the message dispatcher!");
            }
            final var response = this.messageDispatcher.process(requestHeader, payloadPart == null ? null : payloadPart.getInputStream()); //pass null if payloadPart is null, else pass it as inputStream

            //get Response as MultiValueMap
            final var responseAsMap = createMultiValueMap(response.createMultipartMap(serializer));

            // return the ResponseEntity as Multipart content with created MultiValueMap
            if (log.isDebugEnabled()) {
                log.debug("sending response with status OK (200)");
            }
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(responseAsMap);

        } catch (PreProcessingException e) {
            if (log.isErrorEnabled()) {
                log.error("Error during pre-processing with a PreDispatchingFilter!", e);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.BAD_PARAMETERS, String.format("Error during preprocessing: %s", e.getMessage())));
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("incoming message could not be parsed!");
                log.warn(e.getMessage(), e);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.MALFORMED_MESSAGE, "Could not parse incoming message!"));
        } catch (ServletException e) {
            if (log.isWarnEnabled()) {
                log.warn("incoming request was not multipart!");
                log.warn(e.getMessage(), e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createDefaultErrorMessage(RejectionReason.INTERNAL_RECIPIENT_ERROR, String.format("Could not read incoming request! Error: %s", e.getMessage())));
        }
    }

    /**
     * Create a Spring {@link MultiValueMap} from a {@link java.util.Map}.
     *
     * @param map a map as provided by the MessageResponse
     * @return a MultiValueMap used as ResponseEntity for Spring
     */
    private MultiValueMap<String, Object> createMultiValueMap(final Map<String, Object> map) {
        if (log.isDebugEnabled()) {
            log.debug("Creating MultiValueMap for the response");
        }

        final var multiMap = new LinkedMultiValueMap<String, Object>();
        for (final var entry : map.entrySet()) {
            multiMap.put(entry.getKey(), List.of(entry.getValue()));
        }
        return multiMap;
    }

    /**
     * Create a default RejectionMessage with a given RejectionReason and specific error message for the payload.
     *
     * @param rejectionReason reason why the message was rejected
     * @param errorMessage a specific error message for the payload
     * @return MultiValueMap with given error information that can be used for a multipart response
     */
    private MultiValueMap<String, Object> createDefaultErrorMessage(final RejectionReason rejectionReason,
                                                                    final String errorMessage) {
        try {
            final var rejectionMessage = new RejectionMessageBuilder()
                    ._securityToken_(new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("rejected!").build())
                    ._correlationMessage_(URI.create("https://INVALID"))
                    ._senderAgent_(configurationContainer.getConnector().getId())
                    ._modelVersion_(configurationContainer.getConnector().getOutboundModelVersion())
                    ._rejectionReason_(rejectionReason)
                    ._issuerConnector_(configurationContainer.getConnector().getId())
                    ._issued_(IDSUtils.getGregorianNow())
                    .build();
            final var multiMap = new LinkedMultiValueMap<String, Object>();
            multiMap.put(HEADER_MULTIPART_NAME, List.of(serializer.serialize(rejectionMessage)));
            multiMap.put(PAYLOAD_MULTIPART_NAME, List.of(errorMessage));

            return multiMap;
        } catch (IOException e) {
            if (log.isInfoEnabled()) {
                log.info(e.getMessage(), e);
            }

            return null;
        }
    }
}
