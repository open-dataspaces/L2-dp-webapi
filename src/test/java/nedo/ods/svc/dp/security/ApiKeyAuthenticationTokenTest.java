package nedo.ods.svc.dp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unit tests for {@link ApiKeyAuthenticationToken}
 *
 * <p>Generate an authentication token using the API key and verify the state of the properties.
 */
class ApiKeyAuthenticationTokenTest {

  /**
   * Generate an {@link ApiKeyAuthenticationToken} with the authentication status set to false, and
   * check if the API key, principal, credentials, authentication status, and permissions are set
   * correctly.
   */
  @Test
  void testConstructorWithApiKeyOnly() {
    String apiKey = "test-api-key";
    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(apiKey);

    assertEquals(apiKey, token.getPrincipal(), "Principal should be the API key");
    assertEquals(apiKey, token.getApiKey(), "getApiKey() should return the API key");
    assertNull(token.getCredentials(), "Credentials should be null");
    assertFalse(token.isAuthenticated(), "Token should not be authenticated");
    assertTrue(token.getAuthorities().isEmpty(), "Authorities should be empty");
  }

  /**
   * Generate an {@link ApiKeyAuthenticationToken} with an authentication status of true, and check
   * if the API key, authentication status, and permissions are correctly set.
   *
   * <p>Verify that the privileges include "ROLE_ACTUATOR_ADMIN".
   */
  @Test
  void testConstructorWithApiKeyAndAuthenticatedTrue() {
    String apiKey = "admin-api-key";
    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(apiKey, true);

    assertEquals(apiKey, token.getPrincipal(), "Principal should be the API key");
    assertEquals(apiKey, token.getApiKey(), "getApiKey() should return the API key");
    assertNull(token.getCredentials(), "Credentials should be null");
    assertTrue(token.isAuthenticated(), "Token should be authenticated");

    List<GrantedAuthority> authorities = (List<GrantedAuthority>) token.getAuthorities();
    assertEquals(1, authorities.size(), "Should have one authority");
    assertEquals(
        "ROLE_ACTUATOR_ADMIN",
        authorities.get(0).getAuthority(),
        "Authority should be ROLE_ACTUATOR_ADMIN");
  }
}
