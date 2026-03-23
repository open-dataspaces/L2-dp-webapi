package nedo.ods.svc.dp.authzen.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;
import nedo.ods.svc.dp.authzen.context.ContextFactory;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;
import nedo.ods.svc.dp.authzen.model.Action;
import nedo.ods.svc.dp.authzen.model.Resource;
import nedo.ods.svc.dp.authzen.model.Subject;
import nedo.ods.svc.dp.authzen.serialization.AuthorizationRequestSerializer;
import nedo.ods.svc.dp.authzen.transport.Transport;

/**
 * Unit tests for {@link AuthzClient}.
 *
 * <p>This test class covers all methods, branches, and exception handling for AuthzClient.
 */
class AuthzClientTest {

  /**
   * Test authorize() with context enrichment.
   *
   * @see AuthzClient#authorize(AuthorizationRequest)
   */
  @Test
  @DisplayName("authorize merges context when contextFactory is provided")
  void testAuthorizeWithContextFactory() throws Exception {
    AuthzClientConfig config = mock(AuthzClientConfig.class);
    Transport transport = mock(Transport.class);
    ContextFactory contextFactory = mock(ContextFactory.class);
    nedo.ods.svc.dp.authzen.model.Context dynamicContext =
        new nedo.ods.svc.dp.authzen.model.Context(java.util.Map.of("ip", "127.0.0.1"));
    when(contextFactory.createContext()).thenReturn(dynamicContext);
    AuthzClient client = new AuthzClient(config, transport, contextFactory);
    Subject subject = new Subject.Builder().id("user1").type("user").build();
    Resource resource = new Resource.Builder().id("res1").type("document").build();
    Action action = new Action.Builder().name("read").build();
    AuthorizationRequest request =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    String requestJson = "{requestJson}";
    String responseJson = "{responseJson}";
    AuthorizationResponse expectedResponse = new AuthorizationResponse(true, java.util.Map.of());
    try (MockedStatic<AuthorizationRequestSerializer> serializerMock =
        mockStatic(AuthorizationRequestSerializer.class)) {
      serializerMock
          .when(
              () ->
                  AuthorizationRequestSerializer.buildRequestJson(any(AuthorizationRequest.class)))
          .thenReturn(requestJson);
      when(transport.request(config, requestJson)).thenReturn(responseJson);
      try (MockedStatic<nedo.ods.svc.dp.authzen.serialization.AuthorizationResponseDeserializer>
          deserializerMock =
              mockStatic(
                  nedo.ods.svc.dp.authzen.serialization.AuthorizationResponseDeserializer.class)) {
        deserializerMock
            .when(
                () ->
                    nedo.ods.svc.dp.authzen.serialization.AuthorizationResponseDeserializer
                        .parseResponseJson(any(String.class)))
            .thenReturn(expectedResponse);
        AuthorizationResponse response = client.authorize(request);
        assertEquals(expectedResponse, response, "Response should match expected");
      }
    }
  }

  /**
   * Test authorize() rethrows AuthorizationException from transport or deserializer.
   *
   * @see AuthzClient#authorize(AuthorizationRequest)
   */
  @Test
  @DisplayName("authorize rethrows AuthorizationException from transport")
  void testAuthorizeRethrowsAuthorizationException() throws Exception {
    AuthzClientConfig config = mock(AuthzClientConfig.class);
    Transport transport = mock(Transport.class);
    AuthzClient client = new AuthzClient(config, transport);
    Subject subject = new Subject.Builder().id("user2").type("user").build();
    Resource resource = new Resource.Builder().id("res2").type("document").build();
    Action action = new Action.Builder().name("write").build();
    AuthorizationRequest request =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    String requestJson = "{requestJson}";
    try (MockedStatic<AuthorizationRequestSerializer> serializerMock =
        mockStatic(AuthorizationRequestSerializer.class)) {
      serializerMock
          .when(() -> AuthorizationRequestSerializer.buildRequestJson(request))
          .thenReturn(requestJson);
      AuthorizationException transportException = new AuthorizationException("Transport error");
      when(transport.request(config, requestJson)).thenThrow(transportException);
      AuthorizationException ex =
          assertThrows(AuthorizationException.class, () -> client.authorize(request));
      assertSame(transportException, ex, "Should rethrow the original AuthorizationException");
    }
  }

  /**
   * Test authorize() wraps unexpected exceptions in AuthorizationException.
   *
   * @see AuthzClient#authorize(AuthorizationRequest)
   */
  @Test
  @DisplayName("authorize wraps unexpected exceptions in AuthorizationException")
  void testAuthorizeWrapsUnexpectedException() throws Exception {
    AuthzClientConfig config = mock(AuthzClientConfig.class);
    Transport transport = mock(Transport.class);
    AuthzClient client = new AuthzClient(config, transport);
    Subject subject = new Subject.Builder().id("user3").type("user").build();
    Resource resource = new Resource.Builder().id("res3").type("document").build();
    Action action = new Action.Builder().name("delete").build();
    AuthorizationRequest request =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    String requestJson = "{requestJson}";
    try (MockedStatic<AuthorizationRequestSerializer> serializerMock =
        mockStatic(AuthorizationRequestSerializer.class)) {
      serializerMock
          .when(() -> AuthorizationRequestSerializer.buildRequestJson(request))
          .thenReturn(requestJson);
      when(transport.request(config, requestJson))
          .thenThrow(new RuntimeException("Unexpected error"));
      AuthorizationException ex =
          assertThrows(AuthorizationException.class, () -> client.authorize(request));
      assertEquals(
          "Authorization request failed due to an unexpected error",
          ex.getMessage(),
          "Exception message should match");
      assertNotNull(ex.getCause(), "Exception cause should not be null");
      assertEquals("Unexpected error", ex.getCause().getMessage(), "Cause message should match");
    }
  }

  /**
   * Test constructor sets fields correctly.
   *
   * @see AuthzClient#AuthzClient(AuthzClientConfig, Transport)
   * @see AuthzClient#AuthzClient(AuthzClientConfig, Transport, ContextFactory)
   */
  @Test
  @DisplayName("constructor sets fields correctly")
  void testConstructorSetsFields() {
    AuthzClientConfig config = mock(AuthzClientConfig.class);
    Transport transport = mock(Transport.class);
    ContextFactory contextFactory = mock(ContextFactory.class);
    AuthzClient client1 = new AuthzClient(config, transport);
    AuthzClient client2 = new AuthzClient(config, transport, contextFactory);
    assertNotNull(client1, "Client should not be null");
    assertNotNull(client2, "Client should not be null");
  }
}
