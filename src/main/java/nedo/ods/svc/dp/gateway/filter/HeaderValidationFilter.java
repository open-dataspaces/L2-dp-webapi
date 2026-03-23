package nedo.ods.svc.dp.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nedo.ods.svc.dp.gateway.error.ErrorResponse;
import nedo.ods.svc.dp.gateway.logger.GatewayLogFormatter;
import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import nedo.ods.svc.dp.security.config.SecurityProperties;
import reactor.core.publisher.Mono;

/**
 * Header validation filter that performs mandatory header checks. - API-KEY: Optional header,
 * validates against configured values if provided - Authorization: Required header, returns 401
 * Unauthorized if missing - X-TrackingID: Automatically generates UUID if not present Can be used
 * in combination with Spring Security's addFilterBefore configuration.
 */
public class HeaderValidationFilter implements WebFilter {

  /** Logger for logging configuration details. */
  private static final OdsLogger log = OdsLogger.getLogger(HeaderValidationFilter.class);

  /** API-KEY header name */
  private static final String API_KEY_HEADER = "API-KEY";

  /** Authorization header name */
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /** X-TrackingID header name */
  private static final String TRACKING_ID_HEADER = "X-TrackingID";

  /** ObjectMapper for JSON conversion */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Security properties */
  private final SecurityProperties securityProperties;

  /**
   * Constructor for dependency injection.
   *
   * @param securityProperties Security properties
   */
  public HeaderValidationFilter(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  /**
   * Executes filter processing. Validates API-KEY against configured values if provided, checks
   * Authorization header existence, and adds X-TrackingID if missing.
   *
   * @param exchange ServerWebExchange
   * @param chain WebFilterChain
   * @return Mono that signals completion
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestPath = exchange.getRequest().getPath().value();
    String requestMethod = exchange.getRequest().getMethod().name();
    if (log.isDebugEnabled()) {
      log.debug(
          "Header validation filter processing request: " + requestMethod + " " + requestPath);
    }

    // Add X-TrackingID if not present
    ServerWebExchange modifiedExchange = addTrackingIdIfMissing(exchange);
    exchange
    .getAttributes()
    .put("trackingId", modifiedExchange.getRequest().getHeaders().getFirst(TRACKING_ID_HEADER));
    if (log.isDebugEnabled()) {
      log.debug("Header validation passed for request: " + requestMethod + " " + requestPath);
    }

    // Skip header validation for health check endpoints
    if (isManagementEndpoint(requestPath)) {
    	 if (log.isDebugEnabled()) {
        log.debug("Skipping header validation for management endpoint: " + requestPath);
    	 }
      return chain.filter(exchange);
    }

    // Check for API-KEY header and validate against configured values
    String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
    // If API key validation control is enable and valid API keys are configured, validate the
    // provided API key
    if (securityProperties.getValidApiKeysEnabled()
        && securityProperties.getValidApiKeys() != null) {
      if (securityProperties.getValidApiKeys().isEmpty()) {
        // If valid-api-keys is configured but empty, reject all requests
        log.warn(
            "API-KEY validation configured but no valid keys defined for request: "
                + requestMethod
                + " "
                + requestPath);
        if (apiKey == null || apiKey.isEmpty()) {
          return handleMissingHeader(
              exchange,
              "API-KEY",
              HttpStatus.BAD_REQUEST,
              ErrorResponse.ErrorCode.BadRequest,
              "Invalid request parameters");
        }
        return handleMissingHeader(
            exchange,
            "API-KEY",
            HttpStatus.UNAUTHORIZED,
            ErrorResponse.ErrorCode.Unauthorized,
            "Invalid API-Key");
      } else if (!securityProperties.getValidApiKeys().contains(apiKey)) {
        // If valid-api-keys has values but provided key is not in the list, return error
        log.warn("Invalid API-KEY header for request: " + requestMethod + " " + requestPath);
        if (apiKey == null || apiKey.isEmpty()) {
          return handleMissingHeader(
              exchange,
              "API-KEY",
              HttpStatus.BAD_REQUEST,
              ErrorResponse.ErrorCode.BadRequest,
              "Invalid request parameters");
        }
        return handleMissingHeader(
            exchange,
            "API-KEY",
            HttpStatus.UNAUTHORIZED,
            ErrorResponse.ErrorCode.Unauthorized,
            "Invalid API-Key");
      }
    }

    // Check for Authorization header existence
    String authorization = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
    if (authorization == null || authorization.trim().isEmpty()) {
      log.warn(
          "Missing or empty Authorization header for request: "
              + requestMethod
              + " "
              + requestPath);
      return handleMissingHeader(
          exchange,
          "API-KEY",
          HttpStatus.BAD_REQUEST,
          ErrorResponse.ErrorCode.BadRequest,
          "Invalid request parameters");
    }
    return chain.filter(modifiedExchange);
  }

  /**
   * Adds X-TrackingID header if it's missing from the request.
   *
   * @param exchange ServerWebExchange
   * @return Modified ServerWebExchange with X-TrackingID header
   */
  private ServerWebExchange addTrackingIdIfMissing(ServerWebExchange exchange) {
    String trackingId = exchange.getRequest().getHeaders().getFirst(TRACKING_ID_HEADER);

    if (trackingId == null || trackingId.trim().isEmpty()) {
      String generatedTrackingId = UUID.randomUUID().toString();
      if (log.isDebugEnabled()) {
      log.debug(
          "Adding generated X-TrackingID: "
              + generatedTrackingId
              + " for request: "
              + exchange.getRequest().getMethod().name()
              + " "
                + exchange.getRequest().getPath().value());
      }

      ServerHttpRequest modifiedRequest =
          exchange.getRequest().mutate().header(TRACKING_ID_HEADER, generatedTrackingId).build();

      return exchange.mutate().request(modifiedRequest).build();
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "X-TrackingID already present: "
              + trackingId
              + " for request: "
              + exchange.getRequest().getMethod().name()
              + " "
              + exchange.getRequest().getPath().value());
    }
    return exchange;
  }

  /**
   * Determines if the endpoint should skip header validation.
   *
   * @param requestPath request path
   * @return true if header validation should be skipped
   */
  private boolean isManagementEndpoint(String requestPath) {
    String basePath = securityProperties.getManagementBasePath();
    return requestPath.equals(basePath) || requestPath.startsWith(basePath + "/");
  }

  /**
   * Creates an error response when required header is missing.
   *
   * @param exchange ServerWebExchange
   * @param headerName Name of the missing header
   * @param message    the error message to be returned in the response
   * @return Mono<Void>
   */
  private Mono<Void> handleMissingHeader(
      ServerWebExchange exchange,
      String headerName,
      HttpStatus status,
      ErrorResponse.ErrorCode errorCode,
      String message) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    // Use ErrorResponse to create standardized error response
    ErrorResponse errorResponse =
        new ErrorResponse.Builder().code(errorCode).message(message).build();

    try {
      String jsonResponse = objectMapper.writeValueAsString(errorResponse);
      DataBuffer buffer =
          exchange
              .getResponse()
              .bufferFactory()
              .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
      log.info(GatewayLogFormatter.collectAndFormatRequestLog(request, exchange));
      exchange.getAttributes().put("errorMessage", errorResponse.getMessage());
      long responseSentTime = System.currentTimeMillis();
      exchange.getAttributes().put("responseSentTime", String.valueOf(responseSentTime));
      log.info(GatewayLogFormatter.collectAndFormatResponseLog(response, exchange));
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      log.error("Error creating JSON error response", e);
      return exchange.getResponse().setComplete();
    }
  }
}
