package de.fraunhofer.isst.ids.framework.daps;

/**
 * Exception which is thrown when the Response from the DAPS is empty.
 */
public class EmptyDapsResponseException extends Exception {
    static final long serialVersionUID = 42L;

    /**
     * For Throwing a EmptyDapsResponseException with a custom error message.
     *
     * @param message the error message to be included with the exception
     */
    public EmptyDapsResponseException(final String message) {
        super(message);
    }

}
