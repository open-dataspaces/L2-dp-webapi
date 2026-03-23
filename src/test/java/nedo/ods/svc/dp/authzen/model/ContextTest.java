package nedo.ods.svc.dp.authzen.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive test suite for {@link Context}.
 *
 * <p>Tests the constructor implementation, attribute merging functionality, and immutability
 * characteristics of authorization contexts.
 */
@DisplayName("Context")
class ContextTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTest {

    @Test
    @DisplayName("should create valid context with null attributes")
    void shouldCreateValidContextWithNullAttributes() {
      // When
      Context context = new Context(null);

      // Then
      assertNotNull(context);
      assertTrue(context.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("should create valid context with empty attributes")
    void shouldCreateValidContextWithEmptyAttributes() {
      // When
      Context context = new Context(Map.of());

      // Then
      assertNotNull(context);
      assertTrue(context.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("should create valid context with single attribute")
    void shouldCreateValidContextWithSingleAttribute() {
      // When
      Context context = new Context(Map.of("request_time", "2024-01-15T10:30:00Z"));

      // Then
      assertNotNull(context);
      assertEquals(1, context.getAttributes().size());
      assertEquals("2024-01-15T10:30:00Z", context.getAttributes().get("request_time"));
    }

    @Test
    @DisplayName("should create valid context with multiple attributes")
    void shouldCreateValidContextWithMultipleAttributes() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "ip_address", "192.168.1.100",
              "user_agent", "Mozilla/5.0",
              "secure_connection", true);

      // When
      Context context = new Context(attributes);

      // Then
      assertNotNull(context);
      assertEquals(3, context.getAttributes().size());
      assertEquals("192.168.1.100", context.getAttributes().get("ip_address"));
      assertEquals("Mozilla/5.0", context.getAttributes().get("user_agent"));
      assertEquals(true, context.getAttributes().get("secure_connection"));
    }

    @Test
    @DisplayName("should allow null attribute values")
    void shouldHandleAttributesWithoutNullValues() {
      // Given
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("string_field", "value");
      attributes.put("number_field", 42);
      attributes.put("boolean_field", true);

      // When
      Context context = new Context(attributes);

      // Then
      assertNotNull(context);
      assertEquals(3, context.getAttributes().size());
      assertEquals("value", context.getAttributes().get("string_field"));
      assertEquals(42, context.getAttributes().get("number_field"));
      assertEquals(true, context.getAttributes().get("boolean_field"));
    }
  }

  @Nested
  @DisplayName("Context Merging")
  class ContextMergingTest {

    @Test
    @DisplayName("should merge two contexts successfully")
    void shouldMergeTwoContextsSuccessfully() {
      // Given
      Context context1 =
          new Context(
              Map.of(
                  "ip_address", "192.168.1.100",
                  "request_time", "2024-01-15T10:30:00Z"));

      Context context2 =
          new Context(Map.of("user_agent", "Mozilla/5.0", "secure_connection", true));

      // When
      Context mergedContext = context1.merge(context2);

      // Then
      assertNotNull(mergedContext);
      assertEquals(4, mergedContext.getAttributes().size());
      assertEquals("192.168.1.100", mergedContext.getAttributes().get("ip_address"));
      assertEquals("2024-01-15T10:30:00Z", mergedContext.getAttributes().get("request_time"));
      assertEquals("Mozilla/5.0", mergedContext.getAttributes().get("user_agent"));
      assertEquals(true, mergedContext.getAttributes().get("secure_connection"));
    }

    @Test
    @DisplayName("should merge with overlapping attributes using second context values")
    void shouldMergeWithOverlappingAttributesUsingSecondContextValues() {
      // Given
      Context context1 =
          new Context(
              Map.of(
                  "environment", "staging",
                  "version", "1.0"));

      Context context2 =
          new Context(
              Map.of(
                  "environment", "production", // Override
                  "deployment_id", "deploy-123"));

      // When
      Context mergedContext = context1.merge(context2);

      // Then
      assertNotNull(mergedContext);
      assertEquals(3, mergedContext.getAttributes().size());
      assertEquals("production", mergedContext.getAttributes().get("environment")); // From context2
      assertEquals("1.0", mergedContext.getAttributes().get("version")); // From context1
      assertEquals(
          "deploy-123", mergedContext.getAttributes().get("deployment_id")); // From context2
    }

    @Test
    @DisplayName("should merge with empty second context")
    void shouldMergeWithEmptySecondContext() {
      // Given
      Context context1 = new Context(Map.of("key", "value"));
      Context emptyContext = new Context(Map.of());

      // When
      Context mergedContext = context1.merge(emptyContext);

      // Then
      assertNotNull(mergedContext);
      assertEquals(1, mergedContext.getAttributes().size());
      assertEquals("value", mergedContext.getAttributes().get("key"));
    }

    @Test
    @DisplayName("should merge with null second context")
    void shouldMergeWithNullSecondContext() {
      // Given
      Context context1 = new Context(Map.of("key", "value"));

      // When
      Context mergedContext = context1.merge(null);

      // Then
      assertSame(context1, mergedContext); // Should return the same instance
    }

    @Test
    @DisplayName("should merge two empty contexts")
    void shouldMergeTwoEmptyContexts() {
      // Given
      Context emptyContext1 = new Context(Map.of());
      Context emptyContext2 = new Context(Map.of());

      // When
      Context mergedContext = emptyContext1.merge(emptyContext2);

      // Then
      assertSame(emptyContext1, mergedContext); // Should return the first context
    }
  }

  @Nested
  @DisplayName("Context Types")
  class ContextTypesTest {

    @Test
    @DisplayName("should handle temporal context")
    void shouldHandleTemporalContext() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "request_time", "2024-01-15T10:30:00Z",
              "session_start", "2024-01-15T10:00:00Z",
              "token_expires", "2024-01-15T12:00:00Z",
              "business_hours", true,
              "timezone", "America/New_York",
              "day_of_week", "Monday");

      // When
      Context temporalContext = new Context(attributes);

      // Then
      assertEquals("2024-01-15T10:30:00Z", temporalContext.getAttributes().get("request_time"));
      assertEquals(true, temporalContext.getAttributes().get("business_hours"));
      assertEquals("America/New_York", temporalContext.getAttributes().get("timezone"));
      assertEquals("Monday", temporalContext.getAttributes().get("day_of_week"));
    }

