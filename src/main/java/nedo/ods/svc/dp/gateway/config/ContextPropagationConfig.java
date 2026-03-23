package nedo.ods.svc.dp.gateway.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import java.util.Map;
import jakarta.annotation.PostConstruct;

/**
 * Configuration for Micrometer Context Propagation to bridge Reactor Context and MDC.
 *
 * <p>This configuration sets up automatic context propagation between Reactor Context and SLF4J's
 * Mapped Diagnostic Context (MDC), enabling seamless context flow in reactive applications.
 */
@Configuration
public class ContextPropagationConfig {

  /**
   * Configures Micrometer Context Propagation for automatic context bridging. This method sets up
   * ThreadLocal accessors for MDC and enables Reactor hooks to automatically propagate context from
   * Reactor Context to MDC.
   */
  @PostConstruct
  void initialize() {
    // Register ThreadLocal accessor for MDC
    ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcThreadLocalAccessor());

    // Enable automatic context propagation in Reactor
    Hooks.enableAutomaticContextPropagation();
  }

  /**
   * ThreadLocal accessor for MDC that enables Micrometer Context Propagation to automatically
   * bridge Reactor Context to MDC.
   */
  private static class MdcThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

    private static final String KEY = "mdc";

    @Override
    public Object key() {
      return KEY;
    }

    @Override
    public Map<String, String> getValue() {
      return MDC.getCopyOfContextMap();
    }

    @Override
    public void setValue(Map<String, String> value) {
      if (value != null) {
        MDC.setContextMap(value);
      } else {
        MDC.clear();
      }
    }

    @Override
    public void setValue() {
      MDC.clear();
    }
  }
}
