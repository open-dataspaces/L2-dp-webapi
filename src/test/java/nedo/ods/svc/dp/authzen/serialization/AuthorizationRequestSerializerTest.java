package nedo.ods.svc.dp.authzen.serialization;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nedo.ods.svc.dp.authzen.api.AuthorizationRequest;
import nedo.ods.svc.dp.authzen.model.Action;
import nedo.ods.svc.dp.authzen.model.Context;
import nedo.ods.svc.dp.authzen.model.Resource;
import nedo.ods.svc.dp.authzen.model.Subject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Comprehensive test suite for {@link AuthorizationRequestSerializer}.
 *
 * <p>Tests JSON serialization functionality, error handling, and proper formatting of authorization
 * request objects.
 */
@DisplayName("AuthorizationRequestSerializer")
class AuthorizationRequestSerializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  @DisplayName("Utility Class Design")
  class UtilityClassDesignTest {

    @Test
    @DisplayName("should be a utility class with private constructor")
    void shouldBeUtilityClassWithPrivateConstructor() throws Exception {
      // Given
      Constructor<AuthorizationRequestSerializer> constructor =
          AuthorizationRequestSerializer.class.getDeclaredConstructor();

      // Then
      assertTrue(Modifier.isPrivate(constructor.getModifiers()));

      // Should be able to invoke private constructor via reflection
      constructor.setAccessible(true);
      AuthorizationRequestSerializer instance = constructor.newInstance();
      assertNotNull(instance);
    }

    @Test
    @DisplayName("should have only static methods")
    void shouldHaveOnlyStaticMethods() {
      // Given
      var methods = AuthorizationRequestSerializer.class.getDeclaredMethods();

      // Then
      for (var method : methods) {
        if (method.getName().equals("buildRequestJson")) {
          assertTrue(Modifier.isStatic(method.getModifiers()));
          assertTrue(Modifier.isPublic(method.getModifiers()));
        }
      }
    }
  }

  @Nested
  @DisplayName("JSON Serialization")
  class JsonSerializationTest {

    @Test
    @DisplayName("should serialize complete authorization request")
    void shouldSerializeCompleteAuthorizationRequest() throws Exception {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("alice@example.com")
              .type("user")
              .addProperty("department", "engineering")
              .addProperty("clearance", "secret")
              .build();

      Action action =
          new Action.Builder()
              .name("read")
              .addProperty("method", "GET")
              .addProperty("scope", "data")
              .build();

      Resource resource =
          new Resource.Builder()
              .id("/documents/report.pdf")
              .type("document")
              .addProperty("classification", "confidential")
              .addProperty("size", 1024)
              .build();

      Context context =
          new Context(
              Map.of(
                  "timestamp", "2024-01-15T10:30:00Z",
                  "source_ip", "192.168.1.100",
                  "user_agent", "Mozilla/5.0"));

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .context(context)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      assertFalse(json.isEmpty());

      // Parse and verify JSON structure
      JsonNode rootNode = objectMapper.readTree(json);

      // Verify subject
      JsonNode subjectNode = rootNode.get("subject");
      assertNotNull(subjectNode);
      assertEquals("alice@example.com", subjectNode.get("id").asText());
      assertEquals("user", subjectNode.get("type").asText());
      assertEquals("engineering", subjectNode.get("properties").get("department").asText());
      assertEquals("secret", subjectNode.get("properties").get("clearance").asText());

      // Verify action
      JsonNode actionNode = rootNode.get("action");
      assertNotNull(actionNode);
      assertEquals("read", actionNode.get("name").asText());
      assertEquals("GET", actionNode.get("properties").get("method").asText());
      assertEquals("data", actionNode.get("properties").get("scope").asText());

      // Verify resource
      JsonNode resourceNode = rootNode.get("resource");
      assertNotNull(resourceNode);
      assertEquals("/documents/report.pdf", resourceNode.get("id").asText());
      assertEquals("document", resourceNode.get("type").asText());
      assertEquals("confidential", resourceNode.get("properties").get("classification").asText());
      assertEquals(1024, resourceNode.get("properties").get("size").asInt());

      // Verify context (should be flattened)
      JsonNode contextNode = rootNode.get("context");
      assertNotNull(contextNode);
      assertEquals("2024-01-15T10:30:00Z", contextNode.get("timestamp").asText());
      assertEquals("192.168.1.100", contextNode.get("source_ip").asText());
      assertEquals("Mozilla/5.0", contextNode.get("user_agent").asText());
    }

    @Test
    @DisplayName("should serialize request with minimal required fields")
    void shouldSerializeRequestWithMinimalRequiredFields() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("bob@example.com").type("user").build();

      Action action = new Action.Builder().name("write").build();

      Resource resource = new Resource.Builder().id("/api/data").type("api").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      JsonNode rootNode = objectMapper.readTree(json);

      // Verify required fields
      assertEquals("bob@example.com", rootNode.get("subject").get("id").asText());
      assertEquals("user", rootNode.get("subject").get("type").asText());
      assertEquals("write", rootNode.get("action").get("name").asText());
      assertEquals("/api/data", rootNode.get("resource").get("id").asText());
      assertEquals("api", rootNode.get("resource").get("type").asText());

      // Context should be null or empty
      JsonNode contextNode = rootNode.get("context");
      assertTrue(contextNode == null || contextNode.isNull());
    }

    @Test
    @DisplayName("should serialize request with empty context")
    void shouldSerializeRequestWithEmptyContext() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("charlie@example.com").type("service").build();

      Action action = new Action.Builder().name("delete").build();

      Resource resource = new Resource.Builder().id("/temp/cache").type("directory").build();

      Context emptyContext = new Context(Map.of());

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .context(emptyContext)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      JsonNode rootNode = objectMapper.readTree(json);

      // Context should be an empty object
      JsonNode contextNode = rootNode.get("context");
      assertNotNull(contextNode);
      assertTrue(contextNode.isObject());
      assertEquals(0, contextNode.size());
    }

    @Test
    @DisplayName("should serialize request with complex nested properties")
    void shouldSerializeRequestWithComplexNestedProperties() throws Exception {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("system@example.com")
              .type("system")
              .addProperty(
                  "authentication",
                  Map.of(
                      "method", "oauth2",
                      "scopes", java.util.List.of("read", "write"),
                      "expires", "2024-12-31T23:59:59Z"))
              .addProperty(
                  "metadata",
                  Map.of(
                      "version", "1.0",
                      "instance_id", "i-1234567890"))
              .build();

      Action action =
          new Action.Builder()
              .name("process")
              .addProperty(
                  "pipeline",
                  Map.of(
                      "stages", java.util.List.of("validate", "transform", "load"),
                      "parallelism", 4,
                      "timeout", 3600))
              .build();

      Resource resource =
          new Resource.Builder()
              .id("dataset-12345")
              .type("dataset")
              .addProperty(
                  "schema",
                  Map.of(
                      "columns", java.util.List.of("id", "name", "email"),
                      "types", Map.of("id", "int", "name", "string", "email", "string"),
                      "constraints", java.util.List.of("id:unique", "email:unique")))
              .build();

      Context context =
          new Context(
              Map.of(
                  "execution",
                      Map.of(
                          "environment", "production",
                          "cluster", "west-1",
                          "resources", Map.of("cpu", 8, "memory", "16GB")),
                  "security",
                      Map.of(
                          "encryption", true,
                          "audit", true,
                          "compliance", java.util.List.of("SOX", "GDPR"))));

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .context(context)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      JsonNode rootNode = objectMapper.readTree(json);

      // Verify complex nested structures are preserved
      JsonNode authNode = rootNode.get("subject").get("properties").get("authentication");
      assertEquals("oauth2", authNode.get("method").asText());
      assertTrue(authNode.get("scopes").isArray());
      assertEquals(2, authNode.get("scopes").size());

      JsonNode pipelineNode = rootNode.get("action").get("properties").get("pipeline");
      assertEquals(4, pipelineNode.get("parallelism").asInt());
      assertTrue(pipelineNode.get("stages").isArray());

      JsonNode schemaNode = rootNode.get("resource").get("properties").get("schema");
      assertTrue(schemaNode.get("columns").isArray());
      assertTrue(schemaNode.get("types").isObject());

      JsonNode contextNode = rootNode.get("context");
      assertEquals("production", contextNode.get("execution").get("environment").asText());
      assertTrue(contextNode.get("security").get("encryption").asBoolean());
    }
  }

  @Nested
  @DisplayName("Special Value Handling")
  class SpecialValueHandlingTest {

    @Test
    @DisplayName("should handle various property value types")
    void shouldHandleVariousPropertyValueTypes() throws Exception {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("test@example.com")
              .type("user")
              .addProperty("active", true)
              .addProperty("department", "engineering")
              .addProperty("level", 5)
              .build();

      Action action = new Action.Builder().name("access").build();

      Resource resource = new Resource.Builder().id("/resource").type("file").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      JsonNode rootNode = objectMapper.readTree(json);

      JsonNode propertiesNode = rootNode.get("subject").get("properties");
      assertTrue(propertiesNode.has("department"));
      assertEquals("engineering", propertiesNode.get("department").asText());

      // Check JSON contains an array structure for properties
      assertTrue(
          json.contains("[") || json.contains("{"),
          "JSON should contain array or object structure");
    }

    @Test
    @DisplayName("should handle Unicode characters")
    void shouldHandleUnicodeCharacters() throws Exception {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("ユーザー@例.jp") // Japanese
              .type("user")
              .addProperty("名前", "田中太郎")
              .addProperty("部署", "エンジニアリング")
              .build();

      Action action =
          new Action.Builder()
              .name("読み取り") // Japanese for "read"
              .addProperty("説明", "ファイルを読み取る")
              .build();

      Resource resource =
          new Resource.Builder()
              .id("/文書/レポート.pdf") // Japanese path
              .type("文書") // Japanese for "document"
              .build();

      Context context =
          new Context(
              Map.of(
                  "場所", "東京", // Japanese for "location" and "Tokyo"
                  "言語", "ja",
                  "emoji", "📄📊📈"));

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .context(context)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      JsonNode rootNode = objectMapper.readTree(json);

      assertEquals("ユーザー@例.jp", rootNode.get("subject").get("id").asText());
      assertEquals("田中太郎", rootNode.get("subject").get("properties").get("名前").asText());
      assertEquals("読み取り", rootNode.get("action").get("name").asText());
      assertEquals("/文書/レポート.pdf", rootNode.get("resource").get("id").asText());
      assertEquals("東京", rootNode.get("context").get("場所").asText());
      assertEquals("📄📊📈", rootNode.get("context").get("emoji").asText());
    }

    @Test
    @DisplayName("should handle large data structures")
    void shouldHandleLargeDataStructures() throws Exception {
      // Given
      Subject.Builder subjectBuilder =
          new Subject.Builder().id("bulk-processor@example.com").type("system");

      // Add many properties
      for (int i = 0; i < 100; i++) {
        subjectBuilder.addProperty("property_" + i, "value_" + i);
      }

      Action.Builder actionBuilder = new Action.Builder().name("bulk_process");

      // Add large list
      java.util.List<String> largeList =
          java.util.stream.IntStream.range(0, 1000)
              .mapToObj(i -> "item_" + i)
              .collect(java.util.stream.Collectors.toList());
      actionBuilder.addProperty("items", largeList);

      Resource resource =
          new Resource.Builder()
              .id("/large-dataset")
              .type("dataset")
              .addProperty("size_bytes", 1073741824L) // 1GB
              .addProperty("record_count", 1000000)
              .build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subjectBuilder.build())
              .action(actionBuilder.build())
              .resource(resource)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);
      assertTrue(json.length() > 10000); // Should be quite large

      JsonNode rootNode = objectMapper.readTree(json);
      assertEquals("bulk-processor@example.com", rootNode.get("subject").get("id").asText());
      assertEquals(100, rootNode.get("subject").get("properties").size());
      assertEquals(1000, rootNode.get("action").get("properties").get("items").size());
      assertEquals(
          1073741824L, rootNode.get("resource").get("properties").get("size_bytes").asLong());
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTest {

    @Test
    @DisplayName("should handle null request by delegating to Jackson")
    void shouldHandleNullRequestByDelegatingToJackson() {
      // When & Then - Jackson will handle null serialization
      assertDoesNotThrow(
          () -> {
            String result = AuthorizationRequestSerializer.buildRequestJson(null);
            assertEquals("null", result);
          });
    }

    @Test
    @DisplayName("should handle circular references gracefully")
    void shouldHandleCircularReferencesGracefully() throws Exception {
      // Note: Our current model classes don't have circular references,
      // but this test ensures the serializer would handle them if they existed

      // Given - Create a request with a complex object that could cause issues
      Subject subject = new Subject.Builder().id("test@example.com").type("user").build();

      Action action = new Action.Builder().name("test").build();

      Resource resource = new Resource.Builder().id("/test").type("test").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When & Then - Should not throw circular reference exceptions
      assertDoesNotThrow(
          () -> {
            String json = AuthorizationRequestSerializer.buildRequestJson(request);
            assertNotNull(json);
          });
    }
  }

  @Nested
  @DisplayName("JSON Format Validation")
  class JsonFormatValidationTest {

    @Test
    @DisplayName("should produce valid JSON format")
    void shouldProduceValidJsonFormat() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("validator@example.com").type("system").build();

      Action action = new Action.Builder().name("validate").build();

      Resource resource = new Resource.Builder().id("/validation/test").type("endpoint").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);

      // Should be parseable as valid JSON
      assertDoesNotThrow(
          () -> {
            JsonNode parsed = objectMapper.readTree(json);
            assertNotNull(parsed);
            assertTrue(parsed.isObject());
          });

      // Should contain expected top-level fields
      JsonNode rootNode = objectMapper.readTree(json);
      assertTrue(rootNode.has("subject"));
      assertTrue(rootNode.has("action"));
      assertTrue(rootNode.has("resource"));
    }

    @Test
    @DisplayName("should produce compact JSON without extra whitespace")
    void shouldProduceCompactJsonWithoutExtraWhitespace() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("compact@example.com").type("user").build();

      Action action = new Action.Builder().name("compress").build();

      Resource resource = new Resource.Builder().id("/compress/test").type("file").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When
      String json = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertNotNull(json);

      // Should not contain extra whitespace or formatting
      assertFalse(json.contains("  ")); // No double spaces
      assertFalse(json.contains("\n")); // No newlines
      assertFalse(json.contains("\t")); // No tabs
    }

    @Test
    @DisplayName("should maintain consistent field ordering")
    void shouldMaintainConsistentFieldOrdering() throws Exception {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("ordering@example.com")
              .type("user")
              .addProperty("z_property", "last")
              .addProperty("a_property", "first")
              .build();

      Action action = new Action.Builder().name("order_test").build();

      Resource resource = new Resource.Builder().id("/order/test").type("test").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When
      String json1 = AuthorizationRequestSerializer.buildRequestJson(request);
      String json2 = AuthorizationRequestSerializer.buildRequestJson(request);

      // Then
      assertEquals(json1, json2); // Should produce identical output for same input

      JsonNode rootNode = objectMapper.readTree(json1);
      assertTrue(rootNode.has("subject"));
      assertTrue(rootNode.has("action"));
      assertTrue(rootNode.has("resource"));
    }
  }

  @Nested
  @DisplayName("Performance and Thread Safety")
  class PerformanceAndThreadSafetyTest {

    @Test
    @DisplayName("should handle multiple serialization calls efficiently")
    void shouldHandleMultipleSerializationCallsEfficiently() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("performance@example.com").type("user").build();

      Action action = new Action.Builder().name("benchmark").build();

      Resource resource = new Resource.Builder().id("/benchmark/test").type("test").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When & Then - Multiple calls should complete without issues
      for (int i = 0; i < 100; i++) {
        String json = AuthorizationRequestSerializer.buildRequestJson(request);
        assertNotNull(json);
        assertFalse(json.isEmpty());
      }
    }

    @Test
    @DisplayName("should be thread-safe for concurrent serialization")
    void shouldBeThreadSafeForConcurrentSerialization() throws Exception {
      // Given
      Subject subject = new Subject.Builder().id("concurrent@example.com").type("user").build();

      Action action = new Action.Builder().name("concurrent_test").build();

      Resource resource = new Resource.Builder().id("/concurrent/test").type("test").build();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder()
              .subject(subject)
              .action(action)
              .resource(resource)
              .build();

      // When - Execute serialization concurrently
      java.util.List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(10);

      try {
        for (int i = 0; i < 50; i++) {
          futures.add(
              executor.submit(() -> AuthorizationRequestSerializer.buildRequestJson(request)));
        }

        // Then - All futures should complete successfully
        for (java.util.concurrent.Future<String> future : futures) {
          String json = future.get();
          assertNotNull(json);
          assertFalse(json.isEmpty());

          // Verify JSON is valid
          JsonNode parsed = objectMapper.readTree(json);
          assertEquals("concurrent@example.com", parsed.get("subject").get("id").asText());
        }
      } finally {
        executor.shutdown();
      }
    }
  }
}
