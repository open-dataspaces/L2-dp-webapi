package nedo.ods.svc.dp.authzen.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Comprehensive test suite for {@link Action}.
 *
 * <p>Tests the Builder pattern implementation, validation logic, and immutability characteristics
 * of authorization actions.
 */
@DisplayName("Action")
class ActionTest {

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPatternTest {

    @Test
    @DisplayName("should build valid action with name only")
    void shouldBuildValidActionWithNameOnly() {
      // When
      Action action = new Action.Builder().name("read").build();

      // Then
      assertNotNull(action);
      assertEquals("read", action.getName());
      assertTrue(action.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should build valid action with name and properties")
    void shouldBuildValidActionWithNameAndProperties() {
      // When
      Action action =
          new Action.Builder()
              .name("write")
              .addProperty("method", "POST")
              .addProperty("content_type", "application/json")
              .build();

      // Then
      assertNotNull(action);
      assertEquals("write", action.getName());
      assertEquals(2, action.getProperties().size());
      assertEquals("POST", action.getProperties().get("method"));
      assertEquals("application/json", action.getProperties().get("content_type"));
    }

    @Test
    @DisplayName("should allow method chaining")
    void shouldAllowMethodChaining() {
      // When & Then - Should not throw any exceptions
      Action action =
          new Action.Builder()
              .name("execute")
              .addProperty("async", true)
              .addProperty("timeout", 5000)
              .build();

      assertNotNull(action);
      assertEquals("execute", action.getName());
      assertEquals(2, action.getProperties().size());
    }

    @Test
    @DisplayName("should override properties with same key")
    void shouldOverridePropertiesWithSameKey() {
      // When
      Action action =
          new Action.Builder()
              .name("update")
              .addProperty("version", "1.0")
              .addProperty("version", "2.0") // Override
              .build();

      // Then
      assertEquals("update", action.getName());
      assertEquals(1, action.getProperties().size());
      assertEquals("2.0", action.getProperties().get("version"));
    }
  }

  @Nested
  @DisplayName("Validation")
  class ValidationTest {

    @Test
    @DisplayName("should throw exception when name is null")
    void shouldThrowExceptionWhenNameIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new Action.Builder().name(null).build());

      assertEquals("Action 'name' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when name is blank")
    void shouldThrowExceptionWhenNameIsBlank() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new Action.Builder().name("   ").build());

      assertEquals("Action 'name' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should throw exception when name is empty")
    void shouldThrowExceptionWhenNameIsEmpty() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Action.Builder().name("").build());

