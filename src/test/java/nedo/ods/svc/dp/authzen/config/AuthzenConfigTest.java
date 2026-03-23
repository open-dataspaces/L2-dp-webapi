package nedo.ods.svc.dp.authzen.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nedo.ods.svc.dp.authzen.api.AuthzClient;
import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties.Authzen;
import nedo.ods.svc.dp.gateway.filter.AuthzenAuthorizationGlobalFilter;

/**
 * Unit tests for {@link AuthzenConfig}.
 *
 * <p>This test class covers all branches, conditions, and exception handling in AuthzenConfig,
 * ensuring 100% code coverage.
 */
@ExtendWith(MockitoExtension.class)
class AuthzenConfigTest {

  /**
   * Tests for the authzClient bean creation.
   *
   * <p>Verifies normal and edge cases for AuthzClient creation.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("authzClient Bean Creation")
  class AuthzClientBeanTest {
    @Mock ApiGatewayProperties properties;
    @Mock Authzen authzenProps;

    /**
     * Test normal creation of AuthzClient bean.
     *
     * <p>Ensures that the bean is created with correct configuration values.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create AuthzClient bean with valid properties")
    void shouldCreateAuthzClientBeanWithValidProperties() {
      when(properties.getAuthzen()).thenReturn(authzenProps);
      when(authzenProps.getPdpEndpoint()).thenReturn("https://pdp.example.com");
      when(authzenProps.getApiKey()).thenReturn("api-key");
      when(authzenProps.getApiKeyHeader()).thenReturn("X-API-Key");

      AuthzenConfig config = new AuthzenConfig();
      AuthzClient client = config.authzClient(properties);
      assertNotNull(client, "AuthzClient bean should not be null");
    }

    /**
     * Test creation of AuthzClient bean with null API key and API key header properties.
     *
     * <p>Ensures that the bean is created even if API key and API key header properties are null,
     * as long as PDP endpoint is provided.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create AuthzClient bean with null API key and header properties")
    void shouldCreateAuthzClientBeanWithNullProperties() {
      when(properties.getAuthzen()).thenReturn(authzenProps);
      when(authzenProps.getPdpEndpoint()).thenReturn("https://pdp.example.com");
      when(authzenProps.getApiKey()).thenReturn(null);
      when(authzenProps.getApiKeyHeader()).thenReturn(null);

      AuthzenConfig config = new AuthzenConfig();
      AuthzClient client = config.authzClient(properties);
      assertNotNull(
          client,
          "AuthzClient bean should not be null even if API key and header properties are null");
    }

    /**
     * Test that IllegalStateException is thrown when PDP endpoint is null.
     *
     * <p>Ensures that the bean creation fails and throws an exception if the required PDP endpoint
     * property is missing.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should throw IllegalStateException when PDP endpoint is null")
    void shouldThrowIllegalStateExceptionWhenPdpEndpointIsNull() {
      when(properties.getAuthzen()).thenReturn(authzenProps);
      when(authzenProps.getPdpEndpoint()).thenReturn(null);
      when(authzenProps.getApiKey()).thenReturn(null);
      when(authzenProps.getApiKeyHeader()).thenReturn(null);

      AuthzenConfig config = new AuthzenConfig();
      //      AuthzClient client = config.authzClient(properties);
      //      assertNotNull(client, "AuthzClient bean should not be null even if properties are
      // null");
      assertThrows(
          IllegalStateException.class,
          () -> config.authzClient(properties),
          "IllegalStateException should be thrown when PDP endpoint is null");
    }
  }

  /**
   * Tests for the authzenAuthorizationGlobalFilter bean creation.
   *
   * <p>Verifies normal and edge cases for AuthzenAuthorizationGlobalFilter creation.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("authzenAuthorizationGlobalFilter Bean Creation")
  class AuthzenAuthorizationGlobalFilterBeanTest {
    @Mock AuthzClient authzClient;
    @Mock ApiGatewayProperties properties;
    @Mock Authzen authzenProps;

    /**
     * Test normal creation of AuthzenAuthorizationGlobalFilter bean.
     *
     * <p>Ensures that the filter bean is created with valid dependencies.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create AuthzenAuthorizationGlobalFilter bean with valid dependencies")
    void shouldCreateAuthzenAuthorizationGlobalFilterBeanWithValidDependencies() {
      when(properties.getAuthzen()).thenReturn(authzenProps);
      AuthzenConfig config = new AuthzenConfig();
      AuthzenAuthorizationGlobalFilter filter =
          config.authzenAuthorizationGlobalFilter(authzClient, properties);
      assertNotNull(filter, "AuthzenAuthorizationGlobalFilter bean should not be null");
    }

    /**
     * Test creation of AuthzenAuthorizationGlobalFilter bean with null Authzen properties.
     *
     * <p>Ensures that the filter bean is created even if Authzen properties are null.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create AuthzenAuthorizationGlobalFilter bean with null Authzen properties")
    void shouldCreateAuthzenAuthorizationGlobalFilterBeanWithNullAuthzenProperties() {
      when(properties.getAuthzen()).thenReturn(null);
      AuthzenConfig config = new AuthzenConfig();
      AuthzenAuthorizationGlobalFilter filter =
          config.authzenAuthorizationGlobalFilter(authzClient, properties);
      assertNotNull(
          filter,
          "AuthzenAuthorizationGlobalFilter bean should not be null even if Authzen properties are null");
    }
  }
}
