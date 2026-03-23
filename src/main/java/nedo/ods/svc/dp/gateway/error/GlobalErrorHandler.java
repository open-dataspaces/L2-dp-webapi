package nedo.ods.svc.dp.gateway.error;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import nedo.ods.svc.dp.gateway.logger.GatewayLogFormatter;
import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import nedo.ods.svc.dp.security.exception.AuthorizationDeniedException;
import reactor.core.publisher.Mono;

/**
 * Global error handler for handling exceptions thrown during web request processing.
 *
 * <p>This handler intercepts exceptions in the reactive web layer. If a 404 (NOT_FOUND) exception
 * occurs, it returns a custom JSON error response. For other exceptions, it propagates the error
 * further.
 *
 * <h2>IMPORTANT</h2>
 *
 * <p>Which to use {@link #writeErrorResponse} or {@link #writeErrorResponseInternal}?
 *
 * <ul>
 *   <li>{@link #writeErrorResponse}: Use this for exceptions that occur before passing through the
 *       {@link nedo.ods.svc.dp.gateway.filter.LoggingFilter#filter}.
 *   <li>{@link #writeErrorResponseInternal}: Use this for exceptions that occur after passing
 *       through {@link nedo.ods.svc.dp.gateway.filter.LoggingFilter#filter}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorHandler implements WebExceptionHandler {
  private static final OdsLogger logger = OdsLogger.getLogger(GlobalErrorHandler.class);

  /**
   * Handles exceptions thrown during web request processing.
   *
   * <p>If the exception is a {@link ResponseStatusException} with status 404 (NOT_FOUND), this
   * method returns a custom JSON error response using {@link ErrorResponse}. For all other
   * exceptions, the error is propagated.
   *
   * @param exchange the current server exchange
   * @param ex the exception that was thrown
   * @return a {@link Mono} that indicates when exception handling is complete
   */
  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    ServerHttpResponse response = exchange.getResponse();
    ServerHttpRequest request = exchange.getRequest();

    logger.error(ex.getMessage(), ex);

    if (response.isCommitted()) {
      return Mono.error(ex);
    }

    if (ex instanceof ResponseStatusException rse && rse.getStatusCode() == HttpStatus.NOT_FOUND) {
      return writeErrorResponseInternal(
          exchange,
          request,
          response,
          HttpStatus.NOT_FOUND,
          ErrorResponse.ErrorCode.NotFound,
          "Endpoint not found");
    } else if (ex instanceof TimeoutException
        || (ex instanceof ResponseStatusException rse
            && rse.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT)) {
      return writeErrorResponse(
          exchange,
          response,
          HttpStatus.GATEWAY_TIMEOUT,
          ErrorResponse.ErrorCode.GatewayTimeout,
          "Too long to receive a response from outer service");
    } else if (ex instanceof ConnectException
        || ex instanceof WebClientResponseException.ServiceUnavailable) {
      return writeErrorResponse(
          exchange,
          response,
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorResponse.ErrorCode.ServiceUnavailable,
          "Failed to connect to the outer service");
    } else if (ex instanceof AuthorizationDeniedException) {
      return writeErrorResponseInternal(
          exchange,
          request,
          response,
          HttpStatus.FORBIDDEN,
          ErrorResponse.ErrorCode.AuthForbidden,
          "Access denied");
    } else {
      return writeErrorResponseInternal(
          exchange,
          request,
          response,
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorResponse.ErrorCode.InternalServerError,
          "Unexpected error occurred");
    }
  }

  /**
   * Writes a JSON error response with the given status, code, and message.
   *
   * <p><strong>IMPORTANT</strong>: Use this if the caught exception occurred before passing through
   * the LoggingFilter.
   *
   * @param response the server HTTP response
   * @param status the HTTP status to set
   * @param code the error code
   * @param message the error message
   * @return a Mono that completes when the response is written
   */
  private static Mono<Void> writeErrorResponse(
      ServerWebExchange exchange,
      ServerHttpResponse response,
      HttpStatus status,
      ErrorResponse.ErrorCode code,
      String message) {
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    ErrorResponse errorResponse = new ErrorResponse.Builder().code(code).message(message).build();
    DataBuffer buffer =
        response.bufferFactory().wrap(errorResponse.toString().getBytes(StandardCharsets.UTF_8));
    exchange.getAttributes().put("errorResponse", errorResponse.getMessage());
    return response.writeWith(Mono.just(buffer));
  }

  /**
   * Writes an error response to the client with the specified HTTP status, error code, and message.
   * Sets response headers, logs request and response details, and stores error information in the
   * exchange attributes. Returns a Mono signaling completion after writing the error response body.
   *
   * <p><strong>IMPORTANT</strong>: Use this if the caught exception occurred after passing through
   * the LoggingFilter.
   *
   * @param exchange the current server web exchange
   * @param request the incoming HTTP request
   * @param response the HTTP response to write to
   * @param status the HTTP status to set on the response
   * @param code the application-specific error code
   * @param message the error message to include in the response
   * @return a Mono that completes when the response is written
   */
  private static Mono<Void> writeErrorResponseInternal(
      ServerWebExchange exchange,
      ServerHttpRequest request,
      ServerHttpResponse response,
      HttpStatus status,
      ErrorResponse.ErrorCode code,
      String message) {
    ErrorResponse errorResponse = new ErrorResponse.Builder().code(code).message(message).build();

    exchange.getAttributes().put("errorResponse", errorResponse.getMessage());
    exchange.getAttributes().put("errorMessage", errorResponse.getMessage());

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer =
        response.bufferFactory().wrap(errorResponse.toString().getBytes(StandardCharsets.UTF_8));
    logger.info(GatewayLogFormatter.collectAndFormatRequestLog(request, exchange));
    long responseSentTime = System.currentTimeMillis();
    exchange.getAttributes().put("responseSentTime", String.valueOf(responseSentTime));
    logger.info(GatewayLogFormatter.collectAndFormatResponseLog(response, exchange));

    return response.writeWith(Mono.just(buffer));
  }
}
