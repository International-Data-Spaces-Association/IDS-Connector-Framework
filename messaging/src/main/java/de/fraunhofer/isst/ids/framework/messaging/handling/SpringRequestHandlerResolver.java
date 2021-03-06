package de.fraunhofer.isst.ids.framework.messaging.handling;

import java.util.Arrays;
import java.util.Optional;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessageHandler;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.SupportedMessageType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Resolver that uses the Spring dependency injection mechanism to find the matching message handler.
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SpringRequestHandlerResolver implements RequestHandlerResolver {

    private final ApplicationContext appContext;

    @Data
    @RequiredArgsConstructor
    private static class Tuple<K, V> {
        final K key;
        final V value;
    }

    /**
     * Resolve a MessageHandler instance that is able to handle the given messageType parameter.
     *
     * @param messageType type of the message to handle
     * @param <R> generic constraint to get a subtype of RequestMessage
     * @return optionally found matching handler instance
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R extends Message> Optional<MessageHandler<R>> resolveHandler(final Class<R> messageType) {
        //TODO: check if still one handler can support multiple message tpyes (SupportedMessageType)
        return Arrays.stream(appContext.getBeanNamesForAnnotation(SupportedMessageType.class))
                .flatMap(s -> Optional.ofNullable(appContext.findAnnotationOnBean(s, SupportedMessageType.class)).stream().map(msg -> new Tuple<>(s, msg)))
                .filter(t -> t.value.value().equals(messageType))
                .<MessageHandler<R>>map(t -> appContext.getBean(t.key, MessageHandler.class))
                .findFirst();
    }

}
