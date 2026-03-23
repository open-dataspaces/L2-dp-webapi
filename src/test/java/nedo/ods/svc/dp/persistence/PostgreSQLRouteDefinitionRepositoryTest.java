package nedo.ods.svc.dp.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for PostgreSQLRouteDefinitionRepository. */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQLRouteDefinitionRepository Tests")
class PostgreSQLRouteDefinitionRepositoryTest {

  @Mock private GatewayRouteRepository repository;

  @Mock private RouteDefinitionMapper mapper;

  @Mock private ApplicationEventPublisher eventPublisher;

  private PostgreSQLRouteDefinitionRepository routeDefinitionRepository;

  @BeforeEach
  void setUp() {
    routeDefinitionRepository =
        new PostgreSQLRouteDefinitionRepository(repository, mapper, eventPublisher);
  }

  @Test
  @DisplayName("Should get route definitions successfully")
  void shouldGetRouteDefinitionsSuccessfully() throws Exception {
    // Given
    GatewayRoute entity1 = createTestGatewayRoute("route-1", "http://service1.com");
    GatewayRoute entity2 = createTestGatewayRoute("route-2", "http://service2.com");
    List<GatewayRoute> entities = Arrays.asList(entity1, entity2);

    RouteDefinition route1 = createTestRouteDefinition("route-1", "http://service1.com");
    RouteDefinition route2 = createTestRouteDefinition("route-2", "http://service2.com");

    when(repository.findAllEnabledOrderByOrder()).thenReturn(entities);
    when(mapper.toRouteDefinition(entity1)).thenReturn(route1);
    when(mapper.toRouteDefinition(entity2)).thenReturn(route2);

    // When
    Flux<RouteDefinition> result = routeDefinitionRepository.getRouteDefinitions();

    // Then
    StepVerifier.create(result).expectNext(route1).expectNext(route2).verifyComplete();

    verify(repository).findAllEnabledOrderByOrder();
    verify(mapper).toRouteDefinition(entity1);
    verify(mapper).toRouteDefinition(entity2);
  }

  @Test
  @DisplayName("Should handle empty route list")
  void shouldHandleEmptyRouteList() {
    // Given
    when(repository.findAllEnabledOrderByOrder()).thenReturn(Arrays.asList());

    // When
    Flux<RouteDefinition> result = routeDefinitionRepository.getRouteDefinitions();

    // Then
    StepVerifier.create(result).verifyComplete();

    verify(repository).findAllEnabledOrderByOrder();
    verifyNoInteractions(mapper);
  }

  @Test
  @DisplayName("Should handle mapping error gracefully")
  void shouldHandleMappingErrorGracefully() {
    // Given
    GatewayRoute entity = createTestGatewayRoute("route-1", "http://service1.com");
    List<GatewayRoute> entities = Arrays.asList(entity);

    when(repository.findAllEnabledOrderByOrder()).thenReturn(entities);
    when(mapper.toRouteDefinition(entity)).thenThrow(new RuntimeException("Mapping error"));

    // When
    Flux<RouteDefinition> result = routeDefinitionRepository.getRouteDefinitions();

    // Then
    StepVerifier.create(result).verifyComplete(); // Should complete empty due to error handling

    verify(repository).findAllEnabledOrderByOrder();
    verify(mapper).toRouteDefinition(entity);
  }

  @Test
  @DisplayName("Should save new route successfully")
  void shouldSaveNewRouteSuccessfully() throws Exception {
    // Given
    RouteDefinition routeDefinition =
        createTestRouteDefinition("new-route", "http://newservice.com");
    GatewayRoute entity = createTestGatewayRoute("new-route", "http://newservice.com");
    GatewayRoute savedEntity = createTestGatewayRoute("new-route", "http://newservice.com");

    when(mapper.toEntity(routeDefinition)).thenReturn(entity);
    when(repository.existsById("new-route")).thenReturn(false);
    when(repository.save(entity)).thenReturn(savedEntity);

    // When
    Mono<Void> result = routeDefinitionRepository.save(Mono.just(routeDefinition));

    // Then
    StepVerifier.create(result).verifyComplete();

    verify(mapper).toEntity(routeDefinition);
    verify(repository).existsById("new-route");
    verify(repository).save(entity);
    verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
  }

