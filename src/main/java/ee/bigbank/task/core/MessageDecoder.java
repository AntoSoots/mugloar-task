package ee.bigbank.task.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import ee.bigbank.task.api.dto.Message;

/**
 * MessageDecoder is responsible for decoding encrypted messages using specified methods.
 * Supported methods:
 *  - "1" : Base64
 *  - "2" : ROT13
 *
 * If the decoded probability label doesn't map to a known Probability, the original message is returned.
 */
public class MessageDecoder {

    private static final String BASE64 = "1";
    private static final String ROT13 = "2";

    /**
     * Decodes the given message if {@code encrypted} is set to a known method.
     * Returns the original message if:
     *  - message is null,
     *  - encrypted method is null or unknown,
     *  - decoded probability label is not recognized by {@link Probability}.
     */
    public Optional<Message> decode(Message message) {
        if (message == null) return Optional.empty();
        
        String encryptedMethod = message.encrypted();
        if (encryptedMethod == null) return Optional.of(message);

        String adId;
        String msg;
        String prob;

        switch (encryptedMethod) {
            case BASE64 -> {
                adId = decryptBase64String(message.adId());
                msg = decryptBase64String(message.message());
                prob = decryptBase64String(message.probability());
            }
            case ROT13 -> {
                adId = decryptRot13String(message.adId());
                msg = decryptRot13String(message.message());
                prob = decryptRot13String(message.probability());
            }
            default -> {
                return Optional.of(message);
            }
        }

        // Only produce a new Message if probability label is valid
        return Probability.fromLabel(prob)
            .map(p -> new Message(adId, msg, message.reward(), message.expiresIn(), p.label(), null))
            .or(() -> Optional.of(message));
    }

    private static String decryptBase64String(String encryptedString) {
        if (encryptedString == null) return null;
        try {
            return new String(Base64.getDecoder().decode(encryptedString), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not valid base64 -> leave as is
            return encryptedString;
        }
    }

    private static String decryptRot13String(String encryptedString) {
        if (encryptedString == null) return null;
        StringBuilder decrypted = new StringBuilder(encryptedString.length());
        for (char c : encryptedString.toCharArray()) {
            if (c >= 'a' && c <= 'z') decrypted.append((char) ('a' + (c - 'a' + 13) % 26));
            else if (c >= 'A' && c <= 'Z') decrypted.append((char) ('A' + (c - 'A' + 13) % 26));
            else decrypted.append(c);
        }
        return decrypted.toString();
    }
}
