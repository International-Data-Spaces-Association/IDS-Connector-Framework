package de.fraunhofer.isst.ids.framework.configurationmanager.controller;

import java.io.IOException;

import de.fraunhofer.iais.eis.ConfigurationModel;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.communication.broker.IDSBrokerService;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationUpdateException;
import de.fraunhofer.isst.ids.framework.daps.ConnectorMissingCertExtensionException;
import de.fraunhofer.isst.ids.framework.daps.DapsConnectionException;
import de.fraunhofer.isst.ids.framework.daps.DapsEmptyResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller, handling incoming requests from a configmanager
 */
@RestController
public class IDSConfigmanagerController implements IDSConfigmanagerAPI {

    private static final Logger           LOGGER = LoggerFactory.getLogger(IDSConfigmanagerController.class);
    private ConfigurationContainer configContainer;
    private              IDSBrokerService idsBrokerService;

    /**
     * Autowired Constructor for the Controller
     *
     * @param configContainer     the ConfigurationContainer used for getting the current Configuration and updating it
     * @param brokerCommunication brokerCommunication for sending Messages to brokers
     */
    public IDSConfigmanagerController( ConfigurationContainer configContainer, IDSBrokerService brokerCommunication ) {
        this.idsBrokerService = brokerCommunication;
        this.configContainer = configContainer;
    }

    /**
     * Unregister the Connector at the broker which is sent to this endpoint
     *
     * @param brokerID ID of the broker to unregister at
     *
     * @return http response of the broker
     */
    @Override
    public ResponseEntity<String> unregisterConnectorAtBroker( String brokerID )
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException {
        try {
            idsBrokerService.unregisterAtBroker(brokerID);
            return new ResponseEntity<>("Connector is unregistered successfully", HttpStatus.OK);
        } catch( IOException e ) {
            LOGGER.error(e.getMessage());
            return new ResponseEntity<>("Unregistration at the broker failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch( DapsEmptyResponseException | DapsConnectionException | ConnectorMissingCertExtensionException e ) {
            throw e;
        }
    }

    /**
     * Register the Connector at the broker which is sent to this endpoint
     *
     * @param brokerID ID of the broker to update at
     *
     * @return http response of the broker
     */
    @Override
    public ResponseEntity<String> updateConnectorAtBroker( String brokerID )
            throws ConnectorMissingCertExtensionException, DapsConnectionException, DapsEmptyResponseException {
        try {
            idsBrokerService.updateSelfDescriptionAtBroker(brokerID);
            return new ResponseEntity<>("Connector self declaration is updated successfully", HttpStatus.OK);
        } catch( IOException e ) {
            LOGGER.error(e.getMessage());
            return new ResponseEntity<>("Update at the broker failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch( DapsEmptyResponseException | DapsConnectionException | ConnectorMissingCertExtensionException e ) {
            throw e;
        }
    }

    /**
     * Update the Configuration with the new Configurationmodel received in a {@link ConnectorUpdateMessage}
     *
     * @param connectorUpdateMessage the {@link ConnectorUpdateMessage}, containing a new configuration to be set
     *
     * @return OK: when config is used, BAD_REQUEST: if config cannot be used (eg wrong KeyStore etc)
     */
    @Override
    public ResponseEntity<String> updateConfig( ConnectorUpdateMessage connectorUpdateMessage ) {
        LOGGER.info("Received Configuration: " + connectorUpdateMessage.getConnectorJsonLd());
        try {
            var config = connectorUpdateMessage.getConnectorJsonLd();
            var connector = new Serializer().deserialize(config, ConfigurationModel.class);
            configContainer.updateConfiguration(connector);
        } catch( IOException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Could not deserialize the received configuration to ConfigurationModel class!");
        } catch( ConfigurationUpdateException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Could not load Key- and Truststore with new configuration, rejected Config!");
        }
        return ResponseEntity.ok("Received and applied new configuration!");
    }
}
