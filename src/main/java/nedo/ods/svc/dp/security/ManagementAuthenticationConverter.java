package nedo.ods.svc.dp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Authentication converter for management API. */
public class ManagementAuthenticationConverter implements ServerAuthenticationConverter {

  /** HTTP header name used to retrieve the API key. */
  private static final String API_KEY_HEADER = "X-API-KEY";

  /**
   * @see
   *     org.springframework.security.web.server.authentication.ServerAuthenticationConverter#convert(ServerWebExchange)
   */
  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER))
        .map(ApiKeyAuthenticationToken::new);
  }
}
