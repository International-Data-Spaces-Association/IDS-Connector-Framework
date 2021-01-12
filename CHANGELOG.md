# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security 

## [4.0.0] - 2021-01-12
### Changed
- IDSController accepts ResponseMessages as incoming IDS Messages
- ConfigurationModel json can be read from absolute path
- Use IDSTruststore and system truststore as trustmanager
- `IDSHttpService` now checks the DAT of a response, `HttpService` can be used for plain HTTP requests
### Removed
- `messaging-core` and `spring-starter` modules removed, merged to `base` and `messaging` modules
## [3.2.3] - 2020-10-19
### Fixed
- Resolving URI Paths, when loading Key- and Truststore from Classpath
### Changed
- JSON Web Key Set parsing in TokenProvider (specify kid with "daps.kid.url" in application properties, or use kid "default")
## [3.2.2] - 2020-10-14
### Fixed
- Fixed a bug in the ConfigurationContainer, which allowed wrong KeyStore paths to be accepted
## [3.2.1] - 2020-09-25
### Fixed
- Parse incoming messages as generic Message, allowing NotificationMessages to be handled by IDSController
## [3.2.0] - 2020-09-23
### Added
- ConfigurationContainer, managing Configuration and rebuilding Key-/Truststore on changes (when using `spring-starter`:
autowire a ConfigurationContainer instead of Connector, ConfigurationModel or KeyStoreManager from now on)
- Add utility for plain GET requests in IDSHttpCommunication/IDSHttpService
### Changed
- Multipart Responses from IDSController in `messaging-spring` now have order _header, payload_
- If ConnectorDeployMode in the current Configuration is set to _TEST_DEPLOYMENT_: DAT Tokens of incoming messages
are ignored
### Removed
- Removed custom DAT token checking using `@DapsVerification` annotation
### Fixed
- Use standard URI Path to File scheme, when parsing Key- and Truststore
## [3.1.0] - 2020-08-24
### Added
- IDSEndpointService for Dynamic configuration of the MessageHandlings Endpoint (in messaging-spring)
### Changed
- TokenManagerService now uses Daps V2 (change the DAPS URL in your application for the URL to a DAPS using v2)
### Removed
- dropped support for DAPS v1 (see Changed)
## [3.0.1] - 2020-08-13
### Fixed
- BrokerCommunication now uses Connector ID instead of ConfigurationModel ID
### Changed
- IDSHttpCommunication send() method now returns the response instead of a boolean (which just returned true if a response was received)
- InfomodelMessageBuilder now creates Messages using static methods instead of builder pattern
### Added
- Added spring-starter module for fast creation of IDS Connectors. The spring starter modules contains
the base, messaging-spring and configmanager modules, provides simple ways to import a configuration from a json
 file and wraps the functionality of the base module (like comminicating with a broker) into Spring services which
 can be autowired and used directly instead of having to create instances of BrokerCommunication for sending
 messages to brokers.
- Add a MultipartStringParser to base module utilities, for parsing the parts from Multipart responses when they are just
provided as a String.
## [3.0.0] - 2020-08-11
### Changed
- Update to Infomodel 4 (ConnectorAvailableMessage&ConnectorUpdateMessage/ConnectorUnavailableMessage&ConnectorInactiveMessage were merged)
- Calls to InactiveAtBroker and RegisterAtBroker have to be changed to Unregister at Broker and UpdateSelfDescriptionAtBroker
- ConfigurationModel from Infomodel 4 replaces the old ConfigModel. Applications using earlier versions of this framework must
change their configuration to an Infomodel ConfigurationModel object when updating to Framework 3.0.0
### Added
- Utility Method to send Resource(Un)AvailableMessage to a Broker
- Utility Method to send UpdateMessage to a List of Brokers asynchronously
- Simple ConfigManager registration in configurationmanager module
## [2.0.13] - 2020-07-16
### Changed
- MessageResponse now contains header (ResponseMessage/NotificationMessage) and payload
- Projects using Messagehandling have to modify their MessageResponses when updating to this version
## [2.0.12] - 2020-06-23
### Added
- use version 3.1.1 of Infomodel serializer and Validator
## [<2.0.12]
_Versions before 2.0.12 are not available in the gitlab repository._

[Unreleased]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v4.0.0...dev
[4.0.0]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.2.3...v4.0.0
[3.2.3]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.2.2...v3.2.3
[3.2.2]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.2.1...v3.2.2
[3.2.1]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.2.0...v3.2.1
[3.2.0]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.1.0...v3.2.0
[3.1.0]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.0.1...v3.1.0
[3.0.1]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v3.0.0...v3.0.1
[3.0.0]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v2.0.13...v3.0.0
[2.0.13]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/compare/v2.0.12...v2.0.13
[2.0.12]: https://gitlab.cc-asp.fraunhofer.de/fhg-isst-ids/ids-framework/-/tree/v2.0.12