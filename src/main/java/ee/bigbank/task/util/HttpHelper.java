package ee.bigbank.task.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ee.bigbank.task.api.ApiClientException;

/**
 * Thin HTTP utility around Java HttpClient with JSON (Jackson) parsing and basic logging.
 */
public class HttpHelper {
    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

    private final HttpClient http;
    private final ObjectMapper mapper;

    public HttpHelper(ObjectMapper mapper) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = mapper;
    }

    public <T> T get(String url, Class<T> type) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        String body = send(request);
        return parse(body, type);
    }

    public <T> List<T> getList(String url, Class<T> elementType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        String body = send(request);
        return parseList(body, elementType);
    }

    public <T> T post(String url, Class<T> type) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        String body = send(request);
        return parse(body, type);
    }

    private String send(HttpRequest request) {
        Instant started = Instant.now();
        String reqMethod = request.method();
        try {
            log.debug("HTTP -> {} {}", reqMethod, request.uri());
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("HTTP <- {} ({} ms)", response.statusCode(), Duration.between(started, Instant.now()).toMillis());

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ApiClientException("HTTP " + statusCode + " for " + reqMethod + " body=" + safeBody(response.body()));
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiClientException("Transport failure for " + reqMethod + " " + request.uri(), e);
        } catch (IOException e) {
            throw new ApiClientException("Failed to send HTTP request: " + e.getMessage(), e);
        }
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (IOException e) {
            throw new ApiClientException("Failed to parse response body: " + e.getMessage(), e);
        }
    }

    private <T> List<T> parseList(String body, Class<T> elementType) {
        try {
            return mapper.readerForListOf(elementType).readValue(body);
        } catch (IOException e) {
            throw new ApiClientException("Failed to parse response body: " + e.getMessage(), e);
        }
    }

    private static String safeBody(String body) {
        if (body == null) return "null";
        return body.length() > 512 ? body.substring(0, 512) + "...(truncated)" : body;
    }

}
