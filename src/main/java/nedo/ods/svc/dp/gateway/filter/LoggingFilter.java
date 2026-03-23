package nedo.ods.svc.dp.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import nedo.ods.svc.dp.gateway.logger.GatewayLogFormatter;

/**
 * Global Gateway Logging Filter.
 *
 * <p>Logs request and response details and records timing attributes in {@link
 * ServerWebExchange#getAttributes()} for monitoring and debugging.
 *
 * <p>Attributes recorded:
 *
 * <ul>
 *   <li><b>operatorId</b>: Extracted from SecurityContext (OAuth2User).
 *   <li><b>requestSentTime</b>: When the request was sent to the backend.
 *   <li><b>backendResponseTime</b>: When response processing ended (success/error/cancel).
 *   <li><b>responseSentTime</b>: When the response was fully sent to the client.
 * </ul>
 *
 * <p>Uses Reactor hooks ({@code doOnSuccess}, {@code doOnError}, {@code doFinally}) to capture
 * timings and log reliably. Timestamps are stored as Strings for safe formatting.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

  /** Project logger for request/response logs. */
  private static final OdsLogger logger = OdsLogger.getLogger(LoggingFilter.class);

  /**
   * Returns the filter execution order.
   *
   * <p>Configured to run after {@link RouteToRequestUrlFilter} by adding an offset (+400). Lower
   * values run earlier in the filter chain.
   *
   * @return the order value for this filter
   */
  @Override
  public int getOrder() {
    return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
  }

  /**
   * Main GlobalFilter logic.
   *
   * <p>Records request/response timing and operator ID in {@link
   * ServerWebExchange#getAttributes()}, logs request and response, and attaches Reactor hooks to
   * capture success, error, and cancel events.
   *
   * <p>Key actions:
   *
   * <ul>
   *   <li>Store {@code operatorId} from SecurityContext if available.
   *   <li>Record {@code requestSentTime} and log the request.
   *   <li>On termination: record {@code backendResponseTime} (success/error/cancel) and {@code
   *       responseSentTime}, then log the response.
   * </ul>
   *
   * @param exchange current server exchange
   * @param chain downstream filter chain
   * @return a {@link Mono} that completes when the response pipeline finishes
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    Mono<String> operatorIdMono = getOperatorIdFromSecurityContext();
    Map<String, Object> attributes = exchange.getAttributes();

    return operatorIdMono.flatMap(
        operatorId -> {
          if (operatorId != null && !operatorId.trim().isEmpty()) {
            attributes.put("operatorId", operatorId);
          }

          long backendSentTime = System.currentTimeMillis();
          attributes.put("requestSentTime", String.valueOf(backendSentTime));

          logger.info(GatewayLogFormatter.collectAndFormatRequestLog(request, exchange));

          return chain
              .filter(exchange)
              .doOnSuccess(
                  v -> {
                    attributes.put(
                        "backendResponseTime", String.valueOf(System.currentTimeMillis()));
                  })
              .doFinally(
                  signalType -> {
                    long responseSentTime = System.currentTimeMillis();
                    attributes.put("responseSentTime", String.valueOf(responseSentTime));
                    logger.info(
                        GatewayLogFormatter.collectAndFormatResponseLog(response, exchange));
                  });
        });
  }

  /**
   * Retrieves the operator ID from the reactive SecurityContext.
   *
   * <p>If the authenticated principal is an {@link OAuth2User}, checks attributes in this order:
   *
   * <ol>
   *   <li>{@code operator_id}
   *   <li>{@code open_system_id}
   *   <li>Fallback: {@code preferred_username} or {@code sub}
   * </ol>
   *
   * <p>Returns an empty string if no operator information is found or if the context/authentication
   * is missing. Never returns {@code null}.
   *
   * @return a {@link Mono} emitting the operator ID (possibly empty)
   */
  private Mono<String> getOperatorIdFromSecurityContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(
            securityContext -> {
              if (securityContext != null && securityContext.getAuthentication() != null) {
                var authentication = securityContext.getAuthentication();
                if (logger.isDebugEnabled()) {
                  logger.debug("Authentication type: " + authentication.getClass().getName());
                  logger.debug(
                      "Principal type: " + authentication.getPrincipal().getClass().getName());
                }

                if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                  if (logger.isDebugEnabled()) {
                    logger.debug("OAuth2User attributes: " + oauth2User.getAttributes());
                    logger.debug("OAuth2User name: " + oauth2User.getName());
                  }
                  String opId = oauth2User.getAttribute("operator_id");
                  String sysId = oauth2User.getAttribute("open_system_id");
                  if (logger.isDebugEnabled()) {
                    logger.debug("operator_id from OAuth2User: " + opId);
                    logger.debug("open_system_id from OAuth2User: " + sysId);
                  }
                  // Check alternative claim names that might contain operator information
                  if (opId == null && sysId == null) {
                    // Try other common claim names
                    Object sub = oauth2User.getAttribute("sub");
                    Object preferred_username = oauth2User.getAttribute("preferred_username");
                    Object email = oauth2User.getAttribute("email");
                    Object client_id = oauth2User.getAttribute("client_id");
                    if (logger.isDebugEnabled()) {
                      logger.debug(
                          "Alternative claims - sub: "
                              + sub
                              + ", preferred_username: "
                              + preferred_username
                              + ", email: "
                              + email
                              + ", client_id: "
                              + client_id);
                    }

                    // Use sub or preferred_username as fallback
                    if (preferred_username != null) {
                      return preferred_username.toString();
                    } else if (sub != null) {
                      return sub.toString();
                    }
                  }

                  return opId != null ? opId : (sysId != null ? sysId : "");
                } else {
                  if (logger.isDebugEnabled()) {
                    logger.debug("Principal is not OAuth2User: " + authentication.getPrincipal());
                  }
                }
              } else {
                if (logger.isDebugEnabled()) {
                  logger.debug("SecurityContext or Authentication is null");
                }
              }
              return "";
            })
        .defaultIfEmpty("");
  }
}
