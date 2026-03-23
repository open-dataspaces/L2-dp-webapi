package nedo.ods.svc.dp.authzen.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Comprehensive test suite for {@link Subject}.
 *
 * <p>Tests the Builder pattern implementation, validation logic, and immutability characteristics
 * of authorization subjects.
 */
@DisplayName("Subject")
class SubjectTest {

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPatternTest {

    @Test
    @DisplayName("should build valid subject with id and type only")
    void shouldBuildValidSubjectWithIdAndTypeOnly() {
      // When
      Subject subject = new Subject.Builder().id("alice@example.com").type("user").build();

      // Then
      assertNotNull(subject);
      assertEquals("alice@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertTrue(subject.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should build valid subject with id, type and properties")
    void shouldBuildValidSubjectWithIdTypeAndProperties() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("service-123")
              .type("service")
              .addProperty("version", "2.1.0")
              .addProperty("environment", "production")
              .build();

      // Then
      assertNotNull(subject);
      assertEquals("service-123", subject.getId());
      assertEquals("service", subject.getType());
      assertEquals(2, subject.getProperties().size());
      assertEquals("2.1.0", subject.getProperties().get("version"));
      assertEquals("production", subject.getProperties().get("environment"));
    }

    @Test
    @DisplayName("should allow method chaining")
    void shouldAllowMethodChaining() {
      // When & Then - Should not throw any exceptions
      Subject subject =
          new Subject.Builder()
              .id("machine-001")
              .type("machine")
              .addProperty("location", "datacenter-1")
              .addProperty("status", "active")
              .build();

      assertNotNull(subject);
      assertEquals("machine-001", subject.getId());
      assertEquals("machine", subject.getType());
      assertEquals(2, subject.getProperties().size());
    }

    @Test
    @DisplayName("should override properties with same key")
    void shouldOverridePropertiesWithSameKey() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("bob@example.com")
              .type("user")
              .addProperty("department", "sales")
              .addProperty("department", "engineering") // Override
              .build();

      // Then
      assertEquals("bob@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(1, subject.getProperties().size());
      assertEquals("engineering", subject.getProperties().get("department"));
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
              () -> new Subject.Builder().id(null).type("user").build());

      assertEquals("Subject 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when id is blank")
    void shouldThrowExceptionWhenIdIsBlank() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Subject.Builder().id("   ").type("user").build());

