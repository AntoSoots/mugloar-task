package ee.bigbank.task.api.dto;

public record BuyResponse(
    String shoppingSuccess,
    int gold,
    int lives,
    int level,
    int turn
) {}
