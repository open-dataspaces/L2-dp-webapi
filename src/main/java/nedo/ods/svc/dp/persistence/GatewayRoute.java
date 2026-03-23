package nedo.ods.svc.dp.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a Spring Cloud Gateway route definition for database persistence.
 *
 * <p>This entity stores route configurations in PostgreSQL, enabling dynamic route management and
 * persistence across application restarts. Each route contains:
 *
 * <ul>
 *   <li><strong>Basic Properties:</strong> id, uri, order, enabled status
 *   <li><strong>Complex Properties:</strong> predicates, filters, metadata (stored as JSON)
 *   <li><strong>Audit Fields:</strong> createdAt, updatedAt for tracking changes
 * </ul>
 *
 * <p>The complex properties (predicates, filters, metadata) are serialized to JSON for database
 * storage and deserialized back to Java objects when loading routes into Spring Cloud Gateway.
 *
 * <p>Lifecycle management is handled automatically through JPA callbacks: {@link #onCreate()} sets
 * timestamps on creation, {@link #onUpdate()} updates the modification timestamp.
 *
 * @see nedo.ods.svc.dp.persistence.converter.RouteDefinitionMapper
 * @see org.springframework.cloud.gateway.route.RouteDefinition
 */
@Entity
@Table(name = "gateway_routes")
public class GatewayRoute {

  @Id private String id;

  @Column(nullable = false, length = 500)
  private String uri;

  @Column(columnDefinition = "TEXT")
  private String predicates;

  @Column(columnDefinition = "TEXT")
  private String filters;

  @Column(columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "route_order")
  private Integer order;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Default constructor required by JPA. Creates an empty GatewayRoute instance. */
  public GatewayRoute() {}

  /**
   * Constructor for creating a new GatewayRoute with basic properties.
   *
   * <p>Automatically sets:
   *
   * <ul>
   *   <li>enabled = true
   *   <li>createdAt = current timestamp
   *   <li>updatedAt = current timestamp
   * </ul>
   *
   * @param id unique identifier for the route
   * @param uri target URI where requests will be routed
   */
  public GatewayRoute(String id, String uri) {
    this.id = id;
    this.uri = uri;
    this.enabled = true;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Gets the unique identifier for this route.
   *
   * @return the route ID
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique identifier for this route.
   *
   * @param id the route ID to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the target URI where requests matching this route will be forwarded.
   *
   * @return the target URI as a string
   */
  public String getUri() {
    return uri;
  }

  /**
   * Sets the target URI where requests matching this route will be forwarded.
   *
   * @param uri the target URI to set
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * Gets the route predicates as a JSON string.
   *
   * <p>Predicates determine which requests match this route. Stored as JSON serialized from
   * List&lt;PredicateDefinition&gt;.
   *
   * @return JSON string representing the predicates, may be null
   */
  public String getPredicates() {
    return predicates;
  }

  /**
   * Sets the route predicates as a JSON string.
   *
   * @param predicates JSON string representing the predicates
   */
  public void setPredicates(String predicates) {
    this.predicates = predicates;
  }

  /**
   * Gets the route filters as a JSON string.
   *
   * <p>Filters modify requests and responses as they pass through the gateway. Stored as JSON
   * serialized from List&lt;FilterDefinition&gt;.
   *
   * @return JSON string representing the filters, may be null
   */
  public String getFilters() {
    return filters;
  }

  /**
   * Sets the route filters as a JSON string.
   *
   * @param filters JSON string representing the filters
   */
  public void setFilters(String filters) {
    this.filters = filters;
  }

  /**
   * Gets the route metadata as a JSON string.
   *
   * <p>Metadata provides additional information about the route. Stored as JSON serialized from
   * Map&lt;String, Object&gt;.
   *
   * @return JSON string representing the metadata, may be null
   */
  public String getMetadata() {
    return metadata;
  }

  /**
   * Sets the route metadata as a JSON string.
   *
   * @param metadata JSON string representing the metadata
   */
  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets the route order for prioritization.
   *
   * <p>Lower values have higher priority. Routes are evaluated in order of priority.
   *
   * @return the route order, may be null
   */
  public Integer getOrder() {
    return order;
  }

  /**
   * Sets the route order for prioritization.
   *
   * @param order the route order to set
   */
  public void setOrder(Integer order) {
    this.order = order;
  }

  /**
   * Gets whether this route is enabled.
   *
   * <p>Disabled routes are ignored by the gateway.
   *
   * @return true if the route is enabled, false otherwise
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether this route is enabled.
   *
   * @param enabled true to enable the route, false to disable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the timestamp when this route was created.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the timestamp when this route was created.
   *
   * @param createdAt the creation timestamp to set
   */
  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Gets the timestamp when this route was last updated.
   *
   * @return the last update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets the timestamp when this route was last updated.
   *
   * @param updatedAt the update timestamp to set
   */
  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * JPA lifecycle callback executed before persisting a new entity.
   *
   * <p>Automatically sets both createdAt and updatedAt to the current timestamp. This method is
   * called automatically by JPA and should not be invoked manually.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  /**
   * JPA lifecycle callback executed before updating an existing entity.
   *
   * <p>Automatically updates the updatedAt timestamp to the current time. The createdAt timestamp
   * remains unchanged. This method is called automatically by JPA and should not be invoked
   * manually.
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
