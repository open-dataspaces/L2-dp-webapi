package nedo.ods.svc.dp.gateway.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ErrorResponse}.
 *
 * <p>This test class verifies the behavior of the ErrorResponse builder, getters, default values,
 * and JSON output.
 *
 * <ul>
 *   <li>Valid construction and getter values
 *   <li>Timestamp is set when detail is blank or null
 *   <li>Correct JSON output from toString()
 * </ul>
 */
class ErrorResponseTest {

  @Test
  void testBuilderAndGetters() {
    ErrorResponse response =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.NotFound)
            .message("Resource not found")
            .detail("No resource with given ID")
            .build();

    assertEquals("[dataspace] NotFound", response.getCode());
    assertEquals("Resource not found", response.getMessage());
    assertEquals("No resource with given ID", response.getDetail());
  }

  @Test
  void testDetailDefaultsToTimestamp() {
    ErrorResponse response =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.InternalServerError)
            .message("Unexpected error")
            .build();

    assertEquals("[dataspace] InternalServerError", response.getCode());
    assertEquals("Unexpected error", response.getMessage());
    assertTrue(response.getDetail().startsWith("timeStamp:"));
  }

  @Test
  void testDetailBlankUsesTimestamp() {
    ErrorResponse response =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.InternalServerError)
            .message("Unexpected error")
            .detail("") // blank detail
            .build();

    assertEquals("[dataspace] InternalServerError", response.getCode());
    assertEquals("Unexpected error", response.getMessage());
    assertTrue(response.getDetail().startsWith("timeStamp:"));
  }

  @Test
  void testDetailNullUsesTimestamp() {
    ErrorResponse response =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.InternalServerError)
            .message("Unexpected error")
            .detail(null) // null detail
            .build();

    String json = response.toString();
    assertTrue(json.contains("\"detail\":\"timeStamp:"));
  }

  @Test
  void testToStringJsonFormat() {
    ErrorResponse response =
        new ErrorResponse.Builder()
            .code(ErrorResponse.ErrorCode.Unauthorized)
            .message("Unauthorized access")
            .detail("Token expired")
            .build();

    String json = response.toString();
    assertTrue(json.contains("\"code\":\"[dataspace] Unauthorized\""));
    assertTrue(json.contains("\"message\":\"Unauthorized access\""));
    assertTrue(json.contains("\"detail\":\"Token expired\""));
  }

  @Test
  void testToStringJsonFormatWithNulls() {
    ErrorResponse response = new ErrorResponse.Builder().build();
    String json = response.toString();
    assertTrue(json.contains("\"code\":\"\""));
    assertTrue(json.contains("\"message\":\"\""));
    assertTrue(json.contains("\"detail\":\"timeStamp:"));
  }
}
