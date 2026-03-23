package nedo.ods.svc.dp.gateway.error;

import java.util.concurrent.TimeoutException;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Gateway filter factory for injecting errors into the request flow for testing purposes.
 *
 * <p><b>This class is a mock implementation intended for testing error handling in Spring Cloud
 * Gateway.</b> It can simulate timeout and 503 Service Unavailable errors by throwing exceptions
 * during filter execution.
 *
 * <p><b>How to use (mock):</b><br>
 * Configure this filter in your route definition with <code>triggerTimeout</code> or <code>
 * trigger503</code> set to true to simulate a timeout or 503 error respectively. This allows you to
 * verify error handling logic in your gateway.
 */
@Component
public class ErrorInjectionGatewayFilterFactory
    extends AbstractGatewayFilterFactory<ErrorInjectionGatewayFilterFactory.Config> {

  /**
   * Constructs a new ErrorInjectionGatewayFilterFactory.
   *
   * <p>This constructor registers the {@link Config} class for filter configuration.
   */
  public ErrorInjectionGatewayFilterFactory() {
    super(Config.class);
  }

  /**
   * Configuration class for {@link ErrorInjectionGatewayFilterFactory}.
   *
   * <p>Use {@code triggerTimeout} to simulate a timeout error, and {@code trigger503} to simulate a
   * 503 Service Unavailable error.
   */
  public static class Config {
    private boolean triggerTimeout;
    private boolean trigger503;

    /**
     * Returns whether to trigger a timeout error.
     *
     * @return true if timeout error should be triggered
     */
    public boolean isTriggerTimeout() {
      return triggerTimeout;
    }

    /**
     * Sets whether to trigger a timeout error.
     *
     * @param triggerTimeout true to trigger timeout error
     */
    public void setTriggerTimeout(boolean triggerTimeout) {
      this.triggerTimeout = triggerTimeout;
    }

    /**
     * Returns whether to trigger a 503 Service Unavailable error.
     *
     * @return true if 503 error should be triggered
     */
    public boolean isTrigger503() {
      return trigger503;
    }

    /**
     * Sets whether to trigger a 503 Service Unavailable error.
     *
     * @param trigger503 true to trigger 503 error
     */
    public void setTrigger503(boolean trigger503) {
      this.trigger503 = trigger503;
    }
  }

  /**
   * Applies the error injection filter based on the provided configuration.
   *
   * <p>If {@code triggerTimeout} is true, this filter will throw a simulated {@link
   * TimeoutException}. If {@code trigger503} is true, this filter will throw a simulated 503
   * Service Unavailable error. Otherwise, the request will proceed normally.
   *
   * @param config the filter configuration specifying which error to inject
   * @return a {@link GatewayFilter} that injects the specified error or proceeds with the request
   *     chain
   */
  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      if (config.triggerTimeout) {
        return Mono.error(new TimeoutException("Simulated timeout"));
      }
      if (config.trigger503) {
        return Mono.error(
            WebClientResponseException.create(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Simulated 503 Service Unavailable",
                HttpHeaders.EMPTY,
                null,
                null));
      }
      return chain.filter(exchange);
    };
  }
}
