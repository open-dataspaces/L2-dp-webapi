package nedo.ods.svc.dp.authzen.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import nedo.ods.svc.dp.authzen.model.Action;
import nedo.ods.svc.dp.authzen.model.Context;
import nedo.ods.svc.dp.authzen.model.Resource;
import nedo.ods.svc.dp.authzen.model.Subject;

/**
 * Represents a single authorization request, containing the subject, resource, action, and optional
 * context. This class is immutable. A builder is provided for convenient construction.
 *
 * <p>An authorization request contains the core elements needed to evaluate access permissions:
 *
 * <ul>
 *   <li><strong>Subject:</strong> The entity requesting access (e.g., user, service)
 *   <li><strong>Resource:</strong> The target resource being accessed
 *   <li><strong>Action:</strong> The operation being performed on the resource
 *   <li><strong>Context:</strong> Additional contextual information for the request (optional)
 * </ul>
 *
 * <p>This class follows the immutable object pattern and provides a fluent builder interface for
 * construction. Context merging is supported to combine multiple contextual information sources
 * into a single authorization request.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * AuthorizationRequest request = new AuthorizationRequest.Builder()
 *     .subject(new Subject("user123"))
 *     .resource(new Resource("document456"))
 *     .action(new Action("read"))
 *     .context(new Context(Map.of("department", "engineering")))
 *     .build();
 * }</pre>
 *
 * @see Subject
 * @see Resource
 * @see Action
 * @see Context
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationRequest {

  /** The subject requesting access to the resource. */
  private final Subject subject;

  /** The target resource being accessed. */
  private final Resource resource;

  /** The action being performed on the resource. */
  private final Action action;

  /** Optional contextual information for the authorization request. */
  private final Context context;

  /**
   * Private constructor for creating AuthorizationRequest instances.
   *
   * @param builder The builder containing the request components
   */
  private AuthorizationRequest(Builder builder) {
    this.subject = builder.subject;
    this.resource = builder.resource;
    this.action = builder.action;
    this.context = builder.context;
  }

  /**
   * Builder class for constructing AuthorizationRequest instances.
   *
   * <p>This builder follows the fluent interface pattern, allowing method chaining for convenient
   * construction of authorization requests. The builder enforces that required fields (subject,
   * resource, action) are provided before building.
   *
   * <p><strong>Usage example:</strong>
   *
   * <pre>{@code
   * AuthorizationRequest request = new AuthorizationRequest.Builder()
   *     .subject(subject)
   *     .resource(resource)
   *     .action(action)
   *     .context(context)  // Optional
   *     .build();
   * }</pre>
   */
  public static class Builder {
    /** The subject for the authorization request. */
    private Subject subject;

    /** The resource for the authorization request. */
    private Resource resource;

    /** The action for the authorization request. */
    private Action action;

    /** The optional context for the authorization request. */
    private Context context;

    /**
     * Sets the subject for the authorization request.
     *
     * @param subject The entity requesting access (required)
     * @return This builder instance for method chaining
     */
    public Builder subject(Subject subject) {
      this.subject = subject;
      return this;
    }

    /**
     * Sets the resource for the authorization request.
     *
     * @param resource The target resource being accessed (required)
     * @return This builder instance for method chaining
     */
    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    /**
     * Sets the action for the authorization request.
     *
     * @param action The operation being performed on the resource (required)
     * @return This builder instance for method chaining
     */
    public Builder action(Action action) {
      this.action = action;
      return this;
    }

    /**
     * Sets the context for the authorization request.
     *
     * @param context Additional contextual information (optional)
     * @return This builder instance for method chaining
     */
    public Builder context(Context context) {
      this.context = context;
      return this;
    }

    /**
     * Builds the AuthorizationRequest instance.
     *
     * <p>Validates that all required fields (subject, resource, action) are provided before
     * creating the immutable AuthorizationRequest instance.
     *
     * @return A new immutable AuthorizationRequest instance
     * @throws IllegalArgumentException if any required field is missing
     */
    public AuthorizationRequest build() {
      if (subject == null) {
        throw new IllegalArgumentException("Subject must be provided.");
      }

      if (resource == null) {
        throw new IllegalArgumentException("Resource must be provided.");
      }

      if (action == null) {
        throw new IllegalArgumentException("Action must be provided.");
      }
      return new AuthorizationRequest(this);
    }
  }

  /**
   * Returns the subject of this authorization request.
   *
   * @return The entity requesting access
   */
  public Subject getSubject() {
    return subject;
  }

  /**
   * Returns the resource of this authorization request.
   *
   * @return The target resource being accessed
   */
  public Resource getResource() {
    return resource;
  }

  /**
   * Returns the action of this authorization request.
   *
   * @return The operation being performed on the resource
   */
  public Action getAction() {
    return action;
  }

  /**
   * Returns the context of this authorization request.
   *
   * @return The contextual information, or {@code null} if not provided
   */
  public Context getContext() {
    return context;
  }

  /**
   * Creates a new AuthorizationRequest by merging this request's context with another.
   *
   * <p>This method provides a convenient way to combine contextual information from multiple
   * sources. The merging behavior is as follows:
   *
   * <ul>
   *   <li>If the provided context is {@code null} or empty, returns this instance unchanged
   *   <li>If this request has no context, the new request's context will be the provided one
   *   <li>If both contexts exist, they are merged according to {@link Context#merge(Context)}
   * </ul>
   *
   * <p>Since AuthorizationRequest is immutable, this method returns a new instance rather than
   * modifying the existing one.
   *
   * @param otherContext The context to merge with this request's context
   * @return A new AuthorizationRequest with the merged context, or this instance if no merge is
   *     needed
   * @see Context#merge(Context)
   */
  public AuthorizationRequest withMergedContext(Context otherContext) {
    if (otherContext == null || otherContext.getAttributes().isEmpty()) {
      return this; // No changes needed
    }

    Context newContext = (this.context == null) ? otherContext : this.context.merge(otherContext);

    // Use the existing builder to create a new, immutable request instance
    return new AuthorizationRequest.Builder()
        .subject(this.subject)
        .resource(this.resource)
        .action(this.action)
        .context(newContext)
        .build();
  }
}
