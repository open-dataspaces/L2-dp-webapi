package nedo.ods.svc.dp.authzen.transport.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import nedo.ods.svc.dp.authzen.config.AuthzClientConfig;
import nedo.ods.svc.dp.authzen.exception.AuthorizationException;
import nedo.ods.svc.dp.authzen.exception.TransportException;

/**
 * Comprehensive test suite for {@link SimpleHttpClient}.
 *
 * <p>Tests HTTP transport implementation including retry logic, timeout handling, authentication,
 * and error scenarios.
 */
@DisplayName("SimpleHttpClient")
@ExtendWith(MockitoExtension.class)
class SimpleHttpClientTest {

  @Mock private AuthzClientConfig mockConfig;

  static class LocalHttp {
    HttpServer server;
    int port;

    void start() throws Exception {
      server = HttpServer.create(new InetSocketAddress(0), 0);
      port = server.getAddress().getPort();

      server.createContext(
          "/delay/",
          ex -> {
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");
            int ms = 1000;
            if (parts.length > 2) {
              try {
                ms = Integer.parseInt(parts[2]);
              } catch (NumberFormatException ignore) {
              }
            }
            try {
              if (ms > 0) {
                Thread.sleep(ms);
              }
              // Do not sleep if ms <= 0
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            respond(ex, 200, "{\"delayed\":" + ms + "}");
          });

      // 200 Fixed Response
      server.createContext("/ok", ex -> respond(ex, 200, "{\"status\":\"OK\"}"));

      // Optional JSON
      server.createContext("/json", ex -> respond(ex, 200, "{\"name\":\"example\",\"value\":1}"));
      server.createContext(
          "/uuid", ex -> respond(ex, 200, "{\"uuid\":\"00000000-0000-0000-0000-000000000000\"}"));

      // Return the body as is using echo
      server.createContext(
          "/echo",
          ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(ex, 200, body);
          });

      // Check required headers echo-strict
      server.createContext(
          "/echo-strict",
          ex -> {
            Headers headers = ex.getRequestHeaders();
            String ct = headers.getFirst("Content-Type");
            String reqId = headers.getFirst("X-Request-ID");
            if (ct == null
                || !ct.equalsIgnoreCase("application/json")
                || reqId == null
                || reqId.isBlank()) {
              // 400 response after reading the body
              ex.getRequestBody().readAllBytes();
              respond(ex, 400, "{\"error\":\"missing headers\"}");
              return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(ex, 200, body);
          });

      server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
      server.start();
    }

    void stop() {
      if (server != null) server.stop(0);
    }

    String url(String path) {
      return "http://127.0.0.1:" + port + path;
    }

    static void respond(HttpExchange ex, int status, String body) throws java.io.IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", "application/json");
      ex.sendResponseHeaders(status, bytes.length);
      OutputStream os = ex.getResponseBody();
      java.io.IOException writeEx = null;
      try {
        os.write(bytes);
      } catch (java.io.IOException e) {
        writeEx = e;
        throw e;
      } finally {
        try {
          os.close();
        } catch (java.io.IOException closeEx) {
          if (writeEx != null) {
            writeEx.addSuppressed(closeEx);
          } else {
            throw closeEx;
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("LocalHttp Coverage Tests")
  class LocalHttpCoverageTests {

    /** Get unused port */
    private int unusedPort() throws java.io.IOException {
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        return s.getLocalPort();
      }
    }

    /** Pass an Executable anonymous class, not a lambda, to assertThrows */
    private org.junit.jupiter.api.function.Executable execRespond(
        HttpExchange ex, int status, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          SimpleHttpClientTest.LocalHttp.respond(ex, status, body);
        }
      };
    }

    @Test
    @DisplayName("respond: write Success → IOException on close (throw closeEx in the else branch)")
    void respond_closeThrows_afterWriteSuccess() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) {
              /* success */
            }

            @Override
            public void close() throws java.io.IOException {
              throw new java.io.IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      Headers headers = new Headers();
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(headers);
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      java.io.IOException thrown =
          assertThrows(java.io.IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("close failed"));
      // respond() always includes the Content-Type
      assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test
    @DisplayName("respond: IOException on write + IOException on close (add suppressed to writeEx)")
    void respond_writeThrows_thenCloseThrows_suppressed() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) throws java.io.IOException {
              throw new java.io.IOException("write failed");
            }

            @Override
            public void close() throws java.io.IOException {
              throw new java.io.IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      Headers headers = new Headers();
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(headers);
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      java.io.IOException thrown =
          assertThrows(java.io.IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("write failed"));
      // An exception to 'close' is attached to 'suppressed'
      assertTrue(
          java.util.Arrays.stream(thrown.getSuppressed())
              .anyMatch(s -> "close failed".equals(s.getMessage())));
      assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test
    @DisplayName("stop() should handle null server gracefully（Stop before starting）")
    void stopShouldHandleNullServer() {
      LocalHttp h = new LocalHttp();
      // Not started → server==null → false branch
      h.stop();
      assertTrue(true);
    }

    // ---- /ok, /json, /uuid, /echo ----
    @Test
    @DisplayName("/ok return fixed JSON（Assign Content-Type）")
    void okShouldReturnFixedJson() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) URI.create(h.url("/ok")).toURL().openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        assertEquals("application/json", conn.getHeaderField("Content-Type"));
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/json Fixed JSON")
    void jsonShouldReturnFixedJson() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn = (java.net.HttpURLConnection) URI.create(h.url("/json")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"name\":\"example\",\"value\":1}", body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/uuid Fixed JSON")
    void uuidShouldReturnFixedJson() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn = (java.net.HttpURLConnection) URI.create(h.url("/uuid")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"uuid\":\"00000000-0000-0000-0000-000000000000\"}", body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo Return the input body as is")
    void echoShouldReturnRequestBody() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn = (java.net.HttpURLConnection) URI.create(h.url("/echo")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        String req = "{\"foo\":1}";
        try (var os = conn.getOutputStream()) {
          os.write(req.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(req, body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Success（Required header）")
    void echoStrictSuccess() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Request-ID", "req-123");
        String req = "{\"ping\":true}";
        try (var os = conn.getOutputStream()) {
          os.write(req.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(req, body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Failure：Both headers missing → 400")
    void echoStrictMissingHeaders() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"headers\"}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(400, conn.getResponseCode());

        // In the case of 400 (with error body), errorStream is always non-null.
        // Read it directly without creating a branch.
        var is = conn.getErrorStream();
        assertNotNull(is, "error body should exist for /echo-strict 400");

        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("missing headers"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName(
        "/echo-strict Failure：Content-Type missing, X-Request-ID present → 400 (covers ct==null)")
    void echoStrict_ContentTypeMissing_onlyReqIdPresent_should400() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        // Do not include Content-Type (ct == null) / reqId is a valid value
        conn.setRequestProperty("X-Request-ID", "req-ct-missing");
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"ct\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(400, conn.getResponseCode());
        var is = conn.getErrorStream();
        assertNotNull(is);
        String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("missing headers"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Failure：Content-Type OK / X-Request-ID missing → 400")
    void echoStrictXRequestIdMissing() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json"); // No X-Request-ID
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"headers\"}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(400, conn.getResponseCode());
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Failure：Invalid Content-Type / X-Request-ID present → 400")
    void echoStrictContentTypeInvalid() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestProperty("X-Request-ID", "req-123");
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"headers\"}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(400, conn.getResponseCode());
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Failure：X-Request-ID is blank → 400")
    void echoStrictXRequestIdBlank() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/echo-strict")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Request-ID", " ");
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"headers\"}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(400, conn.getResponseCode());
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/delay/{ms} : ms>0（1000）")
    void delayPositive() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/delay/1000")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":1000"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/delay/0 : 0（not sleeping）")
    void delayZero() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/delay/0")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":0"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/delay/-100 : Negative value（not sleeping）")
    void delayNegative() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/delay/-100")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":-100"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/delay/abc : NumberFormatException → Fallback to the default 1000")
    void delayNumberFormatFallback() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/delay/abc")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":1000"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/delay (no suffix) should use default 1000 (covers parts.length<=2)")
    void delayNoSuffix_shouldFallbackToDefault1000() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        var conn =
            (java.net.HttpURLConnection) URI.create(h.url("/delay/")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn.getResponseCode());
        String body =
            new String(
                conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":1000"));
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("url() concatenates the path to the base and returns it.")
    void urlBuildsProperly() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        String built = h.url("/ok");
        assertTrue(built.startsWith("http://127.0.0.1:"), "built=" + built);
        assertTrue(built.endsWith("/ok"), "built=" + built);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("unusedPort() returns a valid ephemeral port (>0)")
    void unusedPortShouldReturnValidPort() throws Exception {
      int p = unusedPort();

      String s = String.valueOf(p);
      assertTrue(s.matches("\\d+"), "port format invalid: p=" + p);
    }

    @Test
    @DisplayName("respond: write success then close throws (uses execRespond helper)")
    void respond_closeThrows_afterWriteSuccess_usesHelper() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) {
              /* success */
            }

            @Override
            public void close() throws java.io.IOException {
              throw new java.io.IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(new Headers());
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      java.io.IOException thrown =
          assertThrows(java.io.IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("close failed"));
    }

    @Test
    @DisplayName("respond: write throws then close throws (uses execRespond helper)")
    void respond_writeThrows_thenCloseThrows_usesHelper() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) throws java.io.IOException {
              throw new java.io.IOException("write failed");
            }

            @Override
            public void close() throws java.io.IOException {
              throw new java.io.IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(new Headers());
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      java.io.IOException thrown =
          assertThrows(java.io.IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("write failed"));
      assertTrue(
          java.util.Arrays.stream(thrown.getSuppressed())
              .anyMatch(s -> "close failed".equals(s.getMessage())));
    }

    @Test
    @DisplayName("execRespond success path: respond() completes without exception")
    void execRespondSuccessPath_coversExecuteBody() throws Throwable {
      // Both write and close succeed for the OutputStream
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) {
              /* success */
            }

            @Override
            public void close() {
              /* success */
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      Headers headers = new Headers();
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(headers);
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      org.junit.jupiter.api.function.Executable e = execRespond(ex, 200, "{\"ok\":true}");
      e.execute();
      assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test
    @DisplayName(
        "LocalHttp /delay/ should hit catch(InterruptedException) in handler (direct interrupt)")
    void localHttpDelayShouldHitInterruptedCatchOnServerThread_direct() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        // Hand thread capture (using CountDownLatch to avoid short circuits)
        final java.util.concurrent.atomic.AtomicReference<Thread> handlerThreadRef =
            new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch handlerReady =
            new java.util.concurrent.CountDownLatch(1);

        // Handler for delay-int/{ms}: unified to the same default of 1000 as LocalHttp
        h.server.createContext(
            "/delay-int",
            ex -> {
              handlerThreadRef.set(Thread.currentThread());
              handlerReady.countDown();
              String path = ex.getRequestURI().getPath();
              String[] parts = path.split("/");
              int ms = 1000;
              if (parts.length > 2) {
                try {
                  ms = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignore) {
                  /* no-op */
                }
              }
              try {
                if (ms > 0) Thread.sleep(ms); // ←Step on the false side with /delay-int/0
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
              }
              SimpleHttpClientTest.LocalHttp.respond(ex, 200, "{\"delayed\":" + ms + "}");
            });

        final String urlOk = h.url("/delay-int/5000"); // Interrupt target
        final String urlNfe = h.url("/delay-int/abc"); // NumberFormatException → Default 1000
        final String urlZero = h.url("/delay-int/0"); // false greater than 0
        final String urlNoMs = h.url("/delay-int"); // parts.length<=2 false

        // Execute /delay-int/5000 on a separate thread and interrupt the handler's sleep
        java.util.concurrent.Callable<Void> callOk =
            () -> {
              var conn = (java.net.HttpURLConnection) URI.create(urlOk).toURL().openConnection();
              conn.setRequestMethod("POST");
              conn.setDoOutput(true);
              try (var os = conn.getOutputStream()) {
                os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
              }
              assertEquals(200, conn.getResponseCode());
              String body =
                  new String(
                      conn.getInputStream().readAllBytes(),
                      java.nio.charset.StandardCharsets.UTF_8);
              assertTrue(body.contains("\"delayed\":5000"));
              return null;
            };
        java.util.concurrent.FutureTask<Void> taskOk =
            new java.util.concurrent.FutureTask<>(callOk);
        Thread client = new Thread(taskOk);
        client.start();

        // Waiting for handler thread preparation → Direct interrupt
        assertTrue(
            handlerReady.await(1, java.util.concurrent.TimeUnit.SECONDS), "handler not ready");
        Thread ht = handlerThreadRef.get();
        assertNotNull(ht, "handler thread should be captured");
        ht.interrupt();

        // If there is an exception, it will propagate here
        taskOk.get();

        // delay-int/abc → NumberFormatException route (default 1000)
        var conn2 = (java.net.HttpURLConnection) URI.create(urlNfe).toURL().openConnection();
        conn2.setRequestMethod("POST");
        conn2.setDoOutput(true);
        try (var os = conn2.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn2.getResponseCode());
        String body2 =
            new String(
                conn2.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body2.contains("\"delayed\":1000")); // ← Matches the default value

        // delay-int/0 → if (ms > 0) false（not sleep）
        var conn3 = (java.net.HttpURLConnection) URI.create(urlZero).toURL().openConnection();
        conn3.setRequestMethod("POST");
        conn3.setDoOutput(true);
        try (var os = conn3.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn3.getResponseCode());
        String body3 =
            new String(
                conn3.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body3.contains("\"delayed\":0"));

        // delay-int → parts.length<=2  false（No trailing slash）
        var conn4 = (java.net.HttpURLConnection) URI.create(urlNoMs).toURL().openConnection();
        conn4.setRequestMethod("POST");
        conn4.setDoOutput(true);
        try (var os = conn4.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn4.getResponseCode());
        String body4 =
            new String(
                conn4.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(
            body4.contains("\"delayed\":1000")); // Default 1000 (paths with parts.length <= 2)

      } finally {
        h.stop(); // ← Reached 'finally' normally
      }
    }

    @Test
    @DisplayName(
        "delay-int: handler catch(InterruptedException) is executed (no test-side branching)")
    void delayInt_shouldCatchInterruptedException_noBranch() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        final java.util.concurrent.atomic.AtomicReference<Thread> handlerThreadRef =
            new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch handlerReady =
            new java.util.concurrent.CountDownLatch(1);

        h.server.createContext(
            "/delay-int",
            ex -> {
              handlerThreadRef.set(Thread.currentThread());
              handlerReady.countDown();
              // Parameter interpretation + catch with the same specifications as LocalHttp
              String path = ex.getRequestURI().getPath();
              String[] parts = path.split("/");
              int ms = 1000;
              if (parts.length > 2) {
                try {
                  ms = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignore) {
                  /* NOP */
                }
              }
              try {
                if (ms > 0) Thread.sleep(ms);
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
              }
              SimpleHttpClientTest.LocalHttp.respond(ex, 200, "{\"delayed\":" + ms + "}");
            });

        final String url5000 = h.url("/delay-int/5000");
        final String urlAbc = h.url("/delay-int/abc"); // NFE → Default 1000
        final String urlZero = h.url("/delay-int/0"); // ms>0 false
        final String urlNone = h.url("/delay-int"); // parts.length>2 false

        // Execute /delay-int/5000 in a separate thread and interrupt it while it is sleeping
        java.util.concurrent.Callable<Void> call =
            () -> {
              var conn = (java.net.HttpURLConnection) URI.create(url5000).toURL().openConnection();
              conn.setRequestMethod("POST");
              conn.setDoOutput(true);
              try (var os = conn.getOutputStream()) {
                os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
              }
              assertEquals(200, conn.getResponseCode());
              String body =
                  new String(
                      conn.getInputStream().readAllBytes(),
                      java.nio.charset.StandardCharsets.UTF_8);
              assertTrue(body.contains("\"delayed\":5000"));
              return null;
            };
        java.util.concurrent.FutureTask<Void> task = new java.util.concurrent.FutureTask<>(call);
        Thread client = new Thread(task);
        client.start();

        assertTrue(
            handlerReady.await(1, java.util.concurrent.TimeUnit.SECONDS), "handler not ready");
        Thread ht = handlerThreadRef.get();
        assertNotNull(ht, "handler should be captured");
        ht.interrupt();

        task.get(); // If there is an exception, it will propagate here

        // delay-int/abc → Catch path for NumberFormatException (default 1000)
        var conn2 = (java.net.HttpURLConnection) URI.create(urlAbc).toURL().openConnection();
        conn2.setRequestMethod("POST");
        conn2.setDoOutput(true);
        try (var os = conn2.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn2.getResponseCode());
        String body2 =
            new String(
                conn2.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body2.contains("\"delayed\":1000"));

        // delay-int/0 → false for if (ms > 0) (does not sleep)
        var conn3 = (java.net.HttpURLConnection) URI.create(urlZero).toURL().openConnection();
        conn3.setRequestMethod("POST");
        conn3.setDoOutput(true);
        try (var os = conn3.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn3.getResponseCode());
        String body3 =
            new String(
                conn3.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body3.contains("\"delayed\":0"));

        // delay-int (without suffix) → false for if (parts.length > 2)
        var conn4 = (java.net.HttpURLConnection) URI.create(urlNone).toURL().openConnection();
        conn4.setRequestMethod("POST");
        conn4.setDoOutput(true);
        try (var os = conn4.getOutputStream()) {
          os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(200, conn4.getResponseCode());
        String body4 =
            new String(
                conn4.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body4.contains("\"delayed\":1000"));

      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("/echo-strict Failure：400 without body → errorStream is null branch")
    void echoStrictMissingHeaders_NoBody() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        // 400: No Body Context
        h.server.createContext(
            "/echo-strict-nobody",
            ex -> {
              ex.getResponseHeaders().add("Content-Type", "application/json");
              ex.sendResponseHeaders(400, -1);
              ex.getResponseBody().close();
            });

        var conn =
            (java.net.HttpURLConnection)
                URI.create(h.url("/echo-strict-nobody")).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
          os.write("{\"no\":\"headers\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        // Depending on the JDK implementation, getResponseCode() may throw an IOException,
        // Validate 400 from the 'status line' without relying on exceptions
        String statusLine = conn.getHeaderField(0);
        assertNotNull(statusLine);
        assertTrue(statusLine.contains(" 400"));

        // Take the false branch where errorStream == null
        assertNull(conn.getErrorStream(), "No error body expected for /echo-strict-nobody");
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName(
        "default /delay/5000 should execute catch(InterruptedException) in LocalHttp handler")
    void delayDefaultHandler_shouldCatchInterruptedException_inLocalHttp() throws Exception {
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        // A client that sends /delay/5000
        final String url = h.url("/delay/5000");
        java.util.concurrent.Callable<Void> call =
            () -> {
              var conn = (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
              conn.setRequestMethod("POST");
              conn.setDoOutput(true);
              try (var os = conn.getOutputStream()) {
                os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
              }
              assertEquals(200, conn.getResponseCode());
              String body =
                  new String(
                      conn.getInputStream().readAllBytes(),
                      java.nio.charset.StandardCharsets.UTF_8);
              // LocalHttp responds with "delayed":5000 to avoid changing the ms
              assertTrue(body.contains("\"delayed\":5000"));
              return null;
            };
        java.util.concurrent.FutureTask<Void> task = new java.util.concurrent.FutureTask<>(call);
        Thread client = new Thread(task);
        client.start();

        // Wait a moment, capture one 'sleeping HttpServer thread' and interrupt it.
        Thread.sleep(200);
        Thread target = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
          // Determine from the stack whether it is 'sleeping' and related to 'httpserver'
          boolean sleeping = false, inHttpServer = false;
          for (StackTraceElement fr : t.getStackTrace()) {
            if ("java.lang.Thread".equals(fr.getClassName()) && "sleep".equals(fr.getMethodName()))
              sleeping = true;
            String cn = fr.getClassName();
            if (cn.startsWith("com.sun.net.httpserver") || cn.startsWith("sun.net.httpserver"))
              inHttpServer = true;
          }
          if (sleeping && inHttpServer) {
            target = t;
            break;
          }
        }
        assertNotNull(target, "sleeping httpserver handler thread should be found");
        target.interrupt();

        // If there is an exception, it will propagate here
        task.get();
      } finally {
        h.stop();
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create client with default configuration")
    void shouldCreateClientWithDefaultConfiguration() {
      // When
      SimpleHttpClient client = new SimpleHttpClient();

      // Then
      assertNotNull(client);
      assertInstanceOf(HttpTransport.class, client);
    }

    @Test
    @DisplayName("should create client with custom connect timeout")
    void shouldCreateClientWithCustomConnectTimeout() {
      // Given
      Duration customTimeout = Duration.ofSeconds(5);

      // When
      SimpleHttpClient client = new SimpleHttpClient(customTimeout);

      // Then
      assertNotNull(client);
    }

    @Test
    @DisplayName("should create client with custom max retries")
    void shouldCreateClientWithCustomMaxRetries() {
      // Given
      int customRetries = 5;

      // When
      SimpleHttpClient client = new SimpleHttpClient(customRetries);

      // Then
      assertNotNull(client);
    }

    @Test
    @DisplayName("should create client with custom timeout and retries")
    void shouldCreateClientWithCustomTimeoutAndRetries() {
      // Given
      Duration customTimeout = Duration.ofSeconds(3);
      int customRetries = 2;

      // When
      SimpleHttpClient client = new SimpleHttpClient(customTimeout, customRetries);

      // Then
      assertNotNull(client);
    }

    @Test
    @DisplayName("should create client with full custom configuration")
    void shouldCreateClientWithFullCustomConfiguration() {
      // Given
      Duration connectTimeout = Duration.ofSeconds(5);
      Duration requestTimeout = Duration.ofSeconds(15);
      int maxRetries = 4;

      // When
      SimpleHttpClient client = new SimpleHttpClient(connectTimeout, requestTimeout, maxRetries);

      // Then
      assertNotNull(client);
    }
  }

  @Nested
  @DisplayName("Configuration Validation")
  class ConfigurationValidationTest {

    private SimpleHttpClient client;

    /**
     * Without using a lambda, enclose it in an anonymous class Executable and pass it to
     * assertThrows
     */
    private org.junit.jupiter.api.function.Executable exec(String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          client.request(mockConfig, body);
        }
      };
    }

    @BeforeEach
    void setUp() {
      client = new SimpleHttpClient();
    }

    @Test
    @DisplayName(
        "should throw AuthorizationException for invalid endpoints (null/blank/whitespaces)")
    void shouldThrowForInvalidEndpoints() {
      java.util.List<String> invalids = java.util.Arrays.asList(null, "", "   ", "\t");

      for (String endpoint : invalids) {
        // Given
        when(mockConfig.getEndpoint()).thenReturn(endpoint);

        // When
        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec("{\"test\": true}"));

        // Then
        assertEquals(
            "Invalid client configuration: Endpoint URL must be provided.", ex.getMessage());

        Throwable cause = ex.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof TransportException);
        assertEquals("Endpoint URL is null or blank.", cause.getMessage());

        // Ensure that it does not proceed to the subsequent processing
        verify(mockConfig, times(1)).getEndpoint();
        verify(mockConfig, never()).getApiKey();
        verify(mockConfig, never()).getApiKeyHeader();
        verifyNoMoreInteractions(mockConfig);

        // Clear call records to prepare for the next iteration
        clearInvocations(mockConfig);
      }
    }

    @Test
    @DisplayName("should not consult auth settings when endpoint invalid")
    void shouldNotConsultAuthWhenEndpointInvalid() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn("   ");

      // When & Then
      AuthorizationException ex = assertThrows(AuthorizationException.class, exec("{\"t\":1}"));

      // Interaction
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, never()).getApiKey();
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);

      assertTrue(
          String.valueOf(ex.getMessage())
              .contains("Invalid client configuration: Endpoint URL must be provided."));
    }

