package nedo.ods.svc.dp.authzen.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents the response from an authorization evaluation, containing the access decision and
 * optional contextual information.
 *
 * <p>An authorization response provides the result of evaluating an authorization request,
 * including:
 *
 * <ul>
 *   <li><strong>Decision:</strong> Whether access is allowed or denied
 *   <li><strong>Context:</strong> Additional information about the authorization decision
 *       (optional)
 * </ul>
 *
 * <p>This class is immutable and designed to be used as the result of authorization policy
 * evaluation. The context may contain additional attributes such as obligations, advice, or other
 * metadata related to the authorization decision.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Simple allow response
 * AuthorizationResponse allowResponse = new AuthorizationResponse(true, null);
 *
 * // Response with context
 * Map<String, Object> context = Map.of(
 *     "reason", "User has admin role",
 *     "expires_at", "2024-12-31T23:59:59Z"
 * );
 * AuthorizationResponse contextResponse = new AuthorizationResponse(true, context);
 * }</pre>
 *
 * @see AuthorizationRequest
 */
public class AuthorizationResponse {
  /**
   * Whether access is allowed. {@code true} indicates access is granted, {@code false} indicates
   * access is denied.
   */
  private final boolean allowed;

  /**
   * Additional contextual information about the authorization decision. May contain obligations,
   * advice, or other metadata. Can be {@code null}.
   */
  private final Map<String, Object> context;

  /**
   * Creates a new AuthorizationResponse with the specified decision and context.
   *
   * <p>This constructor is annotated with Jackson annotations to support JSON deserialization from
   * authorization policy decision points.
   *
   * @param allowed {@code true} if access is allowed, {@code false} if denied
   * @param context Additional contextual information about the decision, may be {@code null}
   */
  @JsonCreator
  public AuthorizationResponse(
      @JsonProperty("decision") boolean allowed,
      @JsonProperty("context") Map<String, Object> context) {
    this.allowed = allowed;
    this.context = context;
  }

  /**
   * Returns whether access is allowed.
   *
   * @return {@code true} if access is granted, {@code false} if access is denied
   */
  public boolean isAllowed() {
    return allowed;
  }

  /**
   * Returns the contextual information associated with this authorization decision.
   *
   * <p>The context may contain additional attributes such as:
   *
   * <ul>
   *   <li>Obligations that must be fulfilled
   *   <li>Advice for the requestor
   *   <li>Metadata about the decision process
   *   <li>Expiration times or other temporal constraints
   * </ul>
   *
   * @return A map of contextual attributes, or {@code null} if no context is provided
   */
  public Map<String, Object> getContext() {
    return context;
  }
}
