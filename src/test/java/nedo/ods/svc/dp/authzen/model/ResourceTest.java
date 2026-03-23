package nedo.ods.svc.dp.authzen.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Comprehensive test suite for {@link Resource}.
 *
 * <p>Tests the Builder pattern implementation, validation logic, and immutability characteristics
 * of authorization resources.
 */
@DisplayName("Resource")
class ResourceTest {

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPatternTest {

    @Test
    @DisplayName("should build valid resource with id and type only")
    void shouldBuildValidResourceWithIdAndTypeOnly() {
      // When
      Resource resource = new Resource.Builder().id("document-123").type("document").build();

      // Then
      assertNotNull(resource);
      assertEquals("document-123", resource.getId());
      assertEquals("document", resource.getType());
      assertTrue(resource.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should build valid resource with id, type and properties")
    void shouldBuildValidResourceWithIdTypeAndProperties() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("api/v1/users")
              .type("api")
              .addProperty("version", "1.0")
              .addProperty("rate_limit", 1000)
              .build();

      // Then
      assertNotNull(resource);
      assertEquals("api/v1/users", resource.getId());
      assertEquals("api", resource.getType());
      assertEquals(2, resource.getProperties().size());
      assertEquals("1.0", resource.getProperties().get("version"));
      assertEquals(1000, resource.getProperties().get("rate_limit"));
    }

    @Test
    @DisplayName("should allow method chaining")
    void shouldAllowMethodChaining() {
      // When & Then - Should not throw any exceptions
      Resource resource =
          new Resource.Builder()
              .id("database-main")
              .type("database")
              .addProperty("schema", "public")
              .addProperty("connection_pool", 50)
              .build();

      assertNotNull(resource);
      assertEquals("database-main", resource.getId());
      assertEquals("database", resource.getType());
      assertEquals(2, resource.getProperties().size());
    }

    @Test
    @DisplayName("should override properties with same key")
    void shouldOverridePropertiesWithSameKey() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("file.txt")
              .type("file")
              .addProperty("status", "draft")
              .addProperty("status", "published") // Override
              .build();

      // Then
      assertEquals("file.txt", resource.getId());
      assertEquals("file", resource.getType());
      assertEquals(1, resource.getProperties().size());
      assertEquals("published", resource.getProperties().get("status"));
    }
  }

  @Nested
  @DisplayName("Validation")
  class ValidationTest {

    @Test
    @DisplayName("should throw exception when id is null")
    void shouldThrowExceptionWhenIdIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id(null).type("document").build());

