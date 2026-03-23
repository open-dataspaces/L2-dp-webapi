package nedo.ods.svc.dp.authzen.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultAuthzClientConfig}.
 *
 * <p>Covers all builder methods, validation, and exception handling.
 */
class DefaultAuthzClientConfigTest {

  /**
   * Test building with only endpoint set.
   *
   * @see DefaultAuthzClientConfig.Builder#endpoint(String)
   */
  @Test
  @DisplayName("Build with only endpoint")
  void testBuildWithOnlyEndpoint() {
    String endpoint = "https://pdp.example.com/authorize";
    DefaultAuthzClientConfig config = DefaultAuthzClientConfig.builder().endpoint(endpoint).build();
    assertEquals(endpoint, config.getEndpoint(), "Endpoint should match");
    assertEquals(Optional.empty(), config.getApiKey(), "API key should be empty");
    assertEquals(Optional.empty(), config.getApiKeyHeader(), "API key header should be empty");
  }

  /**
   * Test building with all parameters set.
   *
   * @see DefaultAuthzClientConfig.Builder#apiKey(String)
   * @see DefaultAuthzClientConfig.Builder#apiKeyHeader(String)
   */
  @Test
  @DisplayName("Build with all parameters")
  void testBuildWithAllParameters() {
    String endpoint = "https://pdp.example.com/authorize";
    String apiKey = "secret-key";
    String apiKeyHeader = "X-API-Key";
    DefaultAuthzClientConfig config =
        DefaultAuthzClientConfig.builder()
            .endpoint(endpoint)
            .apiKey(apiKey)
            .apiKeyHeader(apiKeyHeader)
            .build();
    assertEquals(endpoint, config.getEndpoint(), "Endpoint should match");
    assertEquals(Optional.of(apiKey), config.getApiKey(), "API key should match");
    assertEquals(
        Optional.of(apiKeyHeader), config.getApiKeyHeader(), "API key header should match");
  }

  /** Test building with null API key and header. */
  @Test
  @DisplayName("Build with null API key and header")
  void testBuildWithNullApiKeyAndHeader() {
    String endpoint = "https://pdp.example.com/authorize";
    DefaultAuthzClientConfig config =
        DefaultAuthzClientConfig.builder()
            .endpoint(endpoint)
            .apiKey(null)
            .apiKeyHeader(null)
            .build();
    assertEquals(endpoint, config.getEndpoint(), "Endpoint should match");
    assertEquals(Optional.empty(), config.getApiKey(), "API key should be empty");
    assertEquals(Optional.empty(), config.getApiKeyHeader(), "API key header should be empty");
  }

  /**
   * Test builder throws exception when endpoint is null.
   *
   * @see DefaultAuthzClientConfig.Builder#build()
   */
  @Test
  @DisplayName("Build throws when endpoint is null")
  void testBuildThrowsWhenEndpointIsNull() {
    DefaultAuthzClientConfig.Builder builder = DefaultAuthzClientConfig.builder();
    builder.endpoint(null);
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, builder::build, "Should throw for null endpoint");
    assertEquals("Endpoint must be provided.", ex.getMessage(), "Exception message should match");
  }

  /** Test builder throws exception when endpoint is blank. */
  @Test
  @DisplayName("Build throws when endpoint is blank")
  void testBuildThrowsWhenEndpointIsBlank() {
    DefaultAuthzClientConfig.Builder builder = DefaultAuthzClientConfig.builder();
    builder.endpoint("   ");
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, builder::build, "Should throw for blank endpoint");
    assertEquals("Endpoint must be provided.", ex.getMessage(), "Exception message should match");
  }

  /** Test builder throws exception when endpoint is not a valid URI. */
  @Test
  @DisplayName("Build throws when endpoint is invalid URI")
  void testBuildThrowsWhenEndpointIsInvalidUri() {
    DefaultAuthzClientConfig.Builder builder = DefaultAuthzClientConfig.builder();
    builder.endpoint("ht!tp://invalid uri");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, builder::build, "Should throw for invalid URI");
    assertEquals(
        "Endpoint must be a valid URL.", ex.getMessage(), "Exception message should match");
    assertNotNull(ex.getCause(), "Exception cause should not be null");
  }

  /** Test builder allows method chaining. */
  @Test
  @DisplayName("Builder method chaining")
  void testBuilderMethodChaining() {
    DefaultAuthzClientConfig.Builder builder = DefaultAuthzClientConfig.builder();
    assertSame(
        builder,
        builder.endpoint("https://pdp.example.com/authorize"),
        "endpoint() should return builder");
    assertSame(builder, builder.apiKey("key"), "apiKey() should return builder");
    assertSame(builder, builder.apiKeyHeader("header"), "apiKeyHeader() should return builder");
  }
}
