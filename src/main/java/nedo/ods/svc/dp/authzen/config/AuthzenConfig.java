package nedo.ods.svc.dp.authzen.config;

import nedo.ods.svc.dp.authzen.api.AuthzClient;
import nedo.ods.svc.dp.authzen.transport.http.SimpleHttpClient;
import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;
import nedo.ods.svc.dp.gateway.filter.AuthzenAuthorizationGlobalFilter;

import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for AuthZEN. Activated only when 'apigateway.authzen.enabled' is set to true.
 */
@Configuration
@ConditionalOnProperty(name = "apigateway.authzen.enabled", havingValue = "true")
public class AuthzenConfig {

  /** Logger for logging configuration details. */
  private static final OdsLogger log = OdsLogger.getLogger(AuthzenConfig.class);

  /**
   * Configures and creates an AuthZEN client. Retrieves settings from ApiGatewayProperties and sets
   * PDP endpoint and API key.
   *
   * @param properties API Gateway configuration properties
   * @return AuthzClient instance
   */
  @Bean
  public AuthzClient authzClient(ApiGatewayProperties properties) {
    ApiGatewayProperties.Authzen authzenProps = properties.getAuthzen();
    log.info("Configuring AuthZEN client for PDP endpoint: " + authzenProps.getPdpEndpoint());
    DefaultAuthzClientConfig config =
        DefaultAuthzClientConfig.builder()
            .endpoint(authzenProps.getPdpEndpoint())
            .apiKey(authzenProps.getApiKey())
            .apiKeyHeader(authzenProps.getApiKeyHeader())
            .build();
    return new AuthzClient(config, new SimpleHttpClient());
  }

  /**
   * Creates the authorization global filter.
   *
   * @param authzClient AuthZEN client
   * @param properties API Gateway configuration properties
   * @return Authorization global filter
   */
  @Bean
  public AuthzenAuthorizationGlobalFilter authzenAuthorizationGlobalFilter(
      AuthzClient authzClient, ApiGatewayProperties properties) {
    return new AuthzenAuthorizationGlobalFilter(authzClient, properties.getAuthzen());
  }
}
