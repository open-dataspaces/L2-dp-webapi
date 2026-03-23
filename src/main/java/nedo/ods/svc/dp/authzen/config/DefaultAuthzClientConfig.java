package nedo.ods.svc.dp.authzen.config;

import java.net.URI;
import java.util.Optional;

/**
 * Default implementation of {@link AuthzClientConfig} with builder pattern support.
 *
 * <p>This class provides a concrete implementation of the authorization client configuration
 * interface, offering a convenient way to create immutable configuration instances using the
 * builder pattern. It includes validation to ensure that required configuration parameters are
 * properly set.
 *
 * <p>The implementation supports:
 *
 * <ul>
 *   <li><strong>Immutable configuration:</strong> Once built, configuration values cannot be
 *       changed
 *   <li><strong>Builder pattern:</strong> Fluent interface for convenient construction
 *   <li><strong>Validation:</strong> Ensures endpoint is provided and is a valid URI
 *   <li><strong>Optional parameters:</strong> API key and header name are optional
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Basic configuration with just endpoint
 * DefaultAuthzClientConfig config = DefaultAuthzClientConfig.builder()
 *     .endpoint("https://pdp.example.com/authorize")
 *     .build();
 *
 * // Full configuration with authentication
 * DefaultAuthzClientConfig config = DefaultAuthzClientConfig.builder()
 *     .endpoint("https://pdp.example.com/authorize")
 *     .apiKey("your-secret-api-key")
 *     .apiKeyHeader("X-API-Key")
 *     .build();
 * }</pre>
 *
 * @see AuthzClientConfig
 * @see nedo.ods.svc.dp.authzen.api.AuthzClient
 */
public class DefaultAuthzClientConfig implements AuthzClientConfig {
  /** The endpoint URL of the Policy Decision Point. */
  private final String endpoint;

  /** The API key for authentication, may be null. */
  private final String apiKey;

  /** The HTTP header name for the API key, may be null. */
  private final String apiKeyHeader;

  /**
   * Private constructor used by the builder.
   *
   * @param builder The builder containing the configuration values
   */
  private DefaultAuthzClientConfig(Builder builder) {
    this.endpoint = builder.endpoint;
    this.apiKey = builder.apiKey;
    this.apiKeyHeader = builder.apiKeyHeader;
  }

  /**
   * Creates a new builder for constructing DefaultAuthzClientConfig instances.
   *
   * @return A new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** {@inheritDoc} */
  @Override
  public String getEndpoint() {
    return endpoint;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> getApiKey() {
    return Optional.ofNullable(apiKey);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> getApiKeyHeader() {
    return Optional.ofNullable(apiKeyHeader);
  }

  /**
   * Builder class for constructing DefaultAuthzClientConfig instances.
   *
   * <p>This builder follows the fluent interface pattern, allowing method chaining for convenient
   * construction of configuration objects. The builder enforces validation rules to ensure that
   * required configuration is provided.
   *
   * <p><strong>Validation rules:</strong>
   *
   * <ul>
   *   <li>Endpoint must be provided and cannot be null or blank
   *   <li>Endpoint must be a valid URI format
   *   <li>API key and API key header are optional
   * </ul>
   *
   * <p><strong>Usage example:</strong>
   *
   * <pre>{@code
   * DefaultAuthzClientConfig config = DefaultAuthzClientConfig.builder()
   *     .endpoint("https://pdp.example.com/authorize")
   *     .apiKey("secret-key")
   *     .apiKeyHeader("Authorization")
   *     .build();
   * }</pre>
   */
  public static class Builder {
    /** The endpoint URL being configured. */
    private String endpoint;

    /** The API key being configured. */
    private String apiKey;

    /** The API key header name being configured. */
    private String apiKeyHeader;

    /** Private constructor to enforce use of factory method. */
    private Builder() {}

    /**
     * Sets the endpoint URL for the Policy Decision Point.
     *
     * <p>The endpoint must be a valid URI and will be validated when {@link #build()} is called.
     *
     * @param endpoint The PDP endpoint URL (required)
     * @return This builder instance for method chaining
     */
    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    /**
     * Sets the API key for authenticating with the PDP.
     *
     * <p>This is optional. If not provided, no authentication will be used in requests to the PDP.
     *
     * @param apiKey The API key for authentication (optional)
     * @return This builder instance for method chaining
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Sets the HTTP header name to use for the API key.
     *
     * <p>This is optional. If not provided, the transport implementation will use its default
     * header name (typically "Authorization").
     *
     * @param apiKeyHeader The header name for the API key (optional)
     * @return This builder instance for method chaining
     */
    public Builder apiKeyHeader(String apiKeyHeader) {
      this.apiKeyHeader = apiKeyHeader;
      return this;
    }

    /**
     * Builds and validates the DefaultAuthzClientConfig instance.
     *
     * <p>This method performs validation to ensure that:
     *
     * <ul>
     *   <li>The endpoint is provided and not null or blank
     *   <li>The endpoint is a valid URI format
     * </ul>
     *
     * @return A new immutable DefaultAuthzClientConfig instance
     * @throws IllegalStateException if the endpoint is null, blank, or not a valid URI
     */
    public DefaultAuthzClientConfig build() {
      if (endpoint == null || endpoint.isBlank()) {
        throw new IllegalStateException("Endpoint must be provided.");
      }
      try {
        // Validate that the endpoint is a valid URI
        URI.create(endpoint);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Endpoint must be a valid URL.", e);
      }
      return new DefaultAuthzClientConfig(this);
    }
  }
}
