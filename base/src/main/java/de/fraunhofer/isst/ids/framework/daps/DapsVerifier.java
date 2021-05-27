package de.fraunhofer.isst.ids.framework.daps;

import java.time.LocalDateTime;
import java.time.ZoneId;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * The DefaultVerifier contains some default DAPS verification rules.
 */
@Slf4j
@UtilityClass
public class DapsVerifier {
    /**
     * Check notbefore and expiration of the DAT Token Claims.
     *
     * The default rules check if the current Time is between NotBefore and Expiration
     * @param toVerify the claims to verify
     * @return true if message is valid
     * @throws ClaimsException when the claims of the DAT cannot be verified
     */
    public static boolean verify(final Jws<Claims> toVerify) throws ClaimsException {
        try {
            final Claims body = toVerify.getBody();
            return (LocalDateTime.now().toLocalDate().isAfter(body.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) ||
                   LocalDateTime.now().toLocalDate().isEqual(body.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) &&
                   (LocalDateTime.now().toLocalDate().isBefore(body.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) ||
                   LocalDateTime.now().toLocalDate().isEqual(body.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not verify Claims of the DAT Token!");
            }
            throw new ClaimsException(e.getMessage());
        }
    }
}
