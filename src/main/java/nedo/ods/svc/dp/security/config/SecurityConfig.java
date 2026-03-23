package nedo.ods.svc.dp.security.config;

import nedo.ods.svc.dp.gateway.filter.HeaderValidationFilter;
import nedo.ods.svc.dp.security.ManagementAuthenticationConverter;
import nedo.ods.svc.dp.security.ManagementAuthenticationManager;
import nedo.ods.svc.dp.security.oauth2.OidcAuthenticationConverter;
import nedo.ods.svc.dp.security.server.CustomAuthenticationEntryPoint;

import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

/**
 * Configuration class for WebFlux security. Sets up separate security filter chains for the
 * management port (9090) and the gateway port (8080).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  /** Logger for logging configuration details. */
  private static final OdsLogger log = OdsLogger.getLogger(SecurityConfig.class);

  /** Authentication manager for the management port. */
  private final ManagementAuthenticationManager managementAuthenticationManager;

  /** Authentication convert for OIDC user info. */
  private final OidcAuthenticationConverter oidcAuthenticationConverter;

  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  private final SecurityProperties securityProperties;

  /**
   * Constructor for SecurityConfig. Injects required dependency components.
   *
   * @param managementAuthenticationManager Authentication manager for the management port
   * @param oidcAuthenticationConverter OIDC user info authentication converter
   * @param authenticationEntryPoint Custom authentication entry point
   * @param securityProperties Security configuration properties
   * @param routeDefinitionLocator Route definition locator for filter creation
   */
  public SecurityConfig(
      ManagementAuthenticationManager managementAuthenticationManager,
      OidcAuthenticationConverter oidcAuthenticationConverter,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      SecurityProperties securityProperties,
      RouteDefinitionLocator routeDefinitionLocator) {
    this.managementAuthenticationManager = managementAuthenticationManager;
    this.oidcAuthenticationConverter = oidcAuthenticationConverter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.securityProperties = securityProperties;
    // this.routeDefinitionLocator = routeDefinitionLocator;
  }

  /**
   * Security configuration for the management endpoints. Secures management paths with an API key
   * via the X-API-KEY header.
   *
   * @param http ServerHttpSecurity
   * @return SecurityWebFilterChain for management endpoints
   */
  @Bean
  @Order(1)
  public SecurityWebFilterChain managementFilterChain(ServerHttpSecurity http) {
    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(managementAuthenticationManager);
    authenticationWebFilter.setServerAuthenticationConverter(
        new ManagementAuthenticationConverter());

    ServerWebExchangeMatcher managementMatcher =
        exchange -> {
          String path = exchange.getRequest().getPath().value();
          String managementBasePath = securityProperties.getManagementBasePath();
          // Apply management security to management paths
          if (path.startsWith(managementBasePath)) {
            return ServerWebExchangeMatcher.MatchResult.match();
          }
          return ServerWebExchangeMatcher.MatchResult.notMatch();
        };

    log.info(
        "Configuring Management security filter chain for "
            + securityProperties.getManagementBasePath()
            + " paths");
    return http.securityMatcher(managementMatcher)
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
        .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .build();
  }

  /**
   * Security configuration for the main gateway routes. Secures all routes by validating a JWT
   * Bearer Token (OIDC Resource Server). Excludes management paths.
   *
   * @param http ServerHttpSecurity
   * @return SecurityWebFilterChain for gateway routes
   */
  @Bean
  @Order(2)
  public SecurityWebFilterChain gatewayFilterChain(ServerHttpSecurity http) {
    ServerWebExchangeMatcher gatewayMatcher =
        exchange -> {
          String path = exchange.getRequest().getPath().value();
          String managementBasePath = securityProperties.getManagementBasePath();
          // Apply gateway security to all paths except management paths
          if (path.startsWith(managementBasePath)) {
            return ServerWebExchangeMatcher.MatchResult.notMatch();
          }
          return ServerWebExchangeMatcher.MatchResult.match();
        };

    log.info(
        "Configuring Gateway security filter chain for non-"
            + securityProperties.getManagementBasePath()
            + " paths");

    HeaderValidationFilter headerValidationFilter = new HeaderValidationFilter(securityProperties);

    return http.securityMatcher(gatewayMatcher)
        .csrf(ServerHttpSecurity.CsrfSpec::disable) // Common for API gateways
        .addFilterBefore(headerValidationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(oidcAuthenticationConverter))
                    .authenticationEntryPoint(authenticationEntryPoint))
        .build();
  }
}
