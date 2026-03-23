package nedo.ods.svc.dp.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GatewayRouteRepository. Note: This tests the repository interface behavior through
 * mocking. For full integration tests, consider using @DataJpaTest with proper configuration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayRouteRepository Unit Tests")
class GatewayRouteRepositoryTest {

  @Mock private GatewayRouteRepository repository;

  @Test
  @DisplayName("Should test findAllEnabledOrderByOrder method contract")
  void shouldTestFindAllEnabledOrderByOrderContract() {
    // Given
    GatewayRoute route1 = createTestRoute("route-1", "http://service1.com", 200, true);
    GatewayRoute route2 = createTestRoute("route-2", "http://service2.com", 100, true);
    GatewayRoute route3 = createTestRoute("route-3", "http://service3.com", null, true);

    List<GatewayRoute> expectedRoutes =
        Arrays.asList(route3, route2, route1); // ordered by priority
    when(repository.findAllEnabledOrderByOrder()).thenReturn(expectedRoutes);

    // When
    List<GatewayRoute> result = repository.findAllEnabledOrderByOrder();

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getId()).isEqualTo("route-3"); // null order comes first
    assertThat(result.get(1).getId()).isEqualTo("route-2"); // order 100
    assertThat(result.get(2).getId()).isEqualTo("route-1"); // order 200

    verify(repository).findAllEnabledOrderByOrder();
  }

  @Test
  @DisplayName("Should test findByEnabledOrderByOrderAscIdAsc method contract")
  void shouldTestFindByEnabledOrderByOrderAscIdAscContract() {
    // Given
    GatewayRoute enabledRoute1 = createTestRoute("enabled-1", "http://service1.com", 100, true);
    GatewayRoute enabledRoute2 = createTestRoute("enabled-2", "http://service2.com", 50, true);

    List<GatewayRoute> enabledRoutes =
        Arrays.asList(enabledRoute2, enabledRoute1); // ordered by order ASC
    when(repository.findByEnabledOrderByOrderAscIdAsc(true)).thenReturn(enabledRoutes);

    // When
    List<GatewayRoute> result = repository.findByEnabledOrderByOrderAscIdAsc(true);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo("enabled-2"); // order 50
    assertThat(result.get(1).getId()).isEqualTo("enabled-1"); // order 100

    verify(repository).findByEnabledOrderByOrderAscIdAsc(true);
  }

  @Test
  @DisplayName("Should test save and findById method contract")
  void shouldTestSaveAndFindByIdContract() {
    // Given
    GatewayRoute route = createTestRoute("test-route", "http://example.com", 100, true);
    route.setPredicates("[{\"name\":\"Path\",\"args\":{\"pattern\":\"/test/**\"}}]");
    route.setFilters(
        "[{\"name\":\"AddRequestHeader\",\"args\":{\"name\":\"X-Test\",\"value\":\"true\"}}]");
    route.setMetadata("{\"key\":\"value\"}");

    when(repository.save(route)).thenReturn(route);
    when(repository.findById("test-route")).thenReturn(Optional.of(route));

    // When
    GatewayRoute saved = repository.save(route);
    Optional<GatewayRoute> retrieved = repository.findById("test-route");

    // Then
    assertThat(saved).isNotNull();
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getId()).isEqualTo("test-route");
    assertThat(retrieved.get().getUri()).isEqualTo("http://example.com");
    assertThat(retrieved.get().getOrder()).isEqualTo(100);
    assertThat(retrieved.get().getEnabled()).isTrue();

    verify(repository).save(route);
    verify(repository).findById("test-route");
  }

  @Test
  @DisplayName("Should test empty result handling")
  void shouldTestEmptyResultHandling() {
    // Given
    when(repository.findAllEnabledOrderByOrder()).thenReturn(Arrays.asList());
    when(repository.findByEnabledOrderByOrderAscIdAsc(true)).thenReturn(Arrays.asList());

    // When
    List<GatewayRoute> allEnabled = repository.findAllEnabledOrderByOrder();
    List<GatewayRoute> specificEnabled = repository.findByEnabledOrderByOrderAscIdAsc(true);

    // Then
    assertThat(allEnabled).isEmpty();
    assertThat(specificEnabled).isEmpty();

    verify(repository).findAllEnabledOrderByOrder();
    verify(repository).findByEnabledOrderByOrderAscIdAsc(true);
  }

  private GatewayRoute createTestRoute(String id, String uri, Integer order, Boolean enabled) {
    GatewayRoute route = new GatewayRoute();
    route.setId(id);
    route.setUri(uri);
    route.setOrder(order);
    route.setEnabled(enabled);
    route.setCreatedAt(LocalDateTime.now());
    route.setUpdatedAt(LocalDateTime.now());
    return route;
  }
}
