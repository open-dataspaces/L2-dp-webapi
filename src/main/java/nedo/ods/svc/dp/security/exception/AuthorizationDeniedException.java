package nedo.ods.svc.dp.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** An exception thrown when the authorization is denied. */
public class AuthorizationDeniedException extends ResponseStatusException {

  /**
   * Generate an exception with a 403 (FORBIDDEN) status along with the specified reason.
   *
   * @param reason Reason for denial of approval
   */
  public AuthorizationDeniedException(String reason) {
    super(HttpStatus.FORBIDDEN, reason);
  }
}