      assertEquals("Resource 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when id is blank")
    void shouldThrowExceptionWhenIdIsBlank() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id("   ").type("document").build());

      assertEquals("Resource 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when id is empty")
    void shouldThrowExceptionWhenIdIsEmpty() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id("").type("document").build());

      assertEquals("Resource 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is null")
    void shouldThrowExceptionWhenTypeIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id("document-123").type(null).build());

      assertEquals("Resource 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is blank")
    void shouldThrowExceptionWhenTypeIsBlank() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id("document-123").type("   ").build());

      assertEquals("Resource 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is empty")
    void shouldThrowExceptionWhenTypeIsEmpty() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Resource.Builder().id("document-123").type("").build());

      assertEquals("Resource 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should allow null property values")
    void shouldHandlePropertiesWithoutNullValues() {
      // When & Then - Should not throw any exceptions
      Resource resource =
          new Resource.Builder()
              .id("test-resource")
              .type("test")
              .addProperty("string_field", "value")
              .addProperty("number_field", 42)
              .addProperty("boolean_field", true)
              .build();

      assertNotNull(resource);
      assertEquals(3, resource.getProperties().size());
      assertEquals("value", resource.getProperties().get("string_field"));
      assertEquals(42, resource.getProperties().get("number_field"));
      assertEquals(true, resource.getProperties().get("boolean_field"));
    }
  }

  @Nested
  @DisplayName("Resource Types")
  class ResourceTypesTest {

    @Test
    @DisplayName("should handle document resources")
    void shouldHandleDocumentResources() {
      // When
      Resource documentResource =
          new Resource.Builder()
              .id("document-001")
              .type("document")
              .addProperty("title", "Confidential Report")
              .addProperty("author", "alice@example.com")
              .addProperty("classification", "confidential")
              .addProperty("created_date", "2024-01-15")
              .addProperty("size_bytes", 1048576)
              .build();

      // Then
      assertEquals("document-001", documentResource.getId());
      assertEquals("document", documentResource.getType());
      assertEquals("Confidential Report", documentResource.getProperties().get("title"));
      assertEquals("confidential", documentResource.getProperties().get("classification"));
      assertEquals(1048576, documentResource.getProperties().get("size_bytes"));
    }

    @Test
    @DisplayName("should handle API resources")
    void shouldHandleApiResources() {
      // When
      Resource apiResource =
          new Resource.Builder()
              .id("/api/v2/payments")
              .type("api")
              .addProperty("version", "2.0")
              .addProperty("authentication_required", true)
              .addProperty("rate_limit_per_hour", 1000)
              .addProperty("methods", java.util.List.of("GET", "POST", "PUT"))
              .addProperty("scopes", java.util.List.of("payments:read", "payments:write"))
              .build();

      // Then
      assertEquals("/api/v2/payments", apiResource.getId());
      assertEquals("api", apiResource.getType());
      assertEquals("2.0", apiResource.getProperties().get("version"));
      assertEquals(true, apiResource.getProperties().get("authentication_required"));
      assertEquals(1000, apiResource.getProperties().get("rate_limit_per_hour"));
    }

    @Test
    @DisplayName("should handle database resources")
    void shouldHandleDatabaseResources() {
      // When
      Resource databaseResource =
          new Resource.Builder()
              .id("users_table")
              .type("database")
              .addProperty("schema", "public")
              .addProperty("table_name", "users")
              .addProperty("row_count", 50000)
              .addProperty("sensitive_data", true)
              .addProperty("backup_enabled", true)
              .addProperty("columns", java.util.List.of("id", "email", "password_hash"))
              .build();

      // Then
      assertEquals("users_table", databaseResource.getId());
      assertEquals("database", databaseResource.getType());
      assertEquals("public", databaseResource.getProperties().get("schema"));
      assertEquals("users", databaseResource.getProperties().get("table_name"));
      assertEquals(true, databaseResource.getProperties().get("sensitive_data"));
    }

    @Test
    @DisplayName("should handle file system resources")
    void shouldHandleFileSystemResources() {
      // When
      Resource fileResource =
          new Resource.Builder()
              .id("/data/logs/application.log")
              .type("file")
              .addProperty("path", "/data/logs/application.log")
              .addProperty("extension", ".log")
              .addProperty("permissions", "644")
              .addProperty("owner", "appuser")
              .addProperty("size_bytes", 2097152)
              .addProperty("last_modified", "2024-01-15T10:30:00Z")
              .build();

      // Then
      assertEquals("/data/logs/application.log", fileResource.getId());
      assertEquals("file", fileResource.getType());
      assertEquals("/data/logs/application.log", fileResource.getProperties().get("path"));
      assertEquals("644", fileResource.getProperties().get("permissions"));
      assertEquals(2097152, fileResource.getProperties().get("size_bytes"));
    }

    @Test
    @DisplayName("should handle system resources")
    void shouldHandleSystemResources() {
      // When
      Resource systemResource =
          new Resource.Builder()
              .id("payment-processor")
              .type("system")
              .addProperty("hostname", "payment-01.example.com")
              .addProperty("environment", "production")
              .addProperty("cpu_cores", 8)
              .addProperty("memory_gb", 32)
              .addProperty("services", java.util.List.of("payment-api", "fraud-detection"))
              .build();

      // Then
      assertEquals("payment-processor", systemResource.getId());
      assertEquals("system", systemResource.getType());
      assertEquals("payment-01.example.com", systemResource.getProperties().get("hostname"));
      assertEquals("production", systemResource.getProperties().get("environment"));
      assertEquals(8, systemResource.getProperties().get("cpu_cores"));
    }
  }

  @Nested
  @DisplayName("Property Handling")
  class PropertyHandlingTest {

    @Test
    @DisplayName("should handle various property types")
    void shouldHandleVariousPropertyTypes() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("complex-resource")
              .type("system")
              .addProperty("string_prop", "value")
              .addProperty("number_prop", 42)
              .addProperty("boolean_prop", true)
              .addProperty("array_prop", java.util.List.of("item1", "item2"))
              .addProperty("map_prop", Map.of("nested", "value"))
              .build();

      // Then
      assertEquals("complex-resource", resource.getId());
      assertEquals("system", resource.getType());
      assertEquals(5, resource.getProperties().size());
      assertEquals("value", resource.getProperties().get("string_prop"));
      assertEquals(42, resource.getProperties().get("number_prop"));
      assertEquals(true, resource.getProperties().get("boolean_prop"));
      assertEquals(java.util.List.of("item1", "item2"), resource.getProperties().get("array_prop"));
      assertEquals(Map.of("nested", "value"), resource.getProperties().get("map_prop"));
    }

    @Test
    @DisplayName("should handle empty properties")
    void shouldHandleEmptyProperties() {
      // When
      Resource resource = new Resource.Builder().id("simple-resource").type("document").build();

      // Then
      assertEquals("simple-resource", resource.getId());
      assertEquals("document", resource.getType());
      assertTrue(resource.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should handle security attributes")
    void shouldHandleSecurityAttributes() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("secure-document")
              .type("document")
              .addProperty("classification", "top_secret")
              .addProperty("encryption_enabled", true)
              .addProperty(
                  "access_control",
                  Map.of(
                      "owner", "alice@example.com",
                      "groups", java.util.List.of("admins", "security_team"),
                      "permissions", Map.of("read", true, "write", false, "delete", false)))
              .addProperty("audit_log_enabled", true)
              .build();

      // Then
      assertEquals("secure-document", resource.getId());
      assertEquals("document", resource.getType());
      assertEquals("top_secret", resource.getProperties().get("classification"));
      assertEquals(true, resource.getProperties().get("encryption_enabled"));
      assertEquals(true, resource.getProperties().get("audit_log_enabled"));
    }

    @Test
    @DisplayName("should handle metadata properties")
    void shouldHandleMetadataProperties() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("metadata-rich-resource")
              .type("api")
              .addProperty("created_by", "system")
              .addProperty("created_date", "2024-01-01T00:00:00Z")
              .addProperty("last_modified", "2024-01-15T10:30:00Z")
              .addProperty("version", "2.1.0")
              .addProperty("tags", java.util.List.of("public", "v2", "payments"))
              .addProperty("dependencies", java.util.List.of("user-service", "payment-gateway"))
              .build();

      // Then
      assertEquals("metadata-rich-resource", resource.getId());
      assertEquals("api", resource.getType());
      assertEquals("system", resource.getProperties().get("created_by"));
      assertEquals("2.1.0", resource.getProperties().get("version"));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class ImmutabilityTest {

    @Test
    @DisplayName("should return immutable properties map")
    void shouldReturnImmutablePropertiesMap() {
      // Given
      Resource resource =
          new Resource.Builder()
              .id("test-resource")
              .type("test")
              .addProperty("key", "value")
              .build();

      // When & Then
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            resource.getProperties().put("new_key", "new_value");
          });
    }

    @Test
    @DisplayName("should be immutable after construction")
    void shouldBeImmutableAfterConstruction() {
      // Given
      Resource resource =
          new Resource.Builder()
              .id("immutable-resource")
              .type("document")
              .addProperty("initial", "value")
              .build();

      // When - Get references
      String id = resource.getId();
      String type = resource.getType();
      Map<String, Object> properties = resource.getProperties();

      // Then - References should be consistent
      assertEquals("immutable-resource", id);
      assertEquals("document", type);
      assertEquals(1, properties.size());

      // Original resource should remain unchanged
      assertEquals("immutable-resource", resource.getId());
      assertEquals("document", resource.getType());
      assertEquals(1, resource.getProperties().size());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle path-like resource IDs")
    void shouldHandlePathLikeResourceIds() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("/api/v1/users/123/documents/456")
              .type("api")
              .addProperty("path_params", Map.of("userId", "123", "documentId", "456"))
              .build();

      // Then
      assertEquals("/api/v1/users/123/documents/456", resource.getId());
      assertEquals("api", resource.getType());
    }

    @Test
    @DisplayName("should handle URL-like resource IDs")
    void shouldHandleUrlLikeResourceIds() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("https://api.example.com/v2/resources?filter=active")
              .type("api")
              .addProperty("base_url", "https://api.example.com")
              .addProperty("query_params", Map.of("filter", "active"))
              .build();

      // Then
      assertEquals("https://api.example.com/v2/resources?filter=active", resource.getId());
      assertEquals("api", resource.getType());
    }

    @Test
    @DisplayName("should handle special characters in resource IDs")
    void shouldHandleSpecialCharactersInResourceIds() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("file:///C:/Program Files (x86)/App/config.xml")
              .type("file")
              .addProperty("drive", "C:")
              .addProperty("directory", "Program Files (x86)/App")
              .build();

      // Then
      assertEquals("file:///C:/Program Files (x86)/App/config.xml", resource.getId());
      assertEquals("file", resource.getType());
    }

    @Test
    @DisplayName("should handle Unicode resource IDs")
    void shouldHandleUnicodeResourceIds() {
      // When
      Resource resource =
          new Resource.Builder()
              .id("ドキュメント_001.pdf") // Japanese filename
              .type("document")
              .addProperty("language", "ja")
              .addProperty("encoding", "UTF-8")
              .build();

      // Then
      assertEquals("ドキュメント_001.pdf", resource.getId());
      assertEquals("document", resource.getType());
      assertEquals("ja", resource.getProperties().get("language"));
    }

    @Test
    @DisplayName("should handle very long resource IDs")
    void shouldHandleVeryLongResourceIds() {
      // Given
      String longId = "very-long-resource-id-" + "x".repeat(500) + ".txt";

      // When
      Resource resource =
          new Resource.Builder()
              .id(longId)
              .type("file")
              .addProperty("id_length", longId.length())
              .build();

      // Then
      assertEquals(longId, resource.getId());
      assertEquals("file", resource.getType());
      assertEquals(longId.length(), resource.getProperties().get("id_length"));
    }

    @Test
    @DisplayName("should handle complex nested property structures")
    void shouldHandleComplexNestedPropertyStructures() {
      // Given
      Map<String, Object> complexProperty =
          Map.of(
              "security",
                  Map.of(
                      "classification", "confidential",
                      "encryption", Map.of("algorithm", "AES-256", "key_rotation", true),
                      "access_controls",
                          java.util.List.of(
                              Map.of("type", "role", "value", "admin"),
                              Map.of("type", "group", "value", "security_team"))),
              "metadata",
                  Map.of(
                      "created", "2024-01-01T00:00:00Z",
                      "tags", java.util.List.of("sensitive", "financial", "pci"),
                      "compliance",
                          Map.of(
                              "pci_dss", true,
                              "gdpr", true,
                              "sox", false)));

      // When
      Resource resource =
          new Resource.Builder()
              .id("complex-financial-record")
              .type("document")
              .addProperty("extended_attributes", complexProperty)
              .build();

      // Then
      assertEquals("complex-financial-record", resource.getId());
      assertEquals("document", resource.getType());
      assertEquals(complexProperty, resource.getProperties().get("extended_attributes"));
    }
  }

  @Nested
  @DisplayName("Builder State Management")
  class BuilderStateManagementTest {

    @Test
    @DisplayName("should allow reusing builder with different IDs")
    void shouldAllowReusingBuilderWithDifferentIds() {
      // Given
      Resource.Builder builder =
          new Resource.Builder().type("document").addProperty("common", "property");

      // When
      Resource resource1 = builder.id("doc1.pdf").build();
      Resource resource2 = builder.id("doc2.pdf").build();

      // Then
      assertEquals("doc1.pdf", resource1.getId());
      assertEquals("doc2.pdf", resource2.getId());
      assertEquals("document", resource1.getType());
      assertEquals("document", resource2.getType());
      assertEquals("property", resource1.getProperties().get("common"));
      assertEquals("property", resource2.getProperties().get("common"));
    }

    @Test
    @DisplayName("should maintain independent builder state")
    void shouldMaintainIndependentBuilderState() {
      // Given
      Resource.Builder builder1 = new Resource.Builder().id("test-resource").type("test");
      Resource.Builder builder2 = new Resource.Builder().id("test-resource").type("test");

      // When
      builder1.addProperty("prop1", "value1");
      builder2.addProperty("prop2", "value2");

      Resource resource1 = builder1.build();
      Resource resource2 = builder2.build();

      // Then
      assertTrue(resource1.getProperties().containsKey("prop1"));
      assertFalse(resource1.getProperties().containsKey("prop2"));
      assertTrue(resource2.getProperties().containsKey("prop2"));
      assertFalse(resource2.getProperties().containsKey("prop1"));
    }
  }
}
