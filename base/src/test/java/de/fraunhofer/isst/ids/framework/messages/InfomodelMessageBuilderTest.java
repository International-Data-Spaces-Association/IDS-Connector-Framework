package de.fraunhofer.isst.ids.framework.messages;

import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.RequestMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.isst.ids.framework.communication.http.InfomodelMessageBuilder;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test building an Infomodel Message
 */
public class InfomodelMessageBuilderTest {

    /**
     * build an Infomodel message with header and payload
     * @throws IOException if header cannot be serialized to JsonLD
     */
    @Test
    public void testBuildMessage() throws IOException {
        var header = new RequestMessageBuilder()
                ._issued_(IDSUtils.getGregorianNow())
                ._issuerConnector_(URI.create("https://example.com"))
                ._senderAgent_(URI.create("https://example.com"))
                ._modelVersion_("3.1.2-SNAPSHOT")
                ._securityToken_(new DynamicAttributeTokenBuilder()
                        ._tokenFormat_(TokenFormat.JWT)
                        ._tokenValue_("")
                        .build()
                )
                .build();
        assertNotNull(InfomodelMessageBuilder.messageWithString(header, "somepayload"));
    }

}