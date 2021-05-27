package de.fraunhofer.isst.ids.framework.messaging.handling;

import java.util.Optional;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestMessage;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessageHandler;

/**
 * An instance of RequestHandlerResolver must find a {@link MessageHandler} for a given type of {@link RequestMessage},
 * if a handler exists.
 */
public interface RequestHandlerResolver {

    /**
     * Find the right {@link MessageHandler} for the given MessageType.
     *
     * @param messageType class of the RequestMessage subtype a handler should be found for
     * @param <R> some subtype of RequestMessage
     * @return a MessageHandler for the given messageType or Optional.Empty if no Handler exists
     */
    <R extends Message> Optional<MessageHandler<R>> resolveHandler(Class<R> messageType);
}
