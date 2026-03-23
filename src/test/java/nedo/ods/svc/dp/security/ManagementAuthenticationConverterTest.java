package nedo.ods.svc.dp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link ManagementAuthenticationConverter}
 *
 * <p>This test verifies whether the {@code convert} method correctly reads the {@code X-API-KEY}
 * header and generates an {@link ApiKeyAuthenticationToken}.
 */
class ManagementAuthenticationConverterTest {

  /**
   * A test to confirm that the {@code convert} method reads the {@code X-API-KEY} header and
   * returns an {@link ApiKeyAuthenticationToken}.
   */
  @Test
  void testConvertWithApiKeyHeader() {

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-API-KEY", "test-api-key");

    ServerHttpRequest request = mock(ServerHttpRequest.class);
    when(request.getHeaders()).thenReturn(headers);

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(request);

    ManagementAuthenticationConverter converter = new ManagementAuthenticationConverter();
    Mono<Authentication> resultMono = converter.convert(exchange);

    Authentication result = resultMono.block();
    assertNotNull(result);
    assertTrue(result instanceof ApiKeyAuthenticationToken);
    assertEquals(null, result.getCredentials());
  }

  /**
   * Test to confirm that the {@code convert} method returns an empty result when the {@code
   * X-API-KEY} header is not present.
   */
  @Test
  void testConvertWithoutApiKeyHeader() {
    HttpHeaders headers = new HttpHeaders();

    ServerHttpRequest request = mock(ServerHttpRequest.class);
    when(request.getHeaders()).thenReturn(headers);

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(request);

    ManagementAuthenticationConverter converter = new ManagementAuthenticationConverter();
    Mono<Authentication> resultMono = converter.convert(exchange);

    Authentication result = resultMono.block();
    assertNull(result);
  }
}
