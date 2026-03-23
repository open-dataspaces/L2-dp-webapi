package nedo.ods.svc.dp.authzen.transport.http;

import nedo.ods.svc.dp.authzen.transport.Transport;

/**
 * Marker interface for HTTP-based transport implementations in the authorization framework.
 *
 * <p>This interface extends the base {@link Transport} interface to specifically identify transport
 * implementations that use HTTP/HTTPS protocols for communication with authorization services. It
 * serves as a type marker to distinguish HTTP transports from other potential transport mechanisms
 * (such as gRPC, message queues, or file-based transports).
 *
 * <p><strong>Purpose and Design:</strong>
 *
 * <ul>
 *   <li><strong>Type safety:</strong> Enables compile-time identification of HTTP transports
 *   <li><strong>Dependency injection:</strong> Allows specific injection of HTTP-based
 *       implementations
 *   <li><strong>Configuration:</strong> Facilitates transport-specific configuration and behavior
 *   <li><strong>Extensibility:</strong> Provides a foundation for HTTP-specific transport features
 * </ul>
 *
 * <p><strong>HTTP Transport Characteristics:</strong>
 *
 * <ul>
 *   <li><strong>Protocol support:</strong> HTTP/1.1, HTTP/2, and HTTPS with TLS
 *   <li><strong>Content types:</strong> Typically JSON for authorization requests/responses
 *   <li><strong>Authentication:</strong> Support for various HTTP authentication schemes
 *   <li><strong>Error handling:</strong> HTTP status codes and response body error details
 *   <li><strong>Connection management:</strong> Connection pooling and keep-alive support
 * </ul>
 *
 * <p><strong>Implementation Examples:</strong>
 *
 * <pre>{@code
 * // HTTP client implementation
 * public class RestTemplateHttpTransport implements HttpTransport {
 *     private final RestTemplate restTemplate;
 *
 *     @Override
 *     public AuthorizationResponse send(AuthorizationRequest request, String endpoint) {
 *         // Implementation using RestTemplate for HTTP communication
 *     }
 * }
 *
 * // WebClient implementation
 * public class WebClientHttpTransport implements HttpTransport {
 *     private final WebClient webClient;
 *
 *     @Override
 *     public AuthorizationResponse send(AuthorizationRequest request, String endpoint) {
 *         // Implementation using WebClient for reactive HTTP communication
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Configuration and Usage:</strong>
 *
 * <pre>{@code
 * // Configuration class
 * @Configuration
 * public class TransportConfig {
 *
 *     @Bean
 *     @ConditionalOnProperty(name = "transport.type", havingValue = "http")
 *     public HttpTransport httpTransport() {
 *         return new RestTemplateHttpTransport(restTemplate());
 *     }
 * }
 *
 * // Service usage
 * @Service
 * public class AuthorizationService {
 *
 *     @Autowired
 *     @Qualifier("httpTransport")
 *     private HttpTransport transport;
 *
 *     public boolean authorize(AuthorizationRequest request) {
 *         AuthorizationResponse response = transport.send(request, authzEndpoint);
 *         return response.getDecision() == Decision.PERMIT;
 *     }
 * }
 * }</pre>
 *
 * <p>As a marker interface, this interface does not define additional methods beyond those
 * inherited from {@link Transport}. Its primary value lies in type identification and enabling
 * framework-level features specific to HTTP-based authorization communication.
 *
 * @see Transport
 * @see nedo.ods.svc.dp.authzen.api.AuthorizationRequest
 * @see nedo.ods.svc.dp.authzen.api.AuthorizationResponse
 * @since 1.0
 */
public interface HttpTransport extends Transport {}
