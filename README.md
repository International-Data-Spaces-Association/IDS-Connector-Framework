***

<h3 align="center">
  <a href="https://github.com/International-Data-Spaces-Association/IDS-Messaging-Services">
    -- End-Of-Support: 23.07.2021 -- Please use the IDS-Messaging-Services! --
  </a>
</h3>

***

<p align="center">
<img src="ids-framework-logo.png">
</p>

<p align="center">
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/blob/development/LICENSE">License</a> •
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/issues">Issues</a> •
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/discussions">Discussions</a> •
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/wiki">Wiki</a> •
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/blob/development/CONTRIBUTING.md">Contributing</a> •
<a href="https://github.com/International-Data-Spaces-Association/IDS-Connector-Framework/blob/development/CODE_OF_CONDUCT.md">Code of Conduct</a>
</p>

# IDS Framework

The IDS Framework aims to simplify the development of an IDS Connector.
The Framework provides basic functionality encompassing creation and handling of IDS messages and communication to the DAPS and Brokers. However,
different Connectors can have various requirements regarding protocols and data endpoint types for data exchange. Therefore, 
the final implementation of the data exchange remains for applications leveraging the Framework.  
It will be developed continuously to improve and simplify the development of IDS Connectors. 

## Table of Contents

<!-- TOC -->

- [IDS Framework](#ids-framework)
    - [Table of Contents](#table-of-contents)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Integration into a Java Project](#integration-into-a-java-project)
    - [Usage of the IDS Framework](#usage-of-the-ids-framework)
        - [Write and import a configuration file](#write-and-import-a-configuration-file)
        - [Message Handling](#message-handling)
    - [Versioning](#versioning)
    - [License](#license)

<!-- /TOC -->
---

## Prerequisites

- The Framework uses asymmetric encryption concepts and requires public and private key of the Connector instance. 
- Furthermore, it utilizes the IDS Configurationmodel which was developed within the IDS Informationmodel. Therefor a 
`configmodel.json` file should exist to load the configuration of the Connector into the Configurationmodel. An example of that can be seen inside the Demo Connector project. 
The configuration file should reference the key and trust store.

---

## Installation

It is suggested to use the latest release from the Fraunhofer ISST Maven Repository. In case you want to build 
it by yourself locally, you have to execute the following command:

```
./mvnw -P dev clean install
```

## Integration into a Java Project
In order to use the Framework inside a new java application add the `isst-nexus-public` repository and the dependency to 
the `pom.xml` file.

```xml
<!-- IDS Framework -->
<dependency>
    <groupId>de.fraunhofer.isst.ids.framework</groupId>
    <artifactId>base</artifactId>
    <version>${ids-framework.version}</version>
</dependency>

<repositories>
    <repository>
        <id>isst-nexus-public</id>
        <name>isst-public</name>
        <url>https://mvn.ids.isst.fraunhofer.de/nexus/repository/ids-public/</url>
    </repository>
</repositories>

```

---

## Usage of the IDS Framework

For a detailed description of the usage of the IDS Framework and its modules see the Wiki pages of this project.

### MVP Framework Demo

To create a minimum viable IDS Connector, you only have to do few steps. Precondition is a Spring Boot project.

1. Add the ``@ComponentScan`` annotation to the SpringBoot Application and scan the framework messaging packages
    ```java
    @ComponentScan({
            "de.fraunhofer.isst.ids.framework.messaging.spring.controller",
            "de.fraunhofer.isst.ids.framework.messaging.spring",
            "<APPLICATION-PACKAGE>"
    })
    ```
   
2. Implement a MessageHandler for the instances of RequestMessage your Connector should be able to process.
   An example for a MessageHandler (for RequestMessageImpl) is given below.
    ````java
    @Component
    @SupportedMessageType(RequestMessageImpl.class)
    public class RequestMessageHandler implements MessageHandler<RequestMessageImpl> {
    
        private static final Logger LOGGER = LoggerFactory.getLogger(RequestMessageHandler.class);
    
        private final Connector connector;
        private final TokenProvider provider;
    
        public RequestMessageHandler(ConfigurationContainer container, TokenProvider provider){
            this.connector = container.getConnector();
            this.provider = provider;
        }

        @Override
        public MessageResponse handleMessage(RequestMessageImpl requestMessage, MessagePayload messagePayload) throws MessageHandlingException {
            try {
                LOGGER.info("Received a RequestMessage!");
                // Just return the received payload as plain string
                String receivedPayload = IOUtils.toString(messagePayload.getUnderlyingInputStream(), StandardCharsets.UTF_8.name()) + " - from RequestMessage!";
                var message = new ResponseMessageBuilder()
                        ._securityToken_(provider.getTokenJWS())
                        ._correlationMessage_(requestMessage.getId())
                        ._issued_(Util.getGregorianNow())
                        ._issuerConnector_(connector.getId())
                        ._modelVersion_(this.connector.getOutboundModelVersion())
                        ._senderAgent_(connector.getId())
                        .build();
                return BodyResponse.create(message, receivedPayload);
            } catch (Exception e) {
                return ErrorResponse.withDefaultHeader(RejectionReason.INTERNAL_RECIPIENT_ERROR, e.getMessage(), connector.getId(), connector.getOutboundModelVersion());
            }
        }
    }
    ````

---

## Versioning

The IDS Framework uses the [SemVer](https://semver.org/) for versioning. The release Versions are tagged with their respective version.

---

## License 

This project is licensed under the Apache License 2.0 - see the LICENSE.md file for details.

---
