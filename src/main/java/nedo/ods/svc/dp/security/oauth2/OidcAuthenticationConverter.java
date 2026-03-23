package nedo.ods.svc.dp.security.oauth2;

import java.util.Collection;

import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import reactor.core.publisher.Mono;

/**
 * {@code OidcAuthenticationConverter} is a Spring component that converts a {@link Jwt} into an
 * {@link AbstractAuthenticationToken} for use in reactive security contexts.
 *
 * <p>This converter is designed for OIDC-based authentication flows in a reactive Spring Security
 * environment. It extracts claims from the provided {@link Jwt} and creates an {@link OAuth2User}
 * principal, which is then wrapped in a custom {@link OidcUserAuthenticationToken}.
 *
 * <p>Currently, role-based authorities are not extracted from the JWT; an empty collection of
 * {@link GrantedAuthority} is used. All JWT claims are preserved in the {@link OAuth2User}
 * attributes for downstream access.
 *
 * @see org.springframework.security.oauth2.jwt.Jwt
 * @see org.springframework.security.core.Authentication
 * @see org.springframework.security.core.context.ReactiveSecurityContextHolder
 */
@Component
public class OidcAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {
  private static final OdsLogger log = OdsLogger.getLogger(OidcAuthenticationConverter.class);

  /**
   * Converts a {@link Jwt} into an {@link AbstractAuthenticationToken} for reactive security.
   *
   * <p>This method logs the {@code operator_id} claim for debugging, creates an {@link OAuth2User}
   * using all JWT claims, and wraps it in an {@link OidcUserAuthenticationToken}. The resulting
   * token is stored in the {@link ReactiveSecurityContextHolder} for propagation.
   *
   * @param jwt the decoded JWT containing user claims
   * @return a {@link Mono} emitting the {@link AbstractAuthenticationToken} for the user
   */
  @Override
  public Mono<AbstractAuthenticationToken> convert(@NonNull Jwt jwt) {
    if (log.isDebugEnabled()) {
      log.debug("operator_id from JWT: " + jwt.getClaimAsString("operator_id"));
    }
    OAuth2User user = createOAuthUser(null, jwt);

    OidcUserAuthenticationToken authentication = new OidcUserAuthenticationToken(user, jwt, null);

    return Mono.just((AbstractAuthenticationToken) authentication)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
  }

  /**
   * Creates an {@link OAuth2User} using JWT claims as attributes.
   *
   * <p>The {@code sub} claim is used as the name attribute key.
   *
   * @param authorities the granted authorities (currently empty)
   * @param jwt the JWT containing user claims
   * @return an {@link OAuth2User} representing the authenticated user
   */
  private static OAuth2User createOAuthUser(Collection<GrantedAuthority> authorities, Jwt jwt) {
    return new DefaultOAuth2User(authorities, jwt.getClaims(), "sub");
  }
}
