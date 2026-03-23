package nedo.ods.svc.dp.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import reactor.core.publisher.Mono;

/** Authentication manager for the management API. */
@Component
public class ManagementAuthenticationManager implements ReactiveAuthenticationManager {

  /** Logger for logging configuration details. */
  private static final OdsLogger log = OdsLogger.getLogger(ManagementAuthenticationManager.class);

  /** Settings information for the management API key. */
  private final ApiGatewayProperties gatewayProperties;

  /**
   * Constructor.
   *
   * @param gatewayProperties API Gateway configuration information
   */
  public ManagementAuthenticationManager(ApiGatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  /**
   * @see
   *     org.springframework.security.authentication.ReactiveAuthenticationManager#authenticate(Authentication)
   */
  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    if (!(authentication instanceof ApiKeyAuthenticationToken token)) {
      log.trace(
          "Authentication token is not an instance of ApiKeyAuthenticationToken, skipping management authentication.");
      return Mono.empty();
    }

    String providedKey = token.getApiKey();
    String configuredKey = gatewayProperties.getManagementApiKey();

    if (configuredKey != null && configuredKey.equals(providedKey)) {
      if (log.isDebugEnabled()) {
        log.debug("Management API key authentication successful.");
      }
      return Mono.just(new ApiKeyAuthenticationToken(providedKey, true));
    }
    log.warn("Management API key authentication failed: Invalid API Key provided.");
    return Mono.error(new BadCredentialsException("Invalid API Key"));
  }
}
