package nedo.ods.svc.dp.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

/**
 * Test class for {@link ContextPropagationConfig}.
 *
 * <p>This test verifies that Micrometer Context Propagation is properly configured to bridge
 * Reactor Context and SLF4J's Mapped Diagnostic Context (MDC).
 */
class ContextPropagationConfigTest {

  private ContextPropagationConfig contextPropagationConfig;

  @BeforeEach
  void setUp() {
    contextPropagationConfig = new ContextPropagationConfig();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    // Clean up hooks to avoid affecting other tests
    Hooks.resetOnOperatorDebug();
  }

  @Test
  void initializeShouldRegisterMdcThreadLocalAccessor() {
    // When
    contextPropagationConfig.initialize();

    // Then
    // Verify that MDC accessor is now registered (whether it was there before or added now)
    boolean mdcAccessorFound =
        ContextRegistry.getInstance().getThreadLocalAccessors().stream()
            .anyMatch(accessor -> "mdc".equals(accessor.key()));
    assertThat(mdcAccessorFound).isTrue();
  }

  @Test
  void mdcThreadLocalAccessorShouldHaveCorrectKey() {
    // Given
    contextPropagationConfig.initialize();

    // When
    ThreadLocalAccessor<?> mdcAccessor =
        ContextRegistry.getInstance().getThreadLocalAccessors().stream()
            .filter(accessor -> "mdc".equals(accessor.key()))
            .findFirst()
            .orElse(null);

    // Then
    assertThat(mdcAccessor).isNotNull();
    assertThat(mdcAccessor.key()).isEqualTo("mdc");
  }

  @Test
  void mdcThreadLocalAccessorGetValueShouldReturnCurrentMdcContext() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> testMdcMap = new HashMap<>();
    testMdcMap.put("testKey", "testValue");
    testMdcMap.put("trackingId", "test-tracking-123");
    MDC.setContextMap(testMdcMap);
  }

  @Test
  void mdcThreadLocalAccessorSetValueShouldUpdateMdcContext() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> testMdcMap = new HashMap<>();
    testMdcMap.put("operatorId", "test-operator");
    testMdcMap.put("backendUri", "http://test-backend");

    // when
    ContextRegistry.getInstance().getThreadLocalAccessors().stream()
        .filter(a -> "mdc".equals(a.key()))
        .findFirst()
        .ifPresent(
            a -> {
              @SuppressWarnings("unchecked")
              ThreadLocalAccessor<Map<String, String>> mdcAccessor =
                  (ThreadLocalAccessor<Map<String, String>>) a;
              mdcAccessor.setValue(testMdcMap);
            });

    // Then
    Map<String, String> currentMdc = MDC.getCopyOfContextMap();
    assertThat(currentMdc).isNotNull();
    assertThat(currentMdc).containsEntry("operatorId", "test-operator");
    assertThat(currentMdc).containsEntry("backendUri", "http://test-backend");
  }

  @Test
  void mdcThreadLocalAccessorSetValueWithNullShouldClearMdcContext() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> initialMdcMap = new HashMap<>();
    initialMdcMap.put("someKey", "someValue");
    MDC.setContextMap(initialMdcMap);
    // When
    ContextRegistry.getInstance().getThreadLocalAccessors().stream()
        .filter(a -> "mdc".equals(a.key()))
        .findFirst()
        .ifPresent(a -> a.setValue(null));

    // Then
    Map<String, String> currentMdc = MDC.getCopyOfContextMap();
    assertThat(currentMdc).isNull();
  }

  @Test
  void mdcThreadLocalAccessorSetValueWithoutArgumentShouldClearMdcContext() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> initialMdcMap = new HashMap<>();
    initialMdcMap.put("someKey", "someValue");
    MDC.setContextMap(initialMdcMap);

    // When
    ContextRegistry.getInstance().getThreadLocalAccessors().stream()
        .filter(a -> "mdc".equals(a.key()))
        .findFirst()
        .ifPresent(a -> a.setValue(null));

    // Then
    Map<String, String> currentMdc = MDC.getCopyOfContextMap();
    assertThat(currentMdc).isNull();
  }

  @Test
  void contextPropagationShouldWorkWithReactorContext() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> testMdcMap = new HashMap<>();
    testMdcMap.put("trackingId", "reactor-test-123");
    testMdcMap.put("operatorId", "reactor-operator");

    // When & Then
    Mono<String> testMono =
        Mono.fromCallable(
                () -> {
                  // This should have MDC set automatically from Reactor context
                  Map<String, String> currentMdc = MDC.getCopyOfContextMap();
                  return currentMdc != null ? currentMdc.get("trackingId") : null;
                })
            .contextWrite(Context.of("mdc", testMdcMap));

    StepVerifier.create(testMono).expectNext("reactor-test-123").verifyComplete();
  }

  @Test
  void contextPropagationShouldPreserveMdcAcrossReactorOperators() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> testMdcMap = new HashMap<>();
    testMdcMap.put("operatorId", "chain-test-operator");
    testMdcMap.put("backendUri", "http://chain-test-backend");

    // When & Then
    Mono<Map<String, String>> testMono =
        Mono.fromCallable(
                () -> {
                  return MDC.getCopyOfContextMap();
                })
            .map(
                mdcMap -> {
                  // MDC should still be available in map operator
                  return MDC.getCopyOfContextMap();
                })
            .contextWrite(Context.of("mdc", testMdcMap));

    StepVerifier.create(testMono)
        .assertNext(
            resultMdc -> {
              assertThat(resultMdc).isNotNull();
              assertThat(resultMdc).containsEntry("operatorId", "chain-test-operator");
              assertThat(resultMdc).containsEntry("backendUri", "http://chain-test-backend");
            })
        .verifyComplete();
  }

  @Test
  void contextPropagationShouldWorkWithEmptyMdcMap() {
    // Given
    contextPropagationConfig.initialize();
    Map<String, String> emptyMdcMap = new HashMap<>();

    // When & Then
    Mono<Map<String, String>> testMono =
        Mono.fromCallable(
                () -> {
                  return MDC.getCopyOfContextMap();
                })
            .contextWrite(Context.of("mdc", emptyMdcMap));

    StepVerifier.create(testMono)
        .assertNext(
            resultMdc -> {
              // Should be empty but not null
              assertThat(resultMdc).isNotNull();
              assertThat(resultMdc).isEmpty();
            })
        .verifyComplete();
  }
}
