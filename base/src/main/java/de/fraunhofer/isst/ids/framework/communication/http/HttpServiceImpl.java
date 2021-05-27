package de.fraunhofer.isst.ids.framework.communication.http;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

/**
 * Service for sending Http Requests using configuration settings.
 */
@Slf4j
@Service
public class HttpServiceImpl implements HttpService {

    private ClientProvider provider;
    private TimeoutSettings timeoutSettings;

    /**
     * @param provider the {@link ClientProvider} used to generate HttpClients with the current connector configuration
     */
    public HttpServiceImpl(final ClientProvider provider) {
        this.provider = provider;
    }

    /** {@inheritDoc} */
    @Override
    public void setTimeouts(final Duration connectTimeout,
                            final Duration readTimeout,
                            final Duration writeTimeout,
                            final Duration callTimeout) {
            this.timeoutSettings = new TimeoutSettings(connectTimeout, readTimeout, writeTimeout, callTimeout);
    }

    /** {@inheritDoc} */
    @Override
    public void removeTimeouts() {
        this.timeoutSettings = null;
    }

    /** {@inheritDoc} */
    @Override
    public Response send(final String message, final URI target) throws IOException {
       if (log.isDebugEnabled()) {
           log.debug("Creating requestBody");
       }

       final var body = RequestBody.create(message, MediaType.parse("application/json"));

       return send(body, target);
    }

    /** {@inheritDoc} */
    @Override
    public Response send(final RequestBody requestBody, final URI target) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("building request to %s", target.toString()));
        }

        final var request = buildRequest(requestBody, target);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sending request to %s", target.toString()));
        }

        return sendRequest(request, getClientWithSettings());
    }

    /** {@inheritDoc} */
    @Override
    public Response sendWithHeaders(final RequestBody requestBody,
                                    final URI target,
                                    final Map<String, String> headers) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("building request to %s", target.toString()));
        }

        final var request = buildWithHeaders(requestBody, target, headers);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sending request to %s", target.toString()));
        }

        return sendRequest(request, getClientWithSettings());
    }

    /** {@inheritDoc} */
    @Override
    public Response get(final URI target) throws IOException {
        final var request = new Request.Builder().url(target.toString()).get().build();
        return sendRequest(request, getClientWithSettings());
    }

    /** {@inheritDoc} */
    @Override
    public Response getWithHeaders(final URI target, final Map<String, String> headers) throws IOException {
        final var builder = new Request.Builder().url(target.toString()).get();

        headers.keySet().forEach(key -> {
                                     if (log.isDebugEnabled()) {
                                         log.debug(String.format("adding header part (%s,%s)", key, headers.get(key)));
                                     }
                                    builder.addHeader(key, headers.get(key));
                            });

        final var request = builder.build();
        return sendRequest(request, getClientWithSettings());
    }

    /**
     * Build a {@link Request} from given {@link RequestBody} and target {@link URI}.
     *
     * @param requestBody {@link RequestBody} object to be sent
     * @param target the target to send the request to
     * @return the built http {@link Request}
     */
    private Request buildRequest(final RequestBody requestBody, final URI target) {
        final var targetURL = target.toString();

        if (log.isInfoEnabled()) {
            log.info("URL is valid: " + HttpUrl.parse(targetURL));
        }

        return new Request.Builder()
                .url(targetURL)
                .post(requestBody)
                .build();
    }

    /**
     * Build a {@link Request} from given {@link RequestBody} and target {@link URI},
     * add extra header fields provided in headers map-.
     *
     * @param requestBody {@link RequestBody} object to be sent
     * @param headers a Map of http headers for the header of the built request
     * @param target the target to send the request to
     * @return the build http {@link Request}
     */
    private Request buildWithHeaders(final RequestBody requestBody,
                                     final URI target,
                                     final Map<String, String> headers) {
        final var targetURL = target.toString();

        if (log.isInfoEnabled()) {
            log.info("URL is valid: " + HttpUrl.parse(targetURL));
        }

        //!!! DO NOT PRINT RESPONSE BECAUSE RESPONSE BODY IS JUST ONE TIME READABLE
        // --> Message could not be parsed java.io.IOException: closed
        final var builder = new Request.Builder()
                .url(targetURL)
                .post(requestBody);
        //add all headers to request
        if (log.isDebugEnabled()) {
            log.debug("Adding headers");
        }

        headers.keySet().forEach(key -> {
            log.debug(String.format("adding header part (%s,%s)", key, headers.get(key)));
            builder.addHeader(key, headers.get(key));
        });
        return builder.build();
    }

    /**
     * Sends a generated request http message to the defined address.
     *
     * @param request POST Request with the message as body
     * @param client {@link OkHttpClient} for sending Request
     * @return Response object containing the return message from the broker
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    private Response sendRequest(final Request request, final OkHttpClient client) throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Request is HTTPS: " + request.isHttps());
        }

        final var response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            if (log.isErrorEnabled()) {
                log.error("Error while sending the request!");
            }

            throw new IOException("Unexpected code " + response + " With Body: " + Objects
                    .requireNonNull(response.body()).string());
        }
        return response;
    }

    /**
     * Get an OkHttpClient with the current Timeout Settings.
     *
     * @return client with set timeouts
     */
    private OkHttpClient getClientWithSettings() {
        if (timeoutSettings != null) {
            if (log.isDebugEnabled()) {
                log.debug("Generating a Client with specified timeout settings.");
            }

            return provider.getClientWithTimeouts(
                    timeoutSettings.getConnectTimeout(),
                    timeoutSettings.getReadTimeout(),
                    timeoutSettings.getWriteTimeout(),
                    timeoutSettings.getCallTimeout()
            );
        }

        if (log.isDebugEnabled()) {
            log.debug("No timeout settings specified, using default client.");
        }

        return provider.getClient();
    }

    /**
     * Inner class, managing timeout settings for custom HttpClients.
     */
    @Data
    @AllArgsConstructor
    private class TimeoutSettings {
        private Duration connectTimeout;
        private Duration readTimeout;
        private Duration writeTimeout;
        private Duration callTimeout;
    }
}
