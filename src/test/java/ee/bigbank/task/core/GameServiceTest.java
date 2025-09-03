package ee.bigbank.task.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ee.bigbank.task.api.GameClient;
import ee.bigbank.task.api.dto.GameStartResponse;
import ee.bigbank.task.api.dto.Message;
import ee.bigbank.task.api.dto.ShopItem;
import ee.bigbank.task.api.dto.SolveResponse;
import ee.bigbank.task.core.model.GameResult;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock GameClient client;
    @Mock MessageDecoder decoder;

    @Test
    void playGame_shouldSolveBestMMessage_andStopWhenLivesDeplete() {
        // Arrange
        GameService service = new GameService(client, decoder);

        // startGame snapshot
        when(client.startGame()).thenReturn(new GameStartResponse(
            "game-1", 2, 100, 1, 0, 0, 1));

        when(client.getShop(anyString())).thenReturn(List.of(
            new ShopItem("hpot", "Healing Potion", 50)
        ));

        // First messages batch: two candidates
        Message m1 = new Message("A1", "msg1", 100, 3, "Piece of cake", null);
        Message m2 = new Message("B2", "msg2", 90,  6, "Piece of cake", null);

        when(client.getMessages(anyString()))
                .thenReturn(List.of(m1, m2)); // only one iteration needed

        // decoder returns same messages
        when(decoder.decode(m1)).thenReturn(Optional.of(m1));
        when(decoder.decode(m2)).thenReturn(Optional.of(m2));

        // Solve should be called for m2 (expiresIn=6 > 3)
        when(client.solve("game-1", "B2")).thenReturn(new SolveResponse(
                true, 0, 120, 1234, 0, 2, "ok"  // lives=0 -> loop ends
        ));

        // Act
        GameResult result = service.playGame();

        // Assert
        assertThat(result.gameId()).isEqualTo("game-1");
        assertThat(result.score()).isEqualTo(1234);
        assertThat(result.turns()).isEqualTo(2);

        // verify selection went to B2
        verify(client).solve("game-1", "B2");

        // Verify order: startGame -> getShop (ShopService) -> getMessages -> solve
        InOrder inOrder = inOrder(client, decoder);
        inOrder.verify(client).startGame();
        inOrder.verify(client).getShop("game-1");
        inOrder.verify(client).getMessages("game-1");
        // decoder/scoring can be interleaved per stream; we don't assert strict order there.
        inOrder.verify(client).solve("game-1", "B2");
    }

    @Test
    void playGame_whenNoMessagesFirst_thenShop_thenMessagesAppear_andGameFinishes() {
        GameService service = new GameService(client, decoder);

        when(client.startGame()).thenReturn(new GameStartResponse(
                "game-2", 2, 200, 1, 0, 0, 1));

        // ShopService ctor loads catalog
        when(client.getShop(anyString())).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50)
        ));

        // First: no messages; Second: one message
        when(client.getMessages(anyString()))
                .thenReturn(List.of()) // 1st iteration -> triggers shop.maybeBuyItem(...)
                .thenReturn(List.of(new Message("C3", "msg", 70, 4, "Quite likely", null)));

        Message m = new Message("C3", "msg", 70, 4, "Quite likely", null);
        when(decoder.decode(m)).thenReturn(Optional.of(m));

        // Solving will end the game (lives -> 0)
        when(client.solve("game-2", "C3")).thenReturn(new SolveResponse(
                true, 0, 210, 1010, 0, 2, "ok"
        ));

        GameResult result = service.playGame();

        assertThat(result.gameId()).isEqualTo("game-2");
        assertThat(result.score()).isEqualTo(1010);
        assertThat(result.turns()).isEqualTo(2);

        // ensure solve called for C3
        verify(client).solve("game-2", "C3");
        // ensure messages were fetched twice (empty -> non-empty)
        verify(client, times(2)).getMessages("game-2");
    }

    @Test
    void playGame_comparatorPrefersHigherExpiresIn_whenSameProbability() {
        GameService service = new GameService(client, decoder);

        when(client.startGame()).thenReturn(new GameStartResponse(
                "game-3", 2, 100, 1, 0, 0, 1));

        when(client.getShop(anyString())).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50)
        ));

        Message lowExp = new Message("L", "low", 150, 1, "Piece of cake", null);
        Message highExp = new Message("H", "high", 120, 6, "Piece of cake", null);

        when(client.getMessages(anyString())).thenReturn(List.of(lowExp, highExp));

        when(decoder.decode(lowExp)).thenReturn(Optional.of(lowExp));
        when(decoder.decode(highExp)).thenReturn(Optional.of(highExp));

        when(client.solve(eq("game-3"), anyString())).thenAnswer(inv -> {
            String adId = inv.getArgument(1, String.class);
            // end immediately
            return new SolveResponse(true, 0, 100, 999, 0, 2, "ok (" + adId + ")");
        });

        GameResult result = service.playGame();

        assertThat(result.score()).isEqualTo(999);

        // capture adId we solved
        ArgumentCaptor<String> adIdCap = ArgumentCaptor.forClass(String.class);
        verify(client).solve(eq("game-3"), adIdCap.capture());
        assertThat(adIdCap.getValue()).isEqualTo("H"); // prefers higher expiresIn
    }
}
