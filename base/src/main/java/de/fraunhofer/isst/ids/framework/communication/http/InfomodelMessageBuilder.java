package de.fraunhofer.isst.ids.framework.communication.http;

import java.io.File;
import java.io.IOException;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * This Builder is a utility class for building OkHTTP
 * Multipart RequestBodies with RequestMessage header and String or File payload Part.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InfomodelMessageBuilder {

    MultipartBody.Builder builder;

    static final Serializer SERIALIZER = new Serializer();

    /**
     * Internal builder used by the static methods.
     *
     * @param header the header Part of the MultipartMessage (an implementation of {@link Message})
     * @throws IOException if the given header cannot be serialized by the given serializer
     */
    private InfomodelMessageBuilder(final Message header) throws IOException {
        this.builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("header", SERIALIZER.serialize(header));
    }


    /**
     * Build a MultipartMessage with {@link Message} header and {@link File} payload.
     *
     * @param header the header Part of the MultipartMessage (an implementation of {@link Message})
     * @param payload  the File that is added to the MultipartMessages payload
     * @param fileType the MediaType of the file
     * @return the built Message as OkHttp MultipartBody
     * @throws IOException if the given header cannot be serialized by the given serializer
     */
    public static MultipartBody messageWithFile(final Message header,
                                                final File payload,
                                                final MediaType fileType) throws IOException {
        final var imb = new InfomodelMessageBuilder(header);
        imb.addPayload(payload, fileType);
        return imb.getRequestBody();
    }

    /**
     * Build a MultipartMessage with {@link Message} header and String payload.
     *
     * @param header the header Part of the MultipartMessage (an implementation of {@link Message})
     * @param payload  the (String) payload that is added to the MultipartMessages Payload
     * @return the built Message as OkHttp MultipartBody
     * @throws IOException if the given header cannot be serialized by the given serializer
     */
    public static MultipartBody messageWithString(final Message header,
                                                  final String payload) throws IOException {
        final var imb = new InfomodelMessageBuilder(header);
        imb.addPayload(payload);
        return imb.getRequestBody();
    }

    /**
     * Add a String payload to the builder.
     *
     * @param payload the (String) payload that is added to the MultipartMessages Payload
     */
    private void addPayload(final String payload) {
        builder.addFormDataPart("payload", payload);
    }

    /**
     * Add a File payload to the builder.
     *
     * @param file the File that is added to the MultipartMessages payload
     * @param fileType the MediaType of the file
     */
    private void addPayload(final File file, final MediaType fileType) {
        builder.addFormDataPart("payload", file.getName(), RequestBody.create(file, fileType));
    }

    /**
     * Getter for the built multipart message as OkHttp {@link MultipartBody}.
     *
     * @return the built Message as OkHttp {@link MultipartBody}
     */
    private MultipartBody getRequestBody() {
        return builder.build();
    }

}
