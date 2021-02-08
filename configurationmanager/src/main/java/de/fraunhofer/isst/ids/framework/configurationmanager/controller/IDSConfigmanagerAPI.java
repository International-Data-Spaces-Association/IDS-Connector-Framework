package de.fraunhofer.isst.ids.framework.configurationmanager.controller;

import de.fraunhofer.isst.ids.framework.daps.ConnectorMissingCertExtensionException;
import de.fraunhofer.isst.ids.framework.daps.DapsConnectionException;
import de.fraunhofer.isst.ids.framework.daps.DapsEmptyResponseException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * API Description for the {@link IDSConfigmanagerController}
 */
@RequestMapping("/api/ids/configmanager")
public interface IDSConfigmanagerAPI {

    @ApiOperation(value = "Unregisters the connector at the broker")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Unregistered connector at the broker successfully"),
            @ApiResponse(code = 500, message = "Unregistration at the broker failed")
    })
    @PostMapping(value = "/unregister")
    ResponseEntity<String> unregisterConnectorAtBroker(@RequestBody String brokerID)
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException;

    @ApiOperation(value = "Updates the connector at the broker")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated connector at the broker successfully"),
            @ApiResponse(code = 500, message = "Update at the broker failed")
    })
    @PostMapping(value = "/update")
    ResponseEntity<String> updateConnectorAtBroker(@RequestBody String brokerID)
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException;

    @ApiOperation(value = "Updates the configuration")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated config"),
            @ApiResponse(code = 500, message = "Error when updating config")
    })
    @PostMapping(value = "/config")
    ResponseEntity<String> updateConfig(@RequestBody ConnectorUpdateMessage connectorUpdateMessage) throws IOException;
}
