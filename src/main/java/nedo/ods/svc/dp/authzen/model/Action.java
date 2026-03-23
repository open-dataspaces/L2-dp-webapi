package nedo.ods.svc.dp.authzen.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action (or verb) in an authorization request.
 *
 * <p>An action defines what operation is being requested on a resource within an authorization
 * context. Actions are fundamental components of authorization requests, specifying the type of
 * access or operation that a subject wants to perform.
 *
 * <p>An action consists of:
 *
 * <ul>
 *   <li><strong>Name:</strong> A required identifier that describes the action (e.g., "read",
 *       "write", "delete")
 *   <li><strong>Properties:</strong> Optional additional attributes that provide context about the
 *       action
 * </ul>
 *
 * <p>This class is immutable and thread-safe. Instances should be constructed using the inner
 * {@link Builder} class, which provides a fluent interface for setting the action name and
 * properties.
 *
 * <p><strong>Common action examples:</strong>
 *
 * <ul>
 *   <li><strong>CRUD operations:</strong> "create", "read", "update", "delete"
 *   <li><strong>File operations:</strong> "download", "upload", "share", "copy"
 *   <li><strong>Administrative actions:</strong> "admin", "configure", "manage"
 *   <li><strong>API operations:</strong> "GET", "POST", "PUT", "DELETE"
 *   <li><strong>Business operations:</strong> "approve", "submit", "publish", "review"
 * </ul>
 *
 * <p><strong>Properties usage:</strong>
 *
 * <p>Properties can be used to provide additional context about the action, such as:
 *
 * <ul>
 *   <li>Operation parameters (e.g., "limit": 100 for read operations)
 *   <li>Action metadata (e.g., "method": "GET" for HTTP-based actions)
 *   <li>Contextual information (e.g., "bulk": true for batch operations)
 * </ul>
 *
 * <p><strong>Usage examples:</strong>
 *
 * <pre>{@code
 * // Simple action
 * Action readAction = new Action.Builder()
 *     .name("read")
 *     .build();
 *
 * // Action with properties
 * Action downloadAction = new Action.Builder()
 *     .name("download")
 *     .addProperty("format", "pdf")
 *     .addProperty("compression", true)
 *     .build();
 *
 * // HTTP method action
 * Action httpAction = new Action.Builder()
 *     .name("api_access")
 *     .addProperty("method", "POST")
 *     .addProperty("endpoint", "/api/v1/users")
 *     .build();
 * }</pre>
 *
 * @see <a
 *     href="https://github.com/kkakui/azc/blob/main/docs/authorization-api-1_0_draft_04.md#action">AuthZEN
 *     Authorization API Spec: Action</a>
 * @see nedo.ods.svc.dp.authzen.api.AuthorizationRequest
 * @see Subject
 * @see Resource
 */
public class Action {
  /** The name identifier of the action. */
  private final String name;

  /** Additional properties providing context about the action. */
  private final Map<String, Object> properties;

  /**
   * Private constructor for creating Action instances.
   *
   * @param name The action name identifier
   * @param properties Additional properties for the action
   */
  private Action(String name, Map<String, Object> properties) {
    this.name = name;
    this.properties = Map.copyOf(properties);
  }

  /**
   * Builder class for constructing Action instances.
   *
   * <p>This builder follows the fluent interface pattern, allowing method chaining for convenient
   * construction of Action objects. The builder enforces validation to ensure that required fields
   * are provided.
   *
   * <p><strong>Validation rules:</strong>
   *
   * <ul>
   *   <li>Action name must be provided and cannot be null or blank
   *   <li>Properties are optional and can be empty
   *   <li>Property keys and values can be any valid objects
   * </ul>
   *
   * <p><strong>Usage example:</strong>
   *
   * <pre>{@code
   * Action action = new Action.Builder()
   *     .name("write")
   *     .addProperty("mode", "append")
   *     .addProperty("encoding", "UTF-8")
   *     .build();
   * }</pre>
   */
  public static class Builder {
    /** The action name being configured. */
    private String name;

    /** The properties being configured for the action. */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Sets the name of the action.
     *
     * <p>The action name should be a meaningful identifier that describes the operation being
     * performed. Common conventions include:
     *
     * <ul>
     *   <li>Lowercase verbs (e.g., "read", "write", "delete")
     *   <li>HTTP methods (e.g., "GET", "POST", "PUT")
     *   <li>Business operations (e.g., "approve", "submit")
     * </ul>
     *
     * @param name The action name identifier (required, cannot be null or blank)
     * @return This builder instance for method chaining
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Adds a property to the action.
     *
     * <p>Properties provide additional context about the action and can be used by authorization
     * policies to make more informed decisions. Examples include:
     *
     * <ul>
     *   <li>Operation parameters (e.g., "limit", "offset")
     *   <li>Metadata (e.g., "method", "format")
     *   <li>Flags (e.g., "bulk", "urgent")
     * </ul>
     *
     * <p>If a property with the same key already exists, it will be replaced with the new value.
     *
     * @param key The property key (should not be null)
     * @param value The property value (can be any object, including null)
     * @return This builder instance for method chaining
     */
    public Builder addProperty(String key, Object value) {
      properties.put(key, value);
      return this;
    }

    /**
     * Builds and validates the Action instance.
     *
     * <p>This method performs validation to ensure that the action name is provided and not blank.
     * The resulting Action instance is immutable and thread-safe.
     *
     * @return A new immutable Action instance
     * @throws IllegalArgumentException if the action name is null or blank
     */
    public Action build() {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Action 'name' must not be null or blank.");
      }
      return new Action(name, properties);
    }
  }

  /**
   * Returns the name of this action.
   *
   * <p>The name uniquely identifies the type of operation being requested and is used by
   * authorization policies to determine access permissions.
   *
   * @return The action name identifier (never null or blank)
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the properties associated with this action.
   *
   * <p>Properties provide additional context about the action that can be used by authorization
   * policies for fine-grained access control decisions. The returned map is immutable and cannot be
   * modified.
   *
   * <p>Common property examples:
   *
   * <ul>
   *   <li><strong>Operation parameters:</strong> "limit": 100, "offset": 50
   *   <li><strong>Action metadata:</strong> "method": "POST", "format": "json"
   *   <li><strong>Contextual flags:</strong> "bulk": true, "urgent": false
   * </ul>
   *
   * @return An immutable map of action properties (never null, but may be empty)
   */
  public Map<String, Object> getProperties() {
    return properties;
  }
}
