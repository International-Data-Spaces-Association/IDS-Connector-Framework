package de.fraunhofer.isst.ids.framework.messaging.handling;

import de.fraunhofer.iais.eis.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * REST controller for handling all incoming IDS multipart Messages
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
    @Transactional
    public ResponseEntity<MultiValueMap<String, Object>> handleIDSMessage(final HttpServletRequest request) {
        try {
            final var headerPart = request.getPart(HEADER_MULTIPART_NAME);
            final var payloadPart = request.getPart(PAYLOAD_MULTIPART_NAME);

            if (headerPart == null || payloadPart == null) {
                log.debug("header or payload of incoming message were empty!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.MALFORMED_MESSAGE, "Header or Payload was missing!"));
            }

            String input;
            log.debug("parsing header of incoming message");
            try (Scanner scanner = new Scanner(headerPart.getInputStream(), StandardCharsets.UTF_8.name())) {
                input = scanner.useDelimiter("\\A").next();
            }

            // Deserialize JSON-LD headerPart to its RequestMessage.class
            final var requestHeader = serializer.deserialize(input, Message.class);

            log.debug("hand the incoming message to the message dispatcher!");
            final var response = this.messageDispatcher.process(requestHeader, payloadPart.getInputStream());

            //get Response as MultiValueMap
            final var responseAsMap = createMultiValueMap(response.createMultipartMap(serializer));

            // return the ResponseEntity as Multipart content with created MultiValueMap
            log.debug("sending response with status OK (200)");
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(responseAsMap);

        } catch (PreProcessingException e) {
            log.error("Error during pre-processing with a PreDispatchingFilter!", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.BAD_PARAMETERS, String.format("Error during preprocessing: %s", e.getMessage())));
        } catch (IOException e){
            log.warn("incoming message could not be parsed!");
            log.warn(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createDefaultErrorMessage(RejectionReason.MALFORMED_MESSAGE, "Could not parse incoming message!"));
        } catch (ServletException e){
            log.warn("incoming request was not multipart!");
            log.warn(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createDefaultErrorMessage(RejectionReason.INTERNAL_RECIPIENT_ERROR, String.format("Could not read incoming request! Error: %s", e.getMessage())));
        }
    }

    /**
     * Create a Spring {@link MultiValueMap} from a {@link java.util.Map}
     *
     * @param map a map as provided by the MessageResponse
     * @return a MultiValueMap used as ResponseEntity for Spring
     */
    private MultiValueMap<String, Object> createMultiValueMap(Map<String, Object> map){
        log.debug("Creating MultiValueMap for the response");
        var multiMap = new LinkedMultiValueMap<String, Object>();
        for(var entry : map.entrySet()){
            multiMap.put(entry.getKey(), List.of(entry.getValue()));
        }
        return multiMap;
    }

    /**
     * Create a default RejectionMessage with a given RejectionReason and specific error message for the payload
     *
     * @param rejectionReason reason why the message was rejected
     * @param errorMessage a specific error message for the payload
     * @return MultiValueMap with given error information that can be used for a multipart response
     */
    private MultiValueMap<String, Object> createDefaultErrorMessage(RejectionReason rejectionReason, String errorMessage){
        try {
            var rejectionMessage = new RejectionMessageBuilder()
                    ._securityToken_(new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("rejected!").build())
                    ._correlationMessage_(URI.create("https://INVALID"))
                    ._senderAgent_(configurationContainer.getConnector().getId())
                    ._modelVersion_(configurationContainer.getConnector().getOutboundModelVersion())
                    ._rejectionReason_(rejectionReason)
                    ._issuerConnector_(configurationContainer.getConnector().getId())
                    ._issued_(IDSUtils.getGregorianNow())
                    .build();
            var multiMap = new LinkedMultiValueMap<String, Object>();
            multiMap.put("header", List.of(serializer.serialize(rejectionMessage)));
            multiMap.put("payload", List.of(errorMessage));
            return multiMap;
        } catch (IOException e) {
            log.info(e.getMessage(), e);
            return null;
        }
    }
}
