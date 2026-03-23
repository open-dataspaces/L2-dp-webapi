package nedo.ods.svc.dp.authzen.context;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nedo.ods.svc.dp.authzen.model.Context;

/**
 * Comprehensive unit tests for {@link ContextFactory} interface.
 *
 * <p>This test suite verifies the contract, extensibility, and error handling of ContextFactory
 * implementations.
 */
class ContextFactoryTest {

  /**
   * Tests for interface contract and structure.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTest {
    /**
     * Verifies that ContextFactory is a public interface.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should be a public interface")
    void shouldBePublicInterface() {
      Class<?> clazz = ContextFactory.class;
      assertTrue(clazz.isInterface(), "ContextFactory must be an interface");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "ContextFactory must be public");
    }

    /**
     * Verifies that createContext method has correct signature.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should have createContext method with correct signature")
    void shouldHaveCreateContextMethodWithCorrectSignature() throws Exception {
      Method m = ContextFactory.class.getMethod("createContext");
      assertEquals(Context.class, m.getReturnType(), "createContext must return Context");
      assertEquals(0, m.getParameterCount(), "createContext must have no parameters");
    }

    /**
     * Verifies that ContextFactory is a functional interface (only one abstract method).
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should be a functional interface")
    void shouldBeFunctionalInterface() {
      long abstractCount = 0;
      for (Method m : ContextFactory.class.getMethods()) {
        if (Modifier.isAbstract(m.getModifiers())
            && m.getDeclaringClass() == ContextFactory.class) {
          abstractCount++;
        }
      }
      assertEquals(1, abstractCount, "ContextFactory must have exactly one abstract method");
    }
  }

  /**
   * Tests for mock and lambda implementations.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTest {
    /**
     * Verifies that ContextFactory can be implemented using a lambda expression.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should allow lambda implementation")
    void shouldAllowLambdaImplementation() {
      ContextFactory factory = () -> new Context(Map.of("key", "value"));
      Context ctx = factory.createContext();
      assertEquals(
          "value",
          ctx.getAttributes().get("key"),
          "Lambda implementation should produce correct context");
    }

    /**
     * Verifies that ContextFactory can be implemented using an anonymous class.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should allow anonymous class implementation")
    void shouldAllowAnonymousClassImplementation() {
      ContextFactory factory =
          new ContextFactory() {
            @Override
            public Context createContext() {
              Map<String, Object> attrs = new HashMap<>();
              attrs.put("foo", 123);
              return new Context(attrs);
            }
          };
      Context ctx = factory.createContext();
      assertEquals(
          123, ctx.getAttributes().get("foo"), "Anonymous class should produce correct context");
    }

    /**
     * Verifies that implementation does not return null even on error.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should handle exceptions and return empty context")
    void shouldHandleExceptionsAndReturnEmptyContext() {
      ContextFactory factory =
          () -> {
            try {
              throw new RuntimeException("Simulated error");
            } catch (Exception e) {
              return new Context(null); // Should return empty context, not null
            }
          };
      Context ctx = factory.createContext();
      assertNotNull(ctx, "ContextFactory must not return null even on error");
      assertTrue(
          ctx.getAttributes().isEmpty(), "ContextFactory should return empty context on error");
    }
  }

  /**
   * Tests for integration and extensibility.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("Integration and Extensibility Tests")
  class IntegrationContractTest {
    /**
     * Verifies a timestamp context factory implementation.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create timestamp context")
    void shouldCreateTimestampContext() {
      ContextFactory factory =
          () -> new Context(Map.of("current_time", Instant.now().toString(), "timezone", "UTC"));
      Context ctx = factory.createContext();
      assertTrue(
          ctx.getAttributes().containsKey("current_time"),
          "Timestamp context must contain 'current_time'");
      assertEquals(
          "UTC", ctx.getAttributes().get("timezone"), "Timestamp context must contain 'timezone'");
    }

    /**
     * Verifies a request metadata context factory implementation.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create request metadata context")
    void shouldCreateRequestMetadataContext() {
      ContextFactory factory =
          () ->
              new Context(
                  Map.of(
                      "source_ip", "127.0.0.1",
                      "user_agent", "JUnit/5.0",
                      "request_id", "req-001"));
      Context ctx = factory.createContext();
      assertEquals(
          "127.0.0.1",
          ctx.getAttributes().get("source_ip"),
          "Request context must contain 'source_ip'");
      assertEquals(
          "JUnit/5.0",
          ctx.getAttributes().get("user_agent"),
          "Request context must contain 'user_agent'");
      assertEquals(
          "req-001",
          ctx.getAttributes().get("request_id"),
          "Request context must contain 'request_id'");
    }

    /**
     * Verifies a context factory that dynamically generates multiple attributes.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should create dynamic multi-attribute context")
    void shouldCreateDynamicMultiAttributeContext() {
      ContextFactory factory =
          () -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("timestamp", Instant.now().toString());
            attrs.put("risk_score", 0.7);
            attrs.put("department", "IT");
            return new Context(attrs);
          };
      Context ctx = factory.createContext();
      assertTrue(
          ctx.getAttributes().containsKey("timestamp"), "Dynamic context must contain 'timestamp'");
      assertEquals(
          0.7, ctx.getAttributes().get("risk_score"), "Dynamic context must contain 'risk_score'");
      assertEquals(
          "IT", ctx.getAttributes().get("department"), "Dynamic context must contain 'department'");
    }

    /**
     * Verifies that the ContextFactory implementation is thread-safe under concurrent access.
     *
     * @throws InterruptedException if the thread is interrupted while waiting for completion
     * @since 1.0
     */
    @Test
    @DisplayName("Should be thread-safe if implemented properly (normal case)")
    void shouldBeThreadSafeIfImplementedProperly_NormalCase() throws InterruptedException {
      ContextFactory factory =
          () -> new Context(Map.of("thread", Thread.currentThread().getName()));
      int threadCount = 10;
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicReference<Exception> error = new AtomicReference<>();
      for (int i = 0; i < threadCount; i++) {
        new Thread(
                () -> {
                  try {
                    Context ctx = factory.createContext();
                    assertNotNull(ctx, "Context must not be null in concurrent environment");
                    assertTrue(
                        ctx.getAttributes().containsKey("thread"),
                        "Context must contain 'thread' attribute");
                  } catch (Exception e) {
                    error.set(e);
                  } finally {
                    latch.countDown();
                  }
                })
            .start();
      }
      latch.await();
      if (error.get() != null) throw new AssertionError(error.get());
    }
  }

  /**
   * Verifies that exceptions thrown by the ContextFactory implementation in concurrent threads are
   * properly captured and handled.
   *
   * @throws InterruptedException if the thread is interrupted while waiting for completion
   * @since 1.0
   */
  @Test
  @DisplayName("Should handle exceptions in threads (error case)")
  void shouldBeThreadSafeIfImplementedProperly_ErrorCase() throws InterruptedException {
    ContextFactory factory =
        () -> {
          throw new RuntimeException("Simulated error");
        };
    int threadCount = 3;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicReference<Exception> error = new AtomicReference<>();
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  factory.createContext();
                } catch (Exception e) {
                  error.set(e);
                } finally {
                  latch.countDown();
                }
              })
          .start();
    }
    latch.await();
    assertNotNull(error.get(), "Error should be captured when exception occurs in thread");
  }

  /**
   * Tests for abstraction and extensibility benefits.
   *
   * @since 1.0
   */
  @Nested
  @DisplayName("Abstraction and Extensibility Tests")
  class AbstractionBenefitsTest {
    /**
     * Verifies that multiple ContextFactory implementations can coexist and be switched.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should enable multiple implementations")
    void shouldEnableMultipleImplementations() {
      ContextFactory timestampFactory = () -> new Context(Map.of("ts", Instant.now().toString()));
      ContextFactory deptFactory = () -> new Context(Map.of("department", "HR"));
      assertNotNull(
          timestampFactory.createContext().getAttributes().get("ts"),
          "TimestampFactory must produce 'ts'");
      assertEquals(
          "HR",
          deptFactory.createContext().getAttributes().get("department"),
          "DeptFactory must produce 'department'");
    }

    /**
     * Verifies that the factory selection logic works correctly when the config is "timestamp".
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should support factory selection by config (timestamp case)")
    void shouldSupportFactorySelectionByConfig_Timestamp() {
      String config = "timestamp";
      ContextFactory factory;
      if ("timestamp".equals(config)) {
        factory = () -> new Context(Map.of("ts", Instant.now().toString()));
      } else {
        factory = () -> new Context(Map.of("default", true));
      }
      assertTrue(
          factory.createContext().getAttributes().containsKey("ts"),
          "Factory selection must work by config (timestamp)");
    }

    /**
     * Verifies that the factory selection logic works correctly when the config is not "timestamp".
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should support factory selection by config (default case)")
    void shouldSupportFactorySelectionByConfig_Default() {
      String config = "other";
      ContextFactory factory;
      if ("timestamp".equals(config)) {
        factory = () -> new Context(Map.of("ts", Instant.now().toString()));
      } else {
        factory = () -> new Context(Map.of("default", true));
      }
      assertTrue(
          factory.createContext().getAttributes().containsKey("default"),
          "Factory selection must work by config (default)");
    }

    /**
     * Verifies that error handling can be customized per implementation.
     *
     * @since 1.0
     */
    @Test
    @DisplayName("Should enable custom error handling")
    void shouldEnableCustomErrorHandling() {
      ContextFactory safeFactory =
          () -> {
            try {
              throw new IllegalStateException("Simulated error");
            } catch (Exception e) {
              return new Context(Map.of("error", e.getMessage()));
            }
          };
      Context ctx = safeFactory.createContext();
      assertEquals(
          "Simulated error",
          ctx.getAttributes().get("error"),
          "Custom error handling must propagate error message");
    }
  }
}
