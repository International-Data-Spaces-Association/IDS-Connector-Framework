package de.fraunhofer.isst.ids.framework.util;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.configuration.KeyStoreManager;
import de.fraunhofer.isst.ids.framework.configuration.KeyStoreManagerInitializationException;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test generating an OkHttpClient from a KeyStoreManager
 */
public class IDSUtilsTest {

    /**
     * generate an OkHttpClient with a ConfigModel and KeyStoreManager
     * @throws KeyManagementException if there is an error with any configured key when building an {@link OkHttpClient}
     * @throws NoSuchAlgorithmException if the cryptographic is unknown when building an {@link OkHttpClient}
     * @throws IOException if ConfigurationModel cannot be serialized to JsonLD
     * @throws KeyStoreManagerInitializationException if the KeyStoreManager cannot be initialized
     */
    @Test
    public void testGetClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreManagerInitializationException, IOException {
        final var connectorEndpointBuilder = new ConnectorEndpointBuilder();
        connectorEndpointBuilder._accessURL_(URI.create("https://example.com"));

        final var connector = new BaseConnectorBuilder()
                ._curator_(URI.create("https://example.com"))
                ._inboundModelVersion_(new ArrayList<>(List.of("3.1.2-SNAPSHOT")))
                ._maintainer_(URI.create("https://example.com"))
                ._outboundModelVersion_("3.1.2-SNAPSHOT")
                ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
                ._hasDefaultEndpoint_(connectorEndpointBuilder.build())
                .build();
        final var model = new ConfigurationModelBuilder()
                ._keyStore_(URI.create("file:///isst-testconnector.p12"))
                ._trustStore_(URI.create("file:///isst-testconnector-truststore.jks"))
                ._configurationModelLogLevel_(LogLevel.MINIMAL_LOGGING)
                ._connectorDeployMode_(ConnectorDeployMode.TEST_DEPLOYMENT)
                ._connectorStatus_(ConnectorStatus.CONNECTOR_OFFLINE)
                ._connectorDescription_(connector)
                .build();
        System.out.println(new Serializer().serialize(model));
        final var manager = new KeyStoreManager(model, "password".toCharArray(), "password".toCharArray(), "1");
        final var container = new ConfigurationContainer(model, manager);
        final var provider = new ClientProvider(container);
        final var client = provider.getClient();
    }
}
