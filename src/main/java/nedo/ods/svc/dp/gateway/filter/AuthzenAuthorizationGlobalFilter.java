package nedo.ods.svc.dp.gateway.filter;

import java.time.Instant;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import nedo.ods.svc.dp.authzen.api.AuthorizationRequest;
import nedo.ods.svc.dp.authzen.api.AuthzClient;
import nedo.ods.svc.dp.authzen.model.Action;
import nedo.ods.svc.dp.authzen.model.Resource;
import nedo.ods.svc.dp.authzen.model.Subject;
import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import nedo.ods.svc.dp.security.exception.AuthorizationDeniedException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A Spring Cloud Gateway global filter that performs authorization using AuthZEN. For each request,
 * it sends an authorization request to AuthZEN based on the authentication context, and proceeds
 * only if access is allowed
 */
public class AuthzenAuthorizationGlobalFilter implements GlobalFilter, Ordered {

  /** Logger for logging configuration details. */
  private static final OdsLogger log = OdsLogger.getLogger(AuthzenAuthorizationGlobalFilter.class);

  /** Instance of the AuthZEN client. */
  private final AuthzClient authzClient;

  /** Authorization configuration properties. */
  private final ApiGatewayProperties.Authzen authzenProperties;

  /**
   * Constructor for AuthzenAuthorizationGlobalFilter. Accepts the authorization client and
   * configuration properties.
   *
   * @param authzClient AuthZEN client
   * @param authzenProperties Authorization configuration properties
   */
  public AuthzenAuthorizationGlobalFilter(
      AuthzClient authzClient, ApiGatewayProperties.Authzen authzenProperties) {
    this.authzClient = authzClient;
    this.authzenProperties = authzenProperties;
  }

  /*
   * @see org.springframework.cloud.gateway.filter.GlobalFilter#filter(ServerWebExchange,
   *     GatewayFilterChain)
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  final var reason =
                      "SecurityContext is missing, so authorization processing cannot be performed.";
                  log.error(reason);
                  return Mono.error(new IllegalStateException(reason));
                }))
        .flatMap(
            securityContext -> {
              // The authentication object is set by the OidcUserInfoAuthenticationConverter
              var authentication = securityContext.getAuthentication();
              AuthorizationRequest authzRequest = buildAuthzRequest(exchange, authentication);
              if (log.isDebugEnabled()) {
                log.debug(
                    String.format(
                        "Dispatching AuthZEN request. Request: {subject=%s:%s, resource=%s:%s, action=%s}",
                        authzRequest.getSubject().getType(),
                        authzRequest.getSubject().getId(),
                        authzRequest.getResource().getType(),
                        authzRequest.getResource().getId(),
                        authzRequest.getAction().getName()));
              }

              return Mono.fromCallable(
                      () -> authzClient.authorize(authzRequest)) // The authorize call is blocking
                  .subscribeOn(
                      Schedulers
                          .boundedElastic()) // Move the blocking call to a dedicated thread pool
                  .publishOn(
                      Schedulers
                          .parallel()) // Switch back to a worker thread to handle the response
                  .onErrorResume(
                      e -> {
                        // Catch exceptions from the AuthZEN client (e.g., network issues)
                        log.error("AuthZEN client call failed with an exception.", e);
                        return Mono.error(
                            new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Authorization service unavailable.",
                                e));
                      })
                  .flatMap(
                      response -> {
                        if (response.isAllowed()) {
                          if (log.isDebugEnabled()) {
                            log.debug(
                                String.format(
                                    "AuthZEN check passed (allowed). Request: {subject=%s:%s, resource=%s:%s, action=%s}",
                                    authzRequest.getSubject().getType(),
                                    authzRequest.getSubject().getId(),
                                    authzRequest.getResource().getType(),
                                    authzRequest.getResource().getId(),
                                    authzRequest.getAction().getName()));
                          }
                          return chain.filter(exchange);
                        } else {
                          String reason =
                              String.format(
                                  "AuthZEN check failed (denied). Request: {subject=%s:%s, resource=%s:%s, action=%s}, Context: %s",
                                  authzRequest.getSubject().getType(),
                                  authzRequest.getSubject().getId(),
                                  authzRequest.getResource().getType(),
                                  authzRequest.getResource().getId(),
                                  authzRequest.getAction().getName(),
                                  response.getContext());
                          log.warn(reason);
                          // Signal a specific "access denied" error to be handled by the global
                          // error handler.
                          return Mono.error(new AuthorizationDeniedException(reason));
                        }
                      });
            });
  }

  /**
   * Builds an authorization request. Constructs an AuthorizationRequest for AuthZEN using
   * authentication and request details.
   *
   * @param exchange ServerWebExchange
   * @param authentication Authentication
   * @return AuthorizationRequest instance
   */
  private AuthorizationRequest buildAuthzRequest(
      ServerWebExchange exchange, Authentication authentication) {
    return new AuthorizationRequest.Builder()
        .subject(buildSubject(authentication))
        .resource(buildResource(exchange))
        .action(buildAction())
        .build();
  }

