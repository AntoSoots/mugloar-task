package ee.bigbank.task.core;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.bigbank.task.api.GameClient;
import ee.bigbank.task.api.dto.GameStartResponse;
import ee.bigbank.task.api.dto.Message;
import ee.bigbank.task.api.dto.SolveResponse;
import ee.bigbank.task.core.model.GameResult;

/**
 * GameService is responsible for managing the game flow:
 * - starting the game
 * - fetch and decode messages
 * - select and resolve messages
 * - optionally make shop decisions between steps
 */
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameClient client;
    private final MessageDecoder decoder;

    public GameService(GameClient client, MessageDecoder decoder) {
        this.client = client;
        this.decoder = decoder;
    }

    /**
     * Plays the game until lives run out and returns the final result.
     */
    public GameResult playGame() {
        GameStartResponse game = client.startGame();
        String gameId = game.gameId();
		ShopService shop = new ShopService(client, gameId);

        while (game.lives() > 0) {
            // 1) fetch and decode messages
            List<Message> decodedMessages = client.getMessages(gameId).stream()
                .map(decoder::decode)
                .flatMap(Optional::stream)
                .toList();

            // 2) pick the best by probability -> expiresIn -> reward
            Optional<Message> best = decodedMessages.stream()
                .max(Comparator
                    .comparingDouble((Message m) -> Probability.valueForLabel(m.probability()))
                    .thenComparingInt(Message::expiresIn)
                    .thenComparingInt(Message::reward));

            if (best.isEmpty()) {
                // No valid messages this turn try to buy an item and continue
                shop.maybeBuyItem(game.gold(), game.lives());
                log.debug("Bought item from shop and start again.");
            } else {
                // 3) solve the chosen message
                Message chosen = best.get();
                SolveResponse solveResult = client.solve(gameId, chosen.adId());

                // 4) update local game snapshot from solveResult
                game = new GameStartResponse(
                    gameId,
                    solveResult.lives(),
                    solveResult.gold(),
                    game.level(),
                    solveResult.score(),
                    solveResult.highScore(),
                    solveResult.turn()
                );
                log.debug("Solved message {} -> lives={} gold={} score={} turn={}",
                    chosen.adId(), game.lives(), game.gold(), game.score(), game.turn());

                    // 5) post-solve shop decision (e.g. heal if needed)
                shop.maybeBuyItem(game.gold(), game.lives());
            }

        }
        return new GameResult(gameId, game.score(), game.turn());
    }
}