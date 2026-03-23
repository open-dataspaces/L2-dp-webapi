package nedo.ods.svc.dp.gateway.logger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.LinkedHashMap;
import java.lang.StringBuilder;

import java.net.URI;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;

/**
 * Formats gateway request/response context into structured JSON strings for logging/analytics and
 * emits a human-readable header dump to {@link OdsLogger} at DEBUG level.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Request: logs method/path and all request headers (human-readable), and returns a JSON
 *       string containing {@code logType=gatewayRequest}, timestamps, trackingId, method, path,
 *       destination, operatorId, and ODS-prefixed headers.
 *   <li>Response: logs status and all response headers (human-readable), and returns a JSON string
 *       containing {@code logType=gatewayResponse}, timestamps, trackingId, statusCode, and {@code
 *       errorMessage} only when the status indicates an error.
 * </ul>
 *
 * <p>This class has no mutable state and is safe for concurrent use.
 */
public class GatewayLogFormatter {

  private static final OdsLogger logger = OdsLogger.getLogger(GatewayLogFormatter.class);

  /**
   * Builds a human-readable dump of the incoming request headers and writes it to DEBUG log, then
   * returns a structured JSON string describing the request context.
   *
   * <p>The returned JSON includes:
   *
   * <ul>
   *   <li>{@code logType} = {@code gatewayRequest}
   *   <li>{@code requestReceiveTime}, {@code requestSentTime} (formatted as {@code yyyy-MM-dd
   *       HH:mm:ss.SSS} in Asia/Tokyo)
   *   <li>{@code trackingId}, {@code operatorId}
   *   <li>{@code httpMethod}, {@code urlPath}, {@code sentTo} (backendUri + path when available)
   *   <li>{@code odsHeaders}: only headers starting with {@code X-ODS-}, as name → comma-joined
   *       value
   * </ul>
   *
   * @param request the incoming {@link ServerHttpRequest}
   * @param exchange the current {@link ServerWebExchange} providing attributes such as {@code
   *     backendUri}, {@code requestReceivedTime}, {@code requestSentTime}, {@code trackingId},
   *     {@code operatorId}
   * @return JSON string representing the request context; may be {@code null} if JSON serialization
   *     fails
   */
  public static String collectAndFormatRequestLog(
      ServerHttpRequest request, ServerWebExchange exchange) {
    HttpHeaders headers = request.getHeaders();
    if (logger.isDebugEnabled()) {
      StringBuilder logBuilder = new StringBuilder();

      logBuilder.append("=== REQUEST HEADERS ===\n");
      logBuilder
          .append("Method: ")
          .append(request.getMethod())
          .append(" Path: ")
          .append(request.getPath().value())
          .append("\n");

      headers.forEach(
          (name, values) -> {
            logBuilder
                .append("Request Header: ")
                .append(name)
                .append(" = ")
                .append(String.join(", ", values))
                .append("\n");
          });

      logBuilder.append("=========================");

      logger.debug(logBuilder.toString());
    }

    Map<String, Object> outer = new LinkedHashMap<>();

    String backendPath = request.getPath().value();
    URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

    outer.put("logType", "gatewayRequest");
    outer.put(
        "requestReceiveTime", formatTime((String) exchange.getAttribute("requestReceivedTime")));
    outer.put("requestSentTime", formatTime((String) exchange.getAttribute("requestSentTime")));
    outer.put("trackingId", exchange.getAttribute("trackingId"));
    outer.put("httpMethod", request.getMethod().name());
    outer.put("urlPath", backendPath);
    outer.put("sentTo", requestUrl == null ? "" : requestUrl.toString());
    outer.put(
        "operatorId",
        exchange.getAttribute("operatorId") == null
            ? ""
            : exchange.getAttribute("operatorId").toString());

    Map<String, String> odsHeaders = new LinkedHashMap<>();
    headers.forEach(
        (name, values) -> {
          if (name.toLowerCase().startsWith("x-ods-")) {
            odsHeaders.put(name, String.join(",", values));
          }
        });
    outer.put("odsHeaders", odsHeaders);

    return tojson(outer);
  }

  /**
   * Builds a human-readable dump of the outgoing response headers and writes it to DEBUG log, then
   * returns a structured JSON string describing the response context.
   *
   * <p>The returned JSON includes:
   *
   * <ul>
   *   <li>{@code logType} = {@code gatewayResponse}
   *   <li>{@code responseReceiveTime}, {@code responseSentTime} (formatted as {@code yyyy-MM-dd
   *       HH:mm:ss.SSS} in Asia/Tokyo)
   *   <li>{@code trackingId}
   *   <li>{@code statusCode} as string
   *   <li>{@code errorMessage} only when {@code response.getStatusCode().isError()} is true
   * </ul>
   *
   * @param response the outgoing {@link ServerHttpResponse}
   * @param exchange the current {@link ServerWebExchange} providing attributes such as {@code
   *     backendResponseTime}, {@code responseSentTime}, {@code trackingId}
   * @return JSON string representing the response context; may be {@code null} if JSON
   *     serialization fails
   */
  public static String collectAndFormatResponseLog(
      ServerHttpResponse response, ServerWebExchange exchange) {
    HttpHeaders headers = response.getHeaders();
    if (logger.isDebugEnabled()) {
      StringBuilder logBuilder = new StringBuilder();

      logBuilder.append("=== RESPONSE HEADERS ===\n");
      logBuilder.append("Status Code: ").append(response.getStatusCode()).append("\n");

      headers.forEach(
          (name, values) -> {
            logBuilder
                .append("Response Header: ")
                .append(name)
                .append(" = ")
                .append(String.join(", ", values))
                .append("\n");
          });
      logBuilder.append("=========================");
      logger.debug(logBuilder.toString());
    }

    Map<String, Object> outer = new LinkedHashMap<>();
    outer.put("logType", "gatewayResponse");
    outer.put("trackingId", exchange.getAttribute("trackingId"));
    outer.put(
        "responseReceiveTime", formatTime((String) exchange.getAttribute("backendResponseTime")));
    outer.put("responseSentTime", formatTime((String) exchange.getAttribute("responseSentTime")));
    outer.put("statusCode", String.valueOf(response.getStatusCode().value()));
    if (response.getStatusCode().isError()) {
      if (exchange.getAttribute("errorMessage") != null) {
        outer.put("errorMessage", exchange.getAttribute("errorMessage"));
      } else {
        outer.put("errorMessage", response.getStatusCode());
      }
    }

    return tojson(outer);
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Serializes the given map to a JSON string.
   *
   * @param outer a map of log fields to serialize
   * @return JSON string on success; {@code null} if serialization fails
   */
  private static String tojson(Map<String, Object> outer) {
    try {
      return mapper.writeValueAsString(outer);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      logger.error("Failed to convert log to JSON", e);
      return null;
    }
  }

  /**
   * Formats epoch milliseconds (given as a string) into {@code yyyy-MM-dd HH:mm:ss.SSS}
   * (Asia/Tokyo).
   *
   * @param millisStr epoch milliseconds represented as string; if {@code null}, returns an empty
   *     string
   * @return formatted timestamp string, or empty string when input is {@code null}
   * @throws NumberFormatException if {@code millisStr} is non-null but not a valid long
   */
  private static String formatTime(String millisStr) {
    if (millisStr == null) return "";
    long millis = Long.parseLong(millisStr);

    java.time.format.DateTimeFormatter formatter =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    return java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(formatter);
  }
}
