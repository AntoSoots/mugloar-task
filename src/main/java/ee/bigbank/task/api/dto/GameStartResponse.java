package ee.bigbank.task.api.dto;

public record GameStartResponse(
    String gameId,
    int lives,
    int gold,
    int level,
    int score,
    int highScore,
    int turn
) {}
