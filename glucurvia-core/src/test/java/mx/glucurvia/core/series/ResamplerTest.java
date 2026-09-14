package mx.glucurvia.core.series;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import mx.glucurvia.core.model.ReadingType;
import org.junit.jupiter.api.Test;

class ResamplerTest {
  private static final Instant T0 = Instant.parse("2026-09-13T12:00:00Z");
  private static final Duration STEP = Duration.ofMinutes(5);
  private static final Duration MAX_GAP = Duration.ofMinutes(20);
  private static final Duration TOL = Duration.ofMinutes(15);

  @Test
  void interpolaLinealmenteEntreLecturasCercanas() {
    Series s =
        Series.of(
            Reading.of(T0, 100, ReadingType.HISTORIC),
            Reading.of(T0.plus(Duration.ofMinutes(15)), 130, ReadingType.HISTORIC));
    List<Resampler.GridPoint> grid =
        Resampler.linear(s, T0, T0.plus(Duration.ofMinutes(15)), STEP, MAX_GAP, TOL);
    assertThat(grid)
        .extracting(Resampler.GridPoint::value)
        .containsExactly(100.0, 110.0, 120.0, 130.0);
    assertThat(grid).allMatch(Resampler.GridPoint::observed);
  }

  @Test
  void noInterpolaAtravesDeHuecosLargosNiExtrapola() {
    Series s =
        Series.of(
            Reading.of(T0, 100, ReadingType.HISTORIC),
            Reading.of(T0.plus(Duration.ofMinutes(40)), 140, ReadingType.HISTORIC));
    List<Resampler.GridPoint> grid =
        Resampler.linear(
            s,
            T0.minus(Duration.ofMinutes(10)),
            T0.plus(Duration.ofMinutes(60)),
            STEP,
            MAX_GAP,
            TOL);
    assertThat(grid)
        .extracting(Resampler.GridPoint::ts)
        .containsExactly(T0, T0.plus(Duration.ofMinutes(40)));
  }

  @Test
  void marcaComoNoObservadoElPuntoLejosDeLecturasReales() {
    Series s =
        Series.of(
            Reading.of(T0, 100, ReadingType.HISTORIC),
            Reading.of(T0.plus(Duration.ofMinutes(20)), 120, ReadingType.HISTORIC));
    List<Resampler.GridPoint> grid =
        Resampler.linear(
            s, T0, T0.plus(Duration.ofMinutes(20)), STEP, MAX_GAP, Duration.ofMinutes(4));
    assertThat(grid.get(2).observed()).isFalse();
    assertThat(grid.get(0).observed()).isTrue();
  }
}
