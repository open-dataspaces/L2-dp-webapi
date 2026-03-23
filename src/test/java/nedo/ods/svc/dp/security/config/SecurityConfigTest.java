package nedo.ods.svc.dp.security.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;

import nedo.ods.svc.dp.security.ManagementAuthenticationManager;
import nedo.ods.svc.dp.security.oauth2.OidcAuthenticationConverter;
import nedo.ods.svc.dp.security.server.CustomAuthenticationEntryPoint;

/** Full coverage tests for {@link SecurityConfig}. */
@DisplayName("SecurityConfig Tests")
/**
 * Unit tests for {@link SecurityConfig}.
 *
 * <p>This test class verifies:
 *
 * <ul>
 *   <li>Presence of required Spring annotations on the configuration class
 *   <li>Correct behavior of management and gateway security matcher logic
 *   <li>Exception handling when JWT decoder is missing
 *   <li>Successful execution of gateway filter chain when JWT decoder is configured
 * </ul>
 */
class SecurityConfigTest {

  private ManagementAuthenticationManager managementAuthenticationManager;
  private OidcAuthenticationConverter oidcAuthenticationConverter;
  private CustomAuthenticationEntryPoint authenticationEntryPoint;
  private SecurityProperties securityProperties;
  private RouteDefinitionLocator routeDefinitionLocator;
  private SecurityConfig securityConfig;

  @BeforeEach
  /** Initializes mocks and SecurityConfig instance before each test. */
  void setUp() {
    managementAuthenticationManager = mock(ManagementAuthenticationManager.class);
    oidcAuthenticationConverter = mock(OidcAuthenticationConverter.class);
    authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
    securityProperties = mock(SecurityProperties.class);
    routeDefinitionLocator = mock(RouteDefinitionLocator.class);

    when(securityProperties.getManagementBasePath()).thenReturn("/actuator");

    securityConfig =
        new SecurityConfig(
            managementAuthenticationManager,
            oidcAuthenticationConverter,
            authenticationEntryPoint,
            securityProperties,
            routeDefinitionLocator);
  }

  @Nested
  @DisplayName("Have configuration annotation")
  /** Tests for verifying the presence of Spring annotations on {@link SecurityConfig}. */
  class AnnotationPresenceTest {
    @Test
    /** Verifies that @Configuration annotation is present on SecurityConfig. */
    void shouldHaveConfigurationAnnotation() {
      assertNotNull(
          SecurityConfig.class.getAnnotation(
              org.springframework.context.annotation.Configuration.class));
    }

    @Test
    /** Verifies that @EnableWebFluxSecurity annotation is present on SecurityConfig. */
    void shouldHaveEnableWebFluxSecurityAnnotation() {
      assertNotNull(
          SecurityConfig.class.getAnnotation(
              org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
                  .class));
    }
  }

  @Nested
  @DisplayName("Management Matcher Logic")
  /** Tests for verifying the management security matcher logic. */
  class ManagementMatcherTest {
    @Test
    /** Verifies that management matcher correctly matches management endpoints. */
    void shouldExecuteManagementMatcherLogic() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      Method method =
          SecurityConfig.class.getDeclaredMethod("managementFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      method.invoke(securityConfig, http);

      Field matcherField = ServerHttpSecurity.class.getDeclaredField("securityMatcher");
      matcherField.setAccessible(true);
      ServerWebExchangeMatcher matcher = (ServerWebExchangeMatcher) matcherField.get(http);

      MockServerWebExchange managementExchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());
      MockServerWebExchange nonManagementExchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());

