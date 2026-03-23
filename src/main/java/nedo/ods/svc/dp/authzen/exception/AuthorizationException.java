package nedo.ods.svc.dp.authzen.exception;

/**
 * Exception thrown when authorization operations fail.
 *
 * <p>This checked exception is used to indicate failures during authorization request processing,
 * including communication errors with Policy Decision Points (PDPs), request validation failures,
 * response parsing errors, and other authorization-related issues.
 *
 * <p>Common scenarios that trigger this exception:
 *
 * <ul>
 *   <li><strong>Network failures:</strong> Connection timeouts, DNS resolution errors, network
 *       unavailability
 *   <li><strong>PDP service errors:</strong> HTTP error responses (4xx, 5xx), service
 *       unavailability
 *   <li><strong>Request validation:</strong> Invalid request format, missing required fields,
 *       malformed data
 *   <li><strong>Response processing:</strong> Invalid JSON responses, unexpected response format
 *   <li><strong>Authentication issues:</strong> Invalid API keys, expired tokens, unauthorized
 *       access
 *   <li><strong>Configuration problems:</strong> Invalid endpoint URLs, misconfigured transport
 *       settings
 * </ul>
 *
 * <p>This exception extends {@link Exception}, making it a checked exception that must be handled
 * by calling code. This design choice ensures that authorization failures are explicitly handled
 * rather than propagating unexpectedly through the application.
 *
 * <p><strong>Usage example:</strong>
 *
 * <pre>{@code
 * try {
 *     AuthorizationResponse response = authzClient.authorize(request);
 *     // Process successful response
 * } catch (AuthorizationException e) {
 *     logger.error("Authorization failed: {}", e.getMessage(), e);
 *     // Handle authorization failure (e.g., deny access, retry, fallback)
 * }
 * }</pre>
 *
 * @see
 *     nedo.ods.svc.dp.authzen.api.AuthzClient#authorize(nedo.ods.svc.dp.authzen.api.AuthorizationRequest)
 * @see nedo.ods.svc.dp.authzen.transport.Transport
 */
public class AuthorizationException extends Exception {
  /**
   * Constructs a new AuthorizationException with the specified detail message and cause.
   *
   * <p>This constructor is typically used when wrapping lower-level exceptions (such as network I/O
   * exceptions, JSON parsing exceptions, or HTTP client exceptions) that occur during authorization
   * processing.
   *
   * <p>The cause is preserved for debugging and logging purposes, allowing developers to trace the
   * root cause of authorization failures through the exception chain.
   *
   * @param message The detail message explaining the authorization failure
   * @param cause The underlying cause of the authorization failure (may be {@code null})
   */
  public AuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new AuthorizationException with the specified detail message.
   *
   * <p>This constructor is used when the authorization failure is detected directly by the
   * authorization components without an underlying exception cause. Examples include validation
   * failures, configuration errors, or business logic violations.
   *
   * <p>The cause is implicitly set to {@code null}, indicating that this exception represents the
   * primary failure rather than wrapping another exception.
   *
   * @param message The detail message explaining the authorization failure
   */
  public AuthorizationException(String message) {
    super(message);
  }
}
