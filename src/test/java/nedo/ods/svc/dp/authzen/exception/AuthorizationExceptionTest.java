package nedo.ods.svc.dp.authzen.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuthorizationException}.
 *
 * <p>This test class covers all constructors, exception chaining, and message handling for
 * AuthorizationException.
 */
class AuthorizationExceptionTest {

  /**
   * Test the constructor with message and cause.
   *
   * @see AuthorizationException#AuthorizationException(String, Throwable)
   */
  @Test
  @DisplayName("Constructor with message and cause")
  void testConstructorWithMessageAndCause() {
    String message = "Authorization failed due to network error";
    Throwable cause = new RuntimeException("Network unreachable");
    AuthorizationException ex = new AuthorizationException(message, cause);
    assertEquals(message, ex.getMessage(), "Exception message should match the provided message");
    assertEquals(cause, ex.getCause(), "Exception cause should match the provided cause");
  }

  /**
   * Test the constructor with message only.
   *
   * @see AuthorizationException#AuthorizationException(String)
   */
  @Test
  @DisplayName("Constructor with message only")
  void testConstructorWithMessageOnly() {
    String message = "Authorization failed due to invalid token";
    AuthorizationException ex = new AuthorizationException(message);
    assertEquals(message, ex.getMessage(), "Exception message should match the provided message");
    assertNull(ex.getCause(), "Exception cause should be null when not provided");
  }
}