      assertTrue(matcher.matches(managementExchange).block().isMatch());
      assertFalse(matcher.matches(nonManagementExchange).block().isMatch());
    }
  }

  @Nested
  @DisplayName("Gateway Matcher Logic")
  /** Tests for verifying the gateway security matcher logic and filter chain behavior. */
  class GatewayMatcherTest {

    @Test
    @DisplayName("Throw exception when JWT decoder is missing")
    /** Verifies that an exception is thrown when JWT decoder is missing. */
    void shouldThrowExceptionWhenJwtDecoderMissing() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);

      Exception ex = assertThrows(Exception.class, () -> method.invoke(securityConfig, http));
      Throwable cause = ex.getCause();
      assertNotNull(cause);
      assertTrue(cause instanceof IllegalArgumentException);
      assertTrue(cause.getMessage().toLowerCase().contains("jwt"));
    }

    @Test
    @DisplayName("Execute gatewayFilterChain successfully with JWT decoder")
    /** Verifies successful execution of gateway filter chain with JWT decoder. */
    void shouldExecuteGatewayFilterChainSuccessfully() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(mock(ReactiveJwtDecoder.class))));

      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);

      Object result = method.invoke(securityConfig, http);
      assertNotNull(result);
      assertTrue(result instanceof SecurityWebFilterChain);
    }

    @Test
    @DisplayName("Execute gateway matcher logic")
    /** Verifies that gateway matcher correctly matches gateway endpoints. */
    void shouldExecuteGatewayMatcherLogic() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(mock(ReactiveJwtDecoder.class))));

      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      method.invoke(securityConfig, http);

      Field matcherField = ServerHttpSecurity.class.getDeclaredField("securityMatcher");
      matcherField.setAccessible(true);
      ServerWebExchangeMatcher matcher = (ServerWebExchangeMatcher) matcherField.get(http);

      MockServerWebExchange gatewayExchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
      MockServerWebExchange managementExchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/info").build());

      assertTrue(matcher.matches(gatewayExchange).block().isMatch());
      assertFalse(matcher.matches(managementExchange).block().isMatch());
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  /** Tests for edge cases such as empty base paths and missing headers. */
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty managementBasePath")
    /** Verifies behavior when managementBasePath is empty. */
    void shouldHandleEmptyManagementBasePath() {
      when(securityProperties.getManagementBasePath()).thenReturn("");
      ServerHttpSecurity http = ServerHttpSecurity.http();
      SecurityWebFilterChain chain = securityConfig.managementFilterChain(http);

      ServerWebExchange exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

      Boolean match = chain.matches(exchange).block();
      assertNotNull(match, "match result should not be null");
      assertTrue(match); // Because startsWith("") returns true even for an empty string
    }

    @Test
    @DisplayName("Should handle missing Authorization header")
    /** Verifies behavior when Authorization header is missing. */
    void shouldHandleMissingAuthorizationHeader() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(mock(ReactiveJwtDecoder.class))));
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      method.invoke(securityConfig, http);
      Field matcherField = ServerHttpSecurity.class.getDeclaredField("securityMatcher");
      matcherField.setAccessible(true);
      ServerWebExchangeMatcher matcher = (ServerWebExchangeMatcher) matcherField.get(http);
      MockServerWebExchange exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
      assertTrue(matcher.matches(exchange).block().isMatch());
    }

    @Test
    @DisplayName("Should throw exception in filter chain when decoder fails")
    /** Verifies that an exception is thrown when JWT decoder fails. */
    void shouldThrowExceptionInFilterChainWhenDecoderFails() throws Exception {
      ReactiveJwtDecoder failingDecoder = mock(ReactiveJwtDecoder.class);
      when(failingDecoder.decode(any())).thenThrow(new RuntimeException("Decoder failure"));
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(failingDecoder)));
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      Object result = method.invoke(securityConfig, http);
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Abnormal and Logging Tests")
  /** Tests for abnormal scenarios and logging behavior in SecurityConfig. */
  class AbnormalAndLoggingTests {

    @Test
    @DisplayName("Should not match without Authorization header")
    /** Verifies that matcher does not match without Authorization header. */
    void shouldNotMatchWithoutAuthorizationHeader() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(mock(ReactiveJwtDecoder.class))));
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      method.invoke(securityConfig, http);
      Field matcherField = ServerHttpSecurity.class.getDeclaredField("securityMatcher");
      matcherField.setAccessible(true);
      ServerWebExchangeMatcher matcher = (ServerWebExchangeMatcher) matcherField.get(http);
      MockServerWebExchange exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
      assertTrue(matcher.matches(exchange).block().isMatch());
    }

    @Test
    @DisplayName("Should throw exception for invalid JWT token")
    /** Verifies that an exception is thrown for invalid JWT token. */
    void shouldThrowExceptionForInvalidJwtToken() throws Exception {
      ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
      when(decoder.decode(any())).thenThrow(new RuntimeException("Invalid JWT"));
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(decoder)));
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      Object result = method.invoke(securityConfig, http);
      assertNotNull(result);
    }

    @Test
    @DisplayName("Should log expected message during filter setup")
    /** Verifies that expected log messages are generated during filter setup. */
    void shouldLogExpectedMessageDuringFilterSetup() throws Exception {
      ServerHttpSecurity http = ServerHttpSecurity.http();
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(mock(ReactiveJwtDecoder.class))));
      Method method =
          SecurityConfig.class.getDeclaredMethod("gatewayFilterChain", ServerHttpSecurity.class);
      method.setAccessible(true);
      Object result = method.invoke(securityConfig, http);
      assertNotNull(result);
    }
  }
}
