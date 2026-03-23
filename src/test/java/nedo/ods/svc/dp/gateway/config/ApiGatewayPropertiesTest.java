package nedo.ods.svc.dp.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApiGatewayProperties}
 *
 * <p>Verify the behavior and initial values of the getter/setter for each property.
 */
class ApiGatewayPropertiesTest {

  /**
   * Verify the behavior of {@link ApiGatewayProperties#setManagementApiKey(String)} and {@link
   * ApiGatewayProperties#getManagementApiKey()}.
   *
   * <p>Confirm that the API key is correctly set and obtained.
   */
  @Test
  void testManagementApiKeyGetterSetter() {
    ApiGatewayProperties props = new ApiGatewayProperties();
    props.setManagementApiKey("test-key");

    assertEquals("test-key", props.getManagementApiKey());
  }

  /**
   * Verify that the value can be set using the setter of {@link ApiGatewayProperties.Authzen} and
   * correctly retrieved using the getter.
   *
   * <p>Confirm that each property (enabled, pdpEndpoint, apiKey, apiKeyHeader, resourceType) is
   * being correctly maintained.
   */
  @Test
  void testAuthzenGetterSetter() {
    ApiGatewayProperties props = new ApiGatewayProperties();
    ApiGatewayProperties.Authzen authzen = new ApiGatewayProperties.Authzen();

    authzen.setEnabled(true);
    authzen.setPdpEndpoint("https://pdp.example.com");
    authzen.setApiKey("authzen-key");
    authzen.setApiKeyHeader("X-Custom-Header");
    authzen.setSubjectType("custom-subject");
    authzen.setSubjectIdAttributeName("custom-subject-id-attribute-name");
    authzen.setResourceType("custom-resource");
    authzen.setResourceIdMetadataKey("custom-metadata-key");
    authzen.setActionName("custom-action");

    props.setAuthzen(authzen);

    ApiGatewayProperties.Authzen result = props.getAuthzen();
    assertTrue(result.isEnabled());
    assertEquals("https://pdp.example.com", result.getPdpEndpoint());
    assertEquals("authzen-key", result.getApiKey());
    assertEquals("X-Custom-Header", result.getApiKeyHeader());
    assertEquals("custom-subject", result.getSubjectType());
    assertEquals("custom-subject-id-attribute-name", result.getSubjectIdAttributeName());
    assertEquals("custom-resource", result.getResourceType());
    assertEquals("custom-metadata-key", result.getResourceIdMetadataKey());
    assertEquals("custom-action", result.getActionName());
  }

  /**
   * Validate the initial state of {@link ApiGatewayProperties.Authzen}.
   *
   * <p>Confirm that the default values are correctly set (enabled=false,
   * apiKeyHeader="Authorization", subjectType="user", resourceType="endpoint",
   * resourceIdMetadataKey="endpointId", actionName="can_access").
   */
  @Test
  void testAuthzenDefaultValues() {
    ApiGatewayProperties.Authzen authzen = new ApiGatewayProperties.Authzen();

    assertFalse(authzen.isEnabled());
    assertNull(authzen.getPdpEndpoint());
    assertNull(authzen.getApiKey());
    assertEquals("Authorization", authzen.getApiKeyHeader());
    assertEquals("user", authzen.getSubjectType());
    assertEquals("operator_id", authzen.getSubjectIdAttributeName());
    assertEquals("endpoint", authzen.getResourceType());
    assertEquals("endpointId", authzen.getResourceIdMetadataKey());
    assertEquals("can_access", authzen.getActionName());
  }

}
