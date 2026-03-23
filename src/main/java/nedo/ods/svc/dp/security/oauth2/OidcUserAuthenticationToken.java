package nedo.ods.svc.dp.security.oauth2;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * An {@link AbstractAuthenticationToken} implementation that holds an {@link OAuth2User} enriched
 * with claims from the OIDC UserInfo endpoint, along with the associated JWT.
 */
public class OidcUserAuthenticationToken extends AbstractAuthenticationToken {

  /** Authenticated OAuth2 user information. */
  private final OAuth2User principal;

  /** JWT token retrieved from OIDC. */
  private final Jwt jwt;

  /**
   * Constructor of OidcUserAuthenticationToken.
   *
   * @param principal OAuth2 user information
   * @param jwt JWT token
   * @param authorities Granted authorities for the user
   */
  public OidcUserAuthenticationToken(
      OAuth2User principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.jwt = jwt;
    setAuthenticated(true);
  }

  /**
   * @see org.springframework.security.core.Authentication#getCredentials()
   */
  @Override
  public Object getCredentials() {
    return this.jwt;
  }

  /**
   * @see org.springframework.security.core.Authentication#getPrincipal()
   */
  @Override
  public Object getPrincipal() {
    return this.principal;
  }
}
