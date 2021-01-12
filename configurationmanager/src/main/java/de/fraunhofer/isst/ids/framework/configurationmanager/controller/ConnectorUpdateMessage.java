package de.fraunhofer.isst.ids.framework.configurationmanager.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java DTO for Updates of the Configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorUpdateMessage {

    private String connectorJsonLd;
    private String jwt;

}
