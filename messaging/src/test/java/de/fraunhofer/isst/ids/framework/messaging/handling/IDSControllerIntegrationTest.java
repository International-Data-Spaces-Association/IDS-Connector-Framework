package de.fraunhofer.isst.ids.framework.messaging.handling;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.configuration.ConfigurationContainer;
import de.fraunhofer.isst.ids.framework.daps.DapsTokenProvider;
import de.fraunhofer.isst.ids.framework.messaging.handling.model.TestPayload;
import de.fraunhofer.isst.ids.framework.messaging.model.responses.BodyResponse;
import de.fraunhofer.isst.ids.framework.messaging.util.ResourceIDGenerator;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import de.fraunhofer.isst.ids.framework.util.MultipartStringParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static de.fraunhofer.isst.ids.framework.messaging.handling.IDSControllerIntegrationTest.TestContextConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the IDS Messaging module
 */
@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = {IDSController.class, TestContextConfiguration.class})
public class IDSControllerIntegrationTest {

    @Configuration
    static class TestContextConfiguration {

        @Bean
        public Serializer getSerializer(){
            return new Serializer();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDSController idsController;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @MockBean
    private MessageDispatcher messageDispatcher;

    @MockBean
    private ConfigurationContainer configurationContainer;

    @MockBean
    private Connector connector;

    @Autowired
    private Serializer serializer;

    @MockBean
    private DapsTokenProvider provider;

    /**
     * Test if IDSController returns a valid multipart response when an infomodel message is sent
     */
    @Test
    public void testGetValidMultipartReturn() throws Exception {

        RequestMappingInfo requestMappingInfo = RequestMappingInfo
                .paths("/api/ids/data")
                .methods(RequestMethod.POST)
                .consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
                .produces(MediaType.MULTIPART_FORM_DATA_VALUE)
                .build();
        requestMappingHandlerMapping.registerMapping(requestMappingInfo, idsController, IDSController.class.getDeclaredMethod("handleIDSMessage", HttpServletRequest.class));

        Mockito.when(configurationContainer.getConnector()).thenReturn(connector);
        Mockito.when(connector.getId()).thenReturn(new URL("https://isst.fraunhofer.de/ids/dc967f79-643d-4780-9e8e-3ca4a75ba6a5").toURI());
        Mockito.when(connector.getOutboundModelVersion()).thenReturn("1.0.3");
        Mockito.when(provider.provideDapsToken()).thenReturn("Mocked Token.");

        // Create the message header that shall be send and tested
        //final var queryHeader = new RequestMessageBuilder()._modelVersion_("1.0.3")._issued_(Util.getGregorianNow()).build();
        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_("Token")
                .build();
        final var msgHeader = new RequestMessageBuilder(ResourceIDGenerator.randomURI(IDSControllerIntegrationTest.class))
                ._issuerConnector_(configurationContainer.getConnector().getId())
                ._issued_(IDSUtils.getGregorianNow())
                ._securityToken_(token)
                ._senderAgent_(configurationContainer.getConnector().getId())
                ._modelVersion_(configurationContainer.getConnector().getOutboundModelVersion())
                .build();

        final var notificationHeader = new NotificationMessageBuilder(ResourceIDGenerator.randomURI(IDSControllerIntegrationTest.class))
                ._issuerConnector_(configurationContainer.getConnector().getId())
                ._issued_(IDSUtils.getGregorianNow())
                ._securityToken_(token)
                ._senderAgent_(configurationContainer.getConnector().getId())
                ._modelVersion_(configurationContainer.getConnector().getOutboundModelVersion())
                .build();

        // Define mocking behaviour of the messageDispatcher.process() as well as the connector.getSelfDeclarationURL()
        final var mockResponseBody = "mock response";
        var responseMessage = new ResponseMessageBuilder()
                ._correlationMessage_(msgHeader.getId())
                ._issuerConnector_(configurationContainer.getConnector().getId())
                ._issued_(IDSUtils.getGregorianNow())
                ._securityToken_(token)
                ._senderAgent_(configurationContainer.getConnector().getId())
                ._modelVersion_(configurationContainer.getConnector().getOutboundModelVersion()).build();

        Mockito.when(messageDispatcher.process(Mockito.any(), Mockito.any())).thenReturn(BodyResponse.create(responseMessage, mockResponseBody));

        String header = serializer.serialize(msgHeader);

        String header2 = serializer.serialize(notificationHeader);

        // Create the message payload that shall be send and tested
        final var queryPayload = new TestPayload(Arrays.asList("1", "2", "3"));

        // Build a request Object with the target and the message header and payload as multipart request
        final var requestBuilder = MockMvcRequestBuilders.multipart("/api/ids/data")
                .part(new MockPart("header", header.getBytes()))
                .part(new MockPart("payload", serializer.serialize(queryPayload).getBytes()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.MULTIPART_FORM_DATA);

        // perform the request
        final var result = mockMvc
                .perform(requestBuilder)
                .andExpect(status().is(200))
                .andExpect(content().encoding(StandardCharsets.UTF_8.name()))
                .andReturn();

        // Build a request Object with the target and the message header and payload as multipart request
        final var notificationRequestBuilder = MockMvcRequestBuilders.multipart("/api/ids/data")
                .part(new MockPart("header", header2.getBytes()))
                .part(new MockPart("payload", serializer.serialize(queryPayload).getBytes()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.MULTIPART_FORM_DATA);

        // perform the request
        final var notificationResult = mockMvc
                .perform(notificationRequestBuilder)
                .andExpect(status().is(200))
                .andExpect(content().encoding(StandardCharsets.UTF_8.name()))
                .andReturn();

        final var response = result.getResponse();
        final var multiPartResp = MultipartStringParser.stringToMultipart(response.getContentAsString());
        String respHead = multiPartResp.get("header");//.replaceAll("UTC","+0000");

        final var responseHeader = serializer.deserialize(respHead, ResponseMessage.class);

        // Assert that the received response correlates to the request
        assertEquals(mockResponseBody, multiPartResp.get("payload"));
        assertEquals(msgHeader.getId(), responseHeader.getCorrelationMessage());
    }

}