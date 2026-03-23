package nedo.ods.svc.dp.authzen.api;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;
import nedo.ods.svc.dp.authzen.context.ContextFactory;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;
import nedo.ods.svc.dp.authzen.serialization.AuthorizationRequestSerializer;
import nedo.ods.svc.dp.authzen.serialization.AuthorizationResponseDeserializer;
import nedo.ods.svc.dp.authzen.transport.Transport;

/**
 * Client for performing authorization requests against an AuthZEN-compliant Policy Decision Point
 * (PDP).
 *
 * <p>This client provides a high-level interface for evaluating authorization requests by
 * communicating with external authorization services. It handles:
 *
 * <ul>
 *   <li><strong>Request serialization:</strong> Converts authorization requests to JSON
 *   <li><strong>Transport:</strong> Sends requests via configured transport mechanism
 *   <li><strong>Response deserialization:</strong> Parses JSON responses back to objects
 *   <li><strong>Context enrichment:</strong> Optionally adds contextual information to requests
 *   <li><strong>Error handling:</strong> Provides consistent exception handling
 * </ul>
 *
 * <p>The client supports optional context enrichment through a {@link ContextFactory}, which can
 * add dynamic contextual information (such as current time, request metadata, or environmental
 * data) to authorization requests before sending them to the PDP.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Basic client setup
 * AuthzClientConfig config = new AuthzClientConfig("https://pdp.example.com/authorize");
 * Transport transport = new HttpTransport();
 * AuthzClient client = new AuthzClient(config, transport);
 *
 * // Make authorization request
 * AuthorizationRequest request = new AuthorizationRequest.Builder()
 *     .subject(new Subject("user123"))
 *     .resource(new Resource("document456"))
 *     .action(new Action("read"))
 *     .build();
 *
 * AuthorizationResponse response = client.authorize(request);
 * if (response.isAllowed()) {
 *     // Grant access
 * } else {
 *     // Deny access
 * }
 * }</pre>
 *
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 * @see AuthzClientConfig
 * @see Transport
 * @see ContextFactory
 */
public class AuthzClient {
  /** Configuration settings for the authorization client. */
  private final AuthzClientConfig config;

  /** Transport mechanism for sending requests to the PDP. */
  private final Transport transport;

  /** Optional factory for creating dynamic context information. */
  private final ContextFactory contextFactory;

  /**
   * Creates a new AuthzClient with the specified configuration and transport.
   *
   * <p>This constructor creates a client without context enrichment. Use {@link
   * #AuthzClient(AuthzClientConfig, Transport, ContextFactory)} if you need to add dynamic context
   * to authorization requests.
   *
   * @param config Configuration settings for the client (required)
   * @param transport Transport mechanism for communication with PDP (required)
   */
  public AuthzClient(AuthzClientConfig config, Transport transport) {
    this(config, transport, null);
  }

  /**
   * Creates a new AuthzClient with the specified configuration, transport, and context factory.
   *
   * <p>This constructor allows for context enrichment of authorization requests. If a context
   * factory is provided, it will be used to create additional context information that gets merged
   * with the request context before sending to the PDP.
   *
   * @param config Configuration settings for the client (required)
   * @param transport Transport mechanism for communication with PDP (required)
   * @param contextFactory Factory for creating dynamic context, may be {@code null}
   */
  public AuthzClient(AuthzClientConfig config, Transport transport, ContextFactory contextFactory) {
    this.config = config;
    this.transport = transport;
    this.contextFactory = contextFactory;
  }

  /**
   * Performs an authorization request against the configured Policy Decision Point.
   *
   * <p>This method handles the complete authorization flow:
   *
   * <ol>
   *   <li><strong>Context Enrichment:</strong> If a context factory is configured, merges dynamic
   *       context with the request
   *   <li><strong>Serialization:</strong> Converts the request to JSON format
   *   <li><strong>Transport:</strong> Sends the request to the PDP via the configured transport
   *   <li><strong>Deserialization:</strong> Parses the JSON response back to an object
   * </ol>
   *
   * <p>The method provides comprehensive error handling, distinguishing between
   * authorization-specific errors and unexpected system errors.
   *
   * <p><strong>Thread Safety:</strong> This method is thread-safe if the underlying transport and
   * context factory implementations are thread-safe.
   *
   * @param request The authorization request to evaluate (required)
   * @return The authorization response containing the access decision and optional context
   * @throws AuthorizationException if the authorization request fails due to:
   *     <ul>
   *       <li>Network or communication errors
   *       <li>Invalid request format or missing required fields
   *       <li>PDP service errors or unavailability
   *       <li>Response parsing errors
   *       <li>Any other unexpected errors during the authorization process
   *     </ul>
   *
   * @throws IllegalArgumentException if the request is {@code null}
   */
  public AuthorizationResponse authorize(AuthorizationRequest request)
      throws AuthorizationException {
    try {
      if (contextFactory != null) {
        // Create a new request instance with the context from the factory merged in.
        request = request.withMergedContext(contextFactory.createContext());
      }
      String requestJson = AuthorizationRequestSerializer.buildRequestJson(request);
      String responseJson = transport.request(config, requestJson);
      return AuthorizationResponseDeserializer.parseResponseJson(responseJson);
    } catch (AuthorizationException e) {
      // Re-throw the specific exception from the transport layer or deserialization directly.
      throw e;
    } catch (Exception e) {
      // For any other unexpected exceptions, wrap them in an AuthorizationException.
      throw new AuthorizationException(
          "Authorization request failed due to an unexpected error", e);
    }
  }
}
