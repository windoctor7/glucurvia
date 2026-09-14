package mx.glucurvia.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatsTest {
  @Test
  void percentilConInterpolacionComoPercentileCont() {
    double[] v = {10, 20, 30, 40};
    assertThat(Stats.median(v)).isEqualTo(25);
    assertThat(Stats.percentile(v, 0.9)).isEqualTo(37);
    assertThat(Stats.iqr(v)).isEqualTo(15);
  }

  @Test
  void theilSenIgnoraUnAtipico() {
    double[] x = {0, 1, 2, 3, 4};
    double[] y = {0, 1, 2, 30, 4};
    assertThat(Stats.theilSenSlope(x, y)).isEqualTo(1.0);
  }
}
