package ee.bigbank.task.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.bigbank.task.api.ApiClientException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

class HttpHelperTest {

    private static HttpServer server;
    private static String baseUrl;

    private final HttpHelper http = new HttpHelper(new ObjectMapper());

    // Simple DTOs for parsing
    public static class Foo { public int x; }
    public static class Bar { public String id; }

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;

        server.createContext("/ok", ex -> respondJson(ex, 200, "{\"x\":123}"));
        server.createContext("/list", ex -> respondJson(ex, 200, "[{\"id\":\"a\"},{\"id\":\"b\"}]"));
        server.createContext("/bad", ex -> respondText(ex, 400, "Bad Request"));
        server.createContext("/malformed", ex -> respondText(ex, 200, "{not-json"));

        server.createContext("/post", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                respondText(ex, 405, "Method Not Allowed");
                return;
            }
            respondJson(ex, 200, "{\"x\":999}");
        });

        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void get_parsesPojo() {
        Foo foo = http.get(baseUrl + "/ok", Foo.class);
        assertThat(foo.x).isEqualTo(123);
    }

    @Test
    void getList_parsesArray() {
        List<Bar> list = http.getList(baseUrl + "/list", Bar.class);
        assertThat(list).extracting(b -> b.id).containsExactly("a", "b");
    }

    @Test
    void post_callsPost_andParsesPojo() {
        Foo foo = http.post(baseUrl + "/post", Foo.class);
        assertThat(foo.x).isEqualTo(999);
    }

    @Test
    void non2xx_throwsApiClientException() {
        ApiClientException ex = assertThrows(ApiClientException.class,
                () -> http.get(baseUrl + "/bad", Foo.class));
        assertThat(ex.getMessage()).contains("HTTP 400");
    }

    @Test
    void malformedJson_throwsParseException() {
        ApiClientException ex = assertThrows(ApiClientException.class,
                () -> http.get(baseUrl + "/malformed", Foo.class));
        assertThat(ex.getMessage()).contains("Failed to parse response body");
    }

    // --- helpers ----------------------------------------------------------

    private static void respondJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes();
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void respondText(HttpExchange ex, int code, String text) throws IOException {
        byte[] bytes = text.getBytes();
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
