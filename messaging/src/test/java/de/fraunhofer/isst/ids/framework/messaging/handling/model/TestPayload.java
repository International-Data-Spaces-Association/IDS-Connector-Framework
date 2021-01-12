package de.fraunhofer.isst.ids.framework.messaging.handling.model;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload used for test method in {@link de.fraunhofer.isst.ids.framework.messaging.handling.IDSControllerIntegrationTest}
 */
@Data
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class TestPayload {
    private List<String> testData;
}
