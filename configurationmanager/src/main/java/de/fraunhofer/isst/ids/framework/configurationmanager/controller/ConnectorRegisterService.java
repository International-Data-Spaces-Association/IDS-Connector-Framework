package de.fraunhofer.isst.ids.framework.configurationmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.util.ClientProvider;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;

/**
 * Service for registering at ConfigurationManagers
 */
@Service
public class ConnectorRegisterService {

    @Autowired
    ConfigurationContainer configurationContainer;

    @Autowired
    ClientProvider clientProvider;

    /**
     * Register the Connector at the ConfigManager at managerURI
     *
     * @param managerURI URI of the ConfigManager to register at
     * @param ownURI URI of the own configmanager endpoint
     * @return Body of the Response from the manager
     * @throws IOException when the request cannot be serialized
     */
    public String registerConnector(String managerURI, URI ownURI) throws IOException {
        var request = new ConnectorRegisterRequest();
        request.setEndpoint(ownURI);
        var mapper = new ObjectMapper();
        return clientProvider.getClient().newCall(
                new Request.Builder()
                        .url(managerURI)
                        .post(RequestBody.create(
                                mapper.writeValueAsString(request),
                                MediaType.parse("application/json")
                        ))
                        .build())
                .execute().body().string();
    }
}
