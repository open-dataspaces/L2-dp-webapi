package nedo.ods.svc.dp.gateway.error;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link ErrorInjectionGatewayFilterFactory}.
 *
 * <p>This test class verifies:
 *
 * <ul>
 *   <li>Factory instantiation and mapping to the correct configuration class
 *   <li>Getter/Setter behavior of {@link ErrorInjectionGatewayFilterFactory.Config}
 *   <li>Error injection logic for timeout and 503 scenarios
 *   <li>Normal flow when no error is triggered
 * </ul>
 */
@DisplayName("ErrorInjectionGatewayFilterFactory")
class ErrorInjectionGatewayFilterFactoryTest {

  /** Factory instance under test. */
  private final ErrorInjectionGatewayFilterFactory factory =
      new ErrorInjectionGatewayFilterFactory();

  /**
   * Creates a mock {@link ServerWebExchange} for testing.
   *
   * @return a mock exchange instance
   */
  private ServerWebExchange createExchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost").build());
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTest {
    /** Verifies that the factory is instantiated and maps to the correct config class. */
    @Test
    @DisplayName("should instantiate factory with correct config class")
    void shouldInstantiateFactory() {
      assertNotNull(factory);
      assertEquals(ErrorInjectionGatewayFilterFactory.Config.class, factory.getConfigClass());
    }
  }

  @Nested
  @DisplayName("Config Class")
  class ConfigTest {
    /** Tests setting and getting the triggerTimeout property. */
    @Test
    @DisplayName("should set and get triggerTimeout")
    void shouldSetAndGetTriggerTimeout() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTriggerTimeout(true);
      assertTrue(config.isTriggerTimeout());
    }

    /** Tests setting and getting the trigger503 property. */
    @Test
    @DisplayName("should set and get trigger503")
    void shouldSetAndGetTrigger503() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTrigger503(true);
      assertTrue(config.isTrigger503());
    }
  }

  @Nested
  @DisplayName("Filter Behavior")
  class FilterBehaviorTest {

    /** Verifies that default config values are false. */
    @Test
    @DisplayName("should have default config values as false")
    void shouldHaveDefaultConfigValues() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      assertFalse(config.isTriggerTimeout());
      assertFalse(config.isTrigger503());
    }

    /** Verifies that a timeout error is simulated when triggerTimeout is true. */
    @Test
    @DisplayName("should simulate timeout error when triggerTimeout is true")
    void shouldSimulateTimeoutError() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTriggerTimeout(true);

      GatewayFilterChain chain = mock(GatewayFilterChain.class);
      ServerWebExchange exchange = createExchange();

      Mono<Void> result = factory.apply(config).filter(exchange, chain);

      StepVerifier.create(result)
          .expectErrorSatisfies(
              ex -> {
                assertTrue(ex instanceof TimeoutException);
                assertEquals("Simulated timeout", ex.getMessage());
              })
          .verify();
    }

    /** Verifies that a 503 error is simulated when trigger503 is true. */
    @Test
    @DisplayName("should simulate 503 error when trigger503 is true")
    void shouldSimulate503Error() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTrigger503(true);

      GatewayFilterChain chain = mock(GatewayFilterChain.class);
      ServerWebExchange exchange = createExchange();

      Mono<Void> result = factory.apply(config).filter(exchange, chain);

      StepVerifier.create(result)
          .expectErrorSatisfies(
              ex -> {
                assertTrue(ex instanceof WebClientResponseException);
                assertTrue(ex.getMessage().contains("Simulated 503 Service Unavailable"));
              })
          .verify();
    }

    /** Verifies that timeout is prioritized when both triggerTimeout and trigger503 are true. */
    @Test
    @DisplayName("should prioritize timeout when both triggerTimeout and trigger503 are true")
    void shouldPrioritizeTimeoutOver503() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTriggerTimeout(true);
      config.setTrigger503(true);

      GatewayFilterChain chain = mock(GatewayFilterChain.class);
      ServerWebExchange exchange = createExchange();

      Mono<Void> result = factory.apply(config).filter(exchange, chain);

      StepVerifier.create(result)
          .expectErrorSatisfies(
              ex -> {
                assertTrue(ex instanceof TimeoutException);
                assertEquals("Simulated timeout", ex.getMessage());
              })
          .verify();
    }

    /** Verifies that the filter proceeds normally when no error is triggered. */
    @Test
    @DisplayName("should proceed normally when no error is triggered")
    void shouldProceedNormally() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();

      config.setTriggerTimeout(false);
      config.setTrigger503(false);

      GatewayFilterChain chain = mock(GatewayFilterChain.class);
      ServerWebExchange exchange = createExchange();

      when(chain.filter(exchange)).thenReturn(Mono.empty());

      Mono<Void> result = factory.apply(config).filter(exchange, chain);

      StepVerifier.create(result).verifyComplete();

      verify(chain, times(1)).filter(exchange);
    }

    /** Ensures that when triggerTimeout is true, the filter does not proceed to the next chain. */
    @Test
    @DisplayName("should not call chain.filter when triggerTimeout is true")
    void shouldNotCallChainFilterOnTimeout() {
      ErrorInjectionGatewayFilterFactory.Config config =
          new ErrorInjectionGatewayFilterFactory.Config();
      config.setTriggerTimeout(true);

      GatewayFilterChain chain = mock(GatewayFilterChain.class);
      ServerWebExchange exchange = createExchange();

      Mono<Void> result = factory.apply(config).filter(exchange, chain);

      StepVerifier.create(result).expectError(TimeoutException.class).verify();

      verify(chain, never()).filter(exchange);
    }
  }
}
