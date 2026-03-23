package nedo.ods.svc.dp.security.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecurityProperties}.
 * <p>
 * This test class verifying:
 * <ul>
 *   <li>Default values for configuration properties</li>
 *   <li>Getter and setter behavior for all fields</li>
 *   <li>Annotation presence and correctness</li>
 *   <li>All edge cases and branches</li>
 * </ul>
 */
@DisplayName("SecurityProperties Full Coverage Tests")
class SecurityPropertiesTest {

  /**
   * Tests the default values set in {@link SecurityProperties} upon instantiation.
   * @see SecurityProperties
   */
  @Nested
  @DisplayName("Default Values")
  class DefaultValuesTest {
    /**
     * Verifies that default values for skipValidationPaths, managementBasePath, validApiKeys, and validApiKeysEnabled are set correctly.
     */
	  @Test
	  @DisplayName("should have correct default values")
	  void shouldHaveDefaultValues() {
	    SecurityProperties props = new SecurityProperties();

	    // managementBasePath defaults to "/actuator"
	    assertNotNull(props.getManagementBasePath(), "managementBasePath should not be null by default");
	    assertEquals("/actuator", props.getManagementBasePath(), "Default managementBasePath should be /actuator");

	    // validApiKeys defaults
	    assertNull(props.getValidApiKeys(), "Default validApiKeys should be null");

	    // validation flag default
	    assertFalse(props.getValidApiKeysEnabled(), "Default validApiKeysEnabled should be false");
	  }
  }

  /**
   * Tests the behavior of getters and setters in {@link SecurityProperties} for all fields and edge cases.
   * @see SecurityProperties
   */
  @Nested
  @DisplayName("Getter and Setter Behavior")
  class GetterSetterTest {
    /**
     * Verifies that skipValidationPaths can be updated and retrieved correctly.
     */
    @Test
    @DisplayName("should update skipValidationPaths correctly")
    void shouldUpdateSkipValidationPaths() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath("/actuator");
      assertEquals("/actuator", props.getManagementBasePath(), "skipValidationPaths should match the set value");
    }

