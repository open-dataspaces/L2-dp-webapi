package nedo.ods.svc.dp.authzen.transport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Comprehensive test suite for {@link Transport} interface.
 *
 * <p>Tests the transport abstraction contract and validates that implementations properly handle
 * authorization service communication.
 */
@DisplayName("Transport Interface")
class TransportTest {

  @Nested
  @DisplayName("Interface Contract")
  class InterfaceContractTest {

    @Test
    @DisplayName("should be a public interface")
    void shouldBePublicInterface() {
      // Then
      assertTrue(Transport.class.isInterface());
      assertTrue(Modifier.isPublic(Transport.class.getModifiers()));
    }

    @Test
    @DisplayName("should have request method with correct signature")
    void shouldHaveRequestMethodWithCorrectSignature() throws Exception {
      // Given
      Method requestMethod =
          Transport.class.getDeclaredMethod("request", AuthzClientConfig.class, String.class);

      // Then
      assertNotNull(requestMethod);
      assertTrue(Modifier.isPublic(requestMethod.getModifiers()));
      assertTrue(Modifier.isAbstract(requestMethod.getModifiers()));
      assertEquals(String.class, requestMethod.getReturnType());

      Class<?>[] exceptions = requestMethod.getExceptionTypes();
      assertEquals(1, exceptions.length);
      assertEquals(Exception.class, exceptions[0]);
    }

    @Test
    @DisplayName("should have exactly one method")
    void shouldHaveExactlyOneMethod() {
      // Given
      Method[] methods = Transport.class.getDeclaredMethods();

      // Then
      assertEquals(1, methods.length);
      assertEquals("request", methods[0].getName());
    }

