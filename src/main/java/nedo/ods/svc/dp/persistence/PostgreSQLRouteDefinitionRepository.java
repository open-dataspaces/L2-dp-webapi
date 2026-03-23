package nedo.ods.svc.dp.persistence;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import nedo.ods.svc.dp.gateway.logger.OdsLogger;
import java.util.List;

/**
 * PostgreSQL-backed implementation of Spring Cloud Gateway's RouteDefinitionRepository.
 *
 * <p>This implementation provides persistent storage for gateway routes using PostgreSQL database,
 * replacing the default in-memory storage. Key features include:
 *
 * <ul>
 *   <li><strong>Persistent Storage:</strong> Routes survive application restarts
 *   <li><strong>Dynamic Management:</strong> Routes can be added, updated, and deleted at runtime
 *   <li><strong>Reactive Support:</strong> Non-blocking operations using Project Reactor
 *   <li><strong>Automatic Refresh:</strong> Gateway routes are refreshed after database changes
 *   <li><strong>Error Resilience:</strong> Graceful handling of database and mapping errors
 * </ul>
 *
 * <p><strong>Activation:</strong> This repository is automatically activated when the property
 * {@code spring.cloud.gateway.route.repository.type} is set to {@code postgresql}. If the property
 * is missing, this repository is used as the default.
 *
 * <p><strong>Threading:</strong> Database operations are executed on the bounded elastic scheduler
 * to avoid blocking the main event loop, ensuring optimal performance in reactive applications.
 *
 * <p><strong>Route Lifecycle:</strong>
 *
 * <ul>
 *   <li>Routes are loaded on application startup and cached by Spring Cloud Gateway
 *   <li>Changes trigger {@link org.springframework.cloud.gateway.event.RefreshRoutesEvent}
 *   <li>Gateway automatically reloads routes from this repository after refresh events
 * </ul>
 *
 * @see org.springframework.cloud.gateway.route.RouteDefinitionRepository
 * @see nedo.ods.svc.dp.persistence.GatewayRoute
 * @see nedo.ods.svc.dp.persistence.RouteDefinitionMapper
 */
@Component
public class PostgreSQLRouteDefinitionRepository implements RouteDefinitionRepository {

  private static final OdsLogger logger =
      OdsLogger.getLogger(PostgreSQLRouteDefinitionRepository.class);

  private final GatewayRouteRepository repository;
  private final RouteDefinitionMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs a new PostgreSQL-backed route definition repository.
   *
   * @param repository JPA repository for database operations on GatewayRoute entities
   * @param mapper MapStruct mapper for converting between RouteDefinition and GatewayRoute
   * @param eventPublisher Spring event publisher for triggering route refresh events
   */
  public PostgreSQLRouteDefinitionRepository(
      GatewayRouteRepository repository,
      RouteDefinitionMapper mapper,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
    logger.info("PostgreSQL RouteDefinitionRepository initialized");
  }

  /** {@inheritDoc} */
  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {

    return Mono.fromCallable(
            () -> {
              List<GatewayRoute> routes = repository.findAllEnabledOrderByOrder();
              logger.info("Loaded " + routes.size() + " routes from PostgreSQL");
              return routes;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable)
        .map(
            entity -> {
              try {
                return mapper.toRouteDefinition(entity);
              } catch (Exception e) {
                logger.error(
                    "Error converting entity to RouteDefinition for route: " + entity.getId(), e);
                throw e;
              }
            })
        .onErrorResume(
            error -> {
              logger.error("Error loading routes from PostgreSQL", error);
              return Flux.empty();
            });
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> save(Mono<RouteDefinition> route) {
    return route.flatMap(
        routeDefinition -> {
          logger.info("Saving route: " + routeDefinition.getId());

          return Mono.fromCallable(
                  () -> {
                    GatewayRoute entity = mapper.toEntity(routeDefinition);

                    // Preserve creation time if updating existing route
                    if (repository.existsById(routeDefinition.getId())) {
                      repository
                          .findById(routeDefinition.getId())
                          .ifPresent(
                              existing -> {
                                entity.setCreatedAt(existing.getCreatedAt());
                              });
                    }

                    GatewayRoute saved = repository.save(entity);
                    logger.info("Saved route: " + saved.getId());

                    // Trigger route refresh
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));

                    return saved;
                  })
              .subscribeOn(Schedulers.boundedElastic())
              .then();
        });
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    return routeId.flatMap(
        id -> {
          logger.info("Deleting route: " + id);

          return Mono.fromCallable(
                  () -> {
                    repository.deleteById(id);
                    logger.info("Deleted route: " + id);

                    // Trigger route refresh
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));

                    return id;
                  })
              .subscribeOn(Schedulers.boundedElastic())
              .then();
        });
  }

  /**
   * Triggers route refresh when the application is fully started and ready.
   *
   * <p>This method is automatically called by Spring when the application context is fully
   * initialized and all beans are ready. It ensures that:
   *
   * <ul>
   *   <li>Routes are loaded from database immediately after startup
   *   <li>Gateway route cache is populated with persistent routes
   *   <li>Application is ready to handle requests with configured routes
   * </ul>
   *
   * <p>This is essential for ensuring that database-stored routes are available immediately when
   * the gateway starts accepting traffic.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    logger.info("Application ready - triggering route refresh");
    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
  }
}
