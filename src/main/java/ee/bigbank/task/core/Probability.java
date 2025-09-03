package ee.bigbank.task.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Probability enum represents different levels of probability with associated labels and values.
 * Labels reflect the API's textual probability field.
 */
public enum Probability {
    PIECE_OF_CAKE("Piece of cake", 0.95),
    WALK_IN_THE_PARK("Walk in the park", 0.85),
    SURE_THING("Sure thing", 0.80),
    HMMM("Hmmm....", 0.75),
    QUITE_LIKELY("Quite likely", 0.65),
    GAMBLE("Gamble", 0.60),
    RISKY("Risky", 0.30),
    RATHER_DETRIMENTAL("Rather detrimental", 0.28),
    PLAYING_WITH_FIRE("Playing with fire", 0.25),
    SUICIDE_MISSION("Suicide mission", 0.10),
    IMPOSSIBLE("Impossible", 0.0);

    private final String label;
    private final double value;

    Probability(String label, double value) {
        this.label = label;
        this.value = value;
    }

    /** Display label as used by the API. */
    public String label() {
        return label;
    }

    /** Numeric probability [0..1]. */
    public double value() {
        return value;
    }

    // Precompute case-insensitive lookup map for performance and robustness.
    private static final Map<String, Probability> BY_LABEL_LOWER =
            Arrays.stream(values())
                  .collect(Collectors.toMap(
                          p -> p.label.toLowerCase(Locale.ROOT),
                          p -> p));

    /**
     * Case-insensitive, trimmed lookup by display label.
     * Returns empty if label is null or not recognized.
     */
    public static Optional<Probability> fromLabel(String label) {
        if (label == null) return Optional.empty();
        String key = label.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(BY_LABEL_LOWER.get(key));
    }

    public static Set<String> validProbabilities() {
        return Arrays.stream(values())
            .map(Probability::label)
            .collect(Collectors.toSet());
    }

    /** Convenience: numeric probability for a label, unknown -> 0.0 */
    public static double valueForLabel(String label) {
        return fromLabel(label).map(Probability::value).orElse(0.0);
    }
}