    @Test
    @DisplayName("should be functional interface")
    void shouldBeFunctionalInterface() {
      // Given
      Method[] methods = Transport.class.getDeclaredMethods();

      // Then - Should have exactly one abstract method (functional interface requirement)
      long abstractMethods =
          java.util.Arrays.stream(methods)
              .filter(method -> Modifier.isAbstract(method.getModifiers()))
              .count();
      assertEquals(1, abstractMethods);
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTest {

    @Mock private AuthzClientConfig mockConfig;

    private Transport transport;

    @Test
    @DisplayName("should allow lambda implementation")
    void shouldAllowLambdaImplementation() throws Exception {
      // Given
      String expectedResponse =
          """
                {
                    "decision": true,
                    "context": {
                        "request_id": "test-123"
                    }
                }
                """;

      transport =
          (config, jsonBody) -> {
            // Simulate successful transport
            return expectedResponse;
          };

      String jsonRequest =
          """
                {
                    "subject": {"id": "alice"},
                    "action": {"name": "read"}
                }
                """;

      // When
      String response = transport.request(mockConfig, jsonRequest);

      // Then
      assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("should allow anonymous class implementation")
    void shouldAllowAnonymousClassImplementation() throws Exception {
      // Given
      transport =
          new Transport() {
            @Override
            public String request(AuthzClientConfig config, String jsonBody) throws Exception {
              if (jsonBody.contains("alice")) {
                return """
                            {
                                "decision": true,
                                "context": {"user": "alice"}
                            }
                            """;
              } else {
                return """
                            {
                                "decision": false,
                                "context": {"reason": "unknown_user"}
                            }
                            """;
              }
            }
          };

      String aliceRequest =
          """
                {
                    "subject": {"id": "alice"}
                }
                """;

      String bobRequest =
          """
                {
                    "subject": {"id": "bob"}
                }
                """;

      // When
      String aliceResponse = transport.request(mockConfig, aliceRequest);
      String bobResponse = transport.request(mockConfig, bobRequest);

      // Then
      assertTrue(aliceResponse.contains("\"decision\": true"));
      assertTrue(aliceResponse.contains("\"user\": \"alice\""));
      assertTrue(bobResponse.contains("\"decision\": false"));
      assertTrue(bobResponse.contains("\"unknown_user\""));
    }

    @Test
    @DisplayName("should handle exceptions in implementation")
    void shouldHandleExceptionsInImplementation() {
      // Given
      transport =
          (config, jsonBody) -> {
            if (jsonBody == null || jsonBody.isEmpty()) {
              throw new IllegalArgumentException("JSON body cannot be null or empty");
            }
            throw new RuntimeException("Simulated transport failure");
          };

      // When & Then
      IllegalArgumentException nullException =
          assertThrows(IllegalArgumentException.class, () -> transport.request(mockConfig, null));
      assertEquals("JSON body cannot be null or empty", nullException.getMessage());

      RuntimeException runtimeException =
          assertThrows(
              RuntimeException.class, () -> transport.request(mockConfig, "{\"test\": \"value\"}"));
      assertEquals("Simulated transport failure", runtimeException.getMessage());
    }
  }

  @Nested
  @DisplayName("Integration Contract Tests")
  class IntegrationContractTest {

    @Test
    @DisplayName("should work with different configuration types")
    void shouldWorkWithDifferentConfigurationTypes() throws Exception {
      // Given
      Transport mockTransport = mock(Transport.class);
      when(mockTransport.request(any(AuthzClientConfig.class), anyString()))
          .thenReturn("{\"decision\": true}");

      AuthzClientConfig config1 = mock(AuthzClientConfig.class);
      AuthzClientConfig config2 = mock(AuthzClientConfig.class);

      String jsonBody = "{\"subject\": {\"id\": \"test\"}}";

      // When
      String response1 = mockTransport.request(config1, jsonBody);
      String response2 = mockTransport.request(config2, jsonBody);

      // Then
      assertEquals("{\"decision\": true}", response1);
      assertEquals("{\"decision\": true}", response2);
      verify(mockTransport, times(2)).request(any(AuthzClientConfig.class), eq(jsonBody));
    }

    @Test
    @DisplayName("should handle various JSON body formats")
    void shouldHandleVariousJsonBodyFormats() throws Exception {
      // Given
      Transport echoTransport =
          (config, jsonBody) -> {
            // Echo back the input with a wrapper
            return "{\"received\": " + jsonBody + ", \"decision\": true}";
          };

      AuthzClientConfig config = mock(AuthzClientConfig.class);

      String simpleJson = "{\"test\": \"value\"}";
      String complexJson =
          """
                {
                    "subject": {
                        "id": "alice",
                        "attributes": {
                            "department": "engineering",
                            "roles": ["user", "admin"]
                        }
                    },
                    "action": {
                        "name": "read",
                        "resource_type": "document"
                    }
                }
                """;

      // When
      String simpleResponse = echoTransport.request(config, simpleJson);
      String complexResponse = echoTransport.request(config, complexJson);

      // Then
      assertTrue(simpleResponse.contains("\"test\": \"value\""));
      assertTrue(simpleResponse.contains("\"decision\": true"));
      assertTrue(complexResponse.contains("\"id\": \"alice\""));
      assertTrue(complexResponse.contains("\"department\": \"engineering\""));
      assertTrue(complexResponse.contains("\"decision\": true"));
    }

    @Test
    @DisplayName("should support method chaining patterns")
    void shouldSupportMethodChainingPatterns() throws Exception {
      // Given
      class ChainableTransport implements Transport {
        private String lastResponse;

        @Override
        public String request(AuthzClientConfig config, String jsonBody) {
          lastResponse = "{\"decision\": true, \"body\": \"" + jsonBody + "\"}";
          return lastResponse;
        }

        public String getLastResponse() {
          return lastResponse;
        }
      }

      ChainableTransport chainableTransport = new ChainableTransport();
      AuthzClientConfig config = mock(AuthzClientConfig.class);

      // When
      String response1 = chainableTransport.request(config, "{\"request\": 1}");
      String lastResponse1 = chainableTransport.getLastResponse();

      String response2 = chainableTransport.request(config, "{\"request\": 2}");
      String lastResponse2 = chainableTransport.getLastResponse();

      // Then
      assertEquals(response1, lastResponse1);
      assertEquals(response2, lastResponse2);
      assertTrue(response1.contains("\"request\": 1"));
      assertTrue(response2.contains("\"request\": 2"));
      assertNotEquals(response1, response2);
    }
  }

  @Nested
  @DisplayName("Abstraction Benefits Tests")
  class AbstractionBenefitsTest {

    @Test
    @DisplayName("should enable multiple transport implementations")
    void shouldEnableMultipleTransportImplementations() throws Exception {
      // Given
      Transport httpTransport =
          (config, jsonBody) -> {
            return "{\"transport\": \"http\", \"decision\": true}";
          };

      Transport grpcTransport =
          (config, jsonBody) -> {
            return "{\"transport\": \"grpc\", \"decision\": true}";
          };

      Transport fileTransport =
          (config, jsonBody) -> {
            return "{\"transport\": \"file\", \"decision\": false}";
          };

      AuthzClientConfig config = mock(AuthzClientConfig.class);
      String jsonBody = "{\"test\": \"request\"}";

      // When
      String httpResponse = httpTransport.request(config, jsonBody);
      String grpcResponse = grpcTransport.request(config, jsonBody);
      String fileResponse = fileTransport.request(config, jsonBody);

      // Then
      assertTrue(httpResponse.contains("\"transport\": \"http\""));
      assertTrue(grpcResponse.contains("\"transport\": \"grpc\""));
      assertTrue(fileResponse.contains("\"transport\": \"file\""));

      assertTrue(httpResponse.contains("\"decision\": true"));
      assertTrue(grpcResponse.contains("\"decision\": true"));
      assertTrue(fileResponse.contains("\"decision\": false"));
    }

    @Test
    @DisplayName("should support configuration-based transport selection")
    void shouldSupportConfigurationBasedTransportSelection() throws Exception {
      // Given
      class TransportFactory {
        public Transport createTransport(String type) {
          return switch (type.toLowerCase()) {
            case "http" -> (config, jsonBody) -> "{\"type\": \"http\", \"decision\": true}";
            case "mock" -> (config, jsonBody) -> "{\"type\": \"mock\", \"decision\": false}";
            case "test" -> (config, jsonBody) -> "{\"type\": \"test\", \"decision\": true}";
            default -> throw new IllegalArgumentException("Unknown transport type: " + type);
          };
        }
      }

      TransportFactory factory = new TransportFactory();
      AuthzClientConfig config = mock(AuthzClientConfig.class);
      String jsonBody = "{\"factory\": \"test\"}";

      // When
      Transport httpTransport = factory.createTransport("http");
      Transport mockTransport = factory.createTransport("mock");
      Transport testTransport = factory.createTransport("test");

      String httpResponse = httpTransport.request(config, jsonBody);
      String mockResponse = mockTransport.request(config, jsonBody);
      String testResponse = testTransport.request(config, jsonBody);

      // Then
      assertTrue(httpResponse.contains("\"type\": \"http\""));
      assertTrue(mockResponse.contains("\"type\": \"mock\""));
      assertTrue(testResponse.contains("\"type\": \"test\""));

      assertTrue(httpResponse.contains("\"decision\": true"));
      assertTrue(mockResponse.contains("\"decision\": false"));
      assertTrue(testResponse.contains("\"decision\": true"));
    }

    @Test
    @DisplayName("should enable transport-specific error handling")
    void shouldEnableTransportSpecificErrorHandling() {
      // Given
      Transport networkTransport =
          (config, jsonBody) -> {
            throw new java.net.ConnectException("Network connection failed");
          };

      Transport authTransport =
          (config, jsonBody) -> {
            throw new SecurityException("Authentication failed");
          };

      Transport validationTransport =
          (config, jsonBody) -> {
            throw new IllegalArgumentException("Invalid JSON format");
          };

      AuthzClientConfig config = mock(AuthzClientConfig.class);
      String jsonBody = "{\"test\": true}";

      // When & Then
      java.net.ConnectException networkException =
          assertThrows(
              java.net.ConnectException.class, () -> networkTransport.request(config, jsonBody));
      assertEquals("Network connection failed", networkException.getMessage());

      SecurityException authException =
          assertThrows(SecurityException.class, () -> authTransport.request(config, jsonBody));
      assertEquals("Authentication failed", authException.getMessage());

      IllegalArgumentException validationException =
          assertThrows(
              IllegalArgumentException.class, () -> validationTransport.request(config, jsonBody));
      assertEquals("Invalid JSON format", validationException.getMessage());
    }
  }
}