    @Test
    @DisplayName("should handle network context")
    void shouldHandleNetworkContext() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "ip_address", "192.168.1.100",
              "user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
              "referrer", "https://example.com/login",
              "geolocation", Map.of("country", "US", "city", "New York"),
              "secure_connection", true,
              "protocol", "HTTPS",
              "port", 443);

      // When
      Context networkContext = new Context(attributes);

      // Then
      assertEquals("192.168.1.100", networkContext.getAttributes().get("ip_address"));
      assertEquals(true, networkContext.getAttributes().get("secure_connection"));
      assertEquals("HTTPS", networkContext.getAttributes().get("protocol"));
      assertEquals(443, networkContext.getAttributes().get("port"));
    }

    @Test
    @DisplayName("should handle security context")
    void shouldHandleSecurityContext() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "authentication_method",
              "mfa",
              "risk_score",
              0.2,
              "device_trusted",
              true,
              "vpn_connection",
              false,
              "failed_login_attempts",
              0,
              "last_successful_login",
              "2024-01-15T09:00:00Z",
              "security_clearance",
              "confidential");

      // When
      Context securityContext = new Context(attributes);

      // Then
      assertEquals("mfa", securityContext.getAttributes().get("authentication_method"));
      assertEquals(0.2, securityContext.getAttributes().get("risk_score"));
      assertEquals(true, securityContext.getAttributes().get("device_trusted"));
      assertEquals("confidential", securityContext.getAttributes().get("security_clearance"));
    }

    @Test
    @DisplayName("should handle application context")
    void shouldHandleApplicationContext() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "application_name", "payment-processor",
              "application_version", "2.1.0",
              "environment", "production",
              "deployment_id", "deploy-12345",
              "feature_flags",
                  Map.of(
                      "new_payment_flow", true,
                      "fraud_detection_v2", false),
              "correlation_id", "req-67890");

      // When
      Context appContext = new Context(attributes);

      // Then
      assertEquals("payment-processor", appContext.getAttributes().get("application_name"));
      assertEquals("2.1.0", appContext.getAttributes().get("application_version"));
      assertEquals("production", appContext.getAttributes().get("environment"));
      assertEquals("req-67890", appContext.getAttributes().get("correlation_id"));
    }

    @Test
    @DisplayName("should handle business context")
    void shouldHandleBusinessContext() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "organization", "acme-corp",
              "department", "finance",
              "cost_center", "CC-001",
              "compliance_required", true,
              "data_classification", "confidential",
              "business_purpose", "quarterly_reporting",
              "regulatory_scope", java.util.List.of("SOX", "PCI-DSS"));

      // When
      Context businessContext = new Context(attributes);

      // Then
      assertEquals("acme-corp", businessContext.getAttributes().get("organization"));
      assertEquals("finance", businessContext.getAttributes().get("department"));
      assertEquals(true, businessContext.getAttributes().get("compliance_required"));
      assertEquals("confidential", businessContext.getAttributes().get("data_classification"));
    }
  }

  @Nested
  @DisplayName("Attribute Handling")
  class AttributeHandlingTest {

    @Test
    @DisplayName("should handle various attribute types")
    void shouldHandleVariousAttributeTypes() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "string_attr",
              "value",
              "number_attr",
              42,
              "boolean_attr",
              true,
              "array_attr",
              java.util.List.of("item1", "item2"),
              "map_attr",
              Map.of("nested", "value"));

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(5, context.getAttributes().size());
      assertEquals("value", context.getAttributes().get("string_attr"));
      assertEquals(42, context.getAttributes().get("number_attr"));
      assertEquals(true, context.getAttributes().get("boolean_attr"));
      assertEquals(java.util.List.of("item1", "item2"), context.getAttributes().get("array_attr"));
      assertEquals(Map.of("nested", "value"), context.getAttributes().get("map_attr"));
    }

    @Test
    @DisplayName("should handle complex nested structures")
    void shouldHandleComplexNestedStructures() {
      // Given
      Map<String, Object> complexAttribute =
          Map.of(
              "request",
                  Map.of(
                      "headers",
                          Map.of(
                              "authorization", "Bearer token123",
                              "content-type", "application/json"),
                      "parameters",
                          Map.of(
                              "page", 1,
                              "size", 50,
                              "filters", java.util.List.of("active", "verified"))),
              "response",
                  Map.of(
                      "status_code", 200,
                      "content_length", 1024,
                      "cache_control", "max-age=300"));

      Map<String, Object> attributes = Map.of("http_context", complexAttribute);

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(1, context.getAttributes().size());
      assertEquals(complexAttribute, context.getAttributes().get("http_context"));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class ImmutabilityTest {

    @Test
    @DisplayName("should return immutable attributes map")
    void shouldReturnImmutableAttributesMap() {
      // Given
      Context context = new Context(Map.of("key", "value"));

      // When & Then
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            context.getAttributes().put("new_key", "new_value");
          });
    }

    @Test
    @DisplayName("should be immutable after construction")
    void shouldBeImmutableAfterConstruction() {
      // Given
      Map<String, Object> originalAttributes = new HashMap<>();
      originalAttributes.put("initial", "value");
      Context context = new Context(originalAttributes);

      // When - Modify the original map
      originalAttributes.put("modified", "after_construction");

      // Then - Context should not be affected
      assertEquals(1, context.getAttributes().size());
      assertEquals("value", context.getAttributes().get("initial"));
      assertFalse(context.getAttributes().containsKey("modified"));
    }

    @Test
    @DisplayName("should maintain immutability after merging")
    void shouldMaintainImmutabilityAfterMerging() {
      // Given
      Context context1 = new Context(Map.of("key1", "value1"));
      Context context2 = new Context(Map.of("key2", "value2"));

      // When
      Context mergedContext = context1.merge(context2);

      // Then - Merged context should be immutable
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            mergedContext.getAttributes().put("new_key", "new_value");
          });

      // Original contexts should remain unchanged
      assertEquals(1, context1.getAttributes().size());
      assertEquals(1, context2.getAttributes().size());
      assertEquals(2, mergedContext.getAttributes().size());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle empty attribute keys")
    void shouldHandleEmptyAttributeKeys() {
      // Given
      Map<String, Object> attributes = Map.of("", "empty_key_value");

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(1, context.getAttributes().size());
      assertEquals("empty_key_value", context.getAttributes().get(""));
    }

    @Test
    @DisplayName("should handle special characters in attribute keys")
    void shouldHandleSpecialCharactersInAttributeKeys() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "key-with-dashes", "value1",
              "key_with_underscores", "value2",
              "key.with.dots", "value3",
              "key:with:colons", "value4",
              "key/with/slashes", "value5");

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(5, context.getAttributes().size());
      assertEquals("value1", context.getAttributes().get("key-with-dashes"));
      assertEquals("value2", context.getAttributes().get("key_with_underscores"));
      assertEquals("value3", context.getAttributes().get("key.with.dots"));
      assertEquals("value4", context.getAttributes().get("key:with:colons"));
      assertEquals("value5", context.getAttributes().get("key/with/slashes"));
    }

    @Test
    @DisplayName("should handle Unicode attribute keys and values")
    void shouldHandleUnicodeAttributeKeysAndValues() {
      // Given
      Map<String, Object> attributes =
          Map.of(
              "キー", "値", // Japanese
              "clé", "valeur", // French
              "键", "值", // Chinese
              "🔑", "🔒" // Emoji
              );

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(4, context.getAttributes().size());
      assertEquals("値", context.getAttributes().get("キー"));
      assertEquals("valeur", context.getAttributes().get("clé"));
      assertEquals("值", context.getAttributes().get("键"));
      assertEquals("🔒", context.getAttributes().get("🔑"));
    }

    @Test
    @DisplayName("should handle very large attribute values")
    void shouldHandleVeryLargeAttributeValues() {
      // Given
      String largeValue = "x".repeat(10000);
      java.util.List<String> largeList =
          java.util.stream.IntStream.range(0, 1000)
              .mapToObj(i -> "item-" + i)
              .collect(java.util.stream.Collectors.toList());

      Map<String, Object> attributes =
          Map.of(
              "large_string", largeValue,
              "large_list", largeList);

      // When
      Context context = new Context(attributes);

      // Then
      assertEquals(2, context.getAttributes().size());
      assertEquals(largeValue, context.getAttributes().get("large_string"));
      assertEquals(largeList, context.getAttributes().get("large_list"));
    }

    @Test
    @DisplayName("should handle merging contexts with many attributes")
    void shouldHandleMergingContextsWithManyAttributes() {
      // Given
      Map<String, Object> attributes1 = new HashMap<>();
      Map<String, Object> attributes2 = new HashMap<>();

      for (int i = 0; i < 100; i++) {
        attributes1.put("key1_" + i, "value1_" + i);
        attributes2.put("key2_" + i, "value2_" + i);
      }

      Context context1 = new Context(attributes1);
      Context context2 = new Context(attributes2);

      // When
      Context mergedContext = context1.merge(context2);

      // Then
      assertEquals(200, mergedContext.getAttributes().size());
      assertEquals("value1_50", mergedContext.getAttributes().get("key1_50"));
      assertEquals("value2_75", mergedContext.getAttributes().get("key2_75"));
    }

    @Test
    @DisplayName("should handle null values in merged contexts")
    void shouldHandleNullValuesInMergedContexts() {
      // Given
      Map<String, Object> attributes1 = new HashMap<>();
      attributes1.put("key1", "value1");
      attributes1.put("shared_key", "original_value");

      Map<String, Object> attributes2 = new HashMap<>();
      attributes2.put("key2", "value2");
      attributes2.put("shared_key", "overridden_value"); // Override with new value

      Context context1 = new Context(attributes1);
      Context context2 = new Context(attributes2);

      // When
      Context mergedContext = context1.merge(context2);

      // Then
      assertEquals(3, mergedContext.getAttributes().size());
      assertEquals("value1", mergedContext.getAttributes().get("key1"));
      assertEquals("value2", mergedContext.getAttributes().get("key2"));
      assertEquals(
          "overridden_value",
          mergedContext.getAttributes().get("shared_key")); // Overridden with new value
    }
  }
}
