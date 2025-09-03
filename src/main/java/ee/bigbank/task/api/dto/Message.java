package ee.bigbank.task.api.dto;

public record Message(
    String adId,
    String message,
    int reward,
    int expiresIn,
    String probability,
    String encrypted
) {}