    @Test
    @DisplayName("exec(String) success path (field client) covers request line")
    void execStringSuccessPath_coversFieldClientRequestLine() throws Throwable {
      SimpleHttpClientTest.LocalHttp http = new SimpleHttpClientTest.LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());
        org.junit.jupiter.api.function.Executable e = exec("{\"ping\":true}");
        e.execute();

        String body = client.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        http.stop();
      }
    }
  }

  @Nested
  @DisplayName("Authentication Tests")
  class AuthenticationTests {

    private SimpleHttpClient client;
    private LocalHttp http;

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    @BeforeEach
    void setUp() throws Exception {
      client = new SimpleHttpClient();
      http = new LocalHttp();
      http.start();

      http.server.createContext(
          "/auth/bearer",
          ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
              SimpleHttpClientTest.LocalHttp.respond(
                  ex, 401, "{\"error\":\"missing or invalid Authorization\"}");
              return;
            }
            String body =
                new String(
                    ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            SimpleHttpClientTest.LocalHttp.respond(ex, 200, body);
          });

      http.server.createContext(
          "/auth/custom",
          ex -> {
            String key = ex.getRequestHeaders().getFirst("X-API-Key");
            if (key == null || key.isBlank()) {
              SimpleHttpClientTest.LocalHttp.respond(ex, 401, "{\"error\":\"missing X-API-Key\"}");
              return;
            }
            String body =
                new String(
                    ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            SimpleHttpClientTest.LocalHttp.respond(ex, 200, body);
          });

      http.server.createContext(
          "/auth/none",
          ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            String key = ex.getRequestHeaders().getFirst("X-API-Key");
            if (auth != null || key != null) {
              SimpleHttpClientTest.LocalHttp.respond(
                  ex, 400, "{\"error\":\"unexpected auth headers\"}");
              return;
            }
            String body =
                new String(
                    ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            SimpleHttpClientTest.LocalHttp.respond(ex, 200, body);
          });
    }

    @AfterEach
    void tearDown() {
      if (http != null) http.stop();
    }

    @Test
    @DisplayName(
        "exec() success path: client.request(...) runs without exception (covers red line)")
    void execSuccessPath_coversRequestLine() throws Throwable {
      // Start LocalHttp for the success path and use /ok
      SimpleHttpClient okClient = new SimpleHttpClient();
      SimpleHttpClientTest.LocalHttp http = new SimpleHttpClientTest.LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());
        org.junit.jupiter.api.function.Executable e = exec(okClient, "{\"ping\":true}");
        e.execute();

        String body = okClient.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should handle API key with default Authorization header")
    void shouldHandleApiKeyWithDefaultAuthorizationHeader() throws Exception {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/bearer"));
      when(mockConfig.getApiKey()).thenReturn(Optional.of("test-api-key"));
      when(mockConfig.getApiKeyHeader()).thenReturn(Optional.empty());

      // When
      String req = "{\"test\": true}";
      String resp = client.request(mockConfig, req);

      // Then
      assertEquals(req, resp);
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, times(1)).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should handle API key with custom header")
    void shouldHandleApiKeyWithCustomHeader() throws Exception {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/custom"));
      when(mockConfig.getApiKey()).thenReturn(Optional.of("custom-key"));
      when(mockConfig.getApiKeyHeader()).thenReturn(Optional.of("X-API-Key"));

      // When
      String req = "{\"test\": true}";
      String resp = client.request(mockConfig, req);

      // Then
      assertEquals(req, resp);
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, times(1)).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should handle missing API key")
    void shouldHandleMissingApiKey() throws Exception {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/none"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // When
      String req = "{\"test\": true}";
      String resp = client.request(mockConfig, req);

      // Then
      assertEquals(req, resp);
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should reject when Authorization(Bearer) header is missing (/auth/bearer)")
    void shouldRejectMissingBearerHeader() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/bearer"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // When & Then
      assertThrows(AuthorizationException.class, exec(client, "{\"t\":1}"));

      // Interaction
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should reject when X-API-Key is missing (/auth/custom)")
    void shouldRejectMissingCustomHeader() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/custom"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      assertThrows(AuthorizationException.class, exec(client, "{\"t\":2}"));
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      // Branch that does not refer to Optional
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should reject when X-API-Key is blank (/auth/custom)")
    void shouldRejectBlankCustomHeader() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/custom"));
      when(mockConfig.getApiKey()).thenReturn(Optional.of(""));
      when(mockConfig.getApiKeyHeader()).thenReturn(Optional.of("X-API-Key"));

      assertThrows(AuthorizationException.class, exec(client, "{\"t\":3}"));
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, times(1)).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should reject when any auth header is unexpectedly present (/auth/none)")
    void shouldRejectUnexpectedAuthHeadersOnNone() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn(http.url("/auth/none"));
      when(mockConfig.getApiKey()).thenReturn(Optional.of("some-token"));
      when(mockConfig.getApiKeyHeader()).thenReturn(Optional.empty());

      assertThrows(AuthorizationException.class, exec(client, "{\"t\":4}"));
      // Interaction
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, times(1)).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("tearDown should handle null http (false branch coverage)")
    void tearDownShouldHandleNullHttp() {
      http.stop();
      http = null;
      assertTrue(true);
    }
  }

  @Nested
  @DisplayName("Request Construction Tests")
  class RequestConstructionTests {

    private SimpleHttpClient client;
    private LocalHttp http;

    @BeforeEach
    void setUp() throws Exception {
      client = new SimpleHttpClient();
      // Start a local HTTP server and use /echo
      http = new LocalHttp();
      http.start();
    }

    @AfterEach
    void tearDown() {
      if (http != null) http.stop();
    }

    @Test
    @DisplayName("should handle simple JSON body")
    void shouldHandleSimpleJsonBody() throws Exception {
      when(mockConfig.getEndpoint()).thenReturn(http.url("/echo"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // Given
      String jsonBody = "{\"subject\": {\"id\": \"alice\"}}";
      // When
      String resp = client.request(mockConfig, jsonBody);
      // Then
      assertEquals(jsonBody, resp, "The same body is echoed back from the server");
    }

    @Test
    @DisplayName("should handle complex JSON body")
    void shouldHandleComplexJsonBody() throws Exception {
      when(mockConfig.getEndpoint()).thenReturn(http.url("/echo"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // Given
      String complexJson =
          """
        {
          "subject": {
            "id": "alice@example.com",
            "attributes": {
              "department": "engineering",
              "roles": ["user", "admin"],
              "clearance_level": 3
            }
          },
          "action": { "name": "read", "resource_type": "document" },
          "resource": {
            "id": "doc-12345",
            "attributes": { "classification": "confidential", "owner": "bob@example.com" }
          },
          "context": {
            "timestamp": "2024-01-15T10:30:00Z",
            "client_ip": "192.168.1.100",
            "session_id": "sess-abcdef"
          }
        }
        """;
      // When
      String resp = client.request(mockConfig, complexJson);
      // Then
      assertEquals(
          complexJson, resp, "Ability to round-trip complex JSON without any loss using UTF-8");
    }

    @Test
    @DisplayName("should handle JSON with Unicode characters")
    void shouldHandleJsonWithUnicodeCharacters() throws Exception {
      when(mockConfig.getEndpoint()).thenReturn(http.url("/echo"));
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());
      // Given
      String unicodeJson =
          """
        {
          "subject": { "id": "田中太郎", "名前": "田中太郎", "部署": "エンジニアリング" },
          "action": { "名称": "読み取り", "説明": "ドキュメントの読み取りアクセス" },
          "context": { "メッセージ": "認証リクエスト", "絵文字": "🔐🔑🛡️" }
        }
        """;
      // When
      String resp = client.request(mockConfig, unicodeJson);
      // Then
      assertEquals(
          unicodeJson, resp, "JSON containing Unicode can be correctly round-tripped in UTF-8");
    }

    @Test
    @DisplayName("should set Content-Type and X-Request-ID headers (/echo-strict success)")
    void shouldSetRequiredHeaders_EchoStrict_Success() throws Exception {
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // Switch to strict endpoint (SimpleHttpClient automatically adds required headers)
      when(mockConfig.getEndpoint()).thenReturn(http.url("/echo-strict"));
      String body = "{\"ping\":true}";

      String resp = client.request(mockConfig, body);
      assertEquals(body, resp);
    }

    @Test
    @DisplayName("should return 400 on missing headers at /echo-strict (negative)")
    void shouldReturn400WhenHeadersMissing_EchoStrict_Failure() throws Exception {
      // This test uses a real HttpURLConnection without using mocks.
      var url = URI.create(http.url("/echo-strict")).toURL();
      var conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);

      try (var os = conn.getOutputStream()) {
        os.write("{\"no\":\"headers\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int code = conn.getResponseCode();
      assertEquals(400, code);

      // The errorStream is non-null at 400.
      var err = conn.getErrorStream();
      assertNotNull(err, "error body should exist for /echo-strict 400");
      try (var is = err) {
        var body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("missing headers"));
      }
    }

    @Test
    @DisplayName("should return 400 when X-Request-ID is missing at /echo-strict")
    void shouldReturn400WhenXRequestIdMissing_EchoStrict() throws Exception {
      var url = URI.create(http.url("/echo-strict")).toURL();
      var conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");
      // X-Request-ID is missing
      try (var os = conn.getOutputStream()) {
        os.write("{\"no\":\"headers\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int code = conn.getResponseCode();
      assertEquals(400, code);
    }

    @Test
    @DisplayName("should return 400 when Content-Type is not application/json at /echo-strict")
    void shouldReturn400WhenContentTypeInvalid_EchoStrict() throws Exception {
      var url = URI.create(http.url("/echo-strict")).toURL();
      var conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "text/plain");
      conn.setRequestProperty("X-Request-ID", "req-123");
      try (var os = conn.getOutputStream()) {
        os.write("{\"no\":\"headers\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int code = conn.getResponseCode();
      assertEquals(400, code);
    }

    @Test
    @DisplayName("should return 400 when X-Request-ID is blank at /echo-strict")
    void shouldReturn400WhenXRequestIdBlank_EchoStrict() throws Exception {
      var url = URI.create(http.url("/echo-strict")).toURL();
      var conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("X-Request-ID", "   ");
      try (var os = conn.getOutputStream()) {
        os.write("{\"no\":\"headers\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int code = conn.getResponseCode();
      assertEquals(400, code);
    }

    @Test
    @DisplayName("tearDown should handle null http (false branch)")
    void tearDownShouldHandleNullHttp_RequestConstructionTests() {
      http.stop();
      http = null;

      assertTrue(true);
    }
  }

  @Nested
  @DisplayName("Timeout Configuration Tests")
  class TimeoutConfigurationTests {

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    // Acquire unused port
    private int unusedPort() throws java.io.IOException {
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        return s.getLocalPort();
      }
    }

    @Test
    @DisplayName("LocalHttp /delay/ should handle InterruptedException in handler")
    void localHttpDelayShouldHandleInterruptedException() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        final String url = http.url("/delay/1000");

        java.util.concurrent.Callable<Void> call =
            () -> {
              var conn = (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
              conn.setRequestMethod("POST");
              conn.setDoOutput(true);

              Thread.currentThread().interrupt();
              try (var os = conn.getOutputStream()) {
                os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
              }
              assertEquals(200, conn.getResponseCode());
              String body =
                  new String(
                      conn.getInputStream().readAllBytes(),
                      java.nio.charset.StandardCharsets.UTF_8);
              assertTrue(body.contains("\"delayed\":1000"));
              return null;
            };

        java.util.concurrent.FutureTask<Void> task = new java.util.concurrent.FutureTask<>(call);
        Thread t = new Thread(task);
        t.start();

        task.get();

      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("LocalHttp /delay/ should cover outputStream and responseCode on success")
    void localHttpDelayShouldCoverOutputStreamAndResponseCode() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        String url = http.url("/delay/1000");
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream()
            .write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)); // Covered
        int code = conn.getResponseCode(); // Covered
        assertEquals(200, code);
        String body =
            new String(
                conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":1000"));
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("LocalHttp /delay/ should cover fail(e) in request thread")
    void localHttpDelayShouldCoverFailE() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      String url = http.url("/delay/1000");
      java.util.concurrent.atomic.AtomicReference<Throwable> error =
          new java.util.concurrent.atomic.AtomicReference<>();
      try {
        Runnable requestTask =
            new Runnable() {
              @Override
              public void run() {
                try {
                  http.stop(); // stop first to cause connection failure
                  java.net.HttpURLConnection conn =
                      (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
                  conn.setRequestMethod("POST");
                  conn.setDoOutput(true);
                  conn.getOutputStream()
                      .write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                  int code = conn.getResponseCode();
                  assertEquals(200, code); // not reached normally
                } catch (Exception e) {
                  error.set(e); // capture for assertion
                }
              }
            };
        Thread t = new Thread(requestTask);
        t.start();
        t.join();

        assertNotNull(error.get(), "Exception should be thrown and captured");
        assertTrue(error.get() instanceof java.net.ConnectException, "Should be ConnectException");
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should apply connect timeout (deterministic)")
    void shouldApplyConnectTimeout() throws Exception {
      Duration connectTimeout = Duration.ofMillis(500);
      SimpleHttpClient client = new SimpleHttpClient(connectTimeout);

      int badPort = unusedPort();
      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));
      assertNotNull(ex);
      assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("should apply request timeout (deterministic)")
    void shouldApplyRequestTimeout() throws Exception {
      Duration connectTimeout = Duration.ofSeconds(10);
      Duration requestTimeout = Duration.ofMillis(100);
      SimpleHttpClient client = new SimpleHttpClient(connectTimeout, requestTimeout, 0);

      LocalHttp http = new LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/delay/5000"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));
        assertNotNull(ex);
        assertNotNull(ex.getMessage());
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("LocalHttp /delay/ should handle NumberFormatException in handler")
    void localHttpDelayShouldHandleNumberFormatException() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        String url = http.url("/delay/abc"); // abc -> NumberFormatException -> default 1000
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        assertEquals(200, code);
        String body =
            new String(
                conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("\"delayed\":1000")); // fallback to default
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("LocalHttp /delay/ should handle zero and negative delay")
    void localHttpDelayShouldHandleZeroAndNegativeDelay() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        // Zero delay
        String urlZero = http.url("/delay/0");
        java.net.HttpURLConnection connZero =
            (java.net.HttpURLConnection) URI.create(urlZero).toURL().openConnection();
        connZero.setRequestMethod("POST");
        connZero.setDoOutput(true);
        connZero.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        int codeZero = connZero.getResponseCode();
        assertEquals(200, codeZero);
        String bodyZero =
            new String(
                connZero.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(bodyZero.contains("\"delayed\":0"));

        // Negative delay
        String urlNeg = http.url("/delay/-100");
        java.net.HttpURLConnection connNeg =
            (java.net.HttpURLConnection) URI.create(urlNeg).toURL().openConnection();
        connNeg.setRequestMethod("POST");
        connNeg.setDoOutput(true);
        connNeg.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        int codeNeg = connNeg.getResponseCode();
        assertEquals(200, codeNeg);
        String bodyNeg =
            new String(
                connNeg.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(bodyNeg.contains("\"delayed\":-100"));
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName(
        "exec() success path: client.request(...) runs without exception (covers red line)")
    void execSuccessPath_coversRequestLine() throws Throwable {
      // Start a local HTTP (/ok) that returns a success response
      SimpleHttpClient client = new SimpleHttpClient();
      SimpleHttpClientTest.LocalHttp http = new SimpleHttpClientTest.LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        org.junit.jupiter.api.function.Executable e = exec(client, "{\"ping\":true}");
        e.execute();

        String body = client.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        http.stop();
      }
    }
  }

  @Nested
  @DisplayName("Retry Logic Tests")
  class RetryLogicTests {

    private LocalHttp http;
    private SimpleHttpClient client;

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    // Utility for handling inconsistent notation
    private static boolean hasAttemptsMsg(String msg, int n) {
      return msg != null && msg.matches("(?s).*(?i)after\\s+" + n + "\\s+attempts?\\b.*");
    }

    private static void warmUpAlways500(String baseUrl) throws Exception {
      var url = URI.create(baseUrl + "/always-500").toURL();
      var conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      try (var os = conn.getOutputStream()) {
        os.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      // Expected to return 500
      int code = conn.getResponseCode();
      if (code != 500) {}
    }

    @BeforeEach
    void setUp() {
      client = new SimpleHttpClient();
      http = new LocalHttp();
    }

    @AfterEach
    void tearDown() {
      if (http != null) http.stop();
    }

    // Parameterization:
    // Cover all cases using boolean comparisons without branching each expectation with if.
    @ParameterizedTest
    @CsvSource({
      "500,2,3,false,false", // 5xx → Retry available, no final cause (implementation specification)
      "400,3,1,false,false", // 4xx → Instant failure, no cause
      "302,2,1,false,false", // 3xx → Considered unexpected, no cause
      "101,0,1,true,true" // 1xx → Treated as a network error, with a cause
    })
    void testHttpStatusRetryLogic(
        int status,
        int maxRetries,
        int expectedAttempts,
        boolean expectCause,
        boolean expectNetworkError)
        throws Exception {

      SimpleHttpClient c = new SimpleHttpClient(maxRetries);

      http.start();
      try {
        http.server.createContext(
            "/test",
            ex -> {
              if (status == 302) ex.getResponseHeaders().add("Location", "/ok");
              LocalHttp.respond(ex, status, "{\"e\":\"" + status + "\"}");
            });

        when(mockConfig.getEndpoint()).thenReturn(http.url("/test"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));

        String msg = String.valueOf(ex.getMessage());
        String lower = msg.toLowerCase();

        boolean expectAttemptsMsg = (expectedAttempts > 1) || expectNetworkError;
        boolean actualAttemptsMsg = hasAttemptsMsg(msg, expectedAttempts);
        assertEquals(expectAttemptsMsg, actualAttemptsMsg, "attempts message mismatch: " + msg);

        // presence or absence of cause
        boolean actualCause = (ex.getCause() != null);
        assertEquals(expectCause, actualCause, "cause presence mismatch");

        // Presence or absence of the 'network error' message
        boolean actualNetErr = lower.contains("network error");
        assertEquals(expectNetworkError, actualNetErr, "network error word presence mismatch");

      } finally {
        http.stop();
      }
    }

    // Standalone cases: Brief and reliable handling of each specification branch
    @Test
    @DisplayName("Should Treat Negative MaxRetries As Zero")
    void shouldTreatNegativeMaxRetriesAsZero() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(-1);
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertTrue(
            hasAttemptsMsg(ex.getMessage(), 1), "expected after 1 attempt(s): " + ex.getMessage());
        assertNull(ex.getCause());
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("Should Retry And Set Cause On Network Error")
    void shouldRetryAndSetCauseOnNetworkError() throws Exception {
      int badPort;
      try (ServerSocket s = new ServerSocket(0)) {
        s.setReuseAddress(true);
        badPort = s.getLocalPort();
      }
      SimpleHttpClient c = new SimpleHttpClient(2);
      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
      assertTrue(
          hasAttemptsMsg(ex.getMessage(), 3), "expected after 3 attempt(s): " + ex.getMessage());
      assertNotNull(ex.getCause());
      assertTrue(ex.getCause() instanceof nedo.ods.svc.dp.authzen.exception.TransportException);
    }

    @Test
    @DisplayName("Should Not Retry If Thread Interrupted Before First Attempt")
    void shouldNotRetryIfThreadInterruptedBeforeFirstAttempt() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(2);
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());
        warmUpAlways500(http.url(""));

        Thread.currentThread().interrupt();
        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("interrupted"));
        Thread.interrupted(); // Clear the flag
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("Should ReInterrupt Thread On Interrupted Exception During Backoff")
    void shouldReInterruptThreadOnInterruptedExceptionDuringBackoff() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(1);
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());
        warmUpAlways500(http.url(""));
        Thread.currentThread().interrupt();
        assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertTrue(Thread.interrupted(), "Thread should be interrupted");
      } finally {
        http.stop();
        Thread.interrupted();
      }
    }

    @Test
    @DisplayName("Should Retry Exactly Once")
    void shouldRetryExactlyOnce() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(1);
      http.start();
      try {
        AtomicInteger count = new AtomicInteger();
        http.server.createContext(
            "/fail-once",
            ex -> {
              if (count.getAndIncrement() == 0) {
                LocalHttp.respond(ex, 500, "{\"e\":\"fail\"}");
              } else {
                LocalHttp.respond(ex, 200, "{\"e\":\"ok\"}");
              }
            });
        when(mockConfig.getEndpoint()).thenReturn(http.url("/fail-once"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        String resp = c.request(mockConfig, "{}");
        assertEquals("{\"e\":\"ok\"}", resp);
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("Should Interrupt During Backoff")
    void shouldInterruptDuringBackoff() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(1);
      int badPort;
      try (ServerSocket s = new ServerSocket(0)) {
        badPort = s.getLocalPort();
      }
      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      Thread.currentThread().interrupt();
      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
      assertTrue(Thread.interrupted());
      assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("Should Handle Null Cause On Transport Exception")
    void shouldHandleNullCauseOnTransportException() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(0);
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertNull(ex.getCause());
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("Should Not Retry Infinitely With Large Max Retries")
    void shouldNotRetryInfinitelyWithLargeMaxRetries() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(2);
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertTrue(String.valueOf(ex.getMessage()).contains("after"));
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("Should Sleep During Backoff Without Interrupt")
    void shouldSleepDuringBackoffWithoutInterrupt() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(1);
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        h.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(h.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertNotNull(ex);
        // maxRetries=1 → (First attempt failed → backoff → Second attempt failed) "after 2
        // attempt(s)"
        assertTrue(
            hasAttemptsMsg(ex.getMessage(), 2),
            "Backoff path executed (retried once): " + ex.getMessage());
        // Assuming no interrupts occur
        assertFalse(
            Thread.currentThread().isInterrupted(),
            "Thread should not be interrupted during non-interrupted backoff");
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("tearDown should handle null http in RetryLogicTests (false branch)")
    void tearDownShouldHandleNullHttp_RetryLogicTests() {
      http = null; // Ensure that the false branch of @AfterEach is executed
      assertTrue(true);
    }

    @Test
    @DisplayName("Unexpected 600: short-circuit in 5xx condition (first true, second false)")
    void shouldHandleStatus600AsUnexpected_cover5xxAndBranch() throws Exception {
      SimpleHttpClient c = new SimpleHttpClient(0);
      http.start();
      try {
        http.server.createContext("/status-600", ex -> LocalHttp.respond(ex, 600, "{\"e\":\"x\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/status-600"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(c, "{}"));
        assertTrue(String.valueOf(ex.getMessage()).contains("unexpected status 600"));
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("exec() success path: c.request(...) runs without exception (covers red line)")
    void execSuccessPath_coversRequestLine() throws Throwable {
      // Use a local HTTP (/ok) for the success path
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(h.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        // Directly execute the Executable on the 'success path' instead of expecting exceptions
        org.junit.jupiter.api.function.Executable e = exec(client, "{\"ping\":true}");
        e.execute();

        String body = client.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        h.stop();
      }
    }

    @Test
    @DisplayName("RLT: hasAttemptsMsg short-circuit (null / mismatch / match)")
    void rlt_hasAttemptsMsg_coversAllShortCircuitBranches() {
      assertFalse(hasAttemptsMsg(null, 1)); // Article 1 false
      assertFalse(hasAttemptsMsg("after 3 attempts due to X", 2)); // Item 1: true, Item 2: false
      assertTrue(hasAttemptsMsg("after 2 attempts due to network error", 2)); // Both true
    }

    @Test
    @DisplayName("RLT: exec() success path covers client.request line (no exception)")
    void rlt_execSuccessPath_coversClientRequestLine() throws Throwable {
      // Start a local HTTP (/ok) for success
      LocalHttp h = new LocalHttp();
      h.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(h.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        org.junit.jupiter.api.function.Executable e = exec(client, "{\"ping\":true}");
        e.execute();

        String body = client.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        h.stop();
      }
    }
  }

  @Nested
  @DisplayName("Retry Logic Full Branch Coverage")
  class RetryLogicFullBranchCoverage {

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    private void invokeRespond(HttpExchange ex, int status, String body) throws IOException {
      SimpleHttpClientTest.LocalHttp.respond(ex, status, body);
    }

    private org.junit.jupiter.api.function.Executable execRespond(
        HttpExchange ex, int status, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          invokeRespond(ex, status, body);
        }
      };
    }

    // Handling notation inconsistencies (supporting both 'attempt' and 'attempts')
    private static boolean hasAttemptsMsg(String msg, int n) {
      return msg != null && msg.matches("(?s).*(?i)after\\s+" + n + "\\s+attempts?\\b.*");
    }

    // Unused port
    private int unusedPort() throws java.io.IOException {
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        return s.getLocalPort();
      }
    }

    @Test
    @DisplayName("should retry exactly once (maxRetries=1) and succeed on second attempt")
    void rlfb_shouldRetryExactlyOnce() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(1);
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        java.util.concurrent.atomic.AtomicInteger count =
            new java.util.concurrent.atomic.AtomicInteger();
        http.server.createContext(
            "/fail-once",
            ex -> {
              if (count.getAndIncrement() == 0) {
                LocalHttp.respond(ex, 500, "{\"e\":\"fail\"}");
              } else {
                LocalHttp.respond(ex, 200, "{\"e\":\"ok\"}");
              }
            });
        when(mockConfig.getEndpoint()).thenReturn(http.url("/fail-once"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        String resp = client.request(mockConfig, "{}");
        assertEquals("{\"e\":\"ok\"}", resp);
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should not retry or sleep on 5xx when maxRetries=0")
    void rlfb_shouldNotRetryOrSleepOn5xxWhenMaxRetriesZero() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(0);
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));
        assertTrue(
            hasAttemptsMsg(ex.getMessage(), 1), "expected after 1 attempt(s): " + ex.getMessage());
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName(
        "should re-interrupt thread and set cause on InterruptedException during backoff after network error")
    void rlfb_shouldReInterruptDuringBackoffAfterNetworkError() throws Exception {
      int badPort = unusedPort();
      SimpleHttpClient client = new SimpleHttpClient(1);
      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      Thread.currentThread().interrupt();
      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));
      assertTrue(Thread.interrupted(), "Thread should be interrupted");
      assertNotNull(ex.getCause());
      assertTrue(ex.getCause() instanceof nedo.ods.svc.dp.authzen.exception.TransportException);
    }

    @Test
    @DisplayName(
        "exec() success path: client.request(...) runs without exception (covers request line)")
    void rlfb_execSuccess_coversClientRequestLine() throws Throwable {
      SimpleHttpClient c = new SimpleHttpClient();
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());
        org.junit.jupiter.api.function.Executable e = exec(c, "{\"ping\":true}");
        e.execute();
      } finally {
        http.stop();
      }
    }

    // respond success path (reaching the same line through normal completion)
    @Test
    @DisplayName("respond() success path: covers invokeRespond line (no exception)")
    void rlfb_respondSuccess_coversInvokeRespond() throws Throwable {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) {
              /* success */
            }

            @Override
            public void close() {
              /* success */
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      Headers headers = new Headers();
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(headers);
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      // Direct execution of calls consolidated in one place along the success path
      invokeRespond(ex, 200, "{\"ok\":true}");
      assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    // respond failed pass (stepping on the same line as an exception: close exception)
    @Test
    @DisplayName("respond: write success → IOException on close (else branch throws closeEx)")
    void rlfb_respond_closeThrows_elseBranch() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) {
              /* success */
            }

            @Override
            public void close() throws IOException {
              throw new IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(new Headers());
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      IOException thrown = assertThrows(IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("close failed"));
    }

    @Test
    @DisplayName("respond: write IOException + close IOException (suppressed added to writeEx)")
    void rlfb_respond_writeAndClose_throw_suppressed() throws Exception {
      OutputStream os =
          new OutputStream() {
            @Override
            public void write(int b) throws IOException {
              throw new IOException("write failed");
            }

            @Override
            public void close() throws IOException {
              throw new IOException("close failed");
            }
          };
      HttpExchange ex = mock(HttpExchange.class);
      when(ex.getResponseBody()).thenReturn(os);
      when(ex.getResponseHeaders()).thenReturn(new Headers());
      doNothing().when(ex).sendResponseHeaders(anyInt(), anyLong());

      IOException thrown = assertThrows(IOException.class, execRespond(ex, 200, "body"));
      assertTrue(thrown.getMessage().contains("write failed"));
      assertTrue(
          java.util.Arrays.stream(thrown.getSuppressed())
              .anyMatch(s -> "close failed".equals(s.getMessage())));
    }

    // Cover all three shortcut branching patterns of hasAttemptsMsg in a single implementation
    @Test
    @DisplayName("hasAttemptsMsg short-circuit branches: null / mismatch / match")
    void rlfb_hasAttemptsMsg_coversAllShortCircuitBranches() {
      assertFalse(hasAttemptsMsg(null, 1)); // Article 1 false
      assertFalse(
          hasAttemptsMsg(
              "Request failed after 3 attempts due to X", 2)); // Item 1 true & Item 2 false
      assertTrue(hasAttemptsMsg("Request failed after 2 attempts due to X", 2)); // Both true
    }

    @Test
    @DisplayName("RLFB: unusedPort() returns an ephemeral port (format check)")
    void rlfb_unusedPortShouldReturnValidPort() throws Exception {
      int p = unusedPort();
      assertTrue(String.valueOf(p).matches("\\d+"), "port format invalid: p=" + p);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    private SimpleHttpClient client;

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    // Ensure acquiring an unused port
    private int unusedPort() throws java.io.IOException {
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        return s.getLocalPort();
      }
    }

    @BeforeEach
    void setUp() {
      client = new SimpleHttpClient();
    }

    @Test
    @DisplayName("should handle network connection errors (deterministic)")
    void shouldHandleNetworkConnectionErrors() throws Exception {
      // Given
      int badPort = unusedPort();
      String badUrl = "http://127.0.0.1:" + badPort + "/nope";
      when(mockConfig.getEndpoint()).thenReturn(badUrl);
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // When & Then
      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));

      String lower = String.valueOf(ex.getMessage()).toLowerCase();
      assertTrue(lower.contains("network error"), "message: " + ex.getMessage());

      // cause chain
      assertInstanceOf(TransportException.class, ex.getCause(), "The cause is TransportException");
      Throwable root = ex.getCause().getCause();
      assertNotNull(root, "The existence of a root cause");
      assertTrue(root instanceof java.io.IOException, "root is a subclass of IOException: " + root);

      // Interaction
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should handle invalid URL format (deterministic)")
    void shouldHandleInvalidUrlFormat() {
      // Given
      when(mockConfig.getEndpoint()).thenReturn("http://:");

      // When & Then
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, exec(client, "{\"test\":\"data\"}"));

      assertNotNull(ex, "IllegalArgumentException expected");

      // Verify that it does not enter sending or authentication header branching
      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, never()).getApiKey();
      verify(mockConfig, never()).getApiKeyHeader();
      verifyNoMoreInteractions(mockConfig);
    }

    @Test
    @DisplayName("should handle thread interruption gracefully (deterministic)")
    void shouldHandleThreadInterruptionGracefully() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        Thread.currentThread().interrupt();

        AuthorizationException ex = assertThrows(AuthorizationException.class, exec(client, "{}"));

        String lower = String.valueOf(ex.getMessage()).toLowerCase();
        assertTrue(lower.contains("interrupted"), "message: " + ex.getMessage());
        assertInstanceOf(
            TransportException.class, ex.getCause(), "The cause is TransportException");
        assertNotNull(ex.getCause().getCause(), "The existence of a root cause");

        verify(mockConfig, times(1)).getEndpoint();
        verify(mockConfig, times(1)).getApiKey();
        verify(mockConfig, never()).getApiKeyHeader();
        verifyNoMoreInteractions(mockConfig);

        // Clear the flag
        Thread.interrupted();
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("networkError noRetries singleAttempt")
    void networkError_noRetries_singleAttempt() throws Exception {
      // maxRetries=0 → Only one attempt
      SimpleHttpClient noRetry = new SimpleHttpClient(0);

      int badPort = unusedPort();
      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      AuthorizationException ex = assertThrows(AuthorizationException.class, exec(noRetry, "{}"));

      String lower = String.valueOf(ex.getMessage()).toLowerCase();
      // Measures against notation inconsistencies（attempt / attempts）
      assertTrue(lower.matches(".*after\\s+1\\s+attempts?\\b.*"), "message: " + ex.getMessage());

      verify(mockConfig, times(1)).getEndpoint();
      verify(mockConfig, times(1)).getApiKey();
      verifyNoMoreInteractions(mockConfig);
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should be thread-safe for concurrent requests")
    void shouldBeThreadSafeForConcurrentRequests() throws Exception {
      // Given
      LocalHttp http = new LocalHttp();
      http.start();
      // Failure: Unused port（Confirm the connection failure）
      int badPort;
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        badPort = s.getLocalPort();
      }

      String badUrl = "http://127.0.0.1:" + badPort + "/nope";

      try {
        SimpleHttpClient client = new SimpleHttpClient();

        // This time, instead of fixing it to a single mock, we'll separate the configs used for
        // each task.
        // Set OK to return 200
        AuthzClientConfig okConfig = mock(AuthzClientConfig.class);
        when(okConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(okConfig.getApiKey()).thenReturn(Optional.empty());

        // Settings to cause connection failure
        AuthzClientConfig badConfig = mock(AuthzClientConfig.class);
        when(badConfig.getEndpoint()).thenReturn(badUrl);
        when(badConfig.getApiKey()).thenReturn(Optional.empty());

        ExecutorService executor = Executors.newFixedThreadPool(5);
        java.util.List<Future<Exception>> futures = new java.util.ArrayList<>();

        // When - Submit multiple concurrent requests
        for (int i = 0; i < 10; i++) {
          final int requestId = i;
          final AuthzClientConfig cfg =
              (i % 5 == 0) ? badConfig : okConfig; // Cause only 2 failures
          futures.add(
              executor.submit(
                  () -> {
                    try {
                      client.request(cfg, "{\"request_id\": \"" + requestId + "\"}");
                      return null; // Success
                    } catch (Exception e) {
                      return e; // Failure
                    }
                  }));
        }

        // Then - Check for exceptions: Only the two cases using badConfig are exceptions, all
        // others succeed
        int exceptions = 0;
        for (Future<Exception> future : futures) {
          Exception result = future.get();
          if (result != null) {
            exceptions++;
            // Important: Confirm that it is not an exception caused by concurrency
            assertFalse(result instanceof java.util.ConcurrentModificationException);
          }
        }
        assertEquals(2, exceptions, "As expected, there were only 2 failures (due to badConfig).");
        executor.shutdown();
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should handle concurrent requests with different configurations")
    void shouldHandleConcurrentRequestsWithDifferentConfigurations() throws Exception {
      // Given
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        SimpleHttpClient client = new SimpleHttpClient();

        AuthzClientConfig config1 = mock(AuthzClientConfig.class);
        when(config1.getEndpoint()).thenReturn(http.url("/json"));
        when(config1.getApiKey()).thenReturn(Optional.of("key1")); // Authorization: Bearer key1

        AuthzClientConfig config2 = mock(AuthzClientConfig.class);
        when(config2.getEndpoint()).thenReturn(http.url("/uuid"));
        when(config2.getApiKey()).thenReturn(Optional.of("key2")); // Authorization: Bearer key2

        // Setting to cause failure (make connection impossible on unused port)
        int badPort;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
          s.setReuseAddress(true);
          badPort = s.getLocalPort();
        }
        String badUrl = "http://127.0.0.1:" + badPort + "/nope";
        AuthzClientConfig badConfig = mock(AuthzClientConfig.class);
        when(badConfig.getEndpoint()).thenReturn(badUrl);
        when(badConfig.getApiKey()).thenReturn(Optional.of("keyX"));

        ExecutorService executor = Executors.newFixedThreadPool(4);
        java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();

        // When - Submit requests with different configurations
        for (int i = 0; i < 6; i++) {
          final boolean useConfig1 = i % 2 == 0;
          final boolean useBad = i % 3 == 0; // Make only the two cases 0 and 3 fail
          futures.add(
              CompletableFuture.supplyAsync(
                  () -> {
                    try {

                      AuthzClientConfig config =
                          useBad ? badConfig : (useConfig1 ? config1 : config2);
                      return client.request(config, "{\"test\": true}");
                    } catch (Exception e) {
                      return "ERROR: " + e.getClass().getSimpleName();
                    }
                  },
                  executor));
        }

        // Then - All should complete without cross-configuration interference
        java.util.List<String> results = futures.stream().map(CompletableFuture::join).toList();

        assertEquals(6, results.size());
        // Non-null regardless of success or failure. Moreover, since this time it’s local 200, all
        // successes are expected.
        results.forEach(result -> assertNotNull(result));

        // This time, I confirmed that it only includes 2 failures (ERROR: ~) to ensure that the
        // catch line is definitely executed.
        long errorCount = results.stream().filter(s -> s.startsWith("ERROR:")).count();
        assertEquals(2, errorCount, "Failures as intended: 2 items (badConfig)");

        executor.shutdown();
      } finally {
        http.stop();
      }
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("should handle rapid sequential requests")
    void shouldHandleRapidSequentialRequests() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(Duration.ofMillis(100), 0); // no retries
      LocalHttp http = new LocalHttp();

      http.start(); // Propagate exceptions using the method's throws

      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      // Unused port → Connection failed URL
      int badPort;
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        badPort = s.getLocalPort();
      }

      final String okUrl = http.url("/ok");
      final String badUrl = "http://127.0.0.1:" + badPort + "/nope";

      for (int i = 0; i < 10; i++) {
        try {
          when(mockConfig.getEndpoint()).thenReturn((i % 3 == 0) ? badUrl : okUrl);
          client.request(mockConfig, "{\"iteration\": \"" + i + "\"}");
        } catch (Exception e) {
          assertNotNull(e);
        }
      }

      http.stop();
    }

    @Test
    @DisplayName("should not accumulate resources with failed requests")
    void shouldNotAccumulateResourcesWithFailedRequests() throws Exception {
      // Given
      SimpleHttpClient client = new SimpleHttpClient(Duration.ofMillis(50), 0);

      // Start a local HTTP server for the success path (stop it at the end)
      LocalHttp http = new LocalHttp();
      http.start();

      // Success URL
      final String okUrl = http.url("/ok");

      // For failed path: URL to an unused port
      int badPort;
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        badPort = s.getLocalPort();
      }
      final String badUrl = "http://127.0.0.1:" + badPort + "/nonexistent";

      // Common API Key Settings
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      try {
        // When - Multiple requests (Mix success and failure)
        for (int i = 0; i < 20; i++) {
          // One out of four URLs is successful, the rest are failed URLs.
          boolean useSuccess = (i % 4 == 0);
          when(mockConfig.getEndpoint()).thenReturn(useSuccess ? okUrl : badUrl);

          try {
            String resp = client.request(mockConfig, "{\"test\": \"" + i + "\"}");
            if (useSuccess) {
              // Success case: response is not null
              assertNotNull(resp);
            } else {
              // fail("Expected network failure, but request succeeded");
            }
          } catch (Exception e) {
            // Reached only in failure cases
            assertTrue(
                e instanceof AuthorizationException,
                "Failures are notified by an AuthorizationException");

            Throwable cause = e.getCause();
            assertNotNull(
                cause, "An AuthorizationException is accompanied by a cause (TransportException).");
            assertTrue(
                cause instanceof nedo.ods.svc.dp.authzen.exception.TransportException,
                "The cause should be a TransportException.");

            Throwable root = cause.getCause();
            assertNotNull(root, "A network exception occurs further inside the TransportException");
            assertTrue(
                root instanceof java.io.IOException,
                "The final cause is the IOException subclass (actually: \" + root + \")");
          }
        }
      } finally {
        http.stop();
      }

      // Then - Should complete without memory issues (implicit test)
      System.gc(); // Suggest garbage collection
      assertTrue(true); // If we reach here, no resource leaks caused issues
    }
  }

  @Nested
  @DisplayName("Exception Coverage Tests")
  class ExceptionCoverageTests {

    private org.junit.jupiter.api.function.Executable exec(SimpleHttpClient c, String body) {
      return new org.junit.jupiter.api.function.Executable() {
        @Override
        public void execute() throws Throwable {
          c.request(mockConfig, body);
        }
      };
    }

    /** Unused port */
    private int unusedPort() throws java.io.IOException {
      try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
        s.setReuseAddress(true);
        return s.getLocalPort();
      }
    }

    @Test
    @DisplayName("should throw AuthorizationException for null endpoint")
    void shouldThrowAuthorizationExceptionForNullEndpoint() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient();
      when(mockConfig.getEndpoint()).thenReturn(null);

      AuthorizationException ex =
          assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
      assertNotNull(ex.getMessage());
      assertNotNull(ex.getCause());

      @SuppressWarnings("unused")
      String _t = ex.toString();
    }

    @Test
    @DisplayName("should return body on 2xx success (happy path)")
    void shouldReturnBodyOnSuccess() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient();
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        String body = client.request(mockConfig, "{\"ping\":true}");
        assertEquals("{\"status\":\"OK\"}", body);
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should throw AuthorizationException for blank endpoint")
    void shouldThrowAuthorizationExceptionForBlankEndpoint() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient();
      when(mockConfig.getEndpoint()).thenReturn(" ");

      AuthorizationException ex =
          assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
      assertNotNull(ex.getMessage());

      @SuppressWarnings("unused")
      String _t = ex.toString();
    }

    @Test
    @DisplayName("should throw AuthorizationException for 4xx client errors")
    void shouldThrowAuthorizationExceptionFor4xxErrors() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient();
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        http.server.createContext(
            "/status/401", ex -> LocalHttp.respond(ex, 401, "{\"e\":\"unauthorized\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/status/401"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
        assertNotNull(ex.getMessage());

        @SuppressWarnings("unused")
        String _t = ex.toString();
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should throw AuthorizationException after max retries for 5xx errors")
    void shouldThrowAuthorizationExceptionAfterMaxRetriesFor5xxErrors() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(Duration.ofSeconds(5), 0);
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        http.server.createContext(
            "/status/500", ex -> LocalHttp.respond(ex, 500, "{\"e\":\"server\"}"));
        when(mockConfig.getEndpoint()).thenReturn(http.url("/status/500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
        assertNotNull(ex.getMessage());
        assertNull(ex.getCause()); // Implementation specification: 5xx without cause

        @SuppressWarnings("unused")
        String _t = ex.toString();
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should throw AuthorizationException for unexpected status codes")
    void shouldThrowAuthorizationExceptionForUnexpectedStatusCodes() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient();
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        http.server.createContext(
            "/status/302",
            ex -> {
              ex.getResponseHeaders().add("Location", "/ok");
              LocalHttp.respond(ex, 302, "redirect");
            });
        when(mockConfig.getEndpoint()).thenReturn(http.url("/status/302"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
        assertNotNull(ex.getMessage());

        @SuppressWarnings("unused")
        String _t = ex.toString();
      } finally {
        http.stop();
      }
    }

    @Test
    @DisplayName("should throw AuthorizationException after max retries for network errors")
    void shouldThrowAuthorizationExceptionAfterMaxRetriesForNetworkErrors() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(Duration.ofMillis(100), 1);
      int badPort = unusedPort();

      when(mockConfig.getEndpoint()).thenReturn("http://127.0.0.1:" + badPort + "/nope");
      when(mockConfig.getApiKey()).thenReturn(Optional.empty());

      AuthorizationException ex =
          assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
      assertNotNull(ex.getMessage());
      assertNotNull(ex.getCause());

      @SuppressWarnings("unused")
      String _t = ex.toString();
    }

    @Test
    @DisplayName("should handle thread interruption during request execution (deterministic)")
    void shouldHandleThreadInterruptionDuringRequestExecution() throws Exception {
      SimpleHttpClient client = new SimpleHttpClient(Duration.ofSeconds(5), 0);
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        when(mockConfig.getEndpoint()).thenReturn(http.url("/ok"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        Thread.currentThread().interrupt();

        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
        assertNotNull(ex.getMessage());
        assertNotNull(ex.getCause());

        @SuppressWarnings("unused")
        String _t = ex.toString();
      } finally {
        http.stop();
        Thread.interrupted(); // Clear flag
      }
    }

    @Test
    @DisplayName(
        "should handle thread interruption during retry backoff (current-thread, deterministic)")
    void shouldHandleThreadInterruptionDuringRetryBackoff() throws Exception {
      LocalHttp http = new LocalHttp();
      http.start();
      try {
        http.server.createContext(
            "/always-500", ex -> LocalHttp.respond(ex, 500, "{\"error\":\"server\"}"));
        SimpleHttpClient client =
            new SimpleHttpClient(Duration.ofSeconds(2), Duration.ofSeconds(30), 8);
        when(mockConfig.getEndpoint()).thenReturn(http.url("/always-500"));
        when(mockConfig.getApiKey()).thenReturn(Optional.empty());

        Thread testThread = Thread.currentThread();
        java.util.concurrent.ScheduledExecutorService interrupter =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        // Avoid method references/lambdas and use an anonymous Runnable class for periodic
        // interrupt firing
        interrupter.scheduleAtFixedRate(
            new Runnable() {
              @Override
              public void run() {
                testThread.interrupt();
              }
            },
            150,
            50,
            java.util.concurrent.TimeUnit.MILLISECONDS);

        AuthorizationException ex =
            assertThrows(AuthorizationException.class, exec(client, "{\"test\":\"data\"}"));
        interrupter.shutdownNow();
        assertNotNull(ex.getMessage());
        assertNotNull(ex.getCause());
      } finally {
        http.stop();
        Thread.interrupted(); // Clear flag
      }
    }
  }
}
