package nedo.ods.svc.dp.security.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link AuthorizationDeniedException}
 *
 * <p>Verify that the exception status code, reason, and message are correctly set.
 */
class AuthorizationDeniedExceptionTest {

  /**
   * Verify that the properties of {@link AuthorizationDeniedException} are set correctly.
   *
   * <ul>
   *   <li>The HTTP status is {@code 403 FORBIDDEN}.
   *   <li>The reason for the exception must match the constructor argument.
   *   <li>The exception message contains the status and reason.
   * </ul>
   */
  @Test
  void testExceptionProperties() {
    String reason = "Access denied due to insufficient permissions";
    AuthorizationDeniedException exception = new AuthorizationDeniedException(reason);
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode(), "Status should be FORBIDDEN");
    assertEquals(reason, exception.getReason(), "Reason should match the input");
    assertTrue(exception.getMessage().contains(reason), "Message should contain the reason");
    assertTrue(
        exception.getMessage().contains("403 FORBIDDEN"), "Message should contain the status");
  }
}
