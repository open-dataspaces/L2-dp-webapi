package nedo.ods.svc.dp.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import nedo.ods.svc.dp.authzen.api.AuthorizationRequest;
import nedo.ods.svc.dp.authzen.api.AuthorizationResponse;
import nedo.ods.svc.dp.authzen.api.AuthzClient;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;
import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import nedo.ods.svc.dp.security.exception.AuthorizationDeniedException;
import reactor.core.publisher.Mono;

/** Unit tests for {@link AuthzenAuthorizationGlobalFilter}. */
public class AuthzenAuthorizationGlobalFilterTest {

  /** The mock appender for capturing log events. */
  private Appender<ILoggingEvent> mockAppender;

  /** The mock AuthzClient instance. */
  private AuthzClient authzClient;

  /** The ApiGatewayProperties.Authzen instance with test configuration. */
  private ApiGatewayProperties.Authzen authzenProps;

  /** The instance of AuthzenAuthorizationGlobalFilter under test. */
  private AuthzenAuthorizationGlobalFilter filter;

  /**
   * Sets up the test environment before each test case.
   *
   * <p>Initializes the test environment by creating mock instances of {@link AuthzClient} and
   * {@link ApiGatewayProperties.Authzen}, and instantiating the {@link
   * AuthzenAuthorizationGlobalFilter}.
   *
   * <p>Also sets up a mock appender to capture log events for verification.
   */
  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(AuthzenAuthorizationGlobalFilter.class);
    mockAppender = mock(Appender.class);
    logger.addAppender(mockAppender);
    logger.setLevel(ch.qos.logback.classic.Level.INFO);

    authzClient = mock(AuthzClient.class);
    authzenProps = new ApiGatewayProperties.Authzen();
    authzenProps.setPdpEndpoint("https://authzen.example.com/pdp");
    authzenProps.setApiKey("test-api-key");
    authzenProps.setApiKeyHeader("X-API-KEY");
    authzenProps.setSubjectType("user");
    authzenProps.setSubjectIdAttributeName("operator_id");
    authzenProps.setResourceType("endpoint");
    authzenProps.setResourceIdMetadataKey("endpointId");
    authzenProps.setActionName("can_access");