  @Test
  @DisplayName("Should update existing route preserving creation time")
  void shouldUpdateExistingRoutePreservingCreationTime() throws Exception {
    // Given
    RouteDefinition routeDefinition =
        createTestRouteDefinition("existing-route", "http://updatedservice.com");
    GatewayRoute newEntity = createTestGatewayRoute("existing-route", "http://updatedservice.com");
    GatewayRoute existingEntity = createTestGatewayRoute("existing-route", "http://oldservice.com");
    existingEntity.setCreatedAt(LocalDateTime.now().minusHours(1));

    when(mapper.toEntity(routeDefinition)).thenReturn(newEntity);
    when(repository.existsById("existing-route")).thenReturn(true);
    when(repository.findById("existing-route")).thenReturn(Optional.of(existingEntity));
    when(repository.save(newEntity)).thenReturn(newEntity);

    // When
    Mono<Void> result = routeDefinitionRepository.save(Mono.just(routeDefinition));

    // Then
    StepVerifier.create(result).verifyComplete();

    verify(mapper).toEntity(routeDefinition);
    verify(repository).existsById("existing-route");
    verify(repository).findById("existing-route");
    verify(repository).save(newEntity);
    verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
  }

  @Test
  @DisplayName("Should delete route successfully")
  void shouldDeleteRouteSuccessfully() {
    // Given
    String routeId = "route-to-delete";

    // When
    Mono<Void> result = routeDefinitionRepository.delete(Mono.just(routeId));

    // Then
    StepVerifier.create(result).verifyComplete();

    verify(repository).deleteById(routeId);
    verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
  }

  @Test
  @DisplayName("Should trigger route refresh on application ready")
  void shouldTriggerRouteRefreshOnApplicationReady() {
    // When
    routeDefinitionRepository.onApplicationReady();

    // Then
    verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
  }

  @Test
  @DisplayName("Should handle database error in getRouteDefinitions")
  void shouldHandleDatabaseErrorInGetRouteDefinitions() {
    // Given
    when(repository.findAllEnabledOrderByOrder()).thenThrow(new RuntimeException("Database error"));

    // When
    Flux<RouteDefinition> result = routeDefinitionRepository.getRouteDefinitions();

    // Then
    StepVerifier.create(result).verifyComplete(); // Should complete empty due to error handling

    verify(repository).findAllEnabledOrderByOrder();
  }

  @Test
  @DisplayName("Should handle save error gracefully")
  void shouldHandleSaveErrorGracefully() throws Exception {
    // Given
    RouteDefinition routeDefinition =
        createTestRouteDefinition("error-route", "http://service.com");
    GatewayRoute entity = createTestGatewayRoute("error-route", "http://service.com");

    when(mapper.toEntity(routeDefinition)).thenReturn(entity);
    when(repository.existsById("error-route")).thenReturn(false);
    when(repository.save(entity)).thenThrow(new RuntimeException("Save error"));

    // When
    Mono<Void> result = routeDefinitionRepository.save(Mono.just(routeDefinition));

    // Then
    StepVerifier.create(result).verifyError(RuntimeException.class);

    verify(mapper).toEntity(routeDefinition);
    verify(repository).existsById("error-route");
    verify(repository).save(entity);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("Should handle delete error gracefully")
  void shouldHandleDeleteErrorGracefully() {
    // Given
    String routeId = "error-route";
    doThrow(new RuntimeException("Delete error")).when(repository).deleteById(routeId);

    // When
    Mono<Void> result = routeDefinitionRepository.delete(Mono.just(routeId));

    // Then
    StepVerifier.create(result).verifyError(RuntimeException.class);

    verify(repository).deleteById(routeId);
    verifyNoInteractions(eventPublisher);
  }

  private GatewayRoute createTestGatewayRoute(String id, String uri) {
    GatewayRoute route = new GatewayRoute();
    route.setId(id);
    route.setUri(uri);
    route.setEnabled(true);
    route.setOrder(100);
    route.setCreatedAt(LocalDateTime.now());
    route.setUpdatedAt(LocalDateTime.now());
    return route;
  }

  private RouteDefinition createTestRouteDefinition(String id, String uri) throws Exception {
    RouteDefinition route = new RouteDefinition();
    route.setId(id);
    route.setUri(new URI(uri));
    route.setOrder(100);
    return route;
  }
}
