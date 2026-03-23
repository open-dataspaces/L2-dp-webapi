package nedo.ods.svc.dp.authzen.transport.http;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;
import nedo.ods.svc.dp.authzen.exception.TransportException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Production-ready HTTP client implementation for authorization service communication.
 *
 * <p>This class provides a robust implementation of {@link HttpTransport} using Java's built-in
 * {@link HttpClient} (available since Java 11). It includes comprehensive error handling, automatic
 * retry mechanisms with exponential backoff, and proper timeout management for reliable
 * communication with authorization policy decision points.
 *
 * <p><strong>Key Features:</strong>
 *
 * <ul>
 *   <li><strong>Built-in HTTP/2 support:</strong> Leverages Java's modern HttpClient
 *   <li><strong>Retry mechanism:</strong> Exponential backoff with jitter for resilience
 *   <li><strong>Timeout management:</strong> Separate connect and request timeouts
 *   <li><strong>API key authentication:</strong> Flexible header-based authentication
 *   <li><strong>Request tracking:</strong> UUID-based request correlation
 *   <li><strong>Comprehensive logging:</strong> Detailed operation logging for debugging
 * </ul>
 *
 * <p><strong>Retry Strategy:</strong>
 *
 * <ul>
 *   <li><strong>Server errors (5xx):</strong> Automatic retry with exponential backoff
 *   <li><strong>Network errors:</strong> IOException handling with retry logic
 *   <li><strong>Client errors (4xx):</strong> Immediate failure without retry
 *   <li><strong>Backoff algorithm:</strong> Base 500ms, max 30s, with random jitter
 * </ul>
 *
 * <p><strong>Configuration Examples:</strong>
 *
 * <pre>{@code
 * // Default configuration (10s timeouts, 3 retries)
 * SimpleHttpClient defaultClient = new SimpleHttpClient();
 *
 * // Custom timeout
 * SimpleHttpClient timeoutClient = new SimpleHttpClient(Duration.ofSeconds(5));
 *
 * // Custom retry count
 * SimpleHttpClient retryClient = new SimpleHttpClient(5);
 *
 * // Full customization
 * SimpleHttpClient customClient = new SimpleHttpClient(
 *     Duration.ofSeconds(5),   // connect timeout
 *     Duration.ofSeconds(30),  // request timeout
 *     5                        // max retries
 * );
 * }</pre>
 *
 * <p><strong>Usage with AuthzClientConfig:</strong>
 *
 * <pre>{@code
 * AuthzClientConfig config = new DefaultAuthzClientConfig.Builder()
 *     .endpoint("https://authz.example.com/authorize")
 *     .apiKey("sk-1234567890abcdef")
 *     .apiKeyHeader("X-API-Key")
 *     .build();
 *
 * SimpleHttpClient client = new SimpleHttpClient();
 * String jsonRequest = AuthorizationRequestSerializer.buildRequestJson(request);
 *
 * try {
 *     String response = client.request(config, jsonRequest);
 *     AuthorizationResponse authzResponse =
 *         AuthorizationResponseDeserializer.parseResponseJson(response);
 * } catch (AuthorizationException e) {
 *     // Handle authorization or transport errors
 * }
 * }</pre>
 *
 * <p>This implementation is thread-safe and can be safely shared across multiple threads. The
 * underlying HttpClient uses connection pooling and keep-alive connections for optimal performance.
 *
 * @see HttpTransport
 * @see AuthzClientConfig
 * @see java.net.http.HttpClient
 * @since 1.0
 */
public class SimpleHttpClient implements HttpTransport {
  /** Logger instance for this class. */
  private static final Logger logger = Logger.getLogger(SimpleHttpClient.class.getName());

  /** Default connection timeout duration (10 seconds). */
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default request timeout duration (10 seconds). */
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

  /** Default maximum number of retry attempts (3). */
  private static final int DEFAULT_MAX_RETRIES = 3;

  /** The underlying Java HttpClient instance for HTTP communication. */
  private final HttpClient client;

  /** Maximum number of retry attempts for failed requests. */
  private final int maxRetries;

  /** Timeout duration for individual HTTP requests. */
  private final Duration requestTimeout;

