package mx.glucurvia.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConfidenceTest {
  @Test
  void bandasSinSolape() {
    assertThat(Confidence.of(0.75).band()).isEqualTo(ConfidenceBand.HIGH);
    assertThat(Confidence.of(0.749).band()).isEqualTo(ConfidenceBand.MEDIUM);
    assertThat(Confidence.of(0.5).band()).isEqualTo(ConfidenceBand.MEDIUM);
    assertThat(Confidence.of(0.499).band()).isEqualTo(ConfidenceBand.LOW);
  }

  @Test
  void rangoValidaInvariante() {
    assertThatThrownBy(() -> Range.of(10, 12, 15)).isInstanceOf(IllegalArgumentException.class);
    assertThat(Mgdl.fromMmolL(5.5).value())
        .isCloseTo(99.1, org.assertj.core.data.Offset.offset(0.1));
  }
}
