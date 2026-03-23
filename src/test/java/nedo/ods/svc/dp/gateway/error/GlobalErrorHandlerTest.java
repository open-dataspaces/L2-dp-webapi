package nedo.ods.svc.dp.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import nedo.ods.svc.dp.security.exception.AuthorizationDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link GlobalErrorHandler}.
 *
 * <p>This handler maps various exceptions to appropriate HTTP responses with JSON bodies.
 * Responsibilities tested:
 *
 * <ul>
 *   <li>Return correct HTTP status and message for known exceptions (e.g., NOT_FOUND,
 *       ConnectException, TimeoutException).
 *   <li>Provide descriptive error body including error code and message.
 *   <li>Handle committed responses gracefully by propagating the original error.
 *   <li>Fallback to INTERNAL_SERVER_ERROR for unknown exceptions.
 *   <li>Special handling for OAuth2AuthenticationException (returns UNAUTHORIZED).
 * </ul>
 */
class GlobalErrorHandlerTest {

  /** Mock appender to capture log outputs for verification. */
  private Appender<ILoggingEvent> mockAppender;

  /** Set up the mock appender before each test. */
  @SuppressWarnings("unchecked")
  @BeforeEach
  void beforeEach() {
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalErrorHandler.class);
    mockAppender = mock(Appender.class);
    logger.addAppender(mockAppender);
  }

  /** Detach the mock appender after each test. */
  @AfterEach
  void afterEach() {
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalErrorHandler.class);
    logger.detachAppender(mockAppender);
  }

  /**
   * Scenario: ResponseStatusException with NOT_FOUND.
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 404 NOT_FOUND
   *   <li>Body contains "[dataspace] NotFound" and "Endpoint not found"
   * </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: logged
   *         <li>logType gatewayResponse: logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleNotFound() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/notfound").build());
    Throwable ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exchange.getAttributes())
        .containsKeys("errorResponse", "errorMessage", "responseSentTime");
    assertThat((String) exchange.getAttribute("errorResponse")).isEqualTo("Endpoint not found");
    assertThat((String) exchange.getAttribute("errorMessage")).isEqualTo("Endpoint not found");
    assertThat((String) exchange.getAttribute("responseSentTime")).isNotEmpty();

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] NotFound");
    assertThat(actualBody).contains("Endpoint not found");

    // Verify logType gatewayRequest and gatewayResponse were logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(3)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
    assertThat(loggingEventCaptor.getAllValues().get(1).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(1).getFormattedMessage())
        .contains("\"logType\":\"gatewayRequest\"");
    assertThat(loggingEventCaptor.getAllValues().get(2).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(2).getFormattedMessage())
        .contains("\"logType\":\"gatewayResponse\"");
  }

  /**
   * Scenario: ConnectException (e.g., network failure).
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 503 SERVICE_UNAVAILABLE
   *   <li>Body contains "[dataspace] ServiceUnavailable" and "Failed to connect to the outer
   *       service"
   * </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: not logged
   *         <li>logType gatewayResponse: not logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleConnectException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/connect").build());
    Throwable ex = new ConnectException("Connection refused");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exchange.getAttributes()).containsKeys("errorResponse");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Failed to connect to the outer service");

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] ServiceUnavailable");
    assertThat(actualBody).contains("Failed to connect to the outer service");

    // Verify logType gatewayRequest and gatewayResponse were not logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
  }

  /**
   * Scenario: TimeoutException (e.g., gateway timeout).
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 504 GATEWAY_TIMEOUT
   *         <li>Body contains "[dataspace] GatewayTimeout" and "Too long to receive a response from
   *             outer service"
   *       </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: not logged
   *         <li>logType gatewayResponse: not logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleTimeoutException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/timeout").build());
    Throwable ex = new TimeoutException("Timeout");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(exchange.getAttributes()).containsKeys("errorResponse");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Too long to receive a response from outer service");

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] GatewayTimeout");
    assertThat(actualBody).contains("Too long to receive a response from outer service");

    // Verify logType gatewayRequest and gatewayResponse were not logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
  }

  /**
   * Scenario: ResponseStatusException with GATEWAY_TIMEOUT.
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *         <li>Status: 504 GATEWAY_TIMEOUT
   *         <li>Body contains "[dataspace] GatewayTimeout" and "Too long to receive a response from
   *             outer service"
   *       </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: not logged
   *         <li>logType gatewayResponse: not logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleGatewayTimeoutResponseStatusException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/gateway-timeout").build());
    Throwable ex = new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(exchange.getAttributes()).containsKeys("errorResponse");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Too long to receive a response from outer service");

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] GatewayTimeout");
    assertThat(actualBody).contains("Too long to receive a response from outer service");

    // Verify logType gatewayRequest and gatewayResponse were not logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
  }

  /**
   * Scenario: WebClientResponseException with SERVICE_UNAVAILABLE.
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 503 SERVICE_UNAVAILABLE
   *   <li>Body contains "[dataspace] ServiceUnavailable" and "Failed to connect to the outer
   *       service"
   * </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: not logged
   *         <li>logType gatewayResponse: not logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleServiceUnavailableException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/serviceunavailable").build());
    Throwable ex =
        WebClientResponseException.create(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            HttpHeaders.EMPTY,
            null,
            StandardCharsets.UTF_8);

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exchange.getAttributes()).containsKeys("errorResponse");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Failed to connect to the outer service");

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] ServiceUnavailable");
    assertThat(actualBody).contains("Failed to connect to the outer service");

    // Verify logType gatewayRequest and gatewayResponse were not logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
  }

  /**
   * Scenario: Unknown RuntimeException.
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 500 INTERNAL_SERVER_ERROR
   *   <li>Body contains "[dataspace] InternalServerError" and "Unexpected error occurred"
   * </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: logged
   *         <li>logType gatewayResponse: logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleOtherException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/other").build());
    Throwable ex = new RuntimeException("Other error");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(exchange.getAttributes())
        .containsKeys("errorResponse", "errorMessage", "responseSentTime");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Unexpected error occurred");
    assertThat((String) exchange.getAttribute("errorMessage"))
        .isEqualTo("Unexpected error occurred");
    assertThat((String) exchange.getAttribute("responseSentTime")).isNotEmpty();

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] InternalServerError");
    assertThat(actualBody).contains("Unexpected error occurred");

    // Verify logType gatewayRequest and gatewayResponse were logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(3)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
    assertThat(loggingEventCaptor.getAllValues().get(1).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(1).getFormattedMessage())
        .contains("\"logType\":\"gatewayRequest\"");
    assertThat(loggingEventCaptor.getAllValues().get(2).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(2).getFormattedMessage())
        .contains("\"logType\":\"gatewayResponse\"");
  }

  /**
   * Scenario: Response already committed before handler runs.
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Handler should propagate the original exception without modifying response.
   *   <li>logType gatewayRequest and gatewayResponse should be not logged.
   * </ul>
   */
  @Test
  void testHandleCommittedResponse() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/committed").build());
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    exchange.getResponse().setComplete().block();
    Throwable ex = new RuntimeException("Already committed");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono propagates the original exception.
    StepVerifier.create(mono).expectErrorMatches(error -> error == ex).verify();

    // Verify logType gatewayRequest and gatewayResponse were not logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(1)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
  }

  /**
   * Scenario: ResponseStatusException with BAD_REQUEST (not NOT_FOUND).
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *   <li>Status: 500 INTERNAL_SERVER_ERROR (fallback)
   *   <li>Body contains "[dataspace] InternalServerError" and "Unexpected error occurred"
   * </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: logged
   *         <li>logType gatewayResponse: logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleResponseStatusExceptionNotFoundFalseBranch() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/notfound-false").build());
    Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(exchange.getAttributes())
        .containsKeys("errorResponse", "errorMessage", "responseSentTime");
    assertThat((String) exchange.getAttribute("errorResponse"))
        .isEqualTo("Unexpected error occurred");
    assertThat((String) exchange.getAttribute("errorMessage"))
        .isEqualTo("Unexpected error occurred");
    assertThat((String) exchange.getAttribute("responseSentTime")).isNotEmpty();

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[dataspace] InternalServerError");
    assertThat(actualBody).contains("Unexpected error occurred");

    // Verify logType gatewayRequest and gatewayResponse were logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(3)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
    assertThat(loggingEventCaptor.getAllValues().get(1).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(1).getFormattedMessage())
        .contains("\"logType\":\"gatewayRequest\"");
    assertThat(loggingEventCaptor.getAllValues().get(2).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(2).getFormattedMessage())
        .contains("\"logType\":\"gatewayResponse\"");
  }

  /**
   * Scenario: AuthorizationDeniedException (access forbidden).
   *
   * <p>Expected:
   *
   * <ul>
   *   <li>Response:
   *       <ul>
   *         <li>Status code: 403 FORBIDDEN
   *         <li>Body contains "[auth] Forbidden" and "Access denied"
   *       </ul>
   *   <li>Logging:
   *       <ol>
   *         <li>logType gatewayRequest: logged
   *         <li>logType gatewayResponse: logged
   *       </ol>
   * </ul>
   */
  @Test
  void testHandleAuthorizationDeniedException() {
    // Create the handler instance under test.
    GlobalErrorHandler handler = new GlobalErrorHandler();

    // Prepare the mock exchange and exception.
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/forbidden").build());
    Throwable ex = new AuthorizationDeniedException("Fioden");

    // Execute the test subject method.
    Mono<Void> mono = handler.handle(exchange, ex);

    // Verify the Mono completes successfully.
    StepVerifier.create(mono).expectComplete().verify();

    // Verify effects on the exchange.
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(exchange.getAttributes())
        .containsKeys("errorResponse", "errorMessage", "responseSentTime");
    assertThat((String) exchange.getAttribute("errorResponse")).isEqualTo("Access denied");
    assertThat((String) exchange.getAttribute("errorMessage")).isEqualTo("Access denied");
    assertThat((String) exchange.getAttribute("responseSentTime")).isNotEmpty();

    // Verify the response body.
    String actualBody = exchange.getResponse().getBodyAsString().block();
    assertThat(actualBody).contains("[auth] Forbidden");
    assertThat(actualBody).contains("Access denied");

    // Verify logType gatewayRequest and gatewayResponse were logged.
    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, times(3)).doAppend(loggingEventCaptor.capture());
    assertThat(loggingEventCaptor.getAllValues().get(0).getLevel().toString()).isEqualTo("ERROR");
    assertThat(loggingEventCaptor.getAllValues().get(1).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(1).getFormattedMessage())
        .contains("\"logType\":\"gatewayRequest\"");
    assertThat(loggingEventCaptor.getAllValues().get(2).getLevel().toString()).isEqualTo("INFO");
    assertThat(loggingEventCaptor.getAllValues().get(2).getFormattedMessage())
        .contains("\"logType\":\"gatewayResponse\"");
  }
}
