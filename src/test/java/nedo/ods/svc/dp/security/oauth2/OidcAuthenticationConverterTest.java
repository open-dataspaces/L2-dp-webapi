package nedo.ods.svc.dp.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the conversion rules of {@link OidcAuthenticationConverter}:
 *
 * <ul>
 *   <li>{@code sub} is used as principal name.
 *   <li>{@code operator_id} is included in principal attributes.
 *   <li>{@code roles} are mapped to {@code ROLE_*} authorities; missing roles produces empty
 *       authorities.
 * </ul>
 */
class OidcAuthenticationConverterTest {

  @Test
  @DisplayName("Should convert JWT claims to OidcUserAuthenticationToken")

  /**
   * Given a JWT containing {@code sub}, {@code operator_id}, and {@code roles}, when converted,
   * then the token is {@link OidcUserAuthenticationToken} with principal name = {@code sub},
   * attributes containing {@code operator_id}, and authorities mapped to {@code ROLE_*}.
   */
  void shouldConvertJwtClaimsToAuthenticationToken() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user123");
    claims.put("operator_id", "op-456");
    claims.put("roles", Arrays.asList("admin", "viewer"));

    Jwt jwt =
        new Jwt(
            "mock-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            claims);

    OidcAuthenticationConverter converter = new OidcAuthenticationConverter();

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertNotNull(token, "Authentication token should not be null");
    assertTrue(
        token instanceof OidcUserAuthenticationToken,
        "Token should be OidcUserAuthenticationToken");

    OAuth2User principal = (OAuth2User) token.getPrincipal();
    assertEquals("user123", principal.getName(), "sub should be used as name");
    assertEquals(
        "op-456", principal.getAttributes().get("operator_id"), "operator_id should be present");
  }


  @Test
  @DisplayName("Should handle missing roles gracefully")

  /**
   * When {@code roles} claim is absent, conversion should succeed and produce an empty authorities
   * collection.
   */
  void shouldHandleMissingRolesGracefully() {

    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user123");
    claims.put("operator_id", "op-789");

    Jwt jwt =
        new Jwt(
            "mock-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            claims);

    OidcAuthenticationConverter converter = new OidcAuthenticationConverter();

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertNotNull(token);
    assertTrue(
        token.getAuthorities().isEmpty(), "Authorities should be empty when roles are missing");
  }
}
