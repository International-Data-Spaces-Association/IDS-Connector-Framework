package de.fraunhofer.isst.ids.framework.messaging.handling;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.DapsPublicKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Providing a MessageDispatcher as a bean, for autowiring.
 */
@Slf4j
@Component
public class MessageDispatcherProvider {

    /**
     * Make use of autowiring to get the parameters for the MessageDispatchers constructor and
     * create an Instance of MessageDispatcher with them.
     *
     * @param objectMapper for parsing objects from json
     * @param provider providing DAPS public key for checking DAT Tokens
     * @param configurationContainer container for current configuration
     * @param resolver resolver for finding the right handler for infomodel {@link de.fraunhofer.iais.eis.Message}
     * @return MessageDispatcher as Spring Bean
     */
    @Bean
    public MessageDispatcher provideMessageDispatcher(final ObjectMapper objectMapper,
                                                      final RequestHandlerResolver resolver,
                                                      final DapsPublicKeyProvider provider,
                                                      final ConfigurationContainer configurationContainer) {

        return new MessageDispatcher(objectMapper, resolver, provider, configurationContainer);
    }
}
