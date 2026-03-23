package nedo.ods.svc.dp.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import nedo.ods.svc.dp.gateway.error.ErrorResponse;
import nedo.ods.svc.dp.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link HeaderValidationFilter}.
 *
 * <p>This class covers all branches and exception handling paths to ensure 100% test coverage.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HeaderValidationFilterTest {

  @Mock private ServerWebExchange exchange;

  @Mock private ServerHttpRequest request;

  @Mock private ServerHttpResponse response;

  @Mock private WebFilterChain filterChain;

  @Mock private HttpHeaders headers;

  @Mock private RequestPath requestPath;

  @Mock private SecurityProperties headerValidationProperties;

  private HeaderValidationFilter filter;

  /** Sets up mocks and default filter instance before each test. */
  @BeforeEach
  void setUp() {
    // Mock skip validation paths and default validApiKeys (null)
    when(headerValidationProperties.getManagementBasePath())
        .thenReturn("/actuator");
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(null); // Default null (not configured)
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true); // Default enabled
    filter = new HeaderValidationFilter(headerValidationProperties);
  }

  /** Test: All required headers are present and valid. */
  @Test
  @DisplayName("Should pass filter when all required headers are valid")
  void testFilterWithValidHeaders() {
    // Setup mocks
    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(requestPath);
    when(requestPath.value()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(headers.getFirst("API-KEY")).thenReturn("valid-api-key");
    when(headers.getFirst("Authorization")).thenReturn("Bearer token");
    when(headers.getFirst("X-TrackingID")).thenReturn("existing-tracking-id");
    when(filterChain.filter(exchange)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = filter.filter(exchange, filterChain);

    // Then
    StepVerifier.create(result)
        .as("Expected filter to complete successfully with valid headers")
        .verifyComplete();
    verify(filterChain).filter(exchange);
  }

  /** Test: API-KEY is validated against configured list and is valid. */
  @Test
  @DisplayName("Should pass filter when API-KEY is valid and configured")
  void testFilterWithValidApiKeyFromConfiguredList() {
    // Setup valid API keys
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(new ArrayList<>(Arrays.asList("api-key-1", "api-key-2", "api-key-3")));

    // Create a new filter instance with updated properties
    filter = new HeaderValidationFilter(headerValidationProperties);

    // Setup mocks
    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(requestPath);
    when(requestPath.value()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(headers.getFirst("API-KEY")).thenReturn("api-key-2"); // Valid key from list
    when(headers.getFirst("Authorization")).thenReturn("Bearer token");
    when(headers.getFirst("X-TrackingID")).thenReturn("existing-tracking-id");
    when(filterChain.filter(exchange)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = filter.filter(exchange, filterChain);

    // Then
    StepVerifier.create(result)
        .as("Expected filter to complete successfully with valid API-KEY from list")
        .verifyComplete();
    verify(filterChain).filter(exchange);
  }

  /** Test: API-KEY is not in configured list (invalid). */
  @Test
  @DisplayName("Should reject request when API-KEY is invalid")
  void shouldRejectWhenApiKeyInvalid() {
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "api-key-3"));

    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header("API-KEY", "invalid-api-key") 
            .build();

    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result).verifyComplete();
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /** Test: API-KEY is null but validation is enabled and configured. */
  @DisplayName("Should reject request when API-KEY is null and validation is enabled")
  @Test
  void shouldRejectWhenApiKeyIsNullAndValidationEnabled() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "api-key-3"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Request with API-KEY header explicitly set to null-like scenario:
    // MockServerHttpRequest does not allow null header values; to simulate "missing API-KEY"
    // simply omit the API-KEY header. This matches
    // exchange.getRequest().getHeaders().getFirst("API-KEY") == null.
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header(
                "Authorization", "Bearer token") // Authorization present to isolate API-KEY failure
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock (should NOT be called on rejection)
    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to reject request with null API-KEY when validation is enabled")
        .verifyComplete();

    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /** Test: API-KEY is empty string but validation is enabled and configured. */
  @DisplayName("Should reject request when API-KEY is empty and validation is enabled")
  @Test
  void shouldRejectWhenApiKeyEmptyAndValidationEnabled() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);

    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "api-key-3"));
    // null-safe for skip paths
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // API-KEY is empty, Authorization present
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header("API-KEY", "") // Empty API-KEY
            .header("Authorization", "Bearer token")
            .build();

    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result).verifyComplete();
    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /** Test: API-KEY header is missing and validation is not configured (null). */
  @DisplayName("Should pass filter when API-KEY validation is disabled and header is missing")
  @Test
  void shouldPassWhenApiKeyValidationDisabledAndHeaderMissing() {
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);

    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(false);
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
            .header("Authorization", "Bearer token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    Mono<Void> result = filter.filter(exchange, chain);

    StepVerifier.create(result).verifyComplete();
    // Should not be BAD_REQUEST
    assertNotEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    // Filter should pass through
    verify(chain).filter(any());
  }

  /** Test: Authorization header is missing. */
  @DisplayName("Should reject request when Authorization header is missing")
  @Test
  void shouldRejectWhenAuthorizationHeaderMissing() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);

    // Enable API-KEY validation (not strictly required for this check, but keeps behavior
    // consistent)
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Build request/exchange: Authorization missing
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header("API-KEY", "valid-api-key")
            // no Authorization header -> getFirst("Authorization") will be null
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock (should NOT be called on rejection)
    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to reject request with missing Authorization header")
        .verifyComplete();

    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /** Test: Authorization header is whitespace only. */
  @DisplayName("Should reject request when Authorization header is whitespace only")
  @Test
  void shouldRejectWhenAuthorizationHeaderIsWhitespaceOnly() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Request with whitespace-only Authorization
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header("API-KEY", "valid-api-key")
            .header("Authorization", "   ") // whitespace only
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock (should NOT be called on rejection)
    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to reject request with whitespace Authorization header")
        .verifyComplete();

    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /** Test: Management endpoint should skip header validation. */
  @DisplayName("Should skip header validation for management endpoint")
  @Test
  void shouldSkipHeaderValidationForManagementEndpoint() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);

    when(headerValidationProperties.getManagementBasePath())
        .thenReturn("/actuator/health");

    // Validation flags are irrelevant when skipping, but can be set explicitly
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Build request/exchange for management endpoint
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/actuator/health").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock: should be called because validation is skipped
    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to skip validation for management endpoint")
        .verifyComplete();

    // The filter must pass through to the chain
    verify(chain).filter(exchange);

    // Response should not be BAD_REQUEST
    assertNotEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
  }

  /** Test: Generates X-TrackingID when header is null. */
  @DisplayName("Should generate X-TrackingID when header is null")
  @Test
  void shouldGenerateTrackingIdWhenHeaderIsNull() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // MockServerHttpRequest cannot set a header with a null value.
    // To simulate "X-TrackingID == null", simply omit the header.
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
            .header("API-KEY", "valid-api-key")
            .header("Authorization", "Bearer token")
            // no X-TrackingID header -> getFirst("X-TrackingID") will be null
            .build();

    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock
    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to generate X-TrackingID when header is null (missing)")
        .verifyComplete();

    // Capture the exchange passed to the chain and assert X-TrackingID is generated (non-blank)
    ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
        ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
    verify(chain).filter(captor.capture());

    org.springframework.web.server.ServerWebExchange modifiedExchange = captor.getValue();
    String trackingId = modifiedExchange.getRequest().getHeaders().getFirst("X-TrackingID");

    assertNotNull(trackingId, "X-TrackingID must be present");
    assertFalse(trackingId.trim().isEmpty(), "Generated X-TrackingID must be non-blank");

    // Response should not be BAD_REQUEST in this scenario
    assertNotEquals(HttpStatus.BAD_REQUEST, modifiedExchange.getResponse().getStatusCode());
  }

  /** Test: Generates X-TrackingID when header is empty. */
  @DisplayName("Should generate X-TrackingID when header is empty")
  @Test
  void shouldGenerateTrackingIdWhenHeaderIsEmpty() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Request with empty X-TrackingID, valid API-KEY and Authorization present
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
            .header("API-KEY", "valid-api-key")
            .header("Authorization", "Bearer token")
            .header("X-TrackingID", "") // empty value
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock
    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to generate X-TrackingID when header is empty")
        .verifyComplete();

    // Capture the exchange passed to the chain and assert X-TrackingID is generated (non-blank)
    ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
        ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
    verify(chain).filter(captor.capture());

    org.springframework.web.server.ServerWebExchange modifiedExchange = captor.getValue();
    String trackingId = modifiedExchange.getRequest().getHeaders().getFirst("X-TrackingID");

    assertNotNull(trackingId, "X-TrackingID must be present");
    assertFalse(trackingId.trim().isEmpty(), "Generated X-TrackingID must be non-blank");

    // Response should not be BAD_REQUEST
    assertNotEquals(HttpStatus.BAD_REQUEST, modifiedExchange.getResponse().getStatusCode());
  }

  /** Test: Generates X-TrackingID when header is whitespace only. */
  @DisplayName("Should generate X-TrackingID when header is whitespace only")
  @Test
  void shouldGenerateTrackingIdWhenWhitespaceOnly() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Request with whitespace-only X-TrackingID, valid API-KEY and Authorization present
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
            .header("API-KEY", "valid-api-key")
            .header("Authorization", "Bearer token")
            .header("X-TrackingID", "   ") // whitespace only
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock
    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .as("Expected filter to generate X-TrackingID when header is whitespace only")
        .verifyComplete();

    // Capture the exchange passed to the chain and assert X-TrackingID is generated (non-blank)
    ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
        ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
    verify(chain).filter(captor.capture());

    org.springframework.web.server.ServerWebExchange modifiedExchange = captor.getValue();
    String trackingId = modifiedExchange.getRequest().getHeaders().getFirst("X-TrackingID");

    assertNotNull(trackingId, "X-TrackingID must be present");
    assertFalse(trackingId.trim().isEmpty(), "Generated X-TrackingID must be non-blank");

    // Should not be BAD_REQUEST
    assertNotEquals(HttpStatus.BAD_REQUEST, modifiedExchange.getResponse().getStatusCode());
  }

  /** Test: Keeps existing X-TrackingID header. */
  @Test
  @DisplayName("Should keep existing X-TrackingID header if present")
  void testFilterKeepsExistingTrackingId() {
    // Setup mocks
    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(requestPath);
    when(requestPath.value()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(headers.getFirst("API-KEY")).thenReturn("valid-api-key");
    when(headers.getFirst("Authorization")).thenReturn("Bearer token");
    when(headers.getFirst("X-TrackingID")).thenReturn("existing-tracking-id");
    when(filterChain.filter(exchange)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = filter.filter(exchange, filterChain);

    // Then
    StepVerifier.create(result)
        .as("Expected filter to keep existing X-TrackingID header")
        .verifyComplete();
    verify(filterChain).filter(exchange); // No modification needed
    verify(request, never()).mutate(); // No request mutation
  }

  /** Test: handleMissingHeader throws JsonProcessingException. */
  @DisplayName("Should call setComplete when JSON error occurs in handleMissingHeader")
  @Test
  void shouldSetCompleteOnJsonError() throws Exception {
    // Arrange SecurityProperties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);
    // Enable API-KEY validation to ensure handleMissingHeader is reachable
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(Collections.singletonList("valid-api-key"));
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Build request/exchange: make API-KEY invalid to trigger handleMissingHeader
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
            .header("API-KEY", "invalid-api-key")
            .header("Authorization", "Bearer token")
            .build();
    final MockServerWebExchange exchange = MockServerWebExchange.from(request);

    final WebFilterChain chain = mock(WebFilterChain.class);

    // Mock ObjectMapper to throw JsonProcessingException
    ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    when(mockObjectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Simulated JSON error") {});

    // Replace private field "objectMapper" via reflection
    Field objectMapperField = HeaderValidationFilter.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);
    objectMapperField.set(filter, mockObjectMapper);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert (indirect behavior)
    StepVerifier.create(result)
        .as("Expected filter to complete when JSON error occurs in handleMissingHeader")
        .verifyComplete();

    // Status should be BAD_REQUEST (set before JSON generation)
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());

    // Filter chain should not be called on error
    verify(chain, never()).filter(any());

    // ObjectMapper was invoked
    verify(mockObjectMapper).writeValueAsString(any());
  }

  /** Test: API-KEY validation is not configured (null), should pass even if header is missing. */
  @Test
  @DisplayName("Should pass filter when API-KEY validation is not configured (null)")
  void testFilterWithApiKeyValidationNotConfigured() {
    // Setup with null valid-api-keys (not configured)
    when(headerValidationProperties.getValidApiKeys()).thenReturn(null);

    // Create a new filter instance with updated properties
    filter = new HeaderValidationFilter(headerValidationProperties);

    // Setup mocks
    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(requestPath);
    when(requestPath.value()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(headers.getFirst("API-KEY")).thenReturn(null); // No API key provided
    when(headers.getFirst("Authorization")).thenReturn("Bearer token");
    when(headers.getFirst("X-TrackingID")).thenReturn("existing-tracking-id");
    when(filterChain.filter(exchange)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = filter.filter(exchange, filterChain);

    // Then - Should succeed because API-KEY validation is not configured
    StepVerifier.create(result)
        .as("Expected filter to pass when API-KEY validation is not configured (null)")
        .verifyComplete();
    verify(filterChain).filter(exchange);
  }

  /** Test: API-KEY validation is configured but list is empty, should reject all requests. */
  @DisplayName("Should reject request when API-KEY list is configured but empty")
  @Test
  void shouldRejectWhenValidApiKeysConfiguredButEmpty() {
    // Arrange properties (headerValidationProperties)
    SecurityProperties headerValidationProperties = mock(SecurityProperties.class);

    // Enable validation and configure empty valid-api-keys list (reject all)
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(true);
    when(headerValidationProperties.getValidApiKeys()).thenReturn(Collections.emptyList());
    when(headerValidationProperties.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(headerValidationProperties);

    // Request with any API-KEY and Authorization present
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/test")
            .header("API-KEY", "any-api-key")
            .header("Authorization", "Bearer token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    // Chain mock (should NOT be called on rejection)
    WebFilterChain chain = mock(WebFilterChain.class);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert - Should fail because valid-api-keys is configured but empty
    StepVerifier.create(result)
        .as("Expected filter to reject request when API-KEY list is configured but empty")
        .verifyComplete();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
  }

  /**
   * Test: API-KEY validation is disabled (getValidApiKeysEnabled() == false), should skip API-KEY
   * check.
   */
  @Test
  @DisplayName("Should skip API-KEY validation when getValidApiKeysEnabled() is false")
  void testFilterWithApiKeyValidationDisabled() {
    // Setup valid API keys and disable validation
    when(headerValidationProperties.getValidApiKeys())
        .thenReturn(new ArrayList<>(Arrays.asList("api-key-1", "api-key-2")));
    when(headerValidationProperties.getValidApiKeysEnabled()).thenReturn(false);
    filter = new HeaderValidationFilter(headerValidationProperties);
    // Setup mocks
    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(requestPath);
    when(requestPath.value()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(headers.getFirst("API-KEY"))
        .thenReturn("invalid-api-key"); // Invalid, but validation disabled
    when(headers.getFirst("Authorization")).thenReturn("Bearer token");
    when(headers.getFirst("X-TrackingID")).thenReturn("existing-tracking-id");
    when(filterChain.filter(exchange)).thenReturn(Mono.empty());
    // When
    Mono<Void> result = filter.filter(exchange, filterChain);
    // Then
    StepVerifier.create(result)
        .as("Expected filter to skip API-KEY validation when getValidApiKeysEnabled() is false")
        .verifyComplete();
    verify(filterChain).filter(exchange);
  }

  @DisplayName("DEBUG: covers 'Adding generated X-TrackingID' via appender without Unsafe")
  @Test
  void coversGeneratedTrackingIdDebug_viaAppender() {
    Logger classLogger = (Logger) LoggerFactory.getLogger(HeaderValidationFilter.class);
    Level prev = classLogger.getLevel();
    classLogger.setLevel(Level.DEBUG);

    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    classLogger.addAppender(appender);

    try {
      SecurityProperties props = mock(SecurityProperties.class);
      when(props.getValidApiKeysEnabled()).thenReturn(true);
      when(props.getValidApiKeys())
          .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
      when(props.getManagementBasePath()).thenReturn(null);

      HeaderValidationFilter filter = new HeaderValidationFilter(props);

      MockServerHttpRequest request =
          MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
              .header("API-KEY", "valid-api-key")
              .header("Authorization", "Bearer token")
              .build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);
      when(chain.filter(any())).thenReturn(Mono.empty());

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      boolean found =
          appender.list.stream()
              .anyMatch(e -> e.getFormattedMessage().contains("Adding generated X-TrackingID"));
      assertTrue(found, "DEBUG log 'Adding generated X-TrackingID' should be emitted");

      verify(chain).filter(any());
    } finally {
      classLogger.detachAppender(appender);
      appender.stop();
      classLogger.setLevel(prev);
    }
  }

  @DisplayName("DEBUG: covers 'X-TrackingID already present' via appender without Unsafe")
  @Test
  void coversExistingTrackingIdDebug_viaAppender() {
    Logger classLogger = (Logger) LoggerFactory.getLogger(HeaderValidationFilter.class);
    Level prev = classLogger.getLevel();
    classLogger.setLevel(Level.DEBUG);

    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    classLogger.addAppender(appender);

    try {
      SecurityProperties props = mock(SecurityProperties.class);
      when(props.getValidApiKeysEnabled()).thenReturn(true);
      when(props.getValidApiKeys())
          .thenReturn(Arrays.asList("api-key-1", "api-key-2", "valid-api-key"));
      when(props.getManagementBasePath()).thenReturn(null);

      HeaderValidationFilter filter = new HeaderValidationFilter(props);

      MockServerHttpRequest request =
          MockServerHttpRequest.method(HttpMethod.GET, "/api/test")
              .header("API-KEY", "valid-api-key")
              .header("Authorization", "Bearer token")
              .header("X-TrackingID", "existing-tracking-id")
              .build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);
      when(chain.filter(any())).thenReturn(Mono.empty());

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      boolean found =
          appender.list.stream()
              .anyMatch(e -> e.getFormattedMessage().contains("X-TrackingID already present"));
      assertTrue(found, "DEBUG log 'X-TrackingID already present' should be emitted");

      verify(chain).filter(any());
    } finally {
      classLogger.detachAppender(appender);
      appender.stop();
      classLogger.setLevel(prev);
    }
  }

  @DisplayName("DEBUG: skip validation for exact management endpoint via appender without Unsafe")
  @Test
  void shouldSkipManagementWithExactPathAndDebug_viaAppender() {
    Logger classLogger = (Logger) LoggerFactory.getLogger(HeaderValidationFilter.class);
    Level prev = classLogger.getLevel();
    classLogger.setLevel(Level.DEBUG);

    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    classLogger.addAppender(appender);

    try {
      SecurityProperties props = mock(SecurityProperties.class);
      when(props.getManagementBasePath())
          .thenReturn("/actuator/health");
      when(props.getValidApiKeysEnabled()).thenReturn(true);

      HeaderValidationFilter filter = new HeaderValidationFilter(props);

      MockServerHttpRequest request =
          MockServerHttpRequest.method(HttpMethod.GET, "/actuator/health").build();
      MockServerWebExchange exchange = MockServerWebExchange.from(request);

      WebFilterChain chain = mock(WebFilterChain.class);
      when(chain.filter(exchange)).thenReturn(Mono.empty());

      Mono<Void> result = filter.filter(exchange, chain);
      StepVerifier.create(result).verifyComplete();

      verify(chain).filter(exchange);

      boolean found =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getFormattedMessage()
                          .contains("Skipping header validation for management"));
      assertTrue(
          found, "DEBUG log 'Skipping header validation for management' should be emitted");
    } finally {
      classLogger.detachAppender(appender);
      appender.stop();
      classLogger.setLevel(prev);
    }
  }
  
  
  @DisplayName("handleMissingHeader: completes response when JSON serialization fails (BAD_REQUEST)")
  @Test
  void handleMissingHeader_catch_isCovered() throws Exception {
    // Arrange: validation enabled & non-empty list → ensures we reach handleMissingHeader for missing/blank API-KEY
    SecurityProperties props = mock(SecurityProperties.class);
    when(props.getValidApiKeysEnabled()).thenReturn(true);
    when(props.getValidApiKeys()).thenReturn(Arrays.asList("k1", "k2"));
    when(props.getManagementBasePath()).thenReturn(null);

    HeaderValidationFilter filter = new HeaderValidationFilter(props);

    // API-KEY header omitted → apiKey == null
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header("Authorization", "Bearer token")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    WebFilterChain chain = mock(WebFilterChain.class);

    // Mock ObjectMapper to throw JsonProcessingException
    ObjectMapper failing = mock(ObjectMapper.class);
    when(failing.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Simulated JSON error") {});

    // Replace the private field "objectMapper" via reflection (or refactor to inject via constructor)
    Field f = HeaderValidationFilter.class.getDeclaredField("objectMapper");
    f.setAccessible(true);
    f.set(filter, failing);

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result).verifyComplete();
    // BAD_REQUEST should be set before serialization attempt
    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    verify(chain, never()).filter(any());
    verify(failing).writeValueAsString(any(ErrorResponse.class));
  }

  @Test
   @DisplayName("Returns 400 (Bad Request) when API-KEY is missing and validApiKeys is configured but empty")
   void missingApiKey_withEmptyValidKeys_returnsBadRequest() {
     SecurityProperties props = mock(SecurityProperties.class);
     when(props.getValidApiKeysEnabled()).thenReturn(true);
     when(props.getValidApiKeys()).thenReturn(Collections.emptyList());
     when(props.getManagementBasePath()).thenReturn("/actuator");

     HeaderValidationFilter filter = new HeaderValidationFilter(props);

     MockServerHttpRequest request = MockServerHttpRequest
         .method(HttpMethod.GET, "/api/test")
         .header("Authorization", "Bearer token") 
         .build();
     MockServerWebExchange exchange = MockServerWebExchange.from(request);

     WebFilterChain chain = mock(WebFilterChain.class);
     when(chain.filter(any())).thenReturn(Mono.empty());

     // Act
     Mono<Void> result = filter.filter(exchange, chain);

     // Assert
     StepVerifier.create(result).verifyComplete();
     assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode(), "Should be 400 when API-KEY is missing");
     verify(chain, never()).filter(any());
   }
 
}
