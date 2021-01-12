package de.fraunhofer.isst.ids.framework.messaging.model.messages;

/**
 * An exception that is thrown during MessageHandling of a MessageHandler
 */
public class MessageHandlingException extends Exception {

    public MessageHandlingException() {
        super();
    }

    public MessageHandlingException(final String message) {
        super(message);
    }

    public MessageHandlingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MessageHandlingException(final Throwable cause) {
        super(cause);
    }

    public MessageHandlingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
