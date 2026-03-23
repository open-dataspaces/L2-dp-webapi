package nedo.ods.svc.dp.authzen.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the environmental and contextual data for authorization requests.
 *
 * <p>Context provides "when", "where", and "how" information that supplements the core
 * authorization decision. In the AuthZEN authorization model, context contains arbitrary attributes
 * that can influence policy evaluation, such as temporal information, location data, or request
 * characteristics.
 *
 * <p><strong>Common Context Attributes:</strong>
 *
 * <ul>
 *   <li><strong>Temporal:</strong> "timestamp", "time_of_day", "day_of_week"
 *   <li><strong>Network:</strong> "source_ip", "user_agent", "client_id"
 *   <li><strong>Geographic:</strong> "country", "region", "timezone"
 *   <li><strong>Security:</strong> "authentication_method", "risk_score"
 *   <li><strong>Request:</strong> "request_id", "correlation_id", "session_id"
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Creating context with temporal and network information
 * Map<String, Object> contextData = Map.of(
 *     "timestamp", Instant.now(),
 *     "source_ip", "192.168.1.100",
 *     "user_agent", "Mozilla/5.0...",
 *     "authentication_method", "mfa",
 *     "risk_score", 0.2
 * );
 * Context requestContext = new Context(contextData);
 *
 * // Merging contexts from different sources
 * Context networkContext = new Context(Map.of("source_ip", "10.0.0.1"));
 * Context timeContext = new Context(Map.of("timestamp", Instant.now()));
 * Context combined = networkContext.merge(timeContext);
 * }</pre>
 *
 * <p>This class is immutable and thread-safe. Context attributes are stored as an immutable map,
 * and the {@link #merge(Context)} method creates new instances rather than modifying existing ones.
 *
 * @see <a
 *     href="https://github.com/kkakui/azc/blob/main/docs/authorization-api-1_0_draft_04.md#context">
 *     AuthZEN Authorization API Spec: Context</a>
 * @since 1.0
 */
public class Context {
  /**
   * Immutable map containing contextual attributes for authorization decisions. These attributes
   * provide environmental information that can influence policy evaluation, such as time, location,
   * network details, or request metadata.
   */
  private final Map<String, Object> attributes;

  /**
   * Creates a new Context instance with the provided attributes.
   *
   * <p>The attributes map is copied to ensure immutability. If the provided map is null, an empty
   * context is created.
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * Map<String, Object> attrs = Map.of(
   *     "timestamp", Instant.now(),
   *     "source_ip", "192.168.1.100",
   *     "session_id", "abc123"
   * );
   * Context context = new Context(attrs);
   * }</pre>
   *
   * @param attributes The contextual attributes (can be null for empty context)
   */
  public Context(Map<String, Object> attributes) {
    // Use an immutable copy to prevent external modifications
    this.attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
  }

  /**
   * Returns the contextual attributes for this authorization context.
   *
   * <p>The returned map is immutable and contains all environmental and contextual information that
   * can be used by authorization policies. This method is annotated with {@link JsonValue} to
   * ensure proper JSON serialization where the context is represented as a flat object.
   *
   * <p>Common attribute examples:
   *
   * <ul>
   *   <li><strong>Temporal:</strong> "timestamp": "2024-01-15T10:30:00Z"
   *   <li><strong>Network:</strong> "source_ip": "192.168.1.100"
   *   <li><strong>Request:</strong> "correlation_id": "req-12345"
   * </ul>
   *
   * @return An immutable map of contextual attributes (never null, but may be empty)
   */
  @JsonValue
  public Map<String, Object> getAttributes() {
    // The map is already immutable
    return attributes;
  }

  /**
   * Merges this context with another context, creating a new Context instance.
   *
   * <p>This method combines the attributes from both contexts. If both contexts contain the same
   * key, the value from the {@code other} context takes precedence. The original contexts remain
   * unchanged.
   *
   * <p><strong>Merge Behavior:</strong>
   *
   * <ul>
   *   <li>If {@code other} is null or empty, returns this context unchanged
   *   <li>Duplicate keys are resolved in favor of the {@code other} context
   *   <li>The resulting context is a new immutable instance
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * Context networkCtx = new Context(Map.of("source_ip", "10.0.0.1"));
   * Context timeCtx = new Context(Map.of("timestamp", Instant.now()));
   * Context merged = networkCtx.merge(timeCtx);
   * // merged contains both source_ip and timestamp
   * }</pre>
   *
   * @param other The context to merge with this one (can be null)
   * @return A new Context instance containing attributes from both contexts
   */
  public Context merge(Context other) {
    if (other == null || other.getAttributes().isEmpty()) return this;
    Map<String, Object> mergedAttributes = new HashMap<>(this.attributes);
    mergedAttributes.putAll(other.getAttributes());
    return new Context(mergedAttributes);
  }
}
