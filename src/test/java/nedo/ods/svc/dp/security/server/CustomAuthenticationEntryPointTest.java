package nedo.ods.svc.dp.security.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import nedo.ods.svc.dp.gateway.error.GlobalErrorHandler;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link GlobalErrorHandler}.
 *
 * <p>This test class verifies that the GlobalErrorHandler returns the correct HTTP status and JSON
 * error response for various exception scenarios, such as 404 Not Found, connection errors,
 * timeouts, service unavailable, and others.
 *
 * <p>Each test simulates a specific exception and asserts that the response contains the expected
 * status code and error message.
 */
class CustomAuthenticationEntryPointTest {

  @Test
  void testCommenceWithInvalidBearerTokenException() {
    CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AuthenticationException ex = new InvalidBearerTokenException("Invalid token");

    Mono<Void> result = entryPoint.commence(exchange, ex);

    StepVerifier.create(result).expectComplete().verify();

    String body = exchange.getResponse().getBodyAsString().block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertNotNull(body);
    assertTrue(body.contains("[auth] Unauthorized"));
    assertTrue(body.contains("Invalid or expired token"));
  }
}
