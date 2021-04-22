package de.fraunhofer.isst.ids.framework.daps;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.Request;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.json.JSONObject;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

/**
 * Manages Dynamic Attribute Tokens.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
@Slf4j
@UtilityClass
public class TokenManagerService {
    /**
     * Get the DAT from the DAPS at dapsURL using the current configuration.
     *
     * @param container An IDS Connector Configuration
     * @param dapsUrl The URL of a DAPS Service
     * @param provider providing underlying OkHttpClient
     * @return signed DAPS JWT token for the Connector
     */
    public static String acquireToken(final ConfigurationContainer container,
                                      final ClientProvider provider,
                                      final String dapsUrl) {

        var dynamicAttributeToken = "INVALID_TOKEN";

        final var keyStoreManager = container.getKeyManager();
        final var targetAudience = "idsc:IDS_CONNECTORS_ALL";


        // Try clause for setup phase (loading keys, building trust manager)
        try {

            // get private key
            log.debug("Getting PrivateKey and Certificate from KeyStoreManager");
            final var privKey = keyStoreManager.getPrivateKey();

            // Get certificate of public key
            final var cert = (X509Certificate) keyStoreManager.getCert();

            // Get AKI
            //GET 2.5.29.14 SubjectKeyIdentifier / 2.5.29.35 AuthorityKeyIdentifier
            log.debug("Get AKI from certificate");
            final var authorityKeyIdentifierId = Extension.authorityKeyIdentifier.getId();
            final var rawAuthorityKeyIdentifier = cert.getExtensionValue(authorityKeyIdentifierId);
            if (rawAuthorityKeyIdentifier == null) {
                throw new MissingCertExtensionException("AKI of the Connector Certificate is null!");
            }
            final var akiOc = ASN1OctetString.getInstance(rawAuthorityKeyIdentifier);
            final var aki = AuthorityKeyIdentifier.getInstance(akiOc.getOctets());
            final var authorityKeyIdentifier = aki.getKeyIdentifier();

            //GET SKI
            log.debug("Get SKI from certificate");
            final var subjectKeyIdentifierId = Extension.subjectKeyIdentifier.getId();
            final var rawSubjectKeyIdentifier = cert.getExtensionValue(subjectKeyIdentifierId);

            if (rawSubjectKeyIdentifier == null) {
                throw new MissingCertExtensionException("SKI of the Connector Certificate is null!");
            }

            final var ski0c = ASN1OctetString.getInstance(rawSubjectKeyIdentifier);
            final var ski = SubjectKeyIdentifier.getInstance(ski0c.getOctets());
            final var subjectKeyIdentifier = ski.getKeyIdentifier();
            final var akiResult = beautifyHex(encodeHexString(authorityKeyIdentifier).toUpperCase());
            final var skiResult = beautifyHex(encodeHexString(subjectKeyIdentifier).toUpperCase());

            final var connectorUUID = skiResult + "keyid:" + akiResult.substring(0, akiResult.length() - 1);

            if (log.isInfoEnabled()) {
                log.info("ConnectorUUID: " + connectorUUID);
                log.info("Retrieving Dynamic Attribute Token...");
            }

            // create signed JWT (JWS)
            // Create expiry date one day (86400 seconds) from now
            if (log.isDebugEnabled()) {
                log.debug("Building jwt token");
            }

            final var expiryDate = Date.from(Instant.now().plusSeconds(86_400));
            final var jwtb =
                    Jwts.builder()
                            .setIssuer(connectorUUID)
                            .setSubject(connectorUUID)
                            .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                            .claim("@type", "ids:DatRequestToken")
                            .setExpiration(expiryDate)
                            .setIssuedAt(Date.from(Instant.now().minusSeconds(10)))
                            .setAudience(targetAudience)
                            .setNotBefore(Date.from(Instant.now().minusSeconds(10)));

            if (log.isDebugEnabled()) {
                log.debug("Signing jwt token");
            }

            final var jws = jwtb.signWith(SignatureAlgorithm.RS256, privKey).compact();

            if (log.isInfoEnabled()) {
                log.info("Request token: " + jws);
            }

            // build form body to embed client assertion into post request
            final var formBody =
                    new FormBody.Builder()
                            .add("grant_type", "client_credentials")
                            .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                            .add("client_assertion", jws)
                            .add("scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
                            .build();

            if (log.isDebugEnabled()) {
                log.debug("Getting idsutils client");
            }

            final var client = provider.getClient();
            final var request = new Request.Builder().url(dapsUrl + "/v2/token").post(formBody).build();

            if (log.isDebugEnabled()) {
                log.debug(String.format("Sending request to %s", dapsUrl + "/v2/token"));
            }

            final var jwtResponse = client.newCall(request).execute();

            if (!jwtResponse.isSuccessful()) {
                if (log.isDebugEnabled()) {
                    log.debug("DAPS request was not successful");
                }

                throw new IOException("Unexpected code " + jwtResponse);
            }

            final var responseBody = jwtResponse.body();

            if (responseBody == null) {
                throw new EmptyDapsResponseException("JWT response is null.");
            }
            final var jwtString = responseBody.string();

            if (log.isInfoEnabled()) {
                log.info("Response body of token request:\n{}", jwtString);
            }

            final var jsonObject = new JSONObject(jwtString);
            dynamicAttributeToken = jsonObject.getString("access_token");

            if (log.isInfoEnabled()) {
                log.info("Dynamic Attribute Token: " + dynamicAttributeToken);
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Error retrieving token: %s", e.getMessage()));
            }
        } catch (EmptyDapsResponseException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Something else went wrong: %s", e.getMessage()));
            }
        } catch (MissingCertExtensionException e) {
            if (log.isErrorEnabled()) {
                log.error("Certificate of the Connector is missing aki/ski extensions!");
            }
        }
        return dynamicAttributeToken;
    }

    /***
     * Beautyfies Hex strings and will generate a result later used to create the client id (XX:YY:ZZ).
     *
     * @param hexString HexString to be beautified
     * @return beautifiedHex result
     */
    private static String beautifyHex(final String hexString) {
        return Arrays.stream(split(hexString, 2))
                .map(s -> s + ":")
                .collect(Collectors.joining());
    }

    /***
     * Split string every n chars and return string array.
     *
     * @param src a string that will be split into multiple substrings
     * @param n number of chars per resulting string
     * @return Array of strings resulting from splitting the input string every n chars
     */
    public static String[] split(final String src, final int n) {
        var result = new String[(int) Math.ceil((double) src.length() / (double) n)];
        for (int i = 0; i < result.length; i++) {
            result[i] = src.substring(i * n, Math.min(src.length(), (i + 1) * n));
        }
        return result;
    }

}