  /**
   * Builds the subject for the authorization request.
   *
   * @param authentication Authentication
   * @return Subject instance
   * @throws IllegalArgumentException if the authentication principal is not an OAuth2User
   * @throws IllegalArgumentException if the subject ID attribute is missing or blank in OAuth2User
   */
  private Subject buildSubject(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
      final var reason =
          "Authentication principal is not an instance of OAuth2User, so cannot build subject of authorization request for AuthZEN.";
      log.error(reason);
      throw new IllegalArgumentException(reason);
    }

    final var oauth2User = (OAuth2User) authentication.getPrincipal();
    final var subjectId =
        (String) oauth2User.getAttribute(this.authzenProperties.getSubjectIdAttributeName());

    if (subjectId == null || subjectId.isBlank()) {
      final var reason =
          String.format(
              "'%s' attribute is missing or blank in OAuth2User, so cannot build subject of authorization request for AuthZEN.",
              this.authzenProperties.getSubjectIdAttributeName());
      log.error(reason);
      throw new IllegalArgumentException(reason);
    }

    final var subjectBuilder =
        new Subject.Builder().type(this.authzenProperties.getSubjectType()).id(subjectId);

    // Add all user attributes to the AuthZEN request.
    // We must convert Instant types to a primitive (like epoch seconds) to ensure
    // they can be serialized by any JSON library.
    oauth2User
        .getAttributes()
        .forEach(
            (key, value) -> {
              if (value instanceof Instant instant) {
                subjectBuilder.addProperty(key, instant.getEpochSecond());
              } else {
                subjectBuilder.addProperty(key, value);
              }
            });

    return subjectBuilder.build();
  }

  /**
   * Builds the resource for the authorization request.
   *
   * @param exchange ServerWebExchange
   * @return Resource instance
   * @throws IllegalArgumentException if the resource ID is missing in route metadata
   */
  private Resource buildResource(ServerWebExchange exchange) {
    final var route = (Route) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    final var metadata = route != null ? route.getMetadata() : null;
    if (metadata == null || metadata.isEmpty()) {
      final var reason =
          "Gateway route metadata is missing or empty, so cannot build resource of authorization request for AuthZEN.";
      log.error(reason);
      throw new IllegalArgumentException(reason);
    }

    final var resourceId = (String) metadata.get(this.authzenProperties.getResourceIdMetadataKey());
    if (resourceId == null || resourceId.isBlank()) {
      final var reason =
          String.format(
              "`%s` is missing or blank in gateway route metadata, so cannot build resource of authorization request for AuthZEN.",
              this.authzenProperties.getResourceIdMetadataKey());
      log.error(reason);
      throw new IllegalArgumentException(reason);
    }

    return new Resource.Builder()
        .type(this.authzenProperties.getResourceType())
        .id(resourceId)
        .build();
  }

  /**
   * Builds the action for the authorization request.
   *
   * @return Action instance
   */
  private Action buildAction() {
    return new Action.Builder().name(this.authzenProperties.getActionName()).build();
  }

  @Override
  public int getOrder() {
    // Run after Spring Security's authentication filters.
    return 1;
  }
}
