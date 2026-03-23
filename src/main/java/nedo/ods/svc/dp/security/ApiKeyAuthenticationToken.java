package nedo.ods.svc.dp.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/** Represents an authentication token based on an API key. */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

  /** API key used for authentication. */
  private final String apiKey;

  /**
   * Generate an API key authentication token before authentication. The authentication status is
   * set to false.
   *
   * @param apiKey API key used for authentication
   */
  public ApiKeyAuthenticationToken(String apiKey) {
    super(AuthorityUtils.NO_AUTHORITIES);
    this.apiKey = apiKey;
    setAuthenticated(false);
  }

  /**
   * Creates an authenticated API key authentication token. Grants "ROLE_ACTUATOR_ADMIN" authority.
   *
   * @param apiKey API key used for authentication
   * @param authenticated Whether the token is authenticated
   */
  public ApiKeyAuthenticationToken(String apiKey, boolean authenticated) {
    super(AuthorityUtils.createAuthorityList("ROLE_ACTUATOR_ADMIN"));
    this.apiKey = apiKey;
    setAuthenticated(authenticated);
  }

  /**
   * @see org.springframework.security.core.Authentication#getCredentials()
   */
  @Override
  public Object getCredentials() {
    return null;
  }

  /**
   * @see org.springframework.security.core.Authentication#getPrincipal()
   */
  @Override
  public Object getPrincipal() {
    return apiKey;
  }

  /**
   * Returns the API key.
   *
   * @return API key
   */
  public String getApiKey() {
    return apiKey;
  }
}
