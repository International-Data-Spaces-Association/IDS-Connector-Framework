package de.fraunhofer.isst.ids.framework.daps;

/**
 * Exception that gets thrown, if errors occur while validating a DAT token.
 */
public class ClaimsException extends Exception {
    static final long serialVersionUID = 42L;

    /**
     * For Throwing a ClaimsException with a custom error message.
     *
     * @param message the error message to be included with the exception
     */
    public ClaimsException(final String message) {
        super(message);
    }
}
