package de.fraunhofer.isst.ids.framework.messaging.model.filters;

/**
 * Exception that is thrown when an error occurs during preprocessing of incoming headers with a {@link PreDispatchingFilter}.
 */
public class PreProcessingException extends Exception {
    static final long serialVersionUID = 42L;

    public PreProcessingException() {
        super();
    }

    public PreProcessingException(final String message) {
        super(message);
    }

    public PreProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PreProcessingException(final Throwable cause) {
        super(cause);
    }

    protected PreProcessingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
