package de.fraunhofer.isst.ids.framework.daps;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.Key;

/**
 * Get Daps Tokens and Daps Public Key from specified URLs.
 * Spring Component Wrapper for TokenManagerService
 */
@Service
public class TokenProvider implements DapsTokenProvider, DapsPublicKeyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenProvider.class);

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
    public TokenProvider(ConfigurationContainer configurationContainer, ClientProvider clientProvider) {
        this.configurationContainer = configurationContainer;
        this.clientProvider = clientProvider;
    }

    /**
     * Return the DAT as a Infomodel {@link DynamicAttributeToken}
     *
     * @return acquire a new DAPS Token and return it as a {@link DynamicAttributeToken}
     */
    @Override
    public DynamicAttributeToken getDAT()
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException {
        return new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(provideDapsToken())
                .build();
    }

    /**
     * Return the DAT as JWT String
     *
     * @return acquire a new DAPS Token and return the JWT String value
     */
    @Override
    public String provideDapsToken()
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException {
        LOGGER.debug(String.format("Get a new DAT Token from %s", dapsUrl));
        return TokenManagerService.acquireToken(configurationContainer, clientProvider, dapsUrl);
    }

    /**
     * Return the Public Key from the DAPS JWKS
     *
     * @return the Public Key from the DAPS (used for validating Tokens of incoming Messages)
     */
    @Override
    public Key providePublicKey() {
        if (publicKey == null) {
            LOGGER.debug(String.format("Getting public key from %s!", dapsKeyUrl));
            getPublicKey();
        }
        LOGGER.debug("Provide public key!");
        return publicKey;
    }

    /**
     * Pull the Public Key from the DAPS and save it in the publicKey variable
     */
    private void getPublicKey() {
        try {
            //request the jwks
            LOGGER.debug(String.format("Getting json web keyset from %s", dapsKeyUrl));
            OkHttpClient client = clientProvider.getClient();
            Request request = new Request.Builder()
                    .url(dapsKeyUrl)
                    .build();
            Response response = client.newCall(request).execute();
            String keySetJSON = response.body().string();

            //parse response as JsonWebKeySet
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(keySetJSON);
            JsonWebKey key = jsonWebKeySet.getJsonWebKeys().stream().filter(k -> k.getKeyId().equals(keyKid)).findAny().orElse(null);
            if(key != null){
                this.publicKey = key.getKey();
            }else{
                LOGGER.warn(String.format("Could not get JsonWebKey with kid %s from received KeySet! PublicKey is null!", keyKid));
            }
        } catch (IOException e) {
            LOGGER.warn(String.format("Could not get key from %s!", dapsKeyUrl));
            LOGGER.warn(e.getMessage(), e);
        } catch (JoseException e) {
            LOGGER.warn("Could not create JsonWebKeySet from response!");
            LOGGER.warn(e.getMessage(), e);
        }
    }
}
