package ee.bigbank.task.core.model;

public record GameResult(
    String gameId,
    int score,
    int turns
) {}