  /**
   * Creates a SimpleHttpClient with default configuration.
   *
   * <p>Uses default values:
   *
   * <ul>
   *   <li>Connect timeout: 10 seconds
   *   <li>Request timeout: 10 seconds
   *   <li>Max retries: 3 attempts
   * </ul>
   */
  public SimpleHttpClient() {
    this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_MAX_RETRIES);
  }

  /**
   * Creates a SimpleHttpClient with custom connection timeout.
   *
   * @param connectTimeout The timeout for establishing connections to the server
   */
  public SimpleHttpClient(Duration connectTimeout) {
    this(connectTimeout, DEFAULT_REQUEST_TIMEOUT, DEFAULT_MAX_RETRIES);
  }

  /**
   * Creates a SimpleHttpClient with custom maximum retry count.
   *
   * @param maxRetries The maximum number of retry attempts for failed requests
   */
  public SimpleHttpClient(int maxRetries) {
    this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, maxRetries);
  }

  /**
   * Creates a SimpleHttpClient with custom connection timeout and retry count.
   *
   * @param connectTimeout The timeout for establishing connections to the server
   * @param maxRetries The maximum number of retry attempts for failed requests
   */
  public SimpleHttpClient(Duration connectTimeout, int maxRetries) {
    this(connectTimeout, DEFAULT_REQUEST_TIMEOUT, maxRetries);
  }

  /**
   * Primary constructor for SimpleHttpClient with full configuration control.
   *
   * <p>This constructor allows complete customization of timeout and retry behavior. The underlying
   * HttpClient is configured with the specified connection timeout and uses HTTP/2 when available,
   * falling back to HTTP/1.1.
   *
   * <p><strong>Timeout Behavior:</strong>
   *
   * <ul>
   *   <li><strong>Connect timeout:</strong> Maximum time to establish a connection
   *   <li><strong>Request timeout:</strong> Maximum time for the entire request-response cycle
   * </ul>
   *
   * <p><strong>Retry Behavior:</strong>
   *
   * <ul>
   *   <li>Retries are performed for 5xx server errors and network failures
   *   <li>4xx client errors result in immediate failure
   *   <li>Exponential backoff with jitter prevents thundering herd
   * </ul>
   *
   * @param connectTimeout The timeout for establishing a connection (must be positive)
   * @param requestTimeout The timeout for the entire request-response exchange (must be positive)
   * @param maxRetries The maximum number of retries for transient failures (must be non-negative)
   */
  public SimpleHttpClient(Duration connectTimeout, Duration requestTimeout, int maxRetries) {
    this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    this.maxRetries = maxRetries;
    this.requestTimeout = requestTimeout;
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>Implementation Details:</strong>
   *
   * <ul>
   *   <li><strong>Request ID:</strong> Generates UUID for request correlation and logging
   *   <li><strong>Content-Type:</strong> Always set to "application/json"
   *   <li><strong>Authentication:</strong> Supports API key in Authorization or custom header
   *   <li><strong>Retry Logic:</strong> Exponential backoff for 5xx errors and network failures
   *   <li><strong>Error Handling:</strong> Immediate failure for 4xx client errors
   * </ul>
   *
   * <p><strong>HTTP Status Code Handling:</strong>
   *
   * <ul>
   *   <li><strong>2xx:</strong> Success - returns response body
   *   <li><strong>4xx:</strong> Client error - immediate AuthorizationException
   *   <li><strong>5xx:</strong> Server error - retry with exponential backoff
   * </ul>
   *
   * <p><strong>Retry Algorithm:</strong>
   *
   * <pre>{@code
   * backoff = min(30000ms, 500ms * 2^attempt)
   * sleepTime = random(0, backoff)  // jitter to prevent thundering herd
   * }</pre>
   */
  @Override
  public String request(AuthzClientConfig config, String jsonBody) throws AuthorizationException {
    String url = config.getEndpoint();
    if (url == null || url.isBlank()) {
      throw new AuthorizationException(
          "Invalid client configuration: Endpoint URL must be provided.",
          new TransportException("Endpoint URL is null or blank."));
    }

    String requestId = UUID.randomUUID().toString();

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("X-Request-ID", requestId)
            .timeout(this.requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

    config
        .getApiKey()
        .ifPresent(
            apiKey -> {
              String headerName = config.getApiKeyHeader().orElse("Authorization");
              String headerValue =
                  headerName.equalsIgnoreCase("Authorization") ? "Bearer " + apiKey : apiKey;
              requestBuilder.header(headerName, headerValue);
            });

    HttpRequest request = requestBuilder.build();

    for (int attempt = 0; ; attempt++) {
      try {
        logger.info(
            "Sending request (attempt "
                + (attempt + 1)
                + ") to: "
                + url
                + " with X-Request-ID: "
                + requestId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        logger.info("Received response with status code: " + statusCode);

        if (statusCode >= 200 && statusCode < 300) {
          return response.body();
        }

        // For client errors (4xx), fail immediately without retry.
        if (statusCode >= 400 && statusCode < 500) {
          throw new AuthorizationException(
              "HTTP request failed with status " + statusCode + ": " + response.body());
        }

        // For server errors (5xx), we will enter the retry logic below.
        if (statusCode >= 500 && statusCode < 600) {
          if (attempt >= maxRetries) {
            throw new AuthorizationException(
                "Request failed after "
                    + (attempt + 1)
                    + " attempts with server error: "
                    + statusCode);
          }
          logger.warning("Server error on attempt " + (attempt + 1) + ". Retrying...");
        } else {
          // For other unexpected status codes
          throw new AuthorizationException(
              "HTTP request failed with unexpected status " + statusCode + ": " + response.body());
        }

      } catch (IOException e) { // Retryable network error
        if (attempt >= maxRetries) {
          throw new AuthorizationException(
              "Request failed after " + (attempt + 1) + " attempts due to a network error.",
              new TransportException("Network error.", e));
        }
        logger.warning(
            "Network error on attempt " + (attempt + 1) + ". Retrying... Error: " + e.getMessage());
      } catch (InterruptedException e) {
        // Not retryable. Propagate interruption.
        Thread.currentThread().interrupt();
        throw new AuthorizationException(
            "Request was interrupted.",
            new TransportException("Request thread was interrupted.", e));
      }

      // If we reach here, we are retrying. Perform backoff.
      try {
        long baseBackoff = 500; // 500ms
        long maxBackoff = 30000; // 30s
        long currentCeiling = (long) (baseBackoff * Math.pow(2, attempt));
        long backoff = Math.min(maxBackoff, currentCeiling);
        long sleepTime = (long) (Math.random() * backoff);
        logger.info("Retrying in " + sleepTime + " ms...");
        Thread.sleep(sleepTime);
      } catch (InterruptedException ie) {
        logger.warning("Retry loop interrupted.");
        Thread.currentThread().interrupt();
        throw new AuthorizationException(
            "Request was interrupted during retry backoff.",
            new TransportException("Retry backoff was interrupted.", ie));
      }
    }
  }
}
