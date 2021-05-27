package de.fraunhofer.isst.ids.framework.daps;

import java.io.IOException;
import java.security.Key;
import java.util.Objects;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Get Daps Tokens and Daps Public Key from specified URLs.
 * Spring Component Wrapper for TokenManagerService
 */
@Slf4j
@Service
public class TokenProvider implements DapsTokenProvider, DapsPublicKeyProvider {

    private ConfigurationContainer configurationContainer;
    private ClientProvider clientProvider;
    private Key publicKey;

    @Value("${daps.key.url}")
    private String dapsKeyUrl;

    @Value("${daps.token.url}")
    private String dapsUrl;

    @Value("${daps.kid.url:default}")
    private String keyKid;

    /**
     *
     *
     * @param configurationContainer the {@link ConfigurationContainer} managing the connector configuration
     * @param clientProvider the {@link ClientProvider} providing HttpClients using the current connector configuration
     */
    @Autowired
    public TokenProvider(final ConfigurationContainer configurationContainer,
                         final ClientProvider clientProvider) {
        this.configurationContainer = configurationContainer;
        this.clientProvider = clientProvider;
    }

    /**
     * Return the DAT as a Infomodel {@link DynamicAttributeToken}.
     *
     * @return acquire a new DAPS Token and return it as a {@link DynamicAttributeToken}
     */
    @Override
    public DynamicAttributeToken getDAT() {
        return new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(provideDapsToken())
                .build();
    }

    /**
     * Return the DAT as JWT String.
     *
     * @return acquire a new DAPS Token and return the JWT String value
     */
    @Override
    public String provideDapsToken() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Get a new DAT Token from %s", dapsUrl));
        }
        return TokenManagerService.acquireToken(configurationContainer, clientProvider, dapsUrl);
    }

    /**
     * Return the Public Key from the DAPS JWKS.
     *
     * @return the Public Key from the DAPS (used for validating Tokens of incoming Messages)
     */
    @Override
    public Key providePublicKey() {
        if (publicKey == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Getting public key from %s!", dapsKeyUrl));
            }
            getPublicKey();
        }
        if (log.isDebugEnabled()) {
            log.debug("Provide public key!");
        }
        return publicKey;
    }

    /**
     * Pull the Public Key from the DAPS and save it in the publicKey variable.
     */
    private void getPublicKey() {
        try {
            //request the jwks
            if (log.isDebugEnabled()) {
                log.debug(String.format("Getting json web keyset from %s", dapsKeyUrl));
            }

            final var client = clientProvider.getClient();
            final var request = new Request.Builder().url(dapsKeyUrl).build();
            final var response = client.newCall(request).execute();
            final var keySetJSON = Objects.requireNonNull(response.body()).string();

            //parse response as JsonWebKeySet
            final var jsonWebKeySet = new JsonWebKeySet(keySetJSON);
            final var key = jsonWebKeySet.getJsonWebKeys().stream().filter(k -> k.getKeyId().equals(keyKid)).findAny().orElse(null);
            if (key != null) {
                this.publicKey = key.getKey();
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Could not get JsonWebKey with kid %s from received KeySet! PublicKey is null!", keyKid));
                }
            }
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Could not get key from %s!", dapsKeyUrl));
                log.warn(e.getMessage(), e);
            }
        } catch (JoseException e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not create JsonWebKeySet from response!");
                log.warn(e.getMessage(), e);
            }
        }
    }
}
