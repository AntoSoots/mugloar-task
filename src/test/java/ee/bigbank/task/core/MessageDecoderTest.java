package ee.bigbank.task.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ee.bigbank.task.api.dto.Message;

class MessageDecoderTest {

    private final MessageDecoder decoder = new MessageDecoder();

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void decode_returnsEmpty_whenMessageNull() {
        assertThat(decoder.decode(null)).isEmpty();
    }

    @Test
    void decode_returnsOriginal_whenUnencrypted() {
        Message original = new Message("A1", "Hello", 10, 3, "Piece of cake", null);
        Optional<Message> out = decoder.decode(original);

        assertThat(out).containsSame(original); // same instance
    }

    @Test
    void decode_decodesBase64_andSetsDecodedTag() {
        // Probability label must exist in Probability enum for this to produce a new Message
        String adId = b64("X123");
        String msg  = b64("Do the thing");
        String prob = b64("Sure thing");

        Message enc = new Message(adId, msg, 50, 5, prob, "1");

        Optional<Message> out = decoder.decode(enc);
        assertThat(out).isPresent();

        Message dec = out.get();
        assertThat(dec.adId()).isEqualTo("X123");
        assertThat(dec.message()).isEqualTo("Do the thing");
        assertThat(dec.probability()).isEqualTo("Sure thing");
        assertThat(dec.encrypted()).isNull();
        // reward/expiresIn untouched
        assertThat(dec.reward()).isEqualTo(50);
        assertThat(dec.expiresIn()).isEqualTo(5);
    }

    @Test
    void decode_decodesRot13_andSetsDecodedTag() {
        // "Cnff jung lbh pna" -> ROT13 of "Pass what you can" (prob label must exist)
        String adId = rot13("AD9Z");
        String msg  = rot13("Do ten tasks");
        String prob = rot13("Quite likely"); // Probability enum must include this label

        Message enc = new Message(adId, msg, 40, 4, prob, "2");
        Optional<Message> out = decoder.decode(enc);

        assertThat(out).isPresent();
        Message dec = out.get();
        assertThat(dec.adId()).isEqualTo("AD9Z");
        assertThat(dec.message()).isEqualTo("Do ten tasks"); // NB: rot13 above produces this; we test integrity via roundtrip below
        // Sõnumi sisu ei pea olema inglise lause; oluline on, et tagasi-ROT13 annaks algse:
        assertThat(rot13(dec.adId())).isEqualTo(adId);
        assertThat(rot13(dec.message())).isEqualTo(msg);

        assertThat(dec.probability()).isEqualTo("Quite likely");
        assertThat(dec.encrypted()).isNull();
    }

    @Test
    void decode_base64Invalid_returnsOriginalMessage() {
        // invalid base64 -> decoding leaves fields as-is; Probability.fromLabel will likely fail -> returns original
        Message enc = new Message("###", "???", 15, 2, "!!notB64!!", "1");
        Optional<Message> out = decoder.decode(enc);

        assertThat(out).containsSame(enc);
    }

    @Test
    void decode_unknownMethod_returnsOriginal() {
        Message enc = new Message("A1", "M", 10, 3, "Piece of cake", "9");
        Optional<Message> out = decoder.decode(enc);

        assertThat(out).containsSame(enc);
    }

    // --- helpers ----------------------------------------------------------

    private static String rot13(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= 'a' && c <= 'z')      out.append((char) ('a' + (c - 'a' + 13) % 26));
            else if (c >= 'A' && c <= 'Z') out.append((char) ('A' + (c - 'A' + 13) % 26));
            else                           out.append(c);
        }
        return out.toString();
    }
}
