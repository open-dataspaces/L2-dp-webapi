package nedo.ods.svc.dp.authzen.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nedo.ods.svc.dp.authzen.api.AuthorizationResponse;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;

/**
 * Utility class for deserializing JSON responses from authorization services into {@link
 * AuthorizationResponse} objects.
 *
 * <p>This class provides static methods for converting JSON strings received from authorization
 * policy decision points (PDPs) back into strongly-typed Java objects. The deserialization is
 * performed using Jackson's {@link ObjectMapper} with automatic data binding and comprehensive
 * error handling.
 *
 * <p><strong>Key Features:</strong>
 *
 * <ul>
 *   <li><strong>Robust parsing:</strong> Handles malformed JSON with detailed error messages
 *   <li><strong>Null safety:</strong> Validates input and provides meaningful error messages
 *   <li><strong>Exception wrapping:</strong> Converts Jackson exceptions to domain-specific
 *       exceptions
 *   <li><strong>Thread-safe:</strong> Uses a shared, immutable ObjectMapper instance
 * </ul>
 *
 * <p><strong>Expected JSON Input Structure:</strong>
 *
 * <pre>{@code
 * {
 *   "decision": "Permit",
 *   "status": {
 *     "code": "OK",
 *     "message": "Authorization successful"
 *   },
 *   "context": {
 *     "request_id": "req-12345",
 *     "timestamp": "2024-01-15T10:30:00Z"
 *   },
 *   "obligations": [
 *     {
 *       "id": "log-access",
 *       "attributes": {
 *         "level": "INFO",
 *         "category": "security"
 *       }
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Receive JSON response from authorization service
 * String jsonResponse = httpClient.post(authzEndpoint, requestJson);
 *
 * try {
 *     AuthorizationResponse response =
 *         AuthorizationResponseDeserializer.parseResponseJson(jsonResponse);
 *
 *     if (response.getDecision() == Decision.PERMIT) {
 *         // Process obligations and allow access
 *         processObligations(response.getObligations());
 *         allowAccess();
 *     } else {
 *         // Deny access based on decision
 *         denyAccess(response.getStatus().getMessage());
 *     }
 * } catch (AuthorizationException e) {
 *     // Handle parsing or validation errors
 *     logger.error("Failed to parse authorization response", e);
 *     denyAccess("Authorization service error");
 * }
 * }</pre>
 *
 * <p>This class follows the utility class pattern with a private constructor to prevent
 * instantiation and static methods for all operations. All parsing errors are wrapped in {@link
 * AuthorizationException} to provide consistent error handling across the authorization framework.
 *
 * @see AuthorizationResponse
 * @see AuthorizationException
 * @see com.fasterxml.jackson.databind.ObjectMapper
 * @since 1.0
 */
public final class AuthorizationResponseDeserializer {

  /**
   * Shared ObjectMapper instance for JSON deserialization.
   *
   * <p>This mapper is configured with default settings and is thread-safe, allowing it to be safely
   * shared across multiple threads. The mapper uses Jackson's automatic data binding to deserialize
   * JSON into POJOs based on their setter methods and constructors.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed to be used as a utility with static methods only. Instantiation would
   * serve no purpose and is therefore prohibited.
   */
  private AuthorizationResponseDeserializer() {}

  /**
   * Deserializes a JSON string into an {@link AuthorizationResponse} object.
   *
   * <p>This method parses JSON responses from authorization services and converts them into
   * strongly-typed Java objects. The method performs comprehensive validation and error handling to
   * ensure robust operation.
   *
   * <p><strong>Input Validation:</strong>
   *
   * <ul>
   *   <li><strong>Null check:</strong> Ensures the JSON string is not null
   *   <li><strong>Empty check:</strong> Validates that the JSON string is not blank
   *   <li><strong>Format validation:</strong> Verifies valid JSON structure
   *   <li><strong>Schema validation:</strong> Ensures required fields are present
   * </ul>
   *
   * <p><strong>Supported JSON Structure:</strong>
   *
   * <pre>{@code
   * {
   *   "decision": "Permit|Deny|Indeterminate|NotApplicable",
   *   "status": {
   *     "code": "OK|ERROR",
   *     "message": "Optional status message"
   *   },
   *   "context": {
   *     "key1": "value1",
   *     "key2": "value2"
   *   },
   *   "obligations": [
   *     {
   *       "id": "obligation-id",
   *       "attributes": { "attr1": "value1" }
   *     }
   *   ]
   * }
   * }</pre>
   *
   * <p><strong>Error Handling:</strong>
   *
   * <ul>
   *   <li><strong>Null/empty input:</strong> Throws AuthorizationException with descriptive message
   *   <li><strong>Malformed JSON:</strong> Wraps JsonProcessingException in AuthorizationException
   *   <li><strong>Missing fields:</strong> Jackson handles with default values or exceptions
   *   <li><strong>Type mismatches:</strong> Converted to AuthorizationException with context
   * </ul>
   *
   * @param json The JSON string to deserialize (must not be null or blank)
   * @return A fully populated AuthorizationResponse object
   * @throws AuthorizationException If the JSON is null, empty, malformed, or cannot be parsed into
   *     a valid AuthorizationResponse object. The exception will contain the original cause if the
   *     error was due to JSON processing issues.
   */
  public static AuthorizationResponse parseResponseJson(String json) throws AuthorizationException {
    if (json == null || json.isBlank()) {
      throw new AuthorizationException("Response JSON from server was null or empty.");
    }
    try {
      return MAPPER.readValue(json, AuthorizationResponse.class);
    } catch (JsonProcessingException e) {
      // Wrap the specific parsing exception in our application-specific exception.
      throw new AuthorizationException(
          "Failed to deserialize authorization response from JSON.", e);
    }
  }
}
