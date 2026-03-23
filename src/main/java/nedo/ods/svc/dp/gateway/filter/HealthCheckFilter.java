package nedo.ods.svc.dp.gateway.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * A filter that immediately returns HTTP 200 and short-circuits processing
 * only when the request path exactly matches {@code /health}.
 *
 * <p>No security processing (such as API keys, JWT validation, etc.)
 * is applied for this endpoint.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+1) 
public class HealthCheckFilter implements WebFilter {

    private static final String HEALTH_PATH = "/health";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (HEALTH_PATH.equals(path)) {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            byte[] bytes = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            var buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }
}