package nedo.ods.svc.dp.authzen.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransportException}.
 *
 * <p>This test class covers all constructors, exception chaining, and message handling for
 * TransportException.
 */
class TransportExceptionTest {

  /**
   * Test the constructor with message only.
   *
   * @see TransportException#TransportException(String)
   */
  @Test
  @DisplayName("Constructor with message only")
  void testConstructorWithMessageOnly() {
    String message = "Transport failed due to timeout";
    TransportException ex = new TransportException(message);
    assertEquals(message, ex.getMessage(), "Exception message should match the provided message");
    assertNull(ex.getCause(), "Exception cause should be null when not provided");
  }

  /**
   * Test the constructor with message and cause.
   *
   * @see TransportException#TransportException(String, Throwable)
   */
  @Test
  @DisplayName("Constructor with message and cause")
  void testConstructorWithMessageAndCause() {
    String message = "Transport failed due to network error";
    Throwable cause = new RuntimeException("Network unreachable");
    TransportException ex = new TransportException(message, cause);
    assertEquals(message, ex.getMessage(), "Exception message should match the provided message");
    assertEquals(cause, ex.getCause(), "Exception cause should match the provided cause");
  }

  /**
   * Test the constructor with null cause.
   *
   * @see TransportException#TransportException(String, Throwable)
   */
  @Test
  @DisplayName("Constructor with null cause")
  void testConstructorWithNullCause() {
    String message = "Transport failed due to unknown error";
    TransportException ex = new TransportException(message, null);
    assertEquals(message, ex.getMessage(), "Exception message should match the provided message");
    assertNull(ex.getCause(), "Exception cause should be null when null is provided");
  }
}
