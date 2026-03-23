package nedo.ods.svc.dp.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link OidcUserAuthenticationToken}
 *
 * <p>Verify that the properties of the authentication token generated using OAuth2User and JWT are
 * correctly maintained.
 */
class OidcUserAuthenticationTokenTest {

  /**
   * Verify that the properties of {@link OidcUserAuthenticationToken} are correctly set.
   *
   * <ul>
   *   <li>The principal is an {@link OAuth2User}.
   *   <li>Credentials are of type {@link Jwt}.
   *   <li>Being authenticated
   *   <li>The permissions are set correctly.
   * </ul>
   */
  @Test
  void testOidcUserAuthenticationTokenProperties() {

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "user123");
    attributes.put("name", "Test User");

    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    OAuth2User oAuth2User = new DefaultOAuth2User(authorities, attributes, "sub");

    Jwt jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", "user123")
            .claim("email", "user@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    OidcUserAuthenticationToken token =
        new OidcUserAuthenticationToken(oAuth2User, jwt, authorities);

    assertEquals(oAuth2User, token.getPrincipal(), "Principal should be the OAuth2User");
    assertEquals(jwt, token.getCredentials(), "Credentials should be the Jwt");
    assertTrue(token.isAuthenticated(), "Token should be authenticated");
    assertEquals(authorities, token.getAuthorities(), "Authorities should match");
  }
}