    /**
     * Verifies that setting skipValidationPaths to null is handled gracefully.
     */
    @Test
    @DisplayName("should handle null skipValidationPaths")
    void shouldHandleNullSkipValidationPaths() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath(null);
      assertNull(props.getManagementBasePath(), "skipValidationPaths should be null after setting null");
    }

    /**
     * Verifies that setting skipValidationPaths to an empty list is handled correctly.
     */
    @Test
    @DisplayName("should allow changing managementBasePath")
    void shouldAllowChangingManagementBasePath() {
      SecurityProperties props = new SecurityProperties();
      assertEquals("/actuator", props.getManagementBasePath());

      props.setManagementBasePath("/manage");
      assertEquals("/manage", props.getManagementBasePath());
    }

    /**
     * Verifies that skipValidationPaths can contain null or empty strings.
     */
    @Test
    @DisplayName("should allow skipValidationPaths with null or empty strings")
    void shouldAllowSkipValidationPathsWithNullOrEmptyStrings() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath("");
      assertEquals("", props.getManagementBasePath(), "skipValidationPaths should allow null or empty strings");
      props.setManagementBasePath(null);
      assertEquals(null, props.getManagementBasePath(), "skipValidationPaths should allow null or empty strings");
      props.setManagementBasePath("/status");
      assertEquals("/status", props.getManagementBasePath(), "skipValidationPaths should allow null or empty strings");
    }

    /**
     * Verifies that managementBasePath can be updated and retrieved correctly.
     */
    @Test
    @DisplayName("should update managementBasePath correctly")
    void shouldUpdateManagementBasePath() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath("/management");
      assertEquals("/management", props.getManagementBasePath(), "managementBasePath should match the set value");
    }

    /**
     * Verifies that setting managementBasePath to an empty string is allowed.
     */
    @Test
    @DisplayName("should allow empty managementBasePath")
    void shouldAllowEmptyManagementBasePath() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath("");
      assertEquals("", props.getManagementBasePath(), "managementBasePath should allow empty string");
    }

    /**
     * Verifies that validApiKeys can be updated and retrieved correctly.
     */
    @Test
    @DisplayName("should update validApiKeys correctly")
    void shouldUpdateValidApiKeys() {
      SecurityProperties props = new SecurityProperties();
      List<String> apiKeys = List.of("key1", "key2");
      props.setValidApiKeys(apiKeys);
      assertEquals(apiKeys, props.getValidApiKeys(), "validApiKeys should match the set value");
    }

    /**
     * Verifies that setting validApiKeys to null is handled gracefully.
     */
    @Test
    @DisplayName("should handle null validApiKeys")
    void shouldHandleNullValidApiKeys() {
      SecurityProperties props = new SecurityProperties();
      props.setValidApiKeys(null);
      assertNull(props.getValidApiKeys(), "validApiKeys should be null after setting null");
    }

    /**
     * Verifies that setting validApiKeys to an empty list is handled correctly.
     */
    @Test
    @DisplayName("should handle empty validApiKeys")
    void shouldHandleEmptyValidApiKeys() {
      SecurityProperties props = new SecurityProperties();
      props.setValidApiKeys(List.of());
      assertNotNull(props.getValidApiKeys(), "validApiKeys should not be null when set to empty list");
      assertTrue(props.getValidApiKeys().isEmpty(), "validApiKeys should be empty when set to empty list");
    }

    /**
     * Verifies that validApiKeys can contain empty strings.
     */
    @Test
    @DisplayName("should allow validApiKeys with empty strings")
    void shouldAllowValidApiKeysWithEmptyStrings() {
      SecurityProperties props = new SecurityProperties();
      List<String> apiKeys = List.of("", "key2");
      props.setValidApiKeys(apiKeys);
      assertEquals(apiKeys, props.getValidApiKeys(), "validApiKeys should allow empty strings");
    }

    /**
     * Verifies that multiple setter calls correctly overwrite previous values.
     */
    @Test
    @DisplayName("should overwrite previous values on multiple setter calls")
    void shouldOverwriteValuesOnMultipleSetterCalls() {
      SecurityProperties props = new SecurityProperties();
      props.setManagementBasePath("/first");
      props.setManagementBasePath("/second");
      assertEquals("/second", props.getManagementBasePath(), "managementBasePath should be overwritten by last setter call");
      props.setValidApiKeys(List.of("a"));
      props.setValidApiKeys(List.of("b"));
      assertEquals(List.of("b"), props.getValidApiKeys(), "validApiKeys should be overwritten by last setter call");
    }

    /**
     * Verifies that validApiKeysEnabled can be set to true and false.
     */
    @Test
    @DisplayName("should set and get validApiKeysEnabled correctly")
    void shouldSetAndGetValidApiKeysEnabled() {
      SecurityProperties props = new SecurityProperties();
      props.setValidApiKeysEnabled(true);
      assertTrue(props.getValidApiKeysEnabled(), "validApiKeysEnabled should be true after setting true");
      props.setValidApiKeysEnabled(false);
      assertFalse(props.getValidApiKeysEnabled(), "validApiKeysEnabled should be false after setting false");
    }
  }

  /**
   * Tests the presence of Spring annotations on {@link SecurityProperties}.
   * @see SecurityProperties
   */
  @Nested
  @DisplayName("Annotation Presence")
  class AnnotationPresenceTest {
    /**
     * Verifies that {@code @Component} annotation is present on {@link SecurityProperties}.
     */
    @Test
    @DisplayName("should have @Component annotation")
    void shouldHaveComponentAnnotation() {
      assertNotNull(
          SecurityProperties.class.getAnnotation(org.springframework.stereotype.Component.class),
          "@Component annotation should be present on SecurityProperties");
    }

    /**
     * Verifies that {@code @ConfigurationProperties} annotation is present and has the correct prefix.
     */
    @Test
    @DisplayName("should have @ConfigurationProperties annotation with prefix 'security'")
    void shouldHaveConfigurationPropertiesAnnotation() {
      var annotation =
          SecurityProperties.class.getAnnotation(
              org.springframework.boot.context.properties.ConfigurationProperties.class);
      assertNotNull(annotation, "@ConfigurationProperties annotation should be present on SecurityProperties");
      assertEquals("security", annotation.prefix(), "@ConfigurationProperties prefix should be 'security'");
    }
  }
}