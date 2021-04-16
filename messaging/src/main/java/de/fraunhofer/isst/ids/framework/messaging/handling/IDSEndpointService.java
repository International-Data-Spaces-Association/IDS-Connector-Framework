package de.fraunhofer.isst.ids.framework.messaging.handling;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Service for modifying the route of {@link IDSController} in a running application.
 */
@Slf4j
@Service
public class IDSEndpointService {

    private IDSController idsController;
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Use <code>/api/ids/data</code> and <code>/api/ids/infrastructure</code> routes as default mappings.
     *
     * @param idsController the {@link IDSController} which will be mapped
     * @param requestMappingHandlerMapping for managing Springs http route mappings
     */
    @Autowired
    public IDSEndpointService(final IDSController idsController,
                              final RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.idsController = idsController;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        log.debug("Adding default mappings");
        addMapping("/api/ids/data");
        addMapping("/api/ids/infrastructure");
    }

    /**
     * Add another endpoint to the IDSController.
     *
     * @param url the url for which a route to {@link IDSController} should be added
     */
    public void addMapping(final String url) {
        log.debug(String.format("Adding a mapping for url %s", url));
        final var requestMappingInfo = getRequestMappingInfo(url);
        try {
            requestMappingHandlerMapping.registerMapping(requestMappingInfo, idsController, IDSController.class.getDeclaredMethod("handleIDSMessage", HttpServletRequest.class));
        } catch (NoSuchMethodException e) {
            //cannot happen, method exists
            log.error("IDSController could not be found for mapping route!");
        }
    }

    /**
     * Remove an endpoint from the IDSController.
     *
     * @param url the url for which the {@link IDSController} should be unmapped for (RequestMappingInfo is deleted)
     */
    public void removeMapping(final String url) {
        log.debug(String.format("Remove mapping for url %s", url));
        final var requestMappingInfo = getRequestMappingInfo(url);
        requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
    }

    @NotNull
    private RequestMappingInfo getRequestMappingInfo(final String url) {
        return RequestMappingInfo
                .paths(url)
                .methods(RequestMethod.POST)
                .consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
                .produces(MediaType.MULTIPART_FORM_DATA_VALUE)
                .build();
    }
}
