package de.fraunhofer.isst.ids.framework.daps;

import java.io.IOException;
import java.security.Key;
import java.util.Map;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessageImpl;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.util.MultipartStringParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.stereotype.Service;

/**
 * The DapsValidator checks the DAPS Token of a RequestMessage using a public signingKey.
 */
@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DapsValidator {

    DapsPublicKeyProvider keyProvider;
    Serializer serializer = new Serializer();

    public DapsValidator(final DapsPublicKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * Extract the Claims from the Dat token of a message, given the Message and a signingKey.
     *
     * @param message an incoming RequestMessage
     * @param signingKey a public Key
     * @return the Claims of the messages DAT Token, when it can be signed with the given key
     * @throws ClaimsException if Token cannot be signed with the given key
     */
    public static Jws<Claims> getClaims(final Message message, final Key signingKey) throws ClaimsException {
        final var tokenValue = message.getSecurityToken().getTokenValue();
        try {
            return Jwts.parser()
                    .setSigningKey(signingKey)
                    .parseClaimsJws(tokenValue);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not parse incoming JWT/DAT!");
            }

            throw new ClaimsException(e.getMessage());
        }
    }

    /**
     * Check the DAT of a Message.
     *
     * @param message an Message from a response
     * @return true if DAT of Message is valid
     */
    public boolean checkDat(final Message message) {
        //Don't check DAT of RejectionMessages
        if (message instanceof RejectionMessageImpl) {
            if (log.isWarnEnabled()) {
                log.warn("RejectionMessage, skipping DAT check!");
            }
            return true;
        }

        Jws<Claims> claims;
        try {
            claims = getClaims(message, keyProvider.providePublicKey());
        } catch (ClaimsException e) {
            if (log.isWarnEnabled()) {
                log.warn("Daps token of response could not be pased!");
            }
            return false;
        }
        try {
            return DapsVerifier.verify(claims);
        } catch (ClaimsException e) {
            if (log.isWarnEnabled()) {
                log.warn("Claims could not be verified!");
            }
            return false;
        }
    }

    /**
     * Check the DAT of an incoming Response body (as string).
     *
     * @param responseBody string of incoming response body
     * @return true if DAT of response is valid
     */
    public boolean checkDat(final String responseBody) {
        Map<String, String> responseMap;
        Message responseHeader;
        try {
            responseMap = MultipartStringParser.stringToMultipart(responseBody);
        } catch (FileUploadException e) {
            if (log.isWarnEnabled()) {
                log.warn("Response cannot be parsed to multipart!");
            }
            return false;
        }
        try {
           responseHeader = serializer.deserialize(responseMap.get("header"), Message.class);
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Response header cannot be deserialized to IDS Message!");
            }
            return false;
        }
        return checkDat(responseHeader);
    }

}