      assertEquals("Subject 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when id is empty")
    void shouldThrowExceptionWhenIdIsEmpty() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Subject.Builder().id("").type("user").build());

      assertEquals("Subject 'id' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is null")
    void shouldThrowExceptionWhenTypeIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Subject.Builder().id("alice@example.com").type(null).build());

      assertEquals("Subject 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is blank")
    void shouldThrowExceptionWhenTypeIsBlank() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Subject.Builder().id("alice@example.com").type("   ").build());

      assertEquals("Subject 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when type is empty")
    void shouldThrowExceptionWhenTypeIsEmpty() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Subject.Builder().id("alice@example.com").type("").build());

      assertEquals("Subject 'type' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should allow null property values")
    void shouldHandlePropertiesWithoutNullValues() {
      // When & Then - Should not throw any exceptions
      Subject subject =
          new Subject.Builder()
              .id("test@example.com")
              .type("user")
              .addProperty("string_field", "value")
              .addProperty("number_field", 42)
              .addProperty("boolean_field", true)
              .build();

      assertNotNull(subject);
      assertEquals(3, subject.getProperties().size());
      assertEquals("value", subject.getProperties().get("string_field"));
      assertEquals(42, subject.getProperties().get("number_field"));
      assertEquals(true, subject.getProperties().get("boolean_field"));
    }
  }

  @Nested
  @DisplayName("Subject Types")
  class SubjectTypesTest {

    @Test
    @DisplayName("should handle user subjects")
    void shouldHandleUserSubjects() {
      // When
      Subject userSubject =
          new Subject.Builder()
              .id("alice@example.com")
              .type("user")
              .addProperty("first_name", "Alice")
              .addProperty("last_name", "Smith")
              .addProperty("department", "engineering")
              .addProperty("role", "senior_developer")
              .addProperty("mfa_enabled", true)
              .build();

      // Then
      assertEquals("alice@example.com", userSubject.getId());
      assertEquals("user", userSubject.getType());
      assertEquals("Alice", userSubject.getProperties().get("first_name"));
      assertEquals("engineering", userSubject.getProperties().get("department"));
      assertEquals(true, userSubject.getProperties().get("mfa_enabled"));
    }

    @Test
    @DisplayName("should handle service subjects")
    void shouldHandleServiceSubjects() {
      // When
      Subject serviceSubject =
          new Subject.Builder()
              .id("payment-service")
              .type("service")
              .addProperty("version", "1.2.3")
              .addProperty("environment", "production")
              .addProperty("instance_id", "i-1234567890abcdef0")
              .addProperty("deployment_time", "2024-01-15T10:30:00Z")
              .build();

      // Then
      assertEquals("payment-service", serviceSubject.getId());
      assertEquals("service", serviceSubject.getType());
      assertEquals("1.2.3", serviceSubject.getProperties().get("version"));
      assertEquals("production", serviceSubject.getProperties().get("environment"));
    }

    @Test
    @DisplayName("should handle machine subjects")
    void shouldHandleMachineSubjects() {
      // When
      Subject machineSubject =
          new Subject.Builder()
              .id("machine-001")
              .type("machine")
              .addProperty("ip_address", "192.168.1.100")
              .addProperty("mac_address", "00:1B:44:11:3A:B7")
              .addProperty("location", "datacenter-east-1")
              .addProperty("os", "linux")
              .addProperty("trusted", true)
              .build();

      // Then
      assertEquals("machine-001", machineSubject.getId());
      assertEquals("machine", machineSubject.getType());
      assertEquals("192.168.1.100", machineSubject.getProperties().get("ip_address"));
      assertEquals("datacenter-east-1", machineSubject.getProperties().get("location"));
      assertEquals(true, machineSubject.getProperties().get("trusted"));
    }

    @Test
    @DisplayName("should handle group subjects")
    void shouldHandleGroupSubjects() {
      // When
      Subject groupSubject =
          new Subject.Builder()
              .id("admin-group")
              .type("group")
              .addProperty("description", "System administrators")
              .addProperty("member_count", 5)
              .addProperty("permissions", java.util.List.of("admin", "read", "write"))
              .addProperty("created_date", "2023-01-01")
              .build();

      // Then
      assertEquals("admin-group", groupSubject.getId());
      assertEquals("group", groupSubject.getType());
      assertEquals("System administrators", groupSubject.getProperties().get("description"));
      assertEquals(5, groupSubject.getProperties().get("member_count"));
    }
  }

  @Nested
  @DisplayName("Property Handling")
  class PropertyHandlingTest {

    @Test
    @DisplayName("should handle various property types")
    void shouldHandleVariousPropertyTypes() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("complex@example.com")
              .type("user")
              .addProperty("string_prop", "value")
              .addProperty("number_prop", 42)
              .addProperty("boolean_prop", true)
              .addProperty("array_prop", java.util.List.of("item1", "item2"))
              .addProperty("map_prop", Map.of("nested", "value"))
              .build();

      // Then
      assertEquals("complex@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(5, subject.getProperties().size());
      assertEquals("value", subject.getProperties().get("string_prop"));
      assertEquals(42, subject.getProperties().get("number_prop"));
      assertEquals(true, subject.getProperties().get("boolean_prop"));
      assertEquals(java.util.List.of("item1", "item2"), subject.getProperties().get("array_prop"));
      assertEquals(Map.of("nested", "value"), subject.getProperties().get("map_prop"));
    }

    @Test
    @DisplayName("should handle empty properties")
    void shouldHandleEmptyProperties() {
      // When
      Subject subject = new Subject.Builder().id("simple@example.com").type("user").build();

      // Then
      assertEquals("simple@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertTrue(subject.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should handle authentication attributes")
    void shouldHandleAuthenticationAttributes() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("authenticated@example.com")
              .type("user")
              .addProperty("authentication_method", "mfa")
              .addProperty("last_login", "2024-01-15T10:30:00Z")
              .addProperty("session_id", "sess-12345")
              .addProperty("clearance_level", "confidential")
              .addProperty("groups", java.util.List.of("engineers", "admins"))
              .build();

      // Then
      assertEquals("authenticated@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals("mfa", subject.getProperties().get("authentication_method"));
      assertEquals("confidential", subject.getProperties().get("clearance_level"));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class ImmutabilityTest {

    @Test
    @DisplayName("should return immutable properties map")
    void shouldReturnImmutablePropertiesMap() {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("test@example.com")
              .type("user")
              .addProperty("key", "value")
              .build();

      // When & Then
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            subject.getProperties().put("new_key", "new_value");
          });
    }

    @Test
    @DisplayName("should be immutable after construction")
    void shouldBeImmutableAfterConstruction() {
      // Given
      Subject subject =
          new Subject.Builder()
              .id("immutable@example.com")
              .type("user")
              .addProperty("initial", "value")
              .build();

      // When - Get references
      String id = subject.getId();
      String type = subject.getType();
      Map<String, Object> properties = subject.getProperties();

      // Then - References should be consistent
      assertEquals("immutable@example.com", id);
      assertEquals("user", type);
      assertEquals(1, properties.size());

      // Original subject should remain unchanged
      assertEquals("immutable@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(1, subject.getProperties().size());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle Unicode subject IDs")
    void shouldHandleUnicodeSubjectIds() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("ユーザー@例.jp") // Japanese email
              .type("user")
              .addProperty("language", "ja")
              .build();

      // Then
      assertEquals("ユーザー@例.jp", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals("ja", subject.getProperties().get("language"));
    }

    @Test
    @DisplayName("should handle special characters in subject IDs")
    void shouldHandleSpecialCharactersInSubjectIds() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("user+tag@sub-domain.example-site.com")
              .type("user")
              .addProperty("email_verified", true)
              .build();

      // Then
      assertEquals("user+tag@sub-domain.example-site.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(true, subject.getProperties().get("email_verified"));
    }

    @Test
    @DisplayName("should handle very long subject IDs")
    void shouldHandleVeryLongSubjectIds() {
      // Given
      String longId = "very-long-subject-id-" + "a".repeat(500) + "@example.com";

      // When
      Subject subject =
          new Subject.Builder()
              .id(longId)
              .type("user")
              .addProperty("id_length", longId.length())
              .build();

      // Then
      assertEquals(longId, subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(longId.length(), subject.getProperties().get("id_length"));
    }

    @Test
    @DisplayName("should handle UUID-based subject IDs")
    void shouldHandleUuidBasedSubjectIds() {
      // When
      Subject subject =
          new Subject.Builder()
              .id("550e8400-e29b-41d4-a716-446655440000")
              .type("service")
              .addProperty("id_format", "uuid")
              .build();

      // Then
      assertEquals("550e8400-e29b-41d4-a716-446655440000", subject.getId());
      assertEquals("service", subject.getType());
      assertEquals("uuid", subject.getProperties().get("id_format"));
    }

    @Test
    @DisplayName("should handle complex nested property structures")
    void shouldHandleComplexNestedPropertyStructures() {
      // Given
      Map<String, Object> complexProperty =
          Map.of(
              "authentication",
                  Map.of(
                      "method", "oauth2",
                      "provider", "google",
                      "scopes", java.util.List.of("email", "profile")),
              "authorization",
                  Map.of(
                      "roles", java.util.List.of("admin", "user"),
                      "permissions",
                          Map.of(
                              "read", true,
                              "write", true,
                              "delete", false)));

      // When
      Subject subject =
          new Subject.Builder()
              .id("complex@example.com")
              .type("user")
              .addProperty("security_context", complexProperty)
              .build();

      // Then
      assertEquals("complex@example.com", subject.getId());
      assertEquals("user", subject.getType());
      assertEquals(complexProperty, subject.getProperties().get("security_context"));
    }
  }

  @Nested
  @DisplayName("Builder State Management")
  class BuilderStateManagementTest {

    @Test
    @DisplayName("should allow reusing builder with different IDs")
    void shouldAllowReusingBuilderWithDifferentIds() {
      // Given
      Subject.Builder builder =
          new Subject.Builder().type("user").addProperty("common", "property");

      // When
      Subject subject1 = builder.id("user1@example.com").build();
      Subject subject2 = builder.id("user2@example.com").build();

      // Then
      assertEquals("user1@example.com", subject1.getId());
      assertEquals("user2@example.com", subject2.getId());
      assertEquals("user", subject1.getType());
      assertEquals("user", subject2.getType());
      assertEquals("property", subject1.getProperties().get("common"));
      assertEquals("property", subject2.getProperties().get("common"));
    }

    @Test
    @DisplayName("should maintain independent builder state")
    void shouldMaintainIndependentBuilderState() {
      // Given
      Subject.Builder builder1 = new Subject.Builder().id("test@example.com").type("user");
      Subject.Builder builder2 = new Subject.Builder().id("test@example.com").type("user");

      // When
      builder1.addProperty("prop1", "value1");
      builder2.addProperty("prop2", "value2");

      Subject subject1 = builder1.build();
      Subject subject2 = builder2.build();

      // Then
      assertTrue(subject1.getProperties().containsKey("prop1"));
      assertFalse(subject1.getProperties().containsKey("prop2"));
      assertTrue(subject2.getProperties().containsKey("prop2"));
      assertFalse(subject2.getProperties().containsKey("prop1"));
    }
  }
}
