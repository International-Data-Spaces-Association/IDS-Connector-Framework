package de.fraunhofer.isst.ids.framework.messaging.model.messages;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.isst.ids.framework.messaging.model.responses.MessageResponse;

/**
 * @param <T> subtype of message supported by the message handler
 */
public interface MessageHandler<T extends Message> {

    /**
     * Handle an incoming Message of type T and return a MessageResponse
     *
     * @param queryHeader header part of the incoming Message (an instance of RequestMessage)
     * @param payload payload of the Message (as MessagePayload, access with getUnderlyingInputStream())
     * @return an instance of MessageResponse (BodyResponse, ErrorResponse,...)
     * @throws MessageHandlingException if an error occurs while handling the incoming message
     */
    MessageResponse handleMessage(final T queryHeader, final MessagePayload payload) throws MessageHandlingException;
}