      assertEquals("Action 'name' must not be null or blank.", exception.getMessage());
    }

    @Test
    @DisplayName("should handle properties without null values")
    void shouldHandlePropertiesWithoutNullValues() {
      // When & Then - Should not throw any exceptions
      Action action =
          new Action.Builder()
              .name("test")
              .addProperty("string_field", "value")
              .addProperty("number_field", 42)
              .addProperty("boolean_field", true)
              .build();

      assertNotNull(action);
      assertEquals(3, action.getProperties().size());
      assertEquals("value", action.getProperties().get("string_field"));
      assertEquals(42, action.getProperties().get("number_field"));
      assertEquals(true, action.getProperties().get("boolean_field"));
    }
  }

  @Nested
  @DisplayName("Property Handling")
  class PropertyHandlingTest {

    @Test
    @DisplayName("should handle various property types")
    void shouldHandleVariousPropertyTypes() {
      // When
      Action action =
          new Action.Builder()
              .name("complex_action")
              .addProperty("string_prop", "value")
              .addProperty("number_prop", 42)
              .addProperty("boolean_prop", true)
              .addProperty("array_prop", java.util.List.of("item1", "item2"))
              .addProperty("map_prop", Map.of("nested", "value"))
              .build();

      // Then
      assertEquals("complex_action", action.getName());
      assertEquals(5, action.getProperties().size());
      assertEquals("value", action.getProperties().get("string_prop"));
      assertEquals(42, action.getProperties().get("number_prop"));
      assertEquals(true, action.getProperties().get("boolean_prop"));
      assertEquals(java.util.List.of("item1", "item2"), action.getProperties().get("array_prop"));
      assertEquals(Map.of("nested", "value"), action.getProperties().get("map_prop"));
    }

    @Test
    @DisplayName("should handle empty properties")
    void shouldHandleEmptyProperties() {
      // When
      Action action = new Action.Builder().name("simple_action").build();

      // Then
      assertEquals("simple_action", action.getName());
      assertTrue(action.getProperties().isEmpty());
    }

    @Test
    @DisplayName("should handle many properties")
    void shouldHandleManyProperties() {
      // Given
      Action.Builder builder = new Action.Builder().name("batch_action");

      // When
      for (int i = 0; i < 100; i++) {
        builder.addProperty("prop_" + i, "value_" + i);
      }
      Action action = builder.build();

      // Then
      assertEquals("batch_action", action.getName());
      assertEquals(100, action.getProperties().size());
      assertEquals("value_0", action.getProperties().get("prop_0"));
      assertEquals("value_99", action.getProperties().get("prop_99"));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class ImmutabilityTest {

    @Test
    @DisplayName("should return immutable properties map")
    void shouldReturnImmutablePropertiesMap() {
      // Given
      Action action =
          new Action.Builder().name("immutable_test").addProperty("key", "value").build();

      // When & Then
      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            action.getProperties().put("new_key", "new_value");
          });
    }

    @Test
    @DisplayName("should be immutable after construction")
    void shouldBeImmutableAfterConstruction() {
      // Given
      Action action =
          new Action.Builder().name("test_action").addProperty("initial", "value").build();

      // When - Get references
      String name = action.getName();
      Map<String, Object> properties = action.getProperties();

      // Then - References should be consistent
      assertEquals("test_action", name);
      assertEquals(1, properties.size());

      // Original action should remain unchanged
      assertEquals("test_action", action.getName());
      assertEquals(1, action.getProperties().size());
    }
  }

  @Nested
  @DisplayName("Common Action Types")
  class CommonActionTypesTest {

    @Test
    @DisplayName("should handle HTTP method actions")
    void shouldHandleHttpMethodActions() {
      // Given & When
      Action getAction =
          new Action.Builder()
              .name("GET")
              .addProperty("method", "GET")
              .addProperty("idempotent", true)
              .build();

      Action postAction =
          new Action.Builder()
              .name("POST")
              .addProperty("method", "POST")
              .addProperty("idempotent", false)
              .addProperty("content_type", "application/json")
              .build();

      // Then
      assertEquals("GET", getAction.getName());
      assertEquals(true, getAction.getProperties().get("idempotent"));

      assertEquals("POST", postAction.getName());
      assertEquals(false, postAction.getProperties().get("idempotent"));
      assertEquals("application/json", postAction.getProperties().get("content_type"));
    }

    @Test
    @DisplayName("should handle business operation actions")
    void shouldHandleBusinessOperationActions() {
      // Given & When
      Action approveAction =
          new Action.Builder()
              .name("approve")
              .addProperty("requires_manager", true)
              .addProperty("audit_required", true)
              .build();

      Action submitAction =
          new Action.Builder()
              .name("submit")
              .addProperty("workflow_stage", "initial")
              .addProperty("notification_required", false)
              .build();

      // Then
      assertEquals("approve", approveAction.getName());
      assertEquals(true, approveAction.getProperties().get("requires_manager"));

      assertEquals("submit", submitAction.getName());
      assertEquals("initial", submitAction.getProperties().get("workflow_stage"));
    }

    @Test
    @DisplayName("should handle CRUD operations")
    void shouldHandleCrudOperations() {
      // Given & When
      Action createAction =
          new Action.Builder().name("create").addProperty("operation_type", "CREATE").build();

      Action readAction =
          new Action.Builder()
              .name("read")
              .addProperty("operation_type", "READ")
              .addProperty("cacheable", true)
              .build();

      Action updateAction =
          new Action.Builder()
              .name("update")
              .addProperty("operation_type", "UPDATE")
              .addProperty("partial", false)
              .build();

      Action deleteAction =
          new Action.Builder()
              .name("delete")
              .addProperty("operation_type", "DELETE")
              .addProperty("soft_delete", true)
              .build();

      // Then
      assertEquals("create", createAction.getName());
      assertEquals("CREATE", createAction.getProperties().get("operation_type"));

      assertEquals("read", readAction.getName());
      assertEquals(true, readAction.getProperties().get("cacheable"));

      assertEquals("update", updateAction.getName());
      assertEquals(false, updateAction.getProperties().get("partial"));

      assertEquals("delete", deleteAction.getName());
      assertEquals(true, deleteAction.getProperties().get("soft_delete"));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle Unicode action names")
    void shouldHandleUnicodeActionNames() {
      // When
      Action action =
          new Action.Builder()
              .name("読み取り") // Japanese for "read"
              .addProperty("language", "ja")
              .build();

      // Then
      assertEquals("読み取り", action.getName());
      assertEquals("ja", action.getProperties().get("language"));
    }

    @Test
    @DisplayName("should handle special characters in action names")
    void shouldHandleSpecialCharactersInActionNames() {
      // When
      Action action =
          new Action.Builder()
              .name("action-with-dashes_and_underscores.and.dots")
              .addProperty("special_chars", true)
              .build();

      // Then
      assertEquals("action-with-dashes_and_underscores.and.dots", action.getName());
      assertEquals(true, action.getProperties().get("special_chars"));
    }

    @Test
    @DisplayName("should handle very long action names")
    void shouldHandleVeryLongActionNames() {
      // Given
      String longName = "a".repeat(1000);

      // When
      Action action = new Action.Builder().name(longName).addProperty("length", 1000).build();

      // Then
      assertEquals(longName, action.getName());
      assertEquals(1000, action.getProperties().get("length"));
    }

    @Test
    @DisplayName("should handle complex nested property structures")
    void shouldHandleComplexNestedPropertyStructures() {
      // Given
      Map<String, Object> complexProperty =
          Map.of(
              "level1",
              Map.of(
                  "level2",
                  Map.of("level3", "deep_value", "array", java.util.List.of(1, 2, 3)),
                  "simple",
                  "value"),
              "top_level",
              "top_value");

      // When
      Action action =
          new Action.Builder()
              .name("complex_action")
              .addProperty("complex_structure", complexProperty)
              .build();

      // Then
      assertEquals("complex_action", action.getName());
      assertEquals(complexProperty, action.getProperties().get("complex_structure"));
    }
  }

  @Nested
  @DisplayName("Builder State Management")
  class BuilderStateManagementTest {

    @Test
    @DisplayName("should allow reusing builder with different names")
    void shouldAllowReusingBuilderWithDifferentNames() {
      // Given
      Action.Builder builder = new Action.Builder().addProperty("common", "property");

      // When
      Action action1 = builder.name("action1").build();
      Action action2 = builder.name("action2").build();

      // Then
      assertEquals("action1", action1.getName());
      assertEquals("action2", action2.getName());
      assertEquals("property", action1.getProperties().get("common"));
      assertEquals("property", action2.getProperties().get("common"));
    }

    @Test
    @DisplayName("should maintain independent builder state")
    void shouldMaintainIndependentBuilderState() {
      // Given
      Action.Builder builder1 = new Action.Builder().name("test");
      Action.Builder builder2 = new Action.Builder().name("test");

      // When
      builder1.addProperty("prop1", "value1");
      builder2.addProperty("prop2", "value2");

      Action action1 = builder1.build();
      Action action2 = builder2.build();

      // Then
      assertTrue(action1.getProperties().containsKey("prop1"));
      assertFalse(action1.getProperties().containsKey("prop2"));
      assertTrue(action2.getProperties().containsKey("prop2"));
      assertFalse(action2.getProperties().containsKey("prop1"));
    }
  }
}
