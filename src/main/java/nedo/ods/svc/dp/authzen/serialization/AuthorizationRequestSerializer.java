package nedo.ods.svc.dp.authzen.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import nedo.ods.svc.dp.authzen.api.AuthorizationRequest;

/**
 * Utility class for serializing {@link AuthorizationRequest} objects to JSON format.
 *
 * <p>This class provides static methods for converting authorization request objects into JSON
 * strings that can be transmitted over HTTP or stored for later processing. The serialization is
 * performed using Jackson's {@link ObjectMapper} with automatic data binding based on standard
 * JavaBean conventions.
 *
 * <p><strong>Key Features:</strong>
 *
 * <ul>
 *   <li><strong>Zero-configuration:</strong> Automatic serialization of POJO fields using getters
 *   <li><strong>Context flattening:</strong> Proper handling of {@link
 *       nedo.ods.svc.dp.authzen.model.Context} serialization
 *   <li><strong>Thread-safe:</strong> Uses a shared, immutable ObjectMapper instance
 *   <li><strong>Exception handling:</strong> Propagates Jackson exceptions for proper error
 *       handling
 * </ul>
 *
 * <p><strong>JSON Output Structure:</strong>
 *
 * <pre>{@code
 * {
 *   "subject": {
 *     "id": "alice@example.com",
 *     "type": "user",
 *     "properties": {
 *       "department": "engineering"
 *     }
 *   },
 *   "action": {
 *     "name": "read",
 *     "properties": {
 *       "method": "GET"
 *     }
 *   },
 *   "resource": {
 *     "id": "/documents/report.pdf",
 *     "type": "document",
 *     "properties": {
 *       "classification": "confidential"
 *     }
 *   },
 *   "context": {
 *     "timestamp": "2024-01-15T10:30:00Z",
 *     "source_ip": "192.168.1.100"
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Create authorization request
 * AuthorizationRequest request = new AuthorizationRequest.Builder()
 *     .subject(new Subject.Builder().id("alice").type("user").build())
 *     .action(new Action.Builder().name("read").build())
 *     .resource(new Resource.Builder().id("/data").type("file").build())
 *     .context(new Context(Map.of("ip", "192.168.1.1")))
 *     .build();
 *
 * // Serialize to JSON
 * try {
 *     String json = AuthorizationRequestSerializer.buildRequestJson(request);
 *     // Send JSON to authorization service
 * } catch (Exception e) {
 *     // Handle serialization errors
 * }
 * }</pre>
 *
 * <p>This class follows the utility class pattern with a private constructor to prevent
 * instantiation and static methods for all operations. The underlying ObjectMapper is configured
 * for optimal performance and thread safety.
 *
 * @see AuthorizationRequest
 * @see com.fasterxml.jackson.databind.ObjectMapper
 * @since 1.0
 */
public class AuthorizationRequestSerializer {
  /**
   * Shared ObjectMapper instance for JSON serialization.
   *
   * <p>This mapper is configured with default settings and is thread-safe, allowing it to be safely
   * shared across multiple threads. The mapper uses Jackson's automatic data binding to serialize
   * POJOs based on their getter methods.
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed to be used as a utility with static methods only. Instantiation would
   * serve no purpose and is therefore prohibited.
   */
  private AuthorizationRequestSerializer() {
    // Prevent instantiation of this utility class
  }

  /**
   * Serializes an {@link AuthorizationRequest} object to a JSON string.
   *
   * <p>This method converts the authorization request and all its nested objects (Subject, Action,
   * Resource, Context) into a JSON representation suitable for transmission to authorization
   * services or storage.
   *
   * <p><strong>Serialization Details:</strong>
   *
   * <ul>
   *   <li><strong>POJO mapping:</strong> Uses Jackson's automatic data binding via getter methods
   *   <li><strong>Context flattening:</strong> The {@link nedo.ods.svc.dp.authzen.model.Context}
   *       object is serialized as a flat map due to the {@code @JsonValue} annotation
   *   <li><strong>Null handling:</strong> Follows Jackson's default null handling policies
   *   <li><strong>Property inclusion:</strong> Only includes properties with non-null getters
   * </ul>
   *
   * <p><strong>Example Output:</strong>
   *
   * <pre>{@code
   * {
   *   "subject": { "id": "alice", "type": "user" },
   *   "action": { "name": "read" },
   *   "resource": { "id": "/file.txt", "type": "document" },
   *   "context": { "timestamp": "2024-01-15T10:30:00Z" }
   * }
   * }</pre>
   *
   * @param request The authorization request to serialize (must not be null)
   * @return A JSON string representation of the authorization request
   * @throws Exception If serialization fails due to Jackson configuration issues, circular
   *     references, or other JSON processing errors
   * @throws NullPointerException If the request parameter is null
   */
  public static String buildRequestJson(AuthorizationRequest request) throws Exception {
    // The model classes (AuthorizationRequest, Subject, etc.) are standard POJOs
    // with getters that Jackson can use for automatic data binding.
    // The @JsonValue annotation on Context.getAttributes() ensures the context
    // is serialized as a flat map, not an object containing an 'attributes' field.
    return mapper.writeValueAsString(request);
  }
}
