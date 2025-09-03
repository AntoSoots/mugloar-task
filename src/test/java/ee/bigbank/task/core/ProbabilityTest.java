package ee.bigbank.task.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProbabilityTest {

    @Test
    void fromLabel_exactMatch_returnsEnum() {
        Optional<Probability> p = Probability.fromLabel("Sure thing");
        assertThat(p).contains(Probability.SURE_THING);
        assertThat(p.get().value()).isEqualTo(0.80);
    }

    @Test
    void fromLabel_caseInsensitive_andTrimmed() {
        Optional<Probability> p = Probability.fromLabel("  pIeCe oF cAkE  ");
        assertThat(p).contains(Probability.PIECE_OF_CAKE);
        assertThat(p.get().value()).isEqualTo(0.95);
    }

    @Test
    void fromLabel_unknownOrNull_returnsEmpty() {
        assertThat(Probability.fromLabel("Not a label")).isEmpty();
        assertThat(Probability.fromLabel(null)).isEmpty();
    }

    @Test
    void validProbabilities_containsAllEnumLabels() {
        Set<String> labels = Probability.validProbabilities();
        for (Probability p : Probability.values()) {
            assertThat(labels).contains(p.label());
        }
    }

    @Test
    void values_areWithinZeroToOne() {
        for (Probability p : Probability.values()) {
            assertThat(p.value()).isBetween(0.0, 1.0);
        }
    }
}