    filter = new AuthzenAuthorizationGlobalFilter(authzClient, authzenProps);
  }

  /**
   * Cleans up the test environment after each test case.
   *
   * <p>Detaches the mock appender from the logger to prevent interference with other tests.
   */
  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(AuthzenAuthorizationGlobalFilter.class);
    logger.detachAppender(mockAppender);
  }

  /**
   * Creates a mocked {@link ServerWebExchange} with the specified path, HTTP method and gateway
   * route metadata.
   *
   * @param path the request path
   * @param method the HTTP method
   * @param gatewayRouteMetadata the gateway route metadata
   * @return a mocked {@link ServerWebExchange} instance
   */
  private ServerWebExchange mockExchange(
      String path, HttpMethod method, Map<String, Object> gatewayRouteMetadata) {
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    RequestPath requestPath = mock(RequestPath.class);
    Route route = mock(Route.class);

    when(exchange.getRequest()).thenReturn(request);
    when(request.getPath()).thenReturn(requestPath);
    when(request.getMethod()).thenReturn(method);
    when(requestPath.value()).thenReturn(path);
    when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
    when(route.getMetadata()).thenReturn(gatewayRouteMetadata);

    return exchange;
  }

  /**
   * Test the filter when no security context.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} throws {@link
   * IllegalStateException} when the security context is missing.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenNoSecurityContext() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is an allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. ReactiveSecurityContextHolder will return empty.
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.empty());

      // Execute the test subject method.
      final var mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "SecurityContext is missing, so authorization processing cannot be performed.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "SecurityContext is missing, so authorization processing cannot be performed.");
    }
  }

  /**
   * Test the filter when authorization is denied for authenticated users.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} throws {@link
   * AuthorizationDeniedException} when AuthZEN returns an access denial to an authenticated user.
   *
   * <p>In this test, we will check the following:
   *
   * <ul>
   *   <li>AuthZEN returns a rejection response while the security context is set.
   *   <li>The filter throws {@link AuthorizationDeniedException}.
   * </ul>
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthorizationDeniedForAuthenticatedUser() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a denied pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(false);
    when(response.getContext()).thenReturn(Map.of("reason", "denied"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(AuthorizationDeniedException.class)
          .hasFieldOrPropertyWithValue(
              "reason",
              "AuthZEN check failed (denied). Request: {subject=user:operator123, resource=endpoint:TestEndpoint.GET, action=can_access}, Context: {reason=denied}");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was called once.
      ArgumentCaptor<AuthorizationRequest> requestCaptor =
          ArgumentCaptor.forClass(AuthorizationRequest.class);
      verify(authzClient, times(1)).authorize(requestCaptor.capture());

      // Verify the subject of the captured AuthorizationRequest.
      AuthorizationRequest actualRequest = requestCaptor.getValue();
      assertThat(actualRequest.getSubject().getType()).isEqualTo("user");
      assertThat(actualRequest.getSubject().getId()).isEqualTo("operator123");
      assertThat(actualRequest.getSubject().getProperties())
          .containsOnlyKeys("sub", "email", "operator_id", "created");
      assertThat((String) actualRequest.getSubject().getProperties().get("sub"))
          .isEqualTo("user123");
      assertThat((String) actualRequest.getSubject().getProperties().get("email"))
          .isEqualTo("user@example.com");
      assertThat((String) actualRequest.getSubject().getProperties().get("operator_id"))
          .isEqualTo("operator123");
      assertThat((Long) actualRequest.getSubject().getProperties().get("created")).isEqualTo(0L);

      // Verify the resource of the captured AuthorizationRequest.
      assertThat(actualRequest.getResource().getType()).isEqualTo("endpoint");
      assertThat(actualRequest.getResource().getId()).isEqualTo("TestEndpoint.GET");
      assertThat(actualRequest.getResource().getProperties()).isEmpty();

      // Verify the action of the captured AuthorizationRequest.
      assertThat(actualRequest.getAction().getName()).isEqualTo("can_access");
      assertThat(actualRequest.getAction().getProperties()).isEmpty();

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("WARN");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "AuthZEN check failed (denied). Request: {subject=user:operator123, resource=endpoint:TestEndpoint.GET, action=can_access}, Context: {reason=denied}");
    }
  }

  /**
   * Test the filter when subject ID attribute is missing.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the subject ID attribute is missing in OAuth2User.
   *
   * <p>In this test, we will check the following:
   *
   * <ul>
   *   <li>If the OAuth2User does not have a subject ID attribute, an {@link
   *       IllegalArgumentException} is thrown.
   * </ul>
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenSubjectIdAttributeIsMissing() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. `operator_id` attribute is missing.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "dummy_operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when subject ID attribute is null.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the subject ID attribute is null in OAuth2User.
   *
   * <p>In this test, we will check the following:
   *
   * <ul>
   *   <li>If the OAuth2User does not have a subject ID attribute, an {@link
   *       IllegalArgumentException} is thrown.
   * </ul>
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenSubjectIdAttributeIsNull() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. `operator_id` attribute value is null.
    Map<String, Object> oauth2UserAttributes = new HashMap<>();
    oauth2UserAttributes.put("sub", "user123");
    oauth2UserAttributes.put("email", "user@example.com");
    oauth2UserAttributes.put("operator_id", null);
    oauth2UserAttributes.put("created", Instant.parse("1970-01-01T00:00:00Z"));
    OAuth2User oauth2User = new DefaultOAuth2User(null, oauth2UserAttributes, "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, times(0)).filter(any());

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, times(0)).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when subject ID attribute is blank.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the subject ID attribute is blank in OAuth2User.
   *
   * <p>In this test, we will check the following:
   *
   * <ul>
   *   <li>If the OAuth2User has a blank subject ID attribute, an {@link IllegalArgumentException}
   *       is thrown.
   * </ul>
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenSubjectIdAttributeIsBlank() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. `operator_id` attribute value is blank.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, times(0)).filter(any());

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, times(0)).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "'operator_id' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when authentication is null.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the authentication is null.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthenticationIsNull() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. Authentication is null.
    SecurityContext securityContext = new SecurityContextImpl(null);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Authentication principal is not an instance of OAuth2User, so cannot build subject of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "Authentication principal is not an instance of OAuth2User, so cannot build subject of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when authentication principal is not OAuth2User instance.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the authentication principal is not an instance of {@link
   * OAuth2User}.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthenticationPrincipalNotOAuth2User() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context. Authentication principal is not OAuth2User instance.
    TestingAuthenticationToken auth = new TestingAuthenticationToken("aa", "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Authentication principal is not an instance of OAuth2User, so cannot build subject of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "Authentication principal is not an instance of OAuth2User, so cannot build subject of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when gateway route is null.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the gateway route is null.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenGatewayRouteIsNull() throws AuthorizationException {
    // Prepare the arguments for the test subject method. Gateway route is null.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(null);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when gateway route metadata is null.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the gateway route metadata is null.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenGatewayRouteMetadataIsNull() throws AuthorizationException {
    // Prepare the arguments for the test subject method. Gateway route metadata is null.
    ServerWebExchange exchange = mockExchange("/test", HttpMethod.GET, null);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when gateway route metadata is empty.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the gateway route metadata is empty.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenGatewayRouteMetadataIsEmpty() throws AuthorizationException {
    // Prepare the arguments for the test subject method. Gateway route metadata is empty.
    ServerWebExchange exchange = mockExchange("/test", HttpMethod.GET, Map.of());
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when resource ID metadata is missing.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the resource ID metadata is missing in the gateway route
   * metadata.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenResourceIdMetadataIsMissing() throws AuthorizationException {
    // Prepare the arguments for the test subject method. `endpointId` metadata is missing.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("dummyEndpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when resource ID metadata is null.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the resource ID metadata is null in the gateway route
   * metadata.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenResourceIdMetadataIsNull() throws AuthorizationException {
    // Prepare the arguments for the test subject method. `endpointId` metadata value is null.
    final var gatewayRouteMetadata = new HashMap<String, Object>();
    gatewayRouteMetadata.put("endpointId", null);
    ServerWebExchange exchange = mockExchange("/test", HttpMethod.GET, gatewayRouteMetadata);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when resource ID metadata is blank.
   *
   * <p>A test to verify that {@link AuthzenAuthorizationGlobalFilter#filter} correctly throws
   * {@link IllegalArgumentException} when the resource ID metadata is blank in the gateway route
   * metadata.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenResourceIdMetadataIsBlank() throws AuthorizationException {
    // Prepare the arguments for the test subject method. `endpointId` metadata value is blank.
    ServerWebExchange exchange = mockExchange("/test", HttpMethod.GET, Map.of("endpointId", ""));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is a allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was not called.
      verify(authzClient, never()).authorize(any());

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo(
              "`endpointId` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.");
    }
  }

  /**
   * Test the filter when authorization is allowed for authenticated users.
   *
   * <p>Tests that the filter allows access when the authorization response indicates the request is
   * allowed.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthorizationAllowedForAuthenticatedUser() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is an allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatNoException().isThrownBy(() -> mono.block());

      // Verify that the next filter in the chain was called once.
      verify(chain, times(1)).filter(exchange);

      // Verify that `authzClient.authorize()` was called once.
      ArgumentCaptor<AuthorizationRequest> requestCaptor =
          ArgumentCaptor.forClass(AuthorizationRequest.class);
      verify(authzClient, times(1)).authorize(requestCaptor.capture());

      // Verify the subject of the captured AuthorizationRequest.
      AuthorizationRequest actualRequest = requestCaptor.getValue();
      assertThat(actualRequest.getSubject().getType()).isEqualTo("user");
      assertThat(actualRequest.getSubject().getId()).isEqualTo("operator123");
      assertThat(actualRequest.getSubject().getProperties())
          .containsOnlyKeys("sub", "email", "operator_id", "created");
      assertThat((String) actualRequest.getSubject().getProperties().get("sub"))
          .isEqualTo("user123");
      assertThat((String) actualRequest.getSubject().getProperties().get("email"))
          .isEqualTo("user@example.com");
      assertThat((String) actualRequest.getSubject().getProperties().get("operator_id"))
          .isEqualTo("operator123");
      assertThat((Long) actualRequest.getSubject().getProperties().get("created")).isEqualTo(0L);

      // Verify the resource of the captured AuthorizationRequest.
      assertThat(actualRequest.getResource().getType()).isEqualTo("endpoint");
      assertThat(actualRequest.getResource().getId()).isEqualTo("TestEndpoint.GET");
      assertThat(actualRequest.getResource().getProperties()).isEmpty();

      // Verify the action of the captured AuthorizationRequest.
      assertThat(actualRequest.getAction().getName()).isEqualTo("can_access");
      assertThat(actualRequest.getAction().getProperties()).isEmpty();

      // Verify logging.
      verify(mockAppender, never()).doAppend(any());
    }
  }

  /**
   * Test the filter when authorization is allowed for authenticated users with debug logging
   * enabled.
   *
   * <p>Tests that the filter allows access when the authorization response indicates the request is
   * allowed.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthorizationAllowedForAuthenticatedUserWithDebugLogging()
      throws AuthorizationException {
    // Prepare logger to enable debug logging.
    Logger logger = (Logger) LoggerFactory.getLogger(AuthzenAuthorizationGlobalFilter.class);
    logger.setLevel(ch.qos.logback.classic.Level.DEBUG);

    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. Authorization is an allowed pattern.
    AuthorizationResponse response = mock(AuthorizationResponse.class);
    when(response.isAllowed()).thenReturn(true);
    when(response.getContext()).thenReturn(Map.of("reason", "passed"));
    when(authzClient.authorize(any())).thenReturn(response);

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatNoException().isThrownBy(() -> mono.block());

      // Verify that the next filter in the chain was called once.
      verify(chain, times(1)).filter(exchange);

      // Verify that `authzClient.authorize()` was called once.
      ArgumentCaptor<AuthorizationRequest> requestCaptor =
          ArgumentCaptor.forClass(AuthorizationRequest.class);
      verify(authzClient, times(1)).authorize(requestCaptor.capture());

      // Verify the subject of the captured AuthorizationRequest.
      AuthorizationRequest actualRequest = requestCaptor.getValue();
      assertThat(actualRequest.getSubject().getType()).isEqualTo("user");
      assertThat(actualRequest.getSubject().getId()).isEqualTo("operator123");
      assertThat(actualRequest.getSubject().getProperties())
          .containsOnlyKeys("sub", "email", "operator_id", "created");
      assertThat((String) actualRequest.getSubject().getProperties().get("sub"))
          .isEqualTo("user123");
      assertThat((String) actualRequest.getSubject().getProperties().get("email"))
          .isEqualTo("user@example.com");
      assertThat((String) actualRequest.getSubject().getProperties().get("operator_id"))
          .isEqualTo("operator123");
      assertThat((Long) actualRequest.getSubject().getProperties().get("created")).isEqualTo(0L);

      // Verify the resource of the captured AuthorizationRequest.
      assertThat(actualRequest.getResource().getType()).isEqualTo("endpoint");
      assertThat(actualRequest.getResource().getId()).isEqualTo("TestEndpoint.GET");
      assertThat(actualRequest.getResource().getProperties()).isEmpty();

      // Verify the action of the captured AuthorizationRequest.
      assertThat(actualRequest.getAction().getName()).isEqualTo("can_access");
      assertThat(actualRequest.getAction().getProperties()).isEmpty();

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(2)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getAllValues().getFirst().getLevel().toString())
          .isEqualTo("DEBUG");
      assertThat(loggingEventCaptor.getAllValues().getFirst().getFormattedMessage())
          .isEqualTo(
              "Dispatching AuthZEN request. Request: {subject=user:operator123, resource=endpoint:TestEndpoint.GET, action=can_access}");
      assertThat(loggingEventCaptor.getAllValues().getLast().getLevel().toString())
          .isEqualTo("DEBUG");
      assertThat(loggingEventCaptor.getAllValues().getLast().getFormattedMessage())
          .isEqualTo(
              "AuthZEN check passed (allowed). Request: {subject=user:operator123, resource=endpoint:TestEndpoint.GET, action=can_access}");
    }
  }

  /**
   * Test the filter when authzClient throws AuthorizationException.
   *
   * <p>Tests that the filter throws a {@link ResponseStatusException} when an {@link
   * AuthorizationException} occurs.
   *
   * @throws AuthorizationException An exception occurred during the authorization process.
   */
  @Test
  void testFilterWhenAuthzClientThrowsAuthorizationException() throws AuthorizationException {
    // Prepare the arguments for the test subject method.
    ServerWebExchange exchange =
        mockExchange("/test", HttpMethod.GET, Map.of("endpointId", "TestEndpoint.GET"));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Prepare the mock behavior for authzClient. authzClient throws AuthorizationException.
    when(authzClient.authorize(any())).thenThrow(new AuthorizationException("xxx"));

    // Prepare the security context.
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            null,
            Map.of(
                "sub",
                "user123",
                "email",
                "user@example.com",
                "operator_id",
                "operator123",
                "created",
                Instant.parse("1970-01-01T00:00:00Z")),
            "sub");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(oauth2User, "bb", "cc");
    SecurityContext securityContext = new SecurityContextImpl(auth);
    try (MockedStatic<ReactiveSecurityContextHolder> mocked =
        mockStatic(ReactiveSecurityContextHolder.class)) {
      mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

      // Execute the test subject method.
      Mono<Void> mono = filter.filter(exchange, chain);

      // Verify the result.
      assertThatThrownBy(() -> mono.block())
          .isInstanceOf(ResponseStatusException.class)
          .hasFieldOrPropertyWithValue("status", HttpStatus.INTERNAL_SERVER_ERROR)
          .hasFieldOrPropertyWithValue("reason", "Authorization service unavailable.")
          .hasCauseInstanceOf(AuthorizationException.class)
          .hasRootCauseMessage("xxx");

      // Verify that the next filter in the chain was not called.
      verify(chain, never()).filter(exchange);

      // Verify that `authzClient.authorize()` was called once.
      ArgumentCaptor<AuthorizationRequest> requestCaptor =
          ArgumentCaptor.forClass(AuthorizationRequest.class);
      verify(authzClient, times(1)).authorize(requestCaptor.capture());

      // Verify the subject of the captured AuthorizationRequest.
      AuthorizationRequest actualRequest = requestCaptor.getValue();
      assertThat(actualRequest.getSubject().getType()).isEqualTo("user");
      assertThat(actualRequest.getSubject().getId()).isEqualTo("operator123");
      assertThat(actualRequest.getSubject().getProperties())
          .containsOnlyKeys("sub", "email", "operator_id", "created");
      assertThat((String) actualRequest.getSubject().getProperties().get("sub"))
          .isEqualTo("user123");
      assertThat((String) actualRequest.getSubject().getProperties().get("email"))
          .isEqualTo("user@example.com");
      assertThat((String) actualRequest.getSubject().getProperties().get("operator_id"))
          .isEqualTo("operator123");
      assertThat((Long) actualRequest.getSubject().getProperties().get("created")).isEqualTo(0L);

      // Verify the resource of the captured AuthorizationRequest.
      assertThat(actualRequest.getResource().getType()).isEqualTo("endpoint");
      assertThat(actualRequest.getResource().getId()).isEqualTo("TestEndpoint.GET");
      assertThat(actualRequest.getResource().getProperties()).isEmpty();

      // Verify the action of the captured AuthorizationRequest.
      assertThat(actualRequest.getAction().getName()).isEqualTo("can_access");
      assertThat(actualRequest.getAction().getProperties()).isEmpty();

      // Verify logging.
      ArgumentCaptor<ILoggingEvent> loggingEventCaptor =
          ArgumentCaptor.forClass(LoggingEvent.class);
      verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
      assertThat(loggingEventCaptor.getValue().getLevel().toString()).isEqualTo("ERROR");
      assertThat(loggingEventCaptor.getValue().getFormattedMessage())
          .isEqualTo("AuthZEN client call failed with an exception.");
    }
  }

  /**
   * Test the getOrder() method.
   *
   * <p>Tests that the filter returns the correct order value.
   *
   * <p>Verify the operation of the getOrder() method.
   */
  @Test
  void testGetOrder() {
    AuthzClient mockAuthzClient = mock(AuthzClient.class);
    ApiGatewayProperties.Authzen mockAuthzenProperties = mock(ApiGatewayProperties.Authzen.class);

    AuthzenAuthorizationGlobalFilter filter =
        new AuthzenAuthorizationGlobalFilter(mockAuthzClient, mockAuthzenProperties);

    assertThat(filter.getOrder()).withFailMessage("getOrder() should return 1").isEqualTo(1);
  }
}
