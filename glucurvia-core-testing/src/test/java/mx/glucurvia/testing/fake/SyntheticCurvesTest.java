package mx.glucurvia.testing.fake;

import static org.assertj.core.api.Assertions.assertThat;

import mx.glucurvia.testing.builders.SyntheticCurves;
import org.junit.jupiter.api.Test;

class SyntheticCurvesTest {
  @Test
  void hayNueveCurvasConNombreYLecturas() {
    var all = SyntheticCurves.all();
    assertThat(all).hasSize(9);
    assertThat(all).allMatch(c -> !c.series().isEmpty() && c.name() != null);
    assertThat(SyntheticCurves.classicPeak15().series().max().orElseThrow().mgdl()).isEqualTo(155);
    assertThat(SyntheticCurves.clippedHigh().series().excludingClipped().max().orElseThrow().mgdl())
        .isLessThan(500);
  }
}
