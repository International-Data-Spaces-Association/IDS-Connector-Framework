package de.fraunhofer.isst.ids.framework.communication.http;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import de.fraunhofer.isst.ids.framework.daps.ClaimsException;
import okhttp3.RequestBody;
import org.apache.commons.fileupload.FileUploadException;

/**
 * Interface for IDS Http Communication.
 */
public interface IDSHttpService {

    /**
     * @param body requestBody to be sent
     * @param target targetURI of the request
     * @return Multipart Map with header and payload part of response
     * @throws IOException if request cannot be sent
     * @throws FileUploadException if response cannot be parsed to multipart map
     * @throws ClaimsException if DAT of response is invalid or cannot be parsed
     */
    Map<String, String> sendAndCheckDat(RequestBody body, URI target) throws IOException, FileUploadException, ClaimsException;

    /**
     * @param body requestBody to be sent
     * @param target targetURI of the request
     * @param headers additional headers for the Http header
     * @return Multipart Map with header and payload part of response
     * @throws IOException if request cannot be sent
     * @throws FileUploadException if response cannot be parsed to multipart map
     * @throws ClaimsException if DAT of response is invalid or cannot be parsed
     */
    Map<String, String> sendWithHeadersAndCheckDat(RequestBody body, URI target, Map<String, String> headers) throws IOException, FileUploadException, ClaimsException;
}
