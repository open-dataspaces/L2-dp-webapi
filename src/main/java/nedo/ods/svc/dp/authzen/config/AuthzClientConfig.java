package nedo.ods.svc.dp.authzen.config;

import java.util.Optional;

/**
 * Configuration interface for authorization client settings.
 *
 * <p>This interface defines the contract for providing configuration parameters required by {@link
 * nedo.ods.svc.dp.authzen.api.AuthzClient} to communicate with an AuthZEN-compliant Policy Decision
 * Point (PDP).
 *
 * <p>The configuration includes:
 *
 * <ul>
 *   <li><strong>Endpoint:</strong> The URL of the PDP service
 *   <li><strong>API Key:</strong> Optional authentication credential
 *   <li><strong>API Key Header:</strong> Optional custom header name for the API key
 * </ul>
 *
 * <p>Implementations of this interface should provide these configuration values from various
 * sources such as:
 *
 * <ul>
 *   <li>Application properties files
 *   <li>Environment variables
 *   <li>Command line arguments
 *   <li>Configuration management systems
 *   <li>Hard-coded values for testing
 * </ul>
 *
 * <p><strong>Example implementation:</strong>
 *
 * <pre>{@code
 * public class SimpleAuthzClientConfig implements AuthzClientConfig {
 *     private final String endpoint;
 *     private final String apiKey;
 *     private final String apiKeyHeader;
 *
 *     public SimpleAuthzClientConfig(String endpoint, String apiKey, String apiKeyHeader) {
 *         this.endpoint = endpoint;
 *         this.apiKey = apiKey;
 *         this.apiKeyHeader = apiKeyHeader;
 *     }
 *
 *     @Override
 *     public String getEndpoint() {
 *         return endpoint;
 *     }
 *
 *     @Override
 *     public Optional<String> getApiKey() {
 *         return Optional.ofNullable(apiKey);
 *     }
 *
 *     @Override
 *     public Optional<String> getApiKeyHeader() {
 *         return Optional.ofNullable(apiKeyHeader);
 *     }
 * }
 * }</pre>
 *
 * @see nedo.ods.svc.dp.authzen.api.AuthzClient
 * @see java.util.Optional
 */
public interface AuthzClientConfig {
  /**
   * Returns the endpoint URL of the Policy Decision Point (PDP).
   *
   * <p>This should be a complete URL including protocol, host, port (if non-standard), and path to
   * the authorization evaluation endpoint. The URL should be accessible from the application and
   * support the AuthZEN authorization API specification.
   *
   * <p><strong>Examples:</strong>
   *
   * <ul>
   *   <li>{@code https://pdp.example.com/authorize}
   *   <li>{@code http://localhost:8080/v1/data/authz/allow}
   *   <li>{@code https://api.authz-service.com/eval}
   * </ul>
   *
   * @return The PDP endpoint URL (never {@code null})
   */
  String getEndpoint();

  /**
   * Returns the API key for authenticating with the Policy Decision Point.
   *
   * <p>The API key is used to authenticate the authorization client with the PDP service. If
   * present, it will be included in HTTP requests to the PDP. The specific header used for the API
   * key can be customized via {@link #getApiKeyHeader()}.
   *
   * <p>Common use cases:
   *
   * <ul>
   *   <li><strong>Bearer tokens:</strong> When using OAuth 2.0 or similar token-based auth
   *   <li><strong>API keys:</strong> When using simple API key authentication
   *   <li><strong>Custom tokens:</strong> When using proprietary authentication schemes
   * </ul>
   *
   * <p><strong>Security Note:</strong> API keys should be stored securely and not hard-coded in
   * source code. Consider using environment variables, secure configuration management, or key
   * management services.
   *
   * @return An Optional containing the API key, or empty if no authentication is required
   */
  Optional<String> getApiKey();

  /**
   * Returns the HTTP header name to use for the API key.
   *
   * <p>This allows customization of which HTTP header is used to send the API key to the PDP.
   * Different authorization services may expect the API key in different headers, so this provides
   * flexibility to accommodate various PDP implementations.
   *
   * <p>Common header names:
   *
   * <ul>
   *   <li><strong>Authorization:</strong> Standard HTTP authorization header (most common)
   *   <li><strong>X-API-Key:</strong> Common convention for API key headers
   *   <li><strong>X-Auth-Token:</strong> Alternative authentication token header
   *   <li><strong>Custom headers:</strong> Service-specific header names
   * </ul>
   *
   * <p>If this method returns an empty Optional, a default header name should be used by the
   * transport implementation (typically "Authorization").
   *
   * @return An Optional containing the header name for the API key, or empty to use default
   */
  Optional<String> getApiKeyHeader();
}
