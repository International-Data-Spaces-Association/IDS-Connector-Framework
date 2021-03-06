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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(ConfigProperties.class)
@ConditionalOnClass({ConfigurationModel.class, Connector.class, KeyStoreManager.class})
public class ConfigProducer {

    static final Serializer SERIALIZER = new Serializer();

    ConfigurationContainer configurationContainer;
    ClientProvider clientProvider;

    /**
     * Load the ConfigurationModel from the location specified in the application.properties, initialize the KeyStoreManager.
     *
     * @param properties the {@link ConfigProperties} parsed from an application.properties file
     */
    public ConfigProducer(final ConfigProperties properties) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Loading configuration from %s", properties.getPath()));
            }

            String config;

            //load config jsonLD from given path
            if (Paths.get(properties.getPath()).isAbsolute()) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Loading config from absolute Path %s", properties.getPath()));
                }

                final var fis = new FileInputStream(properties.getPath());
                config = IOUtils.toString(fis);
                fis.close();
            } else {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Loading config from classpath: %s", properties.getPath()));
                }

                final InputStream configurationStream = new ClassPathResource(properties.getPath()).getInputStream();
                config = IOUtils.toString(configurationStream);
                configurationStream.close();
            }

            if (log.isInfoEnabled()) {
                log.info("Importing configuration from file");
            }

            //deserialize to ConfigurationModel
            final var configModel = SERIALIZER.deserialize(config, ConfigurationModel.class);
            if (log.isInfoEnabled()) {
                log.info("Initializing KeyStoreManager");
            }

            //initialize the KeyStoreManager with Key and Truststore locations in the ConfigurationModel
            final var manager = new KeyStoreManager(configModel, properties.getKeyStorePassword().toCharArray(), properties.getTrustStorePassword().toCharArray(), properties.getKeyAlias());
            if (log.isInfoEnabled()) {
                log.info("Imported existing configuration from file.");
            }
            configurationContainer = new ConfigurationContainer(configModel, manager);
            if (log.isInfoEnabled()) {
                log.info("Creating ClientProvider");
            }
            //create a ClientProvider
            clientProvider = new ClientProvider(configurationContainer);
            configurationContainer.setClientProvider(clientProvider);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Configuration cannot be parsed!");
                log.error(e.getMessage(), e);
            }
        } catch (KeyStoreManagerInitializationException e) {
            if (log.isErrorEnabled()) {
                log.error("KeyStoreManager could not be initialized!");
                log.error(e.getMessage(), e);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            if (log.isErrorEnabled()) {
                log.error("ClientProvider could not be initialized!");
                log.error(e.getMessage(), e);
            }
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
