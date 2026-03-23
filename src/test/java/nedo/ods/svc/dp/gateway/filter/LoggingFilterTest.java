package nedo.ods.svc.dp.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

import reactor.core.publisher.Mono;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link LoggingFilter}.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>{@code getOrder()} returns {@code ROUTE_TO_URL_FILTER_ORDER + 1}.
 *   <li>{@code operatorId} resolution from OAuth2User attributes (operator_id, open_system_id,
 *       preferred_username, sub).
 *   <li>Fallback behavior when principal is not OAuth2User or empty context.
 *   <li>Timestamps {@code requestSentTime}, {@code backendResponseTime}, {@code responseSentTime}
 *       are set correctly.
 *   <li>Error case: backendResponseTime missing but responseSentTime present.
 * </ul>
 */
class LoggingFilterTest {

  /** Ensures getOrder returns ROUTE_TO_URL_FILTER_ORDER + 1. */
  @Test
  void getOrder_returnsRouteToUrlPlusOne() {
    LoggingFilter f = new LoggingFilter();
    assertEquals(RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1, f.getOrder());
  }

  /** OperatorId from operator_id attribute; timestamps set. */
  @Test
  void filter_success_operatorIdFromOperatorIdAttribute() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/a").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    Map<String, Object> attrs = ex.getAttributes();
    assertFalse(attrs.containsKey("operatorId"));
    OAuth2User user =
        new DefaultOAuth2User(
            AuthorityUtils.NO_AUTHORITIES, Map.of("operator_id", "op-1"), "operator_id");
    Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    Mono<Void> m =
        f.filter(ex, chain).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    m.block();
    assertEquals("op-1", attrs.get("operatorId"));
    assertNotNull(attrs.get("requestSentTime"));
    assertNotNull(attrs.get("backendResponseTime"));
    assertNotNull(attrs.get("responseSentTime"));
    assertDoesNotThrow(() -> Long.parseLong((String) attrs.get("requestSentTime")));
    assertDoesNotThrow(() -> Long.parseLong((String) attrs.get("backendResponseTime")));
    assertDoesNotThrow(() -> Long.parseLong((String) attrs.get("responseSentTime")));
  }

  /** OperatorId from open_system_id when operator_id missing. */
  @Test
  void filter_success_operatorIdFromOpenSystemIdWhenOperatorIdMissing() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/b").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    OAuth2User user =
        new DefaultOAuth2User(
            AuthorityUtils.NO_AUTHORITIES, Map.of("open_system_id", "sys-9"), "open_system_id");
    Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();
    assertEquals("sys-9", ex.getAttributes().get("operatorId"));
  }

  /** OperatorId fallback to preferred_username. */
  @Test
  void filter_success_operatorIdFallbackPreferredUsername() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/c").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    Map<String, Object> map = new HashMap<>();
    map.put("preferred_username", "u-777");
    OAuth2User user =
        new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, map, "preferred_username");
    Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();
    assertEquals("u-777", ex.getAttributes().get("operatorId"));
  }

  /** OperatorId fallback to sub. */
  @Test
  void filter_success_operatorIdFallbackSub() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/d").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    Map<String, Object> map = new HashMap<>();
    map.put("sub", "sub-123");
    OAuth2User user = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, map, "sub");
    Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();
    assertEquals("sub-123", ex.getAttributes().get("operatorId"));
  }

  /** Principal not OAuth2User → operatorId not stored. */
  @Test
  void filter_success_principalNotOAuth2User_operatorIdNotStored() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/e").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    Authentication auth = new UsernamePasswordAuthenticationToken("str-principal", null);
    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();
    assertFalse(ex.getAttributes().containsKey("operatorId"));
  }

  /** Empty security context → operatorId not stored; timestamps set. */
  @Test
  void filter_success_emptySecurityContext_operatorIdNotStored() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/f").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    f.filter(ex, chain).block();
    assertFalse(ex.getAttributes().containsKey("operatorId"));
    assertNotNull(ex.getAttributes().get("requestSentTime"));
    assertNotNull(ex.getAttributes().get("backendResponseTime"));
    assertNotNull(ex.getAttributes().get("responseSentTime"));
  }

  /** OperatorId is whitespace → not stored. */
  @Test
  void filter_success_operatorIdWhitespace_notStored() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/g").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();
    Map<String, Object> map = new HashMap<>();
    map.put("preferred_username", "  ");
    OAuth2User user =
        new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, map, "preferred_username");
    Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();
    assertFalse(ex.getAttributes().containsKey("operatorId"));
  }

  /**
   * Ensures operatorId is not stored when a security context exists but {@code Authentication} is
   * {@code null}. Confirms timestamps are set. This reaches the branch that logs "SecurityContext
   * or Authentication is null".
   */
  @Test
  void filter_success_securityContextPresent_butAuthenticationNull_operatorIdNotStored() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/auth-null").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.empty();

    var sc = new SecurityContextImpl(null);

    f.filter(ex, chain)
        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(sc)))
        .block();

    assertFalse(ex.getAttributes().containsKey("operatorId"));
    assertNotNull(ex.getAttributes().get("requestSentTime"));
    assertNotNull(ex.getAttributes().get("backendResponseTime"));
    assertNotNull(ex.getAttributes().get("responseSentTime"));
  }

  /** Error case: backendResponseTime missing but responseSentTime set. */
  @Test
  void filter_error_backendResponseTimeNotSetButResponseSentTimeIsSet() {
    LoggingFilter f = new LoggingFilter();
    ServerHttpRequest req = MockServerHttpRequest.get("/h").build();
    ServerWebExchange ex = MockServerWebExchange.from((MockServerHttpRequest) req);
    GatewayFilterChain chain = e -> Mono.error(new IllegalStateException("boom"));
    assertThrows(IllegalStateException.class, () -> f.filter(ex, chain).block());
    assertNotNull(ex.getAttributes().get("requestSentTime"));
    assertNull(ex.getAttributes().get("backendResponseTime"));
    assertNotNull(ex.getAttributes().get("responseSentTime"));
  }

  @Test
  void filter_success_withRealResponse_commitsAndHitsResponseLogLine() {
    LoggingFilter f = new LoggingFilter();

    WebHandler handler =
        exchange -> {
          exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };

    WebTestClient client =
        WebTestClient.bindToWebHandler(
                exchange -> f.filter(exchange, e -> handler.handle(exchange)))
            .build();

    client.get().uri("/ok").exchange().expectStatus().isOk();
  }

}
