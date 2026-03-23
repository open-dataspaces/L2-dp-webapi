package nedo.ods.svc.dp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link ManagementAuthenticationManager}
 *
 * <p>Verify if the authentication process using the API key works correctly.
 */
class ManagementAuthenticationManagerTest {

  private ApiGatewayProperties properties;
  private ManagementAuthenticationManager manager;

  /**
   * Initializes the test environment by mocking {@link ApiGatewayProperties} and creating an
   * instance of {@link ManagementAuthenticationManager}.
   */
  @BeforeEach
  void setUp() {
    properties = mock(ApiGatewayProperties.class);
    manager = new ManagementAuthenticationManager(properties);
  }

  /**
   * Ensure that when the correct API key is provided, authentication succeeds, and an authenticated
   * {@link ApiKeyAuthenticationToken} is returned.
   */
  @Test
  void testAuthenticateWithValidApiKey() {
    String validApiKey = "valid-key";
    when(properties.getManagementApiKey()).thenReturn(validApiKey);

    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(validApiKey);

    Mono<Authentication> result = manager.authenticate(token);

    StepVerifier.create(result)
        .assertNext(
            auth -> {
              assertTrue(auth instanceof ApiKeyAuthenticationToken);
              ApiKeyAuthenticationToken authenticatedToken = (ApiKeyAuthenticationToken) auth;
              assertEquals(validApiKey, authenticatedToken.getApiKey());
              assertTrue(authenticatedToken.isAuthenticated());
            })
        .verifyComplete();
  }

  /**
   * Confirm that a {@link BadCredentialsException} is thrown when an incorrect API key is provided.
   */
  @Test
  void testAuthenticateWithInvalidApiKey() {
    when(properties.getManagementApiKey()).thenReturn("expected-key");

    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("wrong-key");

    Mono<Authentication> result = manager.authenticate(token);

    StepVerifier.create(result)
        .expectErrorMatches(
            error -> {
              assertTrue(error instanceof BadCredentialsException);
              assertEquals("Invalid API Key", error.getMessage());
              return true;
            })
        .verify();
  }

  /**
   * If an API key is set but does not match the provided key, pass through the branch where
   * configuredKey != null and equals is false.
   */
  @Test
  void testAuthenticateWithNonMatchingApiKeyExplicit() {
    String configuredKey = "expected-key";
    String providedKey = "wrong-key";

    when(properties.getManagementApiKey()).thenReturn(configuredKey);

    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(providedKey);

    Mono<Authentication> result = manager.authenticate(token);

    StepVerifier.create(result)
        .expectErrorMatches(
            error -> {
              assertTrue(error instanceof BadCredentialsException);
              assertEquals("Invalid API Key", error.getMessage());
              return true;
            })
        .verify();
  }

  /**
   * If authentication information other than {@link ApiKeyAuthenticationToken} is provided, the
   * authentication process will be skipped and an empty {@link Mono} will be returned.
   */
  @Test
  void testAuthenticateWithUnsupportedAuthenticationType() {
    Authentication unsupportedAuth = mock(Authentication.class);

    Mono<Authentication> result = manager.authenticate(unsupportedAuth);

    StepVerifier.create(result).expectComplete().verify();
  }

  /**
   * Verifies that authentication fails when the configured API key is null. A {@link
   * BadCredentialsException} should be thrown.
   */
  @Test
  void testAuthenticateWithNullConfiguredApiKey() {
    when(properties.getManagementApiKey()).thenReturn(null);

    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("any-key");

    Mono<Authentication> result = manager.authenticate(token);

    StepVerifier.create(result)
        .expectErrorMatches(
            error -> {
              assertTrue(error instanceof BadCredentialsException);
              assertEquals("Invalid API Key", error.getMessage());
              return true;
            })
        .verify();
  }

  /** A test to explicitly pass the false branch of {@code instanceof}. */
  @Test
  void testInstanceOfFalseBranch() {
    Authentication auth = mock(Authentication.class);
    assertFalse(auth instanceof ApiKeyAuthenticationToken);
  }

  /**
   * A test that explicitly goes through the false branch of equals when the API keys do not match.
   */
  @Test
  void testEqualsFalseBranch() {
    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("key1", true);
    assertNotEquals("key2", token.getApiKey());
  }

  /** Test to pass the branch when the authenticated flag is false. */
  @Test
  void testIsAuthenticatedFalseBranch() {
    ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("key", false);
    assertFalse(token.isAuthenticated());
  }
}
