package nedo.ods.svc.dp.authzen.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a subject (user, service, or machine principal) in an authorization request.
 *
 * <p>A subject identifies "who" is requesting access to a resource. In the AuthZEN authorization
 * model, subjects are characterized by their type and unique identifier, with optional properties
 * providing additional context for authorization decisions.
 *
 * <p><strong>Common Subject Types:</strong>
 *
 * <ul>
 *   <li><strong>User:</strong> Human users with authentication credentials
 *   <li><strong>Service:</strong> Applications or microservices
 *   <li><strong>Machine:</strong> Automated systems or devices
 *   <li><strong>Group:</strong> Collections of users or other subjects
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Creating a user subject
 * Subject user = new Subject.Builder()
 *     .id("alice@example.com")
 *     .type("user")
 *     .addProperty("department", "engineering")
 *     .addProperty("clearance_level", "confidential")
 *     .build();
 *
 * // Creating a service subject
 * Subject service = new Subject.Builder()
 *     .id("payment-service")
 *     .type("service")
 *     .addProperty("version", "2.1.0")
 *     .addProperty("environment", "production")
 *     .build();
 * }</pre>
 *
 * <p>This class is immutable and thread-safe. All instances should be created using the {@link
 * Builder} class, which provides validation and ensures the subject is properly configured.
 *
 * @see <a
 *     href="https://github.com/kkakui/azc/blob/main/docs/authorization-api-1_0_draft_04.md#subject">
 *     AuthZEN Authorization API Spec: Subject</a>
 * @since 1.0
 */
public class Subject {
  /**
   * The unique identifier for this subject. This could be an email address, username, service name,
   * or any other unique identifier that distinguishes this subject from others.
   */
  private final String id;

  /**
   * The type of subject (e.g., "user", "service", "machine", "group"). This helps authorization
   * policies understand how to handle the subject and what properties to expect.
   */
  private final String type;

  /**
   * Additional properties that provide context about the subject. These can include attributes like
   * department, role, clearance level, version information, or any other subject-specific metadata.
   */
  private final Map<String, Object> properties;

  /**
   * Private constructor used by the Builder to create immutable Subject instances. Direct
   * instantiation is not allowed; use {@link Builder} instead.
   *
   * @param id The subject identifier
   * @param type The subject type
   * @param properties The subject properties (will be copied to ensure immutability)
   */
  private Subject(String id, String type, Map<String, Object> properties) {
    this.id = id;
    this.type = type;
    this.properties = Map.copyOf(properties);
  }

  /**
   * Builder for creating immutable Subject instances.
   *
   * <p>The Builder pattern ensures that all required fields are set and validated before creating
   * the Subject instance. Both {@code id} and {@code type} are required fields and must be non-null
   * and non-blank.
   *
   * <p><strong>Validation Rules:</strong>
   *
   * <ul>
   *   <li>Subject ID must not be null or blank
   *   <li>Subject type must not be null or blank
   *   <li>Properties are optional and can be added incrementally
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * Subject adminUser = new Subject.Builder()
   *     .id("admin@company.com")
   *     .type("user")
   *     .addProperty("role", "administrator")
   *     .addProperty("mfa_enabled", true)
   *     .build();
   * }</pre>
   */
  public static class Builder {
    /** The subject identifier to be set. */
    private String id;

    /** The subject type to be set. */
    private String type;

    /** Mutable map for collecting properties during building. */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Sets the unique identifier for the subject.
     *
     * <p>The subject ID should uniquely identify the principal making the request. Common patterns
     * include:
     *
     * <ul>
     *   <li><strong>Email addresses:</strong> "alice@example.com" for users
     *   <li><strong>Service names:</strong> "payment-service" for applications
     *   <li><strong>System IDs:</strong> "machine-001" for devices
     *   <li><strong>UUIDs:</strong> "550e8400-e29b-41d4-a716-446655440000"
     * </ul>
     *
     * @param id The subject identifier (required, cannot be null or blank)
     * @return This builder instance for method chaining
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the type of the subject.
     *
     * <p>The subject type categorizes the kind of principal making the request, which helps
     * authorization policies apply appropriate rules. Standard types include:
     *
     * <ul>
     *   <li><strong>"user":</strong> Human users with authentication credentials
     *   <li><strong>"service":</strong> Applications, microservices, or APIs
     *   <li><strong>"machine":</strong> Automated systems, IoT devices, or bots
     *   <li><strong>"group":</strong> Collections of users or other subjects
     * </ul>
     *
     * @param type The subject type identifier (required, cannot be null or blank)
     * @return This builder instance for method chaining
     */
    public Builder type(String type) {
      this.type = type;
      return this;
    }

    /**
     * Adds a property to the subject.
     *
     * <p>Properties provide additional context about the subject that can be used by authorization
     * policies for making access control decisions. Examples include:
     *
     * <ul>
     *   <li><strong>User attributes:</strong> "department", "role", "clearance_level"
     *   <li><strong>Service metadata:</strong> "version", "environment", "instance_id"
     *   <li><strong>Authentication info:</strong> "mfa_enabled", "last_login"
     *   <li><strong>Group memberships:</strong> "groups", "permissions"
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
     * Builds and validates the Subject instance.
     *
     * <p>This method performs validation to ensure that both the subject ID and type are provided
     * and not blank. The resulting Subject instance is immutable and thread-safe.
     *
     * @return A new immutable Subject instance
     * @throws IllegalArgumentException if the subject ID is null or blank, or if the subject type
     *     is null or blank
     */
    public Subject build() {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Subject 'id' must not be null or blank.");
      }
      if (type == null || type.isBlank()) {
        throw new IllegalArgumentException("Subject 'type' must not be null or blank.");
      }
      return new Subject(id, type, properties);
    }
  }

  /**
   * Returns the unique identifier of this subject.
   *
   * <p>The ID uniquely identifies the principal within the authorization context and is used by
   * authorization policies to look up subject-specific information and make access control
   * decisions.
   *
   * @return The subject identifier (never null or blank)
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the type of this subject.
   *
   * <p>The type categorizes the kind of principal and helps authorization policies understand how
   * to process the subject and what properties to expect.
   *
   * @return The subject type (never null or blank)
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the properties associated with this subject.
   *
   * <p>Properties provide additional context about the subject that can be used by authorization
   * policies for fine-grained access control decisions. The returned map is immutable and cannot be
   * modified.
   *
   * <p>Common property examples:
   *
   * <ul>
   *   <li><strong>User properties:</strong> "department": "engineering", "role": "admin"
   *   <li><strong>Service properties:</strong> "version": "1.2.3", "environment": "prod"
   *   <li><strong>Security attributes:</strong> "mfa_enabled": true, "clearance": "secret"
   * </ul>
   *
   * @return An immutable map of subject properties (never null, but may be empty)
   */
  public Map<String, Object> getProperties() {
    return properties;
  }
}
