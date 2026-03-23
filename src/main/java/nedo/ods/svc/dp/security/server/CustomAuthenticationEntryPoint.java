package nedo.ods.svc.dp.security.server;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import nedo.ods.svc.dp.gateway.error.ErrorResponse;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import nedo.ods.svc.dp.gateway.logger.GatewayLogFormatter;

import nedo.ods.svc.dp.gateway.logger.OdsLogger;

/**
 * Custom authentication entry point for Spring WebFlux (Gateway).
 *
 * <p><b>This class is a mock implementation intended for testing authentication error handling.</b>
 * It returns a custom JSON response with appropriate error code and message for unauthorized
 * access.
 *
 * <p><b>How to test (mock):</b><br>
 * To trigger an authentication error and test this class, send a request with an invalid or expired
 * Bearer token, or omit the Authorization header. For example, use the following header in your API
 * request:<br>
 * <code>Authorization: Bearer invalidtoken</code>
 */
@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  private static final OdsLogger log = OdsLogger.getLogger(CustomAuthenticationEntryPoint.class);

  /**
   * Handles authentication failures and writes a custom JSON error response.
   *
   * <p>Returns a 401 Unauthorized response with a JSON body describing the error. If the exception
   * is {@link InvalidBearerTokenException}, the error code is set to {@code [auth] Unauthorized}.
   * Otherwise, the error code is set to {@code [dataspace] Unauthorized}.
   *
   * @param exchange the current server exchange
   * @param authException the authentication exception that was thrown
   * @return a {@link Mono} that completes when the response is written
   */
  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse resp = exchange.getResponse();
    var response = exchange.getResponse();
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    String message;
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    message =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.AuthUnauthorized)
            .message("Invalid or expired token")
            .build()
            .toString();
    log.info(GatewayLogFormatter.collectAndFormatRequestLog(request, exchange));
    exchange.getAttributes().put("errorMessage", "Invalid or expired token");
    long responseSentTime = System.currentTimeMillis();
    exchange.getAttributes().put("responseSentTime", String.valueOf(responseSentTime));
    log.info(GatewayLogFormatter.collectAndFormatResponseLog(resp, exchange));
    var buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
