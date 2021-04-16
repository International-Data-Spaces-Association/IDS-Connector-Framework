package de.fraunhofer.isst.ids.framework.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import de.fraunhofer.iais.eis.ConfigurationModel;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Parse the configuration and initialize the key- and truststores specified in the {@link ConfigProperties} via
 * Spring application.properties.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ConfigProperties.class)
@ConditionalOnClass({ConfigurationModel.class, Connector.class, KeyStoreManager.class})
public class ConfigProducer {

    private ConfigurationContainer configurationContainer;
    private ClientProvider clientProvider;

    /**
     * Load the ConfigurationModel from the location specified in the application.properties, initialize the KeyStoreManager.
     *
     * @param serializer an infomodel serializer for reading the jsonLD configuration
     * @param properties the {@link ConfigProperties} parsed from an application.properties file
     */
    public ConfigProducer(final Serializer serializer, final ConfigProperties properties) {
        try {
            log.debug(String.format("Loading configuration from %s", properties.getPath()));
            String config;
            //load config jsonLD from given path
            if (Paths.get(properties.getPath()).isAbsolute()) {
                log.info(String.format("Loading config from absolute Path %s", properties.getPath()));
                final var fis = new FileInputStream(properties.getPath());
                config = IOUtils.toString(fis);
                fis.close();
            } else {
                log.info(String.format("Loading config from classpath: %s", properties.getPath()));
                final InputStream configurationStream = new ClassPathResource(properties.getPath()).getInputStream();
                config = IOUtils.toString(configurationStream);
                configurationStream.close();
            }
            log.info("Importing configuration from file");
            //deserialize to ConfigurationModel
            final var configModel = serializer.deserialize(config, ConfigurationModel.class);
            log.info("Initializing KeyStoreManager");
            //initialize the KeyStoreManager with Key and Truststore locations in the ConfigurationModel
            final var manager = new KeyStoreManager(configModel, properties.getKeyStorePassword().toCharArray(), properties.getTrustStorePassword().toCharArray(), properties.getKeyAlias());
            log.info("Imported existing configuration from file.");
            configurationContainer = new ConfigurationContainer(configModel, manager);
            log.info("Creating ClientProvider");
            //create a ClientProvider
            clientProvider = new ClientProvider(configurationContainer);
            configurationContainer.setClientProvider(clientProvider);
        } catch (IOException e) {
            log.error("Configuration cannot be parsed!");
            log.error(e.getMessage(), e);
        } catch (KeyStoreManagerInitializationException e) {
            log.error("KeyStoreManager could not be initialized!");
            log.error(e.getMessage(), e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("ClientProvider could not be initialized!");
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Provide the ConfigurationContainer as Bean for autowiring.
     *
     * @return the imported {@link ConfigurationModel} as bean for autowiring
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigurationContainer getConfigContainer() {
        return configurationContainer;
    }

    /**
     * Provide the ClientProvider as bean for autowiring.
     *
     * @return the created {@link ClientProvider} as bean for autowiring
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientProvider getClientProvider() {
        return clientProvider;
    }

}
