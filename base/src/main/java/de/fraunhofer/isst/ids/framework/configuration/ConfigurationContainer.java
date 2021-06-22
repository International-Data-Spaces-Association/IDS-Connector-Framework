package de.fraunhofer.isst.ids.framework.configuration;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import de.fraunhofer.iais.eis.ConfigurationModel;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * The ConfigurationContainer wraps the current configuration with the respective key- and truststore,
 * and manages changes of the configuration.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigurationContainer {

    ConfigurationModel configurationModel;
    KeyStoreManager keyStoreManager;
    ClientProvider clientProvider;

    /**
     * Create a ConfigurationContainer with a ConfigurationModel and KeyStoreManager.
     *
     * @param configurationModel the initial {@link ConfigurationModel} of the Connector
     * @param keyStoreManager the KeyStoreManager, managing Key- and Truststore of the Connector
     */
    public ConfigurationContainer(final ConfigurationModel configurationModel, final KeyStoreManager keyStoreManager) {
        this.configurationModel = configurationModel;
        this.keyStoreManager = keyStoreManager;
    }

    /**
     * Setter for a {@link ClientProvider}.
     *
     * @param provider the ClientProvider
     */
    public void setClientProvider(final ClientProvider provider) {
        this.clientProvider = provider;
    }

    /**
     * Getter for the {@link ConfigurationModel}.
     *
     * @return the managed ConfigurationModel
     */
    public ConfigurationModel getConfigModel() {
        return this.configurationModel;
    }

    /**
     * Getter for the {@link Connector} (ConnectorDescription of the {@link ConfigurationModel}).
     *
     * @return the ConnectorDescription of the managed ConfigurationModel
     */
    public Connector getConnector() {
        return this.configurationModel.getConnectorDescription();
    }

    /**
     * Getter for the {@link KeyStoreManager}.
     *
     * @return the keymanager for Key- and Truststore defined by the ConfigurationModel.
     */
    public KeyStoreManager getKeyManager() {
        return this.keyStoreManager;
    }

    /**
     * Update the ConfigurationContainer with a new {@link ConfigurationModel}, rebuild the KeyStoreManager with
     * new Configuration in the process.
     *
     * @param configurationModel the new configurationModel that replaces the current one
     * @throws ConfigurationUpdateException when the Key- and Truststore in the new Connector cannot be initialized
     */
    public void updateConfiguration(final ConfigurationModel configurationModel) throws ConfigurationUpdateException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Updating the current configuration");
            }

            final var manager = rebuildKeyStoreManager(configurationModel);

            if (log.isDebugEnabled()) {
                log.debug("KeyStoreManager rebuilt");
            }

            this.configurationModel = configurationModel;
            this.keyStoreManager = manager;
            if (clientProvider != null) {
                clientProvider.updateConfig();
                log.debug("ClientProvider updated!");
            }
        } catch (KeyStoreManagerInitializationException e) {
            if (log.isErrorEnabled()) {
                log.error("Configuration could not be updated! Keeping old configuration!");
            }
            throw new ConfigurationUpdateException(e.getMessage(), e.getCause());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            if (log.isErrorEnabled()) {
                log.error("New Key- or Truststore could not be initialized! Keeping old configuration!");
                log.error(e.getMessage(), e);
            }
            throw new ConfigurationUpdateException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Rebuild the {@link KeyStoreManager} with a given configuration.
     *
     * @param configurationModel the current ConfigurationModel
     * @return the newly built KeyStoreManager
     * @throws KeyStoreManagerInitializationException when the new KeyStoreManager cannot be initialized
     */
    private KeyStoreManager rebuildKeyStoreManager(final ConfigurationModel configurationModel)
            throws KeyStoreManagerInitializationException {
        if (log.isDebugEnabled()) {
            log.debug("Creating a new KeyStoreManager using current configuration");
        }
        final var keyPw = keyStoreManager.getKeyStorePw();
        final var trustPw = keyStoreManager.getTrustStorePw();
        final var alias = keyStoreManager.getKeyAlias();
        return new KeyStoreManager(configurationModel, keyPw, trustPw, alias);
    }
}
