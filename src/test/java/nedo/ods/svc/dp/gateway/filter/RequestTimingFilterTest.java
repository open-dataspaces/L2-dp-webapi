package nedo.ods.svc.dp.gateway.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Tests for {@link RequestTimingFilter}.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Non-actuator requests set {@code requestReceivedTime}.
 *   <li>Actuator requests skip these attributes.
 * </ul>
 */
class RequestTimingFilterTest {

  private RequestTimingFilter filter;

  /** Creates a new filter before each test. */
  @BeforeEach
  void setUp() {
    filter = new RequestTimingFilter();
  }

  /**
   * Non-actuator request should.
   *
   * <ul>
   *   <li>Contain {@code requestReceivedTime}.
   *   <li>{@code requestReceivedTime} is parseable as epoch millis.
   * </ul>
   */
  @Test
  void nonActuator_setsTimingAndTracking() {
    ServerHttpRequest request = MockServerHttpRequest.get("/api/data").build();
    ServerWebExchange exchange = MockServerWebExchange.from((MockServerHttpRequest) request);

    WebFilterChain chain = Mockito.mock(WebFilterChain.class);
    Mockito.when(chain.filter(Mockito.any())).thenReturn(Mono.empty());

    Mono<Void> result = filter.filter(exchange, chain);
    assertNotNull(result);
    result.block();

    var attrs = exchange.getAttributes();
    assertTrue(attrs.containsKey("requestReceivedTime"));

    Object ts = attrs.get("requestReceivedTime");
    assertTrue(ts instanceof String);
    assertDoesNotThrow(() -> Long.parseLong((String) ts));
  }

  /** Actuator request should not set timing or tracking attributes. */
  @Test
  void actuator_skipsAttributes() {
    ServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
    ServerWebExchange exchange = MockServerWebExchange.from((MockServerHttpRequest) request);

    WebFilterChain chain = Mockito.mock(WebFilterChain.class);
    Mockito.when(chain.filter(Mockito.any())).thenReturn(Mono.empty());

    Mono<Void> result = filter.filter(exchange, chain);
    assertNotNull(result);
    result.block();

    var attrs = exchange.getAttributes();
    assertFalse(attrs.containsKey("requestReceivedTime"));
  }

  /** Ensure that chain.filter(exchange) is always called exactly once */
  @Test
  void always_invokes_chain_filter_once() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/ping").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    filter.filter(exchange, chain).block();

    ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain, times(1)).filter(captor.capture());
    assertSame(exchange, captor.getValue());
  }

  /** Explicitly indicate that the /actuator route is also subject to skipping */
  @Test
  void actuator_root_skipsAttributes() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/actuator").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    filter.filter(exchange, chain).block();

    assertFalse(exchange.getAttributes().containsKey("requestReceivedTime"));
  }

  /**
   * The current implementation uses path.startsWith("/actuator"), so /actuatorx is also skipped.
   */
  @Test
  void actuator_like_prefix_also_skipped_by_current_implementation() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/actuatorx/metrics").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = mock(WebFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());

    filter.filter(exchange, chain).block();

    assertFalse(exchange.getAttributes().containsKey("requestReceivedTime"));
  }

  /** Check for the presence of @Component and @Order(HIGHEST_PRECEDENCE) */
  @Test
  void component_and_order_annotations_present() {
    Class<RequestTimingFilter> clazz = RequestTimingFilter.class;
    assertTrue(clazz.isAnnotationPresent(Component.class), "@Component is required");
    Order order = clazz.getAnnotation(Order.class);
    assertNotNull(order, "@Order is required");
    assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value(), "Being of HIGHEST_PRECEDENCE");
  }
}
