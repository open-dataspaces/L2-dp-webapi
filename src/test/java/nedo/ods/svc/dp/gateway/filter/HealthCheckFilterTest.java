package nedo.ods.svc.dp.gateway.filter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import reactor.core.publisher.Mono;

import java.util.List;

class HealthCheckFilterTest {

    private WebTestClient createClient(WebFilter filter) {
        WebHandler fallback = exchange -> {
            exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return Mono.empty();
        };

        // Build a FilteringWebHandler with a non-null filter list
        FilteringWebHandler webHandler = new FilteringWebHandler(fallback, List.of(filter));

        // Bind WebTestClient to the WebHandler directly (no HttpHandler API)
        return WebTestClient.bindToWebHandler(webHandler).build();
    }

    @Test
    void healthPathReturns200Json() {
        WebTestClient client = createClient(new HealthCheckFilter());

        client.get()
              .uri("/health")
              .exchange()
              .expectStatus().isOk()
              .expectHeader().contentType(MediaType.APPLICATION_JSON)
              .expectBody()
              .json("{\"status\":\"UP\"}");
    }

    @Test
    void nonHealthPathBypassesFilter() {
        WebTestClient client = createClient(new HealthCheckFilter());

        client.get()
              .uri("/other")
              .exchange()
              .expectStatus().isNoContent();
    }
}