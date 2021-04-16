package de.fraunhofer.isst.ids.framework.configuration;

import de.fraunhofer.iais.eis.ConfigurationModelBuilder;
import de.fraunhofer.iais.eis.ConnectorDeployMode;
import de.fraunhofer.iais.eis.ConnectorStatus;
import de.fraunhofer.iais.eis.LogLevel;
import org.junit.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test loading a KeyStoreManager from existing Key and Truststore files
 */
public class KeyStoreManagerTest {

    /**
     * Try to initialize a KeyStoreManager
     * @throws KeyStoreManagerInitializationException if an error occurs while initializing the KeyStoreManager
     */
    @Test
    public void keyStoreLoads() throws KeyStoreManagerInitializationException {
        final var model = new ConfigurationModelBuilder()
                ._keyStore_(URI.create("file:///isst-testconnector.p12"))
                ._trustStore_(URI.create("file:///isst-testconnector-truststore.jks"))
                ._configurationModelLogLevel_(LogLevel.MINIMAL_LOGGING)
                ._connectorDeployMode_(ConnectorDeployMode.TEST_DEPLOYMENT)
                ._connectorStatus_(ConnectorStatus.CONNECTOR_OFFLINE)
                .build();
        final var manager = new KeyStoreManager(model, "password".toCharArray(), "password".toCharArray(), "1");
        assertNotNull(manager.getPrivateKey());
        assertNotNull(manager.getCert());
        assertNotNull(manager.getTrustManager());
    }

}
