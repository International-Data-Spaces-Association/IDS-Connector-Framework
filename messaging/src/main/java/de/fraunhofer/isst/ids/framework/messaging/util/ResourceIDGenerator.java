package de.fraunhofer.isst.ids.framework.messaging.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

/**
 * Utility for generating Resource IDs for infomodel builders.
 */
@UtilityClass
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class ResourceIDGenerator {

    static String AUTOGEN = "https://w3id.org/idsa/autogen";

    /**
     * Create an URI with callerClazz name and random uuid in path (used as ID URIs).
     *
     * @param callerClazz class for which the randomURI should be generated
     * @return a random URI ID
     */
    public static URI randomURI(final Class<?> callerClazz) {
        try {
            return new URI(String.format("%s/%s/%s", AUTOGEN, callerClazz.getSimpleName(), UUID.randomUUID()));
        } catch (URISyntaxException var2) {
            throw new RuntimeException(var2);
        }
    }
}

