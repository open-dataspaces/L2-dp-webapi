package nedo.ods.svc.dp.authzen.context;

import nedo.ods.svc.dp.authzen.model.Context;

/**
 * Factory interface for creating dynamic context information for authorization requests.
 *
 * <p>This interface defines a contract for generating contextual information that can be
 * dynamically added to authorization requests. Context factories enable the enrichment of
 * authorization requests with runtime information such as:
 *
 * <ul>
 *   <li><strong>Temporal information:</strong> Current timestamp, time of day, timezone
 *   <li><strong>Environmental data:</strong> System load, resource availability, maintenance
 *       windows
 *   <li><strong>Request metadata:</strong> Source IP, user agent, request ID, session information
 *   <li><strong>Security context:</strong> Risk scores, authentication method, previous access
 *       patterns
 *   <li><strong>Business context:</strong> Department, cost center, project codes, compliance
 *       requirements
 * </ul>
 *
 * <p>Context factories are particularly useful in scenarios where authorization decisions depend on
 * dynamic conditions that cannot be predetermined at request creation time. The {@link
 * nedo.ods.svc.dp.authzen.api.AuthzClient} can use these factories to automatically enrich
 * authorization requests before sending them to the Policy Decision Point.
 *
 * <p><strong>Implementation considerations:</strong>
 *
 * <ul>
 *   <li><strong>Performance:</strong> Should execute quickly as it's called for each authorization
 *       request
 *   <li><strong>Thread Safety:</strong> Must be thread-safe if used in concurrent environments
 *   <li><strong>Error Handling:</strong> Should handle failures gracefully, potentially returning
 *       empty context
 *   <li><strong>Caching:</strong> Consider caching expensive operations when appropriate
 * </ul>
 *
 * <p><strong>Example implementations:</strong>
 *
 * <pre>{@code
 * // Simple timestamp context factory
 * public class TimestampContextFactory implements ContextFactory {
 *     @Override
 *     public Context createContext() {
 *         Map<String, Object> attributes = new HashMap<>();
 *         attributes.put("current_time", Instant.now().toString());
 *         attributes.put("timezone", ZoneId.systemDefault().toString());
 *         return new Context(attributes);
 *     }
 * }
 *
 * // Request metadata context factory
 * public class RequestContextFactory implements ContextFactory {
 *     private final HttpServletRequest request;
 *
 *     public RequestContextFactory(HttpServletRequest request) {
 *         this.request = request;
 *     }
 *
 *     @Override
 *     public Context createContext() {
 *         Map<String, Object> attributes = new HashMap<>();
 *         attributes.put("source_ip", request.getRemoteAddr());
 *         attributes.put("user_agent", request.getHeader("User-Agent"));
 *         attributes.put("request_id", request.getHeader("X-Request-ID"));
 *         return new Context(attributes);
 *     }
 * }
 * }</pre>
 *
 * @see Context
 * @see nedo.ods.svc.dp.authzen.api.AuthzClient
 * @see nedo.ods.svc.dp.authzen.api.AuthorizationRequest#withMergedContext(Context)
 */
public interface ContextFactory {

  /**
   * Creates a new context instance with dynamic information.
   *
   * <p>This method is called by the authorization client to generate contextual information that
   * will be merged with the authorization request. The returned context should contain relevant
   * attributes that may influence the authorization decision.
   *
   * <p>Implementations should:
   *
   * <ul>
   *   <li>Execute quickly to avoid impacting authorization performance
   *   <li>Handle any internal errors gracefully
   *   <li>Return a valid Context object (never null)
   *   <li>Be thread-safe for concurrent usage
   * </ul>
   *
   * <p>If no context information is available or an error occurs during context creation,
   * implementations may return an empty context rather than null.
   *
   * @return A Context instance containing dynamic attributes for the authorization request
   */
  Context createContext();
}
