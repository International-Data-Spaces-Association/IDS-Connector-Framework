package de.fraunhofer.isst.ids.framework.communication.http;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import de.fraunhofer.iais.eis.ConnectorDeployMode;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.ClaimsException;
import de.fraunhofer.isst.ids.framework.daps.DapsValidator;
import de.fraunhofer.isst.ids.framework.util.MultipartStringParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.stereotype.Service;

/**
 * Implementation Class of IDSHttpService.
 */
@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IDSHttpServiceImpl implements IDSHttpService {

    HttpService httpService;
    DapsValidator dapsValidator;
    ConfigurationContainer configurationContainer;

    /** {@inheritDoc} */
    @Override
    public Map<String, String> sendAndCheckDat(final RequestBody body, final URI target)
            throws IOException, FileUploadException, ClaimsException {
        Response response;
        try {
            response = httpService.send(body, target);
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Message could not be sent!");
            }
            throw e;
        }
        return checkDatFromResponse(response);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> sendWithHeadersAndCheckDat(final RequestBody body,
                                                          final URI target,
                                                          final Map<String, String> headers) throws IOException, FileUploadException, ClaimsException {
        Response response;
        try {
            response = httpService.sendWithHeaders(body, target, headers);
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Message could not be sent!");
            }
            throw e;
        }
        return checkDatFromResponse(response);
    }

    /**
     * @param response {@link Response} from an IDS Http request
     * @return Multipart Map with header and payload part of response
     * @throws IOException if request cannot be sent
     * @throws FileUploadException if response cannot be parsed to multipart map
     * @throws ClaimsException if DAT of response is invalid or cannot be parsed
     */
    private Map<String, String> checkDatFromResponse(final Response response)
            throws IOException, ClaimsException, FileUploadException {
        //if connector is set to test deployment: ignore DAT Tokens
        final var ignoreDAT = configurationContainer.getConfigModel().getConnectorDeployMode() == ConnectorDeployMode.TEST_DEPLOYMENT;
        final var responseString = response.body().string();
        final var valid = ignoreDAT || dapsValidator.checkDat(responseString);

        if(!valid){
            if (log.isWarnEnabled()) {
                log.warn("DAT of incoming response is not valid!");
            }
            throw new ClaimsException("DAT of incoming response is not valid!");
        }
        try {
            return MultipartStringParser.stringToMultipart(responseString);
        } catch (FileUploadException e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not parse incoming response to multipart map!");
            }
            throw e;
        }
    }
}
