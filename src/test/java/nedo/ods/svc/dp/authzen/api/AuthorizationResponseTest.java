package nedo.ods.svc.dp.authzen.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuthorizationResponse}.
 *
 * <p>This test class covers all methods, branches, and exception handling for
 * AuthorizationResponse.
 */
class AuthorizationResponseTest {

  /**
   * Test isAllowed() returns true when allowed is true.
   *
   * @see AuthorizationResponse#isAllowed()
   */
  @Test
  @DisplayName("isAllowed returns true when allowed is true")
  void testIsAllowedTrue() {
    AuthorizationResponse response = new AuthorizationResponse(true, null);
    assertTrue(response.isAllowed(), "isAllowed should return true when allowed is true");
  }

  /**
   * Test isAllowed() returns false when allowed is false.
   *
   * @see AuthorizationResponse#isAllowed()
   */
  @Test
  @DisplayName("isAllowed returns false when allowed is false")
  void testIsAllowedFalse() {
    AuthorizationResponse response = new AuthorizationResponse(false, null);
    assertFalse(response.isAllowed(), "isAllowed should return false when allowed is false");
  }

  /**
   * Test getContext() returns null when context is null.
   *
   * @see AuthorizationResponse#getContext()
   */
  @Test
  @DisplayName("getContext returns null when context is null")
  void testGetContextNull() {
    AuthorizationResponse response = new AuthorizationResponse(true, null);
    assertNull(response.getContext(), "getContext should return null when context is null");
  }

  /**
   * Test getContext() returns the correct map when context is provided.
   *
   * @see AuthorizationResponse#getContext()
   */
  @Test
  @DisplayName("getContext returns correct map when context is provided")
  void testGetContextProvided() {
    Map<String, Object> context =
        Map.of("reason", "User has admin role", "expires_at", "2024-12-31T23:59:59Z");
    AuthorizationResponse response = new AuthorizationResponse(true, context);
    assertEquals(
        context, response.getContext(), "getContext should return the provided context map");
  }

  /**
   * Test constructor sets allowed and context correctly.
   *
   * @see AuthorizationResponse#AuthorizationResponse(boolean, Map)
   */
  @Test
  @DisplayName("Constructor sets allowed and context correctly")
  void testConstructorSetsFields() {
    Map<String, Object> context = Map.of("obligation", "log_access");
    AuthorizationResponse response = new AuthorizationResponse(false, context);
    assertFalse(response.isAllowed(), "isAllowed should match constructor argument");
    assertEquals(context, response.getContext(), "getContext should match constructor argument");
  }
}
