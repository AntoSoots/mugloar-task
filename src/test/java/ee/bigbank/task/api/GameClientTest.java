package ee.bigbank.task.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ee.bigbank.task.api.dto.BuyResponse;
import ee.bigbank.task.api.dto.GameStartResponse;
import ee.bigbank.task.api.dto.Message;
import ee.bigbank.task.api.dto.ReputationResponse;
import ee.bigbank.task.api.dto.ShopItem;
import ee.bigbank.task.api.dto.SolveResponse;
import ee.bigbank.task.util.HttpHelper;

@ExtendWith(MockitoExtension.class)
class GameClientTest {

    @Mock HttpHelper http;

    @Test
    void startGame_buildsCorrectUrl_noTrailingSlash() {
        GameClient client = new GameClient("http://host/api/v4", http);
        when(http.post(anyString(), eq(GameStartResponse.class)))
                .thenReturn(new GameStartResponse("g1", 3, 0, 1, 0, 0, 1));

        client.startGame();

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).post(urlCap.capture(), eq(GameStartResponse.class));
        assertThat(urlCap.getValue()).isEqualTo("http://host/api/v4/game/start");
    }

    @Test
    void startGame_buildsCorrectUrl_withTrailingSlash() {
        GameClient client = new GameClient("http://host/api/v4/", http);
        when(http.post(anyString(), eq(GameStartResponse.class)))
                .thenReturn(new GameStartResponse("g1", 3, 0, 1, 0, 0, 1));

        client.startGame();

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).post(urlCap.capture(), eq(GameStartResponse.class));
        assertThat(urlCap.getValue()).isEqualTo("http://host/api/v4/game/start");
    }

    @Test
    void getMessages_encodesGameId() {
        GameClient client = new GameClient("http://h/api/v4", http);
        when(http.getList(anyString(), eq(Message.class))).thenReturn(List.of());

        client.getMessages("g id/ü");

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).getList(urlCap.capture(), eq(Message.class));

        String encodedGameId = URLEncoder.encode("g id/ü", StandardCharsets.UTF_8).replace("+", "%20");
        assertThat(urlCap.getValue()).isEqualTo("http://h/api/v4/" + encodedGameId + "/messages");
    }

    @Test
    void solve_encodesAdId() {
        GameClient client = new GameClient("http://h/api/v4", http);
        when(http.post(anyString(), eq(SolveResponse.class)))
                .thenReturn(new SolveResponse(true, 3, 10, 10, 0, 2, "ok"));

        String gameId = "g1";
        String adId = "U211Y1NzaUM="; // base64-like

        client.solve(gameId, adId);

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).post(urlCap.capture(), eq(SolveResponse.class));

        String encGame = URLEncoder.encode(gameId, StandardCharsets.UTF_8).replace("+", "%20");
        String encAd   = URLEncoder.encode(adId, StandardCharsets.UTF_8).replace("+", "%20");
        assertThat(urlCap.getValue()).isEqualTo("http://h/api/v4/" + encGame + "/solve/" + encAd);
    }

    @Test
    void buyItem_encodesItemId() {
        GameClient client = new GameClient("http://h/api/v4", http);
        when(http.post(anyString(), eq(BuyResponse.class)))
                .thenReturn(new BuyResponse("OK", 50, 3, 1, 2));

        client.buyItem("g1", "wingpot max");

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).post(urlCap.capture(), eq(BuyResponse.class));

        String encGame = URLEncoder.encode("g1", StandardCharsets.UTF_8).replace("+", "%20");
        String encItem = URLEncoder.encode("wingpot max", StandardCharsets.UTF_8).replace("+", "%20");
        assertThat(urlCap.getValue()).isEqualTo("http://h/api/v4/" + encGame + "/shop/buy/" + encItem);
    }

    @Test
    void getShop_buildsUrl() {
        GameClient client = new GameClient("http://h/api/v4", http);
        when(http.getList(anyString(), eq(ShopItem.class))).thenReturn(List.of());

        client.getShop("g1");

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).getList(urlCap.capture(), eq(ShopItem.class));
        String encGame = URLEncoder.encode("g1", StandardCharsets.UTF_8).replace("+", "%20");
        assertThat(urlCap.getValue()).isEqualTo("http://h/api/v4/" + encGame + "/shop");
    }

    @Test
    void investigate_buildsUrl_andPosts() {
        GameClient client = new GameClient("http://h/api/v4", http);
        when(http.post(anyString(), eq(ReputationResponse.class)))
                .thenReturn(new ReputationResponse(10, 20, 30));

        client.investigate("g1");

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(http).post(urlCap.capture(), eq(ReputationResponse.class));
        String encGame = URLEncoder.encode("g1", StandardCharsets.UTF_8).replace("+", "%20");
        assertThat(urlCap.getValue()).isEqualTo("http://h/api/v4/" + encGame + "/investigate/reputation");
    }

    @Test
    void constructor_throwsOnInvalidBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> new GameClient("   ", http));
    }
}
