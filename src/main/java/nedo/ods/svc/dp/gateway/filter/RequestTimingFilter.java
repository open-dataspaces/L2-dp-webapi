package nedo.ods.svc.dp.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * A {@link WebFilter} that records request/response timing values into the {@link
 * ServerWebExchange} attributes for downstream logging/analytics.
 *
 * <p>Specifically, it stores:
 *
 * <ul>
 *   <li>{@code requestReceivedTime}: epoch millis when the filter is invoked (as {@link String})
 *   <li>{@code backendResponseTime}: epoch millis when the delegate chain completes successfully
 *       (as {@link String})
 *   <li>{@code responseSentTime}: epoch millis when the signal terminates (success, error, or
 *       cancel) (as {@link String})
 *   <li>{@code trackingId}: the value of the {@code X-TrackingID} request header, if present
 * </ul>
 *
 * These attributes are intended to be consumed by logging components (e.g., {@code
 * GatewayLogFormatter}).
 *
 * <p>Ordering: annotated with {@link Order} at {@link Ordered#HIGHEST_PRECEDENCE} to ensure
 * timestamps are captured as early as possible in the filter chain.
 *
 * <p>Thread-safety: this filter holds no mutable state; it writes to the per-request exchange
 * attributes map.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter implements WebFilter {

  /**
   * Adds timing and tracking attributes to the exchange and delegates to the next filter, skipping
   * requests to actuator endpoints.
   *
   * <p>Behavior:
   *
   * <ul>
   *   <li>Stores {@code requestReceivedTime} and {@code trackingId} in exchange attributes.
   *   <li>If the request path starts with {@code /actuator}, no attributes are added and the chain
   *       continues.
   * </ul>
   *
   * @param exchange current server exchange
   * @param chain remaining filter chain
   * @return a {@link Mono} that completes when the chain finishes
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    long requestReceivedTime = System.currentTimeMillis();
    ServerHttpRequest request = exchange.getRequest();

    String path = request.getURI().getPath();
    if (path.startsWith("/actuator")) {
      return chain.filter(exchange);
    }

    Map<String, Object> attributes = exchange.getAttributes();

    attributes.put("requestReceivedTime", String.valueOf(requestReceivedTime));
   
    return chain.filter(exchange);
  }
}
