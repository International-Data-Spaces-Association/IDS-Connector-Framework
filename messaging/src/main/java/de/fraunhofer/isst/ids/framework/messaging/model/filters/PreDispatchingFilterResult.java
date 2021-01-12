package de.fraunhofer.isst.ids.framework.messaging.model.filters;

import java.util.Objects;

/**
 * Result that is returned by a PreDispatchingFilter (with information about why a message was accepted or rejected)
 */
public class PreDispatchingFilterResult {
    private final Throwable error;
    private final boolean success;
    private final String message;

    /**
     * The result contains a message about its result, can contain an error if something went wrong and has a boolean flag for success
     *
     * @param error an error that occured during PreDispatchingFilter processing
     * @param success true if the predispatchingfilter successfully checked and accepted the message
     * @param message information about why the {@link PreDispatchingFilter} accepted (or rejected) the message.
     */
    protected PreDispatchingFilterResult(final Throwable error, final boolean success, final String message) {
        this.error = error;
        this.success = success;
        this.message = message;
    }

    /**
     * Static method returning a builder.
     *
     * @return a builder instance for this class
     */
    public static PreDispatchingFilterResultBuilder builder() {
        return new PreDispatchingFilterResultBuilder();
    }

    /**
     * Static method for a default successResult
     *
     * @return a PreDispatchingFilterResult where success = true, but with empty message
     */
    public static PreDispatchingFilterResult successResult() {
        return successResult("");
    }

    /**
     * Create a successResult with given message
     *
     * @param message the message of the Result
     * @return a PreDispatchingFilterResult where success = true, with given message
     */
    public static PreDispatchingFilterResult successResult(final String message) {
        return new PreDispatchingFilterResult(null, true, message);
    }

    /**
     * Getter for the error
     *
     * @return the error of the result
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Getter for success status
     *
     * @return true if filter was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Getter for the message
     *
     * @return the message of the filter result
     */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (PreDispatchingFilterResult) o;
        return isSuccess() == that.isSuccess() &&
                Objects.equals(getError(), that.getError()) &&
                Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getError(), isSuccess(), getMessage());
    }

    /**
     * Builder class for PreDispatchingFilterResults
     */
    public static final class PreDispatchingFilterResultBuilder {
        private Throwable error;
        private boolean success;
        private String message;

        private PreDispatchingFilterResultBuilder() {
        }

        public PreDispatchingFilterResultBuilder withError(final Throwable error) {
            this.error = error;
            return this;
        }

        public PreDispatchingFilterResultBuilder withSuccess(final boolean success) {
            this.success = success;
            return this;
        }

        public PreDispatchingFilterResultBuilder withMessage(final String message) {
            this.message = message;
            return this;
        }

        public PreDispatchingFilterResult build() {
            return new PreDispatchingFilterResult(error, success, message);
        }
    }
}
