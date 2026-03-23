package nedo.ods.svc.dp.authzen.exception;

/**
 * Exception thrown when transport-layer operations fail during authorization request processing.
 *
 * <p>This checked exception indicates failures at the transport layer when communicating with
 * Policy Decision Points (PDPs). It represents low-level communication problems that prevent
 * successful delivery or receipt of authorization requests and responses.
 *
 * <p>Common transport-layer failures that trigger this exception:
 *
 * <ul>
 *   <li><strong>Network connectivity issues:</strong> Connection timeouts, connection refused,
 *       network unreachable
 *   <li><strong>DNS resolution failures:</strong> Unknown host, DNS lookup timeouts
 *   <li><strong>SSL/TLS problems:</strong> Certificate validation failures, protocol mismatches,
 *       handshake failures
 *   <li><strong>HTTP protocol issues:</strong> Malformed requests/responses, protocol version
 *       mismatches
 *   <li><strong>Server errors:</strong> 5xx HTTP status codes indicating PDP service unavailability
 *   <li><strong>Client errors:</strong> 4xx HTTP status codes indicating request problems (except
 *       401/403 which may be authorization-specific)
 *   <li><strong>I/O failures:</strong> Socket exceptions, stream corruption, premature connection
 *       closure
 * </ul>
 *
 * <p>This exception is typically thrown by {@link nedo.ods.svc.dp.authzen.transport.Transport}
 * implementations and is caught and potentially wrapped by higher-level components like {@link
 * nedo.ods.svc.dp.authzen.api.AuthzClient}.
 *
 * <p><strong>Error handling strategy:</strong>
 *
 * <ul>
 *   <li><strong>Transient errors:</strong> Network timeouts, temporary service unavailability - may
 *       be retried
 *   <li><strong>Configuration errors:</strong> Invalid URLs, DNS failures - require configuration
 *       fixes
 *   <li><strong>Authentication errors:</strong> Should be handled at the authorization layer
 *   <li><strong>Protocol errors:</strong> May indicate version mismatches or implementation bugs
 * </ul>
 *
 * <p><strong>Usage example:</strong>
 *
 * <pre>{@code
 * try {
 *     String response = transport.request(config, requestJson);
 *     // Process successful response
 * } catch (TransportException e) {
 *     if (isRetryableError(e)) {
 *         // Implement retry logic for transient failures
 *         logger.warn("Transient transport error, retrying: {}", e.getMessage());
 *     } else {
 *         // Log and fail for non-retryable errors
 *         logger.error("Transport failure: {}", e.getMessage(), e);
 *         throw new AuthorizationException("Authorization service unavailable", e);
 *     }
 * }
 * }</pre>
 *
 * @see nedo.ods.svc.dp.authzen.transport.Transport
 * @see nedo.ods.svc.dp.authzen.api.AuthzClient
 * @see AuthorizationException
 */
public class TransportException extends Exception {
  /**
   * Constructs a new TransportException with the specified detail message.
   *
   * <p>This constructor is used when the transport failure is detected directly without an
   * underlying exception cause. Examples include:
   *
   * <ul>
   *   <li>HTTP status code validation failures
   *   <li>Response format validation errors
   *   <li>Configuration validation failures
   *   <li>Protocol-level constraint violations
   * </ul>
   *
   * <p>The message should provide specific details about the transport failure to aid in
   * troubleshooting and error resolution.
   *
   * @param message The detail message explaining the transport failure (should not be {@code null})
   */
  public TransportException(String message) {
    super(message);
  }

  /**
   * Constructs a new TransportException with the specified detail message and cause.
   *
   * <p>This constructor is typically used when wrapping lower-level exceptions that occur during
   * transport operations. Common underlying causes include:
   *
   * <ul>
   *   <li>{@link java.net.SocketTimeoutException} - Connection or read timeouts
   *   <li>{@link java.net.ConnectException} - Connection refused by server
   *   <li>{@link java.net.UnknownHostException} - DNS resolution failures
   *   <li>{@link javax.net.ssl.SSLException} - SSL/TLS handshake or certificate issues
   *   <li>{@link java.io.IOException} - General I/O failures during communication
   *   <li>HTTP client-specific exceptions from underlying transport implementations
   * </ul>
   *
   * <p>Preserving the cause is crucial for debugging transport issues, as it provides the complete
   * exception chain from the root cause to the transport layer.
   *
   * @param message The detail message explaining the transport failure (should not be {@code null})
   * @param cause The underlying cause of the transport failure (may be {@code null})
   */
  public TransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
