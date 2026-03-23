package nedo.ods.svc.dp.authzen.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a resource that is the target of an access request in authorization decisions.
 *
 * <p>A resource identifies "what" a subject is trying to access. In the AuthZEN authorization
 * model, resources are characterized by their type and unique identifier, with optional properties
 * providing additional context for authorization policies.
 *
 * <p><strong>Common Resource Types:</strong>
 *
 * <ul>
 *   <li><strong>Document:</strong> Files, reports, or other document-based resources
 *   <li><strong>API:</strong> REST endpoints, GraphQL queries, or service operations
 *   <li><strong>Database:</strong> Tables, records, or database objects
 *   <li><strong>System:</strong> Applications, services, or infrastructure components
 *   <li><strong>Data:</strong> Datasets, streams, or data collections
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Creating a document resource
 * Resource document = new Resource.Builder()
 *     .id("/documents/financial-report-2024")
 *     .type("document")
 *     .addProperty("classification", "confidential")
 *     .addProperty("owner", "finance-department")
 *     .addProperty("size", 2048576)
 *     .build();
 *
 * // Creating an API resource
 * Resource apiEndpoint = new Resource.Builder()
 *     .id("/api/v1/users")
 *     .type("api")
 *     .addProperty("method", "GET")
 *     .addProperty("version", "1.0")
 *     .addProperty("rate_limited", true)
 *     .build();
 * }</pre>
 *
 * <p>This class is immutable and thread-safe. All instances should be created using the {@link
 * Builder} class, which provides validation and ensures the resource is properly configured.
 *
 * @see <a
 *     href="https://github.com/kkakui/azc/blob/main/docs/authorization-api-1_0_draft_04.md#resource">
 *     AuthZEN Authorization API Spec: Resource</a>
 * @since 1.0
 */
public class Resource {
  /**
   * The unique identifier for this resource. This could be a file path, URL, database ID, or any
   * other unique identifier that distinguishes this resource from others.
   */
  private final String id;

  /**
   * The type of resource (e.g., "document", "api", "database", "system"). This helps authorization
   * policies understand how to handle the resource and what properties to expect.
   */
  private final String type;

  /**
   * Additional properties that provide context about the resource. These can include attributes
   * like classification level, owner, size, location, or any other resource-specific metadata.
   */
  private final Map<String, Object> properties;

  /**
   * Private constructor used by the Builder to create immutable Resource instances. Direct
   * instantiation is not allowed; use {@link Builder} instead.
   *
   * @param id The resource identifier
   * @param type The resource type
   * @param properties The resource properties (will be copied to ensure immutability)
   */
  private Resource(String id, String type, Map<String, Object> properties) {
    this.id = id;
    this.type = type;
    this.properties = Map.copyOf(properties);
  }

  /**
   * Builder for creating immutable Resource instances.
   *
   * <p>The Builder pattern ensures that all required fields are set and validated before creating
   * the Resource instance. Both {@code id} and {@code type} are required fields and must be
   * non-null and non-blank.
   *
   * <p><strong>Validation Rules:</strong>
   *
   * <ul>
   *   <li>Resource ID must not be null or blank
   *   <li>Resource type must not be null or blank
   *   <li>Properties are optional and can be added incrementally
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * Resource secureFile = new Resource.Builder()
   *     .id("/secure/documents/contract.pdf")
   *     .type("document")
   *     .addProperty("classification", "top-secret")
   *     .addProperty("encryption", "AES-256")
   *     .build();
   * }</pre>
   */
  public static class Builder {
    /** The resource identifier to be set. */
    private String id;

    /** The resource type to be set. */
    private String type;

    /** Mutable map for collecting properties during building. */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Sets the unique identifier for the resource.
     *
     * <p>The resource ID should uniquely identify the target of the access request. Common patterns
     * include:
     *
     * <ul>
     *   <li><strong>File paths:</strong> "/documents/report.pdf" or "s3://bucket/file.txt"
     *   <li><strong>URLs:</strong> "/api/v1/users/123" or "https://api.example.com/data"
     *   <li><strong>Database identifiers:</strong> "table:users" or "record:user:123"
     *   <li><strong>UUIDs:</strong> "550e8400-e29b-41d4-a716-446655440000"
     * </ul>
     *
     * @param id The resource identifier (required, cannot be null or blank)
     * @return This builder instance for method chaining
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the type of the resource.
     *
     * <p>The resource type categorizes the kind of asset being accessed, which helps authorization
     * policies apply appropriate rules. Standard types include:
     *
     * <ul>
     *   <li><strong>"document":</strong> Files, reports, or document-based resources
     *   <li><strong>"api":</strong> REST endpoints, GraphQL queries, or service operations
     *   <li><strong>"database":</strong> Tables, records, or database objects
     *   <li><strong>"system":</strong> Applications, services, or infrastructure components
     *   <li><strong>"data":</strong> Datasets, streams, or data collections
     * </ul>
     *
     * @param type The resource type identifier (required, cannot be null or blank)
     * @return This builder instance for method chaining
     */
    public Builder type(String type) {
      this.type = type;
      return this;
    }

    /**
     * Adds a property to the resource.
     *
     * <p>Properties provide additional context about the resource that can be used by authorization
     * policies for making access control decisions. Examples include:
     *
     * <ul>
     *   <li><strong>Security attributes:</strong> "classification", "encryption", "sensitivity"
     *   <li><strong>Ownership info:</strong> "owner", "department", "project"
     *   <li><strong>Technical metadata:</strong> "size", "format", "version"
     *   <li><strong>Location info:</strong> "region", "datacenter", "availability_zone"
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
     * Builds and validates the Resource instance.
     *
     * <p>This method performs validation to ensure that both the resource ID and type are provided
     * and not blank. The resulting Resource instance is immutable and thread-safe.
     *
     * @return A new immutable Resource instance
     * @throws IllegalArgumentException if the resource ID is null or blank, or if the resource type
     *     is null or blank
     */
    public Resource build() {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Resource 'id' must not be null or blank.");
      }
      if (type == null || type.isBlank()) {
        throw new IllegalArgumentException("Resource 'type' must not be null or blank.");
      }
      return new Resource(id, type, properties);
    }
  }

  /**
   * Returns the unique identifier of this resource.
   *
   * <p>The ID uniquely identifies the resource within the authorization context and is used by
   * authorization policies to look up resource-specific information and make access control
   * decisions.
   *
   * @return The resource identifier (never null or blank)
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the type of this resource.
   *
   * <p>The type categorizes the kind of asset and helps authorization policies understand how to
   * process the resource and what properties to expect.
   *
   * @return The resource type (never null or blank)
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the properties associated with this resource.
   *
   * <p>Properties provide additional context about the resource that can be used by authorization
   * policies for fine-grained access control decisions. The returned map is immutable and cannot be
   * modified.
   *
   * <p>Common property examples:
   *
   * <ul>
   *   <li><strong>Security properties:</strong> "classification": "confidential", "encrypted": true
   *   <li><strong>Ownership properties:</strong> "owner": "alice", "department": "finance"
   *   <li><strong>Technical properties:</strong> "size": 1048576, "format": "pdf"
   * </ul>
   *
   * @return An immutable map of resource properties (never null, but may be empty)
   */
  public Map<String, Object> getProperties() {
    return properties;
  }
}
