package nedo.ods.svc.dp.authzen.transport;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;

/**
 * Core transport abstraction for authorization service communication.
 *
 * <p>This interface defines the contract for sending authorization requests to policy decision
 * points (PDPs) and receiving responses. It abstracts the underlying communication mechanism,
 * allowing different transport implementations such as HTTP, gRPC, message queues, or other
 * protocols.
 *
 * <p><strong>Transport Abstraction Benefits:</strong>
 *
 * <ul>
 *   <li><strong>Protocol independence:</strong> Switch between HTTP, gRPC, or other protocols
 *   <li><strong>Configuration flexibility:</strong> Different transports can have specific settings
 *   <li><strong>Testing support:</strong> Easy mocking and testing with different implementations
 *   <li><strong>Extensibility:</strong> Add new transport mechanisms without changing client code
 * </ul>
 *
 * <p><strong>Implementation Types:</strong>
 *
 * <ul>
 *   <li><strong>HTTP transports:</strong> REST API calls with various HTTP clients
 *   <li><strong>gRPC transports:</strong> High-performance binary protocol communication
 *   <li><strong>Message queue transports:</strong> Asynchronous messaging systems
 *   <li><strong>File-based transports:</strong> Local file system or shared storage
 *   <li><strong>Mock transports:</strong> Testing and development implementations
 * </ul>
 *
 * <p><strong>Configuration Integration:</strong>
 *
 * <p>Transport implementations receive configuration through {@link AuthzClientConfig}, which
 * provides endpoint URLs, authentication credentials, timeouts, and other transport-specific
 * settings. This allows the same transport implementation to work with different authorization
 * services.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Configure transport
 * AuthzClientConfig config = new DefaultAuthzClientConfig.Builder()
 *     .endpoint("https://authz.example.com/authorize")
 *     .apiKey("sk-1234567890abcdef")
 *     .timeout(Duration.ofSeconds(10))
 *     .build();
 *
 * // Use transport
 * Transport transport = new SimpleHttpClient();
 * String jsonRequest = AuthorizationRequestSerializer.buildRequestJson(request);
 *
 * try {
 *     String response = transport.request(config, jsonRequest);
 *     AuthorizationResponse authzResponse =
 *         AuthorizationResponseDeserializer.parseResponseJson(response);
 * } catch (Exception e) {
 *     // Handle transport or authorization errors
 * }
 * }</pre>
 *
 * <p><strong>Error Handling:</strong>
 *
 * <p>Implementations should throw meaningful exceptions that help diagnose transport-related
 * issues. Common error scenarios include network failures, authentication problems, timeout issues,
 * and malformed responses.
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <p>Implementations should be thread-safe to allow concurrent usage across multiple threads. This
 * enables efficient resource utilization and better performance in multi-threaded applications.
 *
 * @see AuthzClientConfig
 * @see nedo.ods.svc.dp.authzen.transport.http.HttpTransport
 * @see nedo.ods.svc.dp.authzen.exception.AuthorizationException
 * @see nedo.ods.svc.dp.authzen.exception.TransportException
 * @since 1.0
 */
public interface Transport {

  /**
   * Sends an authorization request to the configured endpoint and returns the response.
   *
   * <p>This method is responsible for transmitting the JSON-serialized authorization request to the
   * policy decision point and returning the raw response body. The implementation handles all
   * transport-specific concerns including connection management, authentication, error handling,
   * and response processing.
   *
   * <p><strong>Request Processing:</strong>
   *
   * <ul>
   *   <li><strong>Endpoint resolution:</strong> Uses the endpoint from the provided configuration
   *   <li><strong>Authentication:</strong> Applies authentication credentials as configured
   *   <li><strong>Content handling:</strong> Sends the JSON body with appropriate headers
   *   <li><strong>Timeout management:</strong> Respects configured timeout values
   * </ul>
   *
   * <p><strong>Response Handling:</strong>
   *
   * <ul>
   *   <li><strong>Success responses:</strong> Returns the response body as a string
   *   <li><strong>Error responses:</strong> Throws appropriate exceptions with context
   *   <li><strong>Network errors:</strong> Wraps transport errors in meaningful exceptions
   *   <li><strong>Timeout errors:</strong> Handles and reports timeout scenarios
   * </ul>
   *
   * <p><strong>Implementation Requirements:</strong>
   *
   * <ul>
   *   <li>Must validate the configuration before attempting the request
   *   <li>Should provide detailed error messages for debugging
   *   <li>Must handle interrupted operations gracefully
   *   <li>Should log important events for monitoring and troubleshooting
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * // Prepare request
   * String jsonBody = "{\"subject\":{\"id\":\"alice\"},\"action\":{\"name\":\"read\"}}";
   *
   * // Send request
   * String response = transport.request(config, jsonBody);
   *
   * // Response example: "{\"decision\":\"Permit\",\"status\":{\"code\":\"OK\"}}"
   * }</pre>
   *
   * @param config The authorization client configuration containing endpoint, authentication, and
   *     other transport settings (must not be null)
   * @param jsonBody The JSON-serialized authorization request body to send (must not be null or
   *     empty)
   * @return The raw response body as a string from the authorization service
   * @throws Exception If the request fails due to network issues, authentication problems, timeout,
   *     malformed response, or other transport-related errors. Implementations should provide
   *     specific exception types and detailed error messages to help with troubleshooting.
   */
  String request(AuthzClientConfig config, String jsonBody) throws Exception;
}
