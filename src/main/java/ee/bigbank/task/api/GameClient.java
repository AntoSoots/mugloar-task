package ee.bigbank.task.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ee.bigbank.task.api.dto.BuyResponse;
import ee.bigbank.task.api.dto.GameStartResponse;
import ee.bigbank.task.api.dto.Message;
import ee.bigbank.task.api.dto.ReputationResponse;
import ee.bigbank.task.api.dto.ShopItem;
import ee.bigbank.task.api.dto.SolveResponse;
import ee.bigbank.task.util.HttpHelper;

/**
 * Client for interacting with the game API.
 */
public class GameClient {
    private static final Logger log = LoggerFactory.getLogger(GameClient.class);

    private static final String PATH_START = "/game/start";
    private static final String PATH_MESSAGES = "/%s/messages";
    private static final String PATH_SOLVE = "/%s/solve/%s";
    private static final String PATH_SHOP = "/%s/shop";
    private static final String PATH_BUY = "/%s/shop/buy/%s";
    private static final String PATH_INVESTIGATE = "/%s/investigate/reputation";

    private final String baseUrl;
    private final HttpHelper http;

    public GameClient(String baseUrl) {
        this(stripTrailingSlash(baseUrl), new HttpHelper(new ObjectMapper()));
    }

    public GameClient(String baseUrl, HttpHelper http) {
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.http = Objects.requireNonNull(http, "http");
    }

    public GameStartResponse startGame() {
        String url = buildUrl(PATH_START);
        log.info("Starting a new game...");
        return http.post(url, GameStartResponse.class);
    }

    public List<Message> getMessages(String gameId) {
        String url = buildUrl(String.format(PATH_MESSAGES, encodePathSegment(gameId)));
        return http.getList(url, Message.class);
    }

    public SolveResponse solve(String gameId, String adId) {
        String url = buildUrl(String.format(PATH_SOLVE, encodePathSegment(gameId), encodePathSegment(adId)));
        return http.post(url, SolveResponse.class);
    }

    public List<ShopItem> getShop(String gameId) {
        String url = buildUrl(String.format(PATH_SHOP, encodePathSegment(gameId)));
        return http.getList(url, ShopItem.class);
    }

    public BuyResponse buyItem(String gameId, String itemId) {
        String url = buildUrl(String.format(PATH_BUY, encodePathSegment(gameId), encodePathSegment(itemId)));
        return http.post(url, BuyResponse.class);
    }

    public ReputationResponse investigate(String gameId) {
        String url = buildUrl(String.format(PATH_INVESTIGATE, encodePathSegment(gameId)));
        return http.post(url, ReputationResponse.class);
    }

    private String buildUrl(String path) {
        boolean baseEndsWithSlash = baseUrl.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");

        if (baseEndsWithSlash && pathStartsWithSlash) {
            return baseUrl + path.substring(1);
        } else if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return baseUrl + "/" + path;
        } else {
            return baseUrl + path;
        }
    }

    /** Percent-encode a single URI path segment (space -> %20, not '+'). */
    private static String encodePathSegment(String s) {
        if (s == null) return null;
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid URL");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
