package nedo.ods.svc.dp.persistence;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for GatewayRoute entity. */
@DisplayName("GatewayRoute Entity Tests")
class GatewayRouteTest {

  private GatewayRoute gatewayRoute;

  @BeforeEach
  void setUp() {
    gatewayRoute = new GatewayRoute();
  }

  @Test
  @DisplayName("Should create default GatewayRoute with empty constructor")
  void shouldCreateDefaultGatewayRoute() {
    // Given & When
    GatewayRoute route = new GatewayRoute();

    // Then
    assertThat(route.getId()).isNull();
    assertThat(route.getUri()).isNull();
    assertThat(route.getEnabled()).isTrue(); // Default value is true
    assertThat(route.getOrder()).isNull();
    assertThat(route.getPredicates()).isNull();
    assertThat(route.getFilters()).isNull();
    assertThat(route.getMetadata()).isNull();
    assertThat(route.getCreatedAt()).isNull();
    assertThat(route.getUpdatedAt()).isNull();
  }

  @Test
  @DisplayName("Should create GatewayRoute with parameterized constructor")
  void shouldCreateGatewayRouteWithParameters() {
    // Given
    String id = "test-route";
    String uri = "http://example.com";

    // When
    GatewayRoute route = new GatewayRoute(id, uri);

    // Then
    assertThat(route.getId()).isEqualTo(id);
    assertThat(route.getUri()).isEqualTo(uri);
    assertThat(route.getEnabled()).isTrue();
    assertThat(route.getCreatedAt()).isNotNull();
    assertThat(route.getUpdatedAt()).isNotNull();
    assertThat(Duration.between(route.getCreatedAt(), route.getUpdatedAt()).abs().getSeconds()).isLessThan(1);
  }

  @Test
  @DisplayName("Should set and get all properties correctly")
  void shouldSetAndGetAllProperties() {
    // Given
    String id = "route-1";
    String uri = "http://test.com";
    String predicates = "[{\"name\":\"Path\",\"args\":{\"pattern\":\"/test/**\"}}]";
    String filters =
        "[{\"name\":\"AddRequestHeader\",\"args\":{\"name\":\"X-Test\",\"value\":\"true\"}}]";
    String metadata = "{\"key\":\"value\"}";
    Integer order = 100;
    Boolean enabled = false;
    LocalDateTime now = LocalDateTime.now();

    // When
    gatewayRoute.setId(id);
    gatewayRoute.setUri(uri);
    gatewayRoute.setPredicates(predicates);
    gatewayRoute.setFilters(filters);
    gatewayRoute.setMetadata(metadata);
    gatewayRoute.setOrder(order);
    gatewayRoute.setEnabled(enabled);
    gatewayRoute.setCreatedAt(now);
    gatewayRoute.setUpdatedAt(now);

    // Then
    assertThat(gatewayRoute.getId()).isEqualTo(id);
    assertThat(gatewayRoute.getUri()).isEqualTo(uri);
    assertThat(gatewayRoute.getPredicates()).isEqualTo(predicates);
    assertThat(gatewayRoute.getFilters()).isEqualTo(filters);
    assertThat(gatewayRoute.getMetadata()).isEqualTo(metadata);
    assertThat(gatewayRoute.getOrder()).isEqualTo(order);
    assertThat(gatewayRoute.getEnabled()).isEqualTo(enabled);
    assertThat(gatewayRoute.getCreatedAt()).isEqualTo(now);
    assertThat(gatewayRoute.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should trigger onCreate lifecycle callback")
  void shouldTriggerOnCreateCallback() {
    // Given
    final GatewayRoute route = new GatewayRoute();
    final LocalDateTime beforeCreate = LocalDateTime.now().minusSeconds(1);
    // When
    route.onCreate();
    // Then
    final LocalDateTime afterCreate = LocalDateTime.now().plusSeconds(1);
    assertThat(route.getCreatedAt()).isNotNull();
    assertThat(route.getUpdatedAt()).isNotNull();
    assertThat(route.getCreatedAt()).isAfter(beforeCreate);
    assertThat(route.getCreatedAt()).isBefore(afterCreate);
    assertThat(route.getUpdatedAt()).isAfter(beforeCreate);
    assertThat(route.getUpdatedAt()).isBefore(afterCreate);
    assertThat(Duration.between(route.getCreatedAt(), route.getUpdatedAt()).abs().getSeconds()).isLessThan(1);
  }

  @Test
  @DisplayName("Should trigger onUpdate lifecycle callback")
  void shouldTriggerOnUpdateCallback() {
    // Given
    final GatewayRoute route = new GatewayRoute();
    final LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
    final LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
    route.setCreatedAt(originalTime);
    route.setUpdatedAt(originalTime);
    // When
    route.onUpdate();
    // Then
    final LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
    assertThat(route.getCreatedAt()).isEqualTo(originalTime); // Should not change
    assertThat(route.getUpdatedAt()).isNotNull();
    assertThat(route.getUpdatedAt()).isAfter(beforeUpdate);
    assertThat(route.getUpdatedAt()).isBefore(afterUpdate);
    assertThat(route.getUpdatedAt()).isNotEqualTo(route.getCreatedAt());
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValues() {
    // When
    gatewayRoute.setId(null);
    gatewayRoute.setUri(null);
    gatewayRoute.setPredicates(null);
    gatewayRoute.setFilters(null);
    gatewayRoute.setMetadata(null);
    gatewayRoute.setOrder(null);
    gatewayRoute.setEnabled(null);
    gatewayRoute.setCreatedAt(null);
    gatewayRoute.setUpdatedAt(null);

    // Then
    assertThat(gatewayRoute.getId()).isNull();
    assertThat(gatewayRoute.getUri()).isNull();
    assertThat(gatewayRoute.getPredicates()).isNull();
    assertThat(gatewayRoute.getFilters()).isNull();
    assertThat(gatewayRoute.getMetadata()).isNull();
    assertThat(gatewayRoute.getOrder()).isNull();
    assertThat(gatewayRoute.getEnabled()).isNull();
    assertThat(gatewayRoute.getCreatedAt()).isNull();
    assertThat(gatewayRoute.getUpdatedAt()).isNull();
  }
}
