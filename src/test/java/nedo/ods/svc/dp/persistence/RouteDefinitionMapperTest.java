package nedo.ods.svc.dp.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for RouteDefinitionMapper. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouteDefinitionMapper Tests")
class RouteDefinitionMapperTest {

  private RouteDefinitionMapper mapper;

  @BeforeEach
  void setUp() {
    // Using the implementation class for testing
    mapper = new RouteDefinitionMapperImpl();
  }

  @Test
  @DisplayName("Should convert URI to string correctly")
  void shouldConvertUriToString() throws URISyntaxException {
    // Given
    URI uri = new URI("http://example.com:8080/api");

    // When
    String result = mapper.uriToString(uri);

    // Then
    assertThat(result).isEqualTo("http://example.com:8080/api");
  }

  @Test
  @DisplayName("Should handle null URI in uriToString")
  void shouldHandleNullUriInUriToString() {
    // When
    String result = mapper.uriToString(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should convert string to URI correctly")
  void shouldConvertStringToUri() {
    // Given
    String uriString = "http://example.com:8080/api";

    // When
    URI result = mapper.stringToUri(uriString);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.toString()).isEqualTo(uriString);
  }

  @Test
  @DisplayName("Should handle null string in stringToUri")
  void shouldHandleNullStringInStringToUri() {
    // When
    URI result = mapper.stringToUri(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should throw RuntimeException for invalid URI")
  void shouldThrowRuntimeExceptionForInvalidUri() {
    // Given
    String invalidUri = "invalid://uri with spaces";

    // When & Then
    assertThatThrownBy(() -> mapper.stringToUri(invalidUri))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid URI")
        .hasCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  @DisplayName("Should validate valid JSON array string")
  void shouldValidateValidJsonArrayString() {
    // Given
    String validJsonArray = "[{\"name\":\"Path\",\"args\":{\"pattern\":\"/test/**\"}}]";

    // When
    boolean result = mapper.isValidJsonString(validJsonArray);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should validate valid JSON object string")
  void shouldValidateValidJsonObjectString() {
    // Given
    String validJsonObject = "{\"key\":\"value\",\"number\":123}";

    // When
    boolean result = mapper.isValidJsonString(validJsonObject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should reject invalid JSON string")
  void shouldRejectInvalidJsonString() {
    // Given
    String invalidJson = "not a json string";

    // When
    boolean result = mapper.isValidJsonString(invalidJson);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should reject null JSON string")
  void shouldRejectNullJsonString() {
    // When
    boolean result = mapper.isValidJsonString(null);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should reject empty JSON string")
  void shouldRejectEmptyJsonString() {
    // When
    boolean result = mapper.isValidJsonString("");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should reject whitespace-only JSON string")
  void shouldRejectWhitespaceOnlyJsonString() {
    // When
    boolean result = mapper.isValidJsonString("   ");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should convert RouteDefinition to GatewayRoute entity")
  void shouldConvertRouteDefinitionToGatewayRoute() throws URISyntaxException {
    // Given
    RouteDefinition routeDefinition = new RouteDefinition();
    routeDefinition.setId("test-route");
    routeDefinition.setUri(new URI("http://example.com"));
    routeDefinition.setOrder(100);

    PredicateDefinition predicate = new PredicateDefinition();
    predicate.setName("Path");
    predicate.getArgs().put("pattern", "/test/**");
    routeDefinition.setPredicates(List.of(predicate));

    FilterDefinition filter = new FilterDefinition();
    filter.setName("AddRequestHeader");
    filter.getArgs().put("name", "X-Test");
    filter.getArgs().put("value", "true");
    routeDefinition.setFilters(List.of(filter));

    routeDefinition.setMetadata(Map.of("key", "value"));

    // When
    GatewayRoute result = mapper.toEntity(routeDefinition);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("test-route");
    assertThat(result.getUri()).isEqualTo("http://example.com");
    assertThat(result.getOrder()).isEqualTo(100);
    assertThat(result.getEnabled()).isTrue();
    // JSON fields are set in @AfterMapping, so they might be null in basic mapping
  }

  @Test
  @DisplayName("Should convert GatewayRoute entity to RouteDefinition")
  void shouldConvertGatewayRouteToRouteDefinition() {
    // Given
    GatewayRoute entity = new GatewayRoute();
    entity.setId("test-route");
    entity.setUri("http://example.com");
    entity.setOrder(100);
    entity.setPredicates("[{\"name\":\"Path\",\"args\":{\"pattern\":\"/test/**\"}}]");
    entity.setFilters(
        "[{\"name\":\"AddRequestHeader\",\"args\":{\"name\":\"X-Test\",\"value\":\"true\"}}]");
    entity.setMetadata("{\"key\":\"value\"}");

    // When
    RouteDefinition result = mapper.toRouteDefinition(entity);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("test-route");
    assertThat(result.getUri().toString()).isEqualTo("http://example.com");
    assertThat(result.getOrder()).isEqualTo(100);
    // JSON fields are deserialized in @AfterMapping
  }

  @Test
  @DisplayName("Should handle null RouteDefinition gracefully")
  void shouldHandleNullRouteDefinition() {
    // When
    GatewayRoute result = mapper.toEntity(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should handle null GatewayRoute gracefully")
  void shouldHandleNullGatewayRoute() {
    // When
    RouteDefinition result = mapper.toRouteDefinition(null);

    // Then
    assertThat(result).isNull();
  }

  /**
   * Simple implementation of RouteDefinitionMapper for testing. In real scenarios, this would be
   * generated by MapStruct.
   */
  private static class RouteDefinitionMapperImpl implements RouteDefinitionMapper {

    @Override
    public GatewayRoute toEntity(RouteDefinition routeDefinition) {
      if (routeDefinition == null) {
        return null;
      }

      GatewayRoute gatewayRoute = new GatewayRoute();
      gatewayRoute.setId(routeDefinition.getId());
      gatewayRoute.setUri(uriToString(routeDefinition.getUri()));
      gatewayRoute.setOrder(routeDefinition.getOrder());
      gatewayRoute.setEnabled(true);

      // Simulate @AfterMapping
      mapJsonFields(routeDefinition, gatewayRoute);

      return gatewayRoute;
    }

    @Override
    public RouteDefinition toRouteDefinition(GatewayRoute entity) {
      if (entity == null) {
        return null;
      }

      RouteDefinition routeDefinition = new RouteDefinition();
      routeDefinition.setId(entity.getId());
      routeDefinition.setUri(stringToUri(entity.getUri()));
      routeDefinition.setOrder(entity.getOrder());

      // Simulate @AfterMapping
      mapJsonFields(entity, routeDefinition);

      return routeDefinition;
    }
  }
}
