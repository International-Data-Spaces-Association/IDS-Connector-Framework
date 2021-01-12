package de.fraunhofer.isst.ids.framework.configurationmanager.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.security.PublicKey;

/**
 * Java DTO for ConnectorRegisterRequests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorRegisterRequest {
    private URI endpoint;
    private PublicKey connectorPublicKey;
    private PublicKey managerPublicKey;
}