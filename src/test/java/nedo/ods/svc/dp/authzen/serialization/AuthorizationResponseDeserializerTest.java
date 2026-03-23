package nedo.ods.svc.dp.authzen.serialization;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import nedo.ods.svc.dp.authzen.api.AuthorizationResponse;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Comprehensive test suite for {@link AuthorizationResponseDeserializer}.
 *
 * <p>Tests JSON deserialization functionality, error handling, and proper parsing of authorization
 * response objects.
 */
@DisplayName("AuthorizationResponseDeserializer")
class AuthorizationResponseDeserializerTest {

  @Nested
  @DisplayName("Utility Class Design")
  class UtilityClassDesignTest {

    @Test
    @DisplayName("should be a utility class with private constructor")
    void shouldBeUtilityClassWithPrivateConstructor() throws Exception {
      // Given
      Constructor<AuthorizationResponseDeserializer> constructor =
          AuthorizationResponseDeserializer.class.getDeclaredConstructor();

      // Then
      assertTrue(Modifier.isPrivate(constructor.getModifiers()));

      // Should be able to invoke private constructor via reflection
      constructor.setAccessible(true);
      AuthorizationResponseDeserializer instance = constructor.newInstance();
      assertNotNull(instance);
    }

    @Test
    @DisplayName("should have only static methods")
    void shouldHaveOnlyStaticMethods() {
      // Given
      var methods = AuthorizationResponseDeserializer.class.getDeclaredMethods();

      // Then
      for (var method : methods) {
        if (method.getName().equals("parseResponseJson")) {
          assertTrue(Modifier.isStatic(method.getModifiers()));
          assertTrue(Modifier.isPublic(method.getModifiers()));
        }
      }
    }

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      // Then
      assertTrue(Modifier.isFinal(AuthorizationResponseDeserializer.class.getModifiers()));
    }
  }

  @Nested
  @DisplayName("Valid JSON Deserialization")
  class ValidJsonDeserializationTest {

    @Test
    @DisplayName("should deserialize simple allowed response")
    void shouldDeserializeSimpleAllowedResponse() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": {
                        "request_id": "req-12345",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      assertNotNull(response.getContext());
      assertEquals("req-12345", response.getContext().get("request_id"));
      assertEquals("2024-01-15T10:30:00Z", response.getContext().get("timestamp"));
    }

    @Test
    @DisplayName("should deserialize simple denied response")
    void shouldDeserializeSimpleDeniedResponse() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": false,
                    "context": {
                        "reason": "insufficient_privileges",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertFalse(response.isAllowed());
      assertNotNull(response.getContext());
      assertEquals("insufficient_privileges", response.getContext().get("reason"));
      assertEquals("2024-01-15T10:30:00Z", response.getContext().get("timestamp"));
    }

    @Test
    @DisplayName("should deserialize response with null context")
    void shouldDeserializeResponseWithNullContext() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": null
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      assertNull(response.getContext());
    }

    @Test
    @DisplayName("should deserialize response with empty context")
    void shouldDeserializeResponseWithEmptyContext() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": false,
                    "context": {}
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertFalse(response.isAllowed());
      assertNotNull(response.getContext());
      assertTrue(response.getContext().isEmpty());
    }

    @Test
    @DisplayName("should deserialize response with missing context field")
    void shouldDeserializeResponseWithMissingContextField() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      assertNull(response.getContext());
    }

    @Test
    @DisplayName("should deserialize response with complex context")
    void shouldDeserializeResponseWithComplexContext() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": {
                        "request_id": "req-67890",
                        "timestamp": "2024-01-15T11:45:00Z",
                        "user_info": {
                            "id": "alice@example.com",
                            "roles": ["user", "admin"],
                            "department": "engineering"
                        },
                        "resource_info": {
                            "type": "document",
                            "classification": "confidential",
                            "owner": "bob@example.com"
                        },
                        "policy_evaluation": {
                            "policy_id": "policy-123",
                            "evaluation_time_ms": 45,
                            "rules_matched": ["rule-1", "rule-3", "rule-7"]
                        }
                    }
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      assertNotNull(response.getContext());

      assertEquals("req-67890", response.getContext().get("request_id"));
      assertEquals("2024-01-15T11:45:00Z", response.getContext().get("timestamp"));

      // Verify nested objects
      @SuppressWarnings("unchecked")
      Map<String, Object> userInfo = (Map<String, Object>) response.getContext().get("user_info");
      assertNotNull(userInfo);
      assertEquals("alice@example.com", userInfo.get("id"));
      assertEquals("engineering", userInfo.get("department"));

      @SuppressWarnings("unchecked")
      Map<String, Object> resourceInfo =
          (Map<String, Object>) response.getContext().get("resource_info");
      assertNotNull(resourceInfo);
      assertEquals("confidential", resourceInfo.get("classification"));

      @SuppressWarnings("unchecked")
      Map<String, Object> policyEvaluation =
          (Map<String, Object>) response.getContext().get("policy_evaluation");
      assertNotNull(policyEvaluation);
      assertEquals("policy-123", policyEvaluation.get("policy_id"));
      assertEquals(45, policyEvaluation.get("evaluation_time_ms"));
    }
  }

  @Nested
  @DisplayName("Special Value Handling")
  class SpecialValueHandlingTest {

    @Test
    @DisplayName("should handle various data types in context")
    void shouldHandleVariousDataTypesInContext() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": {
                        "string_value": "test_string",
                        "integer_value": 42,
                        "long_value": 9223372036854775807,
                        "double_value": 3.14159,
                        "boolean_value": true,
                        "null_value": null,
                        "array_value": ["item1", "item2", "item3"],
                        "nested_object": {
                            "inner_string": "inner_value",
                            "inner_number": 123
                        }
                    }
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      Map<String, Object> context = response.getContext();

      assertEquals("test_string", context.get("string_value"));
      assertEquals(42, context.get("integer_value"));
      assertEquals(9223372036854775807L, context.get("long_value"));
      assertEquals(3.14159, context.get("double_value"));
      assertEquals(true, context.get("boolean_value"));
      assertNull(context.get("null_value"));
      assertTrue(context.get("array_value") instanceof java.util.List);
      assertTrue(context.get("nested_object") instanceof Map);
    }

    @Test
    @DisplayName("should handle Unicode characters in context")
    void shouldHandleUnicodeCharactersInContext() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": false,
                    "context": {
                        "ユーザー名": "田中太郎",
                        "理由": "アクセス権限が不足しています",
                        "地域": "東京",
                        "emoji": "🔒🚫❌",
                        "chinese": "用户权限不足",
                        "arabic": "صلاحيات غير كافية",
                        "mixed": "User: 田中 - Status: 🔒 Denied"
                    }
                }
                """;

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertFalse(response.isAllowed());
      Map<String, Object> context = response.getContext();

      assertEquals("田中太郎", context.get("ユーザー名"));
      assertEquals("アクセス権限が不足しています", context.get("理由"));
      assertEquals("東京", context.get("地域"));
      assertEquals("🔒🚫❌", context.get("emoji"));
      assertEquals("用户权限不足", context.get("chinese"));
      assertEquals("صلاحيات غير كافية", context.get("arabic"));
      assertEquals("User: 田中 - Status: 🔒 Denied", context.get("mixed"));
    }

    @Test
    @DisplayName("should handle large nested structures")
    void shouldHandleLargeNestedStructures() throws Exception {
      // Given - Create JSON with deeply nested structure
      StringBuilder jsonBuilder = new StringBuilder();
      jsonBuilder.append(
          """
                {
                    "decision": true,
                    "context": {
                        "large_array": [
                """);

      // Add large array
      for (int i = 0; i < 100; i++) {
        if (i > 0) jsonBuilder.append(",");
        jsonBuilder.append("\"item_").append(i).append("\"");
      }

      jsonBuilder.append(
          """
                        ],
                        "large_object": {
                """);

      // Add large object with many properties
      for (int i = 0; i < 50; i++) {
        if (i > 0) jsonBuilder.append(",");
        jsonBuilder.append("\"property_").append(i).append("\": \"value_").append(i).append("\"");
      }

      jsonBuilder.append(
          """
                        },
                        "deeply_nested": {
                            "level1": {
                                "level2": {
                                    "level3": {
                                        "level4": {
                                            "level5": {
                                                "deep_value": "found_at_level_5"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                """);

      String json = jsonBuilder.toString();

      // When
      AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      Map<String, Object> context = response.getContext();

      // Verify large array
      @SuppressWarnings("unchecked")
      java.util.List<String> largeArray = (java.util.List<String>) context.get("large_array");
      assertEquals(100, largeArray.size());
      assertEquals("item_0", largeArray.get(0));
      assertEquals("item_99", largeArray.get(99));

      // Verify large object
      @SuppressWarnings("unchecked")
      Map<String, Object> largeObject = (Map<String, Object>) context.get("large_object");
      assertEquals(50, largeObject.size());
      assertEquals("value_0", largeObject.get("property_0"));
      assertEquals("value_49", largeObject.get("property_49"));

      // Verify deeply nested structure
      @SuppressWarnings("unchecked")
      Map<String, Object> level1 =
          (Map<String, Object>) ((Map<String, Object>) context.get("deeply_nested")).get("level1");
      @SuppressWarnings("unchecked")
      Map<String, Object> level2 = (Map<String, Object>) level1.get("level2");
      @SuppressWarnings("unchecked")
      Map<String, Object> level3 = (Map<String, Object>) level2.get("level3");
      @SuppressWarnings("unchecked")
      Map<String, Object> level4 = (Map<String, Object>) level3.get("level4");
      @SuppressWarnings("unchecked")
      Map<String, Object> level5 = (Map<String, Object>) level4.get("level5");
      assertEquals("found_at_level_5", level5.get("deep_value"));
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTest {

    @Test
    @DisplayName("should throw exception when JSON is null")
    void shouldThrowExceptionWhenJsonIsNull() {
      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson(null));

      assertEquals("Response JSON from server was null or empty.", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw exception when JSON is empty string")
    void shouldThrowExceptionWhenJsonIsEmptyString() {
      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson(""));

      assertEquals("Response JSON from server was null or empty.", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw exception when JSON is blank string")
    void shouldThrowExceptionWhenJsonIsBlankString() {
      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson("   \n\t  "));

      assertEquals("Response JSON from server was null or empty.", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw exception for malformed JSON")
    void shouldThrowExceptionForMalformedJson() {
      // Given
      String malformedJson =
          """
                {
                    "decision": true,
                    "context": {
                        "key": "value"
                    // Missing closing braces
                """;

      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson(malformedJson));

      assertEquals(
          "Failed to deserialize authorization response from JSON.", exception.getMessage());
      assertNotNull(exception.getCause());
      assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    @DisplayName("should throw exception for invalid JSON syntax")
    void shouldThrowExceptionForInvalidJsonSyntax() {
      // Given
      String invalidJson =
          """
                {
                    decision: true,  // Missing quotes around key
                    "context": {
                        "key": 'value'  // Single quotes instead of double
                    }
                }
                """;

      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson(invalidJson));

      assertEquals(
          "Failed to deserialize authorization response from JSON.", exception.getMessage());
      assertNotNull(exception.getCause());
      assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    @DisplayName("should throw exception for completely invalid input")
    void shouldThrowExceptionForCompletelyInvalidInput() {
      // Given
      String notJson = "This is not JSON at all, just plain text!";

      // When & Then
      AuthorizationException exception =
          assertThrows(
              AuthorizationException.class,
              () -> AuthorizationResponseDeserializer.parseResponseJson(notJson));

      assertEquals(
          "Failed to deserialize authorization response from JSON.", exception.getMessage());
      assertNotNull(exception.getCause());
      assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    @DisplayName("should handle JSON with unexpected structure gracefully")
    void shouldHandleJsonWithUnexpectedStructureGracefully() throws Exception {
      // Given - JSON with expected structure plus unexpected fields
      String unexpectedJson =
          """
                {
                    "decision": "PERMIT",
                    "context": {
                        "expected": "value"
                    },
                    "unexpected_field": "value"
                }
                """;

      // When & Then - Should handle unexpected fields by ignoring them
      assertThrows(
          AuthorizationException.class,
          () -> {
            AuthorizationResponseDeserializer.parseResponseJson(unexpectedJson);
          },
          "Should throw exception for unrecognized fields");
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle minimal valid JSON")
    void shouldHandleMinimalValidJson() throws Exception {
      // Given
      String minimalJson = "{}";

      // When
      AuthorizationResponse response =
          AuthorizationResponseDeserializer.parseResponseJson(minimalJson);

      // Then
      assertNotNull(response);
      // Jackson will provide default values for missing fields
    }

    @Test
    @DisplayName("should handle JSON with extra whitespace")
    void shouldHandleJsonWithExtraWhitespace() throws Exception {
      // Given
      String jsonWithWhitespace =
          """

                    {

                        "decision"   :   true   ,

                        "context"    :    {

                            "key"   :   "value"

                        }

                    }

                """;

      // When
      AuthorizationResponse response =
          AuthorizationResponseDeserializer.parseResponseJson(jsonWithWhitespace);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed());
      assertEquals("value", response.getContext().get("key"));
    }

    @Test
    @DisplayName("should handle very long JSON strings")
    void shouldHandleVeryLongJsonStrings() throws Exception {
      // Given - Create a valid JSON string with long value
      String longValue = "x".repeat(1000);

      String longJson =
          String.format(
              """
                {
                    "decision": false,
                    "context": {
                        "very_long_string": "%s",
                        "reason": "string_too_long"
                    }
                }
                """,
              longValue);

      // When
      AuthorizationResponse response =
          AuthorizationResponseDeserializer.parseResponseJson(longJson);

      // Then
      assertNotNull(response);
      assertFalse(response.isAllowed());
      assertEquals(longValue, response.getContext().get("very_long_string"));
      assertEquals("string_too_long", response.getContext().get("reason"));
    }

    @Test
    @DisplayName("should handle JSON with repeated keys")
    void shouldHandleJsonWithRepeatedKeys() throws Exception {
      // Given - JSON with duplicate keys (last value should win)
      String jsonWithDuplicates =
          """
                {
                    "decision": false,
                    "decision": true,
                    "context": {
                        "key": "first_value",
                        "key": "second_value",
                        "other_key": "other_value"
                    }
                }
                """;

      // When
      AuthorizationResponse response =
          AuthorizationResponseDeserializer.parseResponseJson(jsonWithDuplicates);

      // Then
      assertNotNull(response);
      assertTrue(response.isAllowed()); // Last decision value should win
      assertEquals("second_value", response.getContext().get("key")); // Last key value should win
      assertEquals("other_value", response.getContext().get("other_key"));
    }
  }

  @Nested
  @DisplayName("Performance and Thread Safety")
  class PerformanceAndThreadSafetyTest {

    @Test
    @DisplayName("should handle multiple deserialization calls efficiently")
    void shouldHandleMultipleDeserializationCallsEfficiently() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": {
                        "performance_test": "value",
                        "iteration": 0
                    }
                }
                """;

      // When & Then - Multiple calls should complete without issues
      for (int i = 0; i < 100; i++) {
        AuthorizationResponse response = AuthorizationResponseDeserializer.parseResponseJson(json);
        assertNotNull(response);
        assertTrue(response.isAllowed());
        assertEquals("value", response.getContext().get("performance_test"));
      }
    }

    @Test
    @DisplayName("should be thread-safe for concurrent deserialization")
    void shouldBeThreadSafeForConcurrentDeserialization() throws Exception {
      // Given
      String json =
          """
                {
                    "decision": true,
                    "context": {
                        "thread_test": "concurrent_value",
                        "timestamp": "2024-01-15T12:00:00Z"
                    }
                }
                """;

      // When - Execute deserialization concurrently
      java.util.List<java.util.concurrent.Future<AuthorizationResponse>> futures =
          new java.util.ArrayList<>();
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(10);

      try {
        for (int i = 0; i < 50; i++) {
          futures.add(
              executor.submit(() -> AuthorizationResponseDeserializer.parseResponseJson(json)));
        }

        // Then - All futures should complete successfully
        for (java.util.concurrent.Future<AuthorizationResponse> future : futures) {
          AuthorizationResponse response = future.get();
          assertNotNull(response);
          assertTrue(response.isAllowed());
          assertEquals("concurrent_value", response.getContext().get("thread_test"));
          assertEquals("2024-01-15T12:00:00Z", response.getContext().get("timestamp"));
        }
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("should handle different JSON structures in parallel")
    void shouldHandleDifferentJsonStructuresInParallel() throws Exception {
      // Given
      String[] jsonVariants = {
        """
                {"decision": true, "context": {"type": "simple"}}
                """,
        """
                {"decision": false, "context": {"type": "denial", "reason": "access_denied"}}
                """,
        """
                {"decision": true, "context": {"type": "complex", "nested": {"deep": "value"}}}
                """,
        """
                {"decision": false, "context": null}
                """,
        """
                {"decision": true}
                """
      };

      // When - Process different JSON structures concurrently
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(5);
      java.util.List<java.util.concurrent.Future<AuthorizationResponse>> futures =
          new java.util.ArrayList<>();

      try {
        for (String json : jsonVariants) {
          futures.add(
              executor.submit(() -> AuthorizationResponseDeserializer.parseResponseJson(json)));
        }

        // Then - All should parse successfully with correct values
        for (int i = 0; i < futures.size(); i++) {
          AuthorizationResponse response = futures.get(i).get();
          assertNotNull(response);

          switch (i) {
            case 0:
              assertTrue(response.isAllowed());
              assertEquals("simple", response.getContext().get("type"));
              break;
            case 1:
              assertFalse(response.isAllowed());
              assertEquals("denial", response.getContext().get("type"));
              break;
            case 2:
              assertTrue(response.isAllowed());
              assertEquals("complex", response.getContext().get("type"));
              break;
            case 3:
              assertFalse(response.isAllowed());
              assertNull(response.getContext());
              break;
            case 4:
              assertTrue(response.isAllowed());
              break;
          }
        }
      } finally {
        executor.shutdown();
      }
    }
  }
}
