package ee.bigbank.task;

import org.slf4j.Logger;

import ee.bigbank.task.api.GameClient;
import ee.bigbank.task.core.GameService;
import ee.bigbank.task.core.MessageDecoder;
import ee.bigbank.task.core.model.GameResult;

public class MugloarTaskApplication {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(MugloarTaskApplication.class);

	public static void main(String[] args) {

		String baseUrl = "https://www.dragonsofmugloar.com/api/v2";
		GameClient client = new GameClient(baseUrl);
		MessageDecoder decoder = new MessageDecoder();
		GameService gameService = new GameService(client, decoder);
		
		GameResult result = gameService.playGame();
		log.info("Game finished: id={} score={} turns={}", result.gameId(), result.score(), result.turns());
	}

}
