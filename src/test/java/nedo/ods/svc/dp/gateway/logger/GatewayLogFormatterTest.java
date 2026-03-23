package nedo.ods.svc.dp.gateway.logger;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Unit tests for {@link GatewayLogFormatter}.
 *
 * <p>These tests aim for near-100% coverage of both request and response log formatting paths,
 * including:
 *
 * <ul>
 *   <li>Presence/absence of exchange attributes (timestamps, trackingId, operatorId)
 *   <li>Presence/absence of backend request URL (sentTo)
 *   <li>ODS headers extraction (only headers starting with {@code X-ODS-})
 *   <li>Error/non-error response status handling and errorMessage selection
 *   <li>JSON serialization failure path in {@code tojson}
 *   <li>Number format error path in {@code formatTime}
 * </ul>
 *
 * <p>Time formatting is validated for using {@code yyyy-MM-dd-HH:mm:ss.SSS} pattern.
 */
class GatewayLogFormatterTest {

  /** Shared JSON mapper for asserting returned JSON payloads. */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Formats the given epoch milliseconds to {@code yyyy-MM-dd HH:mm:ss.SSS} in {@code Asia/Tokyo}.
   *
   * @param millis epoch milliseconds
   * @return formatted timestamp string in Tokyo timezone
   */
  private String formatSystemDefault(long millis) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter);
  }

  /**
   * Covers the class declaration of {@link GatewayLogFormatter} to ensure the default constructor
   * is executed for code coverage purposes. This test does not perform any assertions on behavior.
   */
  @Test
  void coverClassDeclaration() {
    new GatewayLogFormatter();
  }

  /**
   * Verifies request log formatting when all relevant fields are present.
   *
   * <ul>
   *   <li>{@code requestReceivedTime} and {@code requestSentTime}
   *   <li>{@code trackingId} and {@code operatorId}
   *   <li>Gateway backend URL attribute for {@code sentTo}
   *   <li>ODS headers are extracted and combined when multiple values exist
   * </ul>
   *
   * @throws Exception when JSON parsing fails
   */
  @Test
  void requestLog_fullFields_includesOdsHeaders_andSentTo() throws Exception {

    long recv =
        ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 678_000_000, ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli();
    long sent = recv + 1234;

    ServerHttpRequest request =
        MockServerHttpRequest.get("/api/data")
            .header("Content-Type", "application/json")
            .header("X-ODS-Alpha", "v1")
            .header("X-ODS-Alpha", "v2")
            .header("X-ODS-Beta", "b")
            .build();

    ServerWebExchange exchange = MockServerWebExchange.from((MockServerHttpRequest) request);
    exchange.getAttributes().put("requestReceivedTime", String.valueOf(recv));
    exchange.getAttributes().put("requestSentTime", String.valueOf(sent));
    exchange.getAttributes().put("trackingId", "trk-001");
    exchange.getAttributes().put("operatorId", "op-123");
    exchange
        .getAttributes()
        .put(
            ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
            URI.create("http://backend.example.local/service/v1/resource"));

    String json = GatewayLogFormatter.collectAndFormatRequestLog(request, exchange);
    assertNotNull(json);

    Map<?, ?> m = mapper.readValue(json, Map.class);

    assertEquals("gatewayRequest", m.get("logType"));
    assertEquals(formatSystemDefault(recv), m.get("requestReceiveTime"));
    assertEquals(formatSystemDefault(sent), m.get("requestSentTime"));
    assertEquals("trk-001", m.get("trackingId"));
    assertEquals("GET", m.get("httpMethod"));
    assertEquals("/api/data", m.get("urlPath"));
    assertEquals("http://backend.example.local/service/v1/resource", m.get("sentTo"));
    assertEquals("op-123", m.get("operatorId"));

    Map<?, ?> ods = (Map<?, ?>) m.get("odsHeaders");
    assertEquals("v1,v2", ods.get("X-ODS-Alpha"));
    assertEquals("b", ods.get("X-ODS-Beta"));
    assertFalse(ods.containsKey("Content-Type"));
  }

  /**
   * Verifies request log formatting when optional attributes are absent.
   *
   * <ul>
   *   <li>{@code requestSentTime} is absent → formatted as empty string
   *   <li>{@code operatorId} is absent → empty string
   *   <li>Gateway backend URL attribute is absent → {@code sentTo} empty string
   * </ul>
   *
   * @throws Exception when JSON parsing fails
   */
  @Test
  void requestLog_nullSentTime_andNullOperatorId_andNoBackendUri() throws Exception {
    long recv = System.currentTimeMillis();

    MockServerHttpRequest request = MockServerHttpRequest.get("/api/none").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    exchange.getAttributes().put("requestReceivedTime", String.valueOf(recv));
    exchange.getAttributes().put("trackingId", "trk-002");

    String json = GatewayLogFormatter.collectAndFormatRequestLog(request, exchange);
    assertNotNull(json);

    Map<?, ?> m = mapper.readValue(json, Map.class);
    assertEquals(formatSystemDefault(recv), m.get("requestReceiveTime"));
    assertEquals("", m.get("requestSentTime"));
    assertEquals("", m.get("sentTo"));
    assertEquals("", m.get("operatorId"));
  }

  /**
   * Verifies that an invalid {@code requestReceivedTime} string triggers {@link
   * NumberFormatException} via {@code formatTime}.
   */
  @Test
  void requestLog_invalidMillis_throwsNumberFormatException() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/bad").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    exchange.getAttributes().put("requestReceivedTime", "not-a-number");
    exchange.getAttributes().put("requestSentTime", String.valueOf(System.currentTimeMillis()));

    assertThrows(
        NumberFormatException.class,
        () -> GatewayLogFormatter.collectAndFormatRequestLog(request, exchange));
  }

  /**
   * Verifies response log formatting for a successful status (200 OK).
   *
   * <ul>
   *   <li>{@code errorMessage} key is not present
   *   <li>Timestamps are formatted
   *   <li>{@code statusCode} is string "200"
   * </ul>
   *
   * @throws Exception when JSON parsing fails
   */
  @Test
  void responseLog_success200_noErrorMessageKey() throws Exception {
    MockServerHttpResponse response = new MockServerHttpResponse();
    response.setStatusCode(HttpStatus.OK);
    response.getHeaders().add("X-Return", "x");

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/resp").build());
    long recv = System.currentTimeMillis();
    long sent = recv + 50;

    exchange.getAttributes().put("backendResponseTime", String.valueOf(recv));
    exchange.getAttributes().put("responseSentTime", String.valueOf(sent));
    exchange.getAttributes().put("trackingId", "trk-resp-001");

    String json = GatewayLogFormatter.collectAndFormatResponseLog(response, exchange);
    assertNotNull(json);

    Map<?, ?> m = mapper.readValue(json, Map.class);
    assertEquals("gatewayResponse", m.get("logType"));
    assertEquals("trk-resp-001", m.get("trackingId"));
    assertEquals(formatSystemDefault(recv), m.get("responseReceiveTime"));
    assertEquals(formatSystemDefault(sent), m.get("responseSentTime"));
    assertEquals("200", m.get("statusCode"));
    assertFalse(m.containsKey("errorMessage"));
  }

  /**
   * Verifies response log formatting for an error status (502 Bad Gateway) when an explicit {@code
   * errorMessage} is provided via exchange attributes.
   *
   * <ul>
   *   <li>{@code errorMessage} is the provided string
   *   <li>{@code statusCode} equals "502"
   * </ul>
   *
   * @throws Exception when JSON parsing fails
   */
  @Test
  void responseLog_error502_withExplicitMessage() throws Exception {
    MockServerHttpResponse response = new MockServerHttpResponse();
    response.setStatusCode(HttpStatus.BAD_GATEWAY);
    response.getHeaders().add(HttpHeaders.RETRY_AFTER, "5");

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/resp").build());

    long recv = System.currentTimeMillis();
    long sent = recv + 500;
    exchange.getAttributes().put("backendResponseTime", String.valueOf(recv));
    exchange.getAttributes().put("responseSentTime", String.valueOf(sent));
    exchange.getAttributes().put("trackingId", "trk-resp-002");
    exchange.getAttributes().put("errorMessage", "Upstream timeout");

    String json = GatewayLogFormatter.collectAndFormatResponseLog(response, exchange);
    assertNotNull(json);

    Map<?, ?> m = mapper.readValue(json, Map.class);
    assertEquals("502", m.get("statusCode"));
    assertEquals("Upstream timeout", m.get("errorMessage"));
  }

  /**
   * Verifies response log formatting for an error status (500 Internal Server Error) when no
   * explicit {@code errorMessage} is provided.
   *
   * <ul>
   *   <li>{@code errorMessage} falls back to the {@link HttpStatus} enum string
   *   <li>{@code statusCode} equals "500"
   * </ul>
   *
   * @throws Exception when JSON parsing fails
   */
  @Test
  void responseLog_error500_withoutMessage_usesStatusEnum() throws Exception {
    MockServerHttpResponse response = new MockServerHttpResponse();

    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/resp").build());

    long recv = System.currentTimeMillis();
    long sent = recv + 1;
    exchange.getAttributes().put("backendResponseTime", String.valueOf(recv));
    exchange.getAttributes().put("responseSentTime", String.valueOf(sent));
    exchange.getAttributes().put("trackingId", "trk-resp-003");

    String json = GatewayLogFormatter.collectAndFormatResponseLog(response, exchange);
    assertNotNull(json);

    Map<?, ?> m = mapper.readValue(json, Map.class);
    assertEquals("500", m.get("statusCode"));
    assertTrue(m.containsKey("errorMessage"));
    assertEquals("INTERNAL_SERVER_ERROR", m.get("errorMessage"));
  }

  /**
   * Verifies that JSON serialization failure in {@code tojson} is handled gracefully by returning
   * {@code null}. A self-referential map is used to trigger a Jackson serialization error.
   */
  static final class Boom {
    public Object getValue() {
      throw new RuntimeException("boom");
    }
  }

  @Test
  void responseLog_errorWithUnserializableMessage_returnsNull() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/resp").build());

    long recv = System.currentTimeMillis();
    long sent = recv + 10;
    exchange.getAttributes().put("backendResponseTime", String.valueOf(recv));
    exchange.getAttributes().put("responseSentTime", String.valueOf(sent));
    exchange.getAttributes().put("trackingId", "trk-resp-004");

    exchange.getAttributes().put("errorMessage", new Boom());

    String json = GatewayLogFormatter.collectAndFormatResponseLog(response, exchange);

    assertNull(json);
  }

  /**
   * Verifies that when DEBUG logging is enabled, {@link
   * GatewayLogFormatter#collectAndFormatRequestLog} outputs a detailed dump of request headers to
   * the logger. The test attaches a {@link ListAppender} to capture log messages and asserts that
   * expected header information and formatting markers are present in the log output.
   */
  @Test
  void debugRequestHeaders_areLogged_whenDebugEnabled() {
    Logger logger = (Logger) LoggerFactory.getLogger(GatewayLogFormatter.class);
    Level original = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/data")
              .header(HttpHeaders.CONTENT_TYPE, "application/json")
              .header("X-ODS-Alpha", "v1")
              .header("X-ODS-Alpha", "v2")
              .build();
      ServerWebExchange exchange = MockServerWebExchange.from(request);
      String json = GatewayLogFormatter.collectAndFormatRequestLog(request, exchange);
      assertNotNull(json);

      String allLogs =
          appender.list.stream()
              .map(e -> e.getFormattedMessage())
              .reduce("", (a, b) -> a + "\n" + b);

      assertTrue(allLogs.contains("=== REQUEST HEADERS ==="));
      assertTrue(allLogs.contains("Method: GET"));
      assertTrue(allLogs.contains("Path: /api/data"));
      assertTrue(allLogs.contains("Request Header: Content-Type = application/json"));
      assertTrue(allLogs.contains("Request Header: X-ODS-Alpha = v1, v2"));
      assertTrue(allLogs.contains("========================="));
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(original);
      appender.stop();
    }
  }

  /**
   * Verifies that when DEBUG logging is enabled, {@link
   * GatewayLogFormatter#collectAndFormatResponseLog} outputs a detailed dump of response headers to
   * the logger. The test attaches a {@link ListAppender} to capture log messages and asserts that
   * expected status code and header information are present in the log output.
   */
  @Test
  void debugResponseHeaders_areLogged_whenDebugEnabled() {
    Logger logger = (Logger) LoggerFactory.getLogger(GatewayLogFormatter.class);
    Level original = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      MockServerHttpResponse response = new MockServerHttpResponse();
      response.setStatusCode(HttpStatus.OK);
      response.getHeaders().add(HttpHeaders.RETRY_AFTER, "5");
      response.getHeaders().add("X-Return", "x");

      ServerWebExchange exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/resp").build());

      String json = GatewayLogFormatter.collectAndFormatResponseLog(response, exchange);
      assertNotNull(json);

      String allLogs =
          appender.list.stream()
              .map(e -> e.getFormattedMessage())
              .reduce("", (a, b) -> a + "\n" + b);

      assertTrue(allLogs.contains("=== RESPONSE HEADERS ==="));
      assertTrue(allLogs.contains("Status Code: 200"));
      assertTrue(allLogs.contains("Response Header: X-Return = x"));
      assertTrue(allLogs.contains("Response Header: Retry-After = 5"));
      assertTrue(allLogs.contains("========================="));
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(original);
      appender.stop();
    }
  }
}
