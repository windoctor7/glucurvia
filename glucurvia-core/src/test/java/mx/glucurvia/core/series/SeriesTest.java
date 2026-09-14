package mx.glucurvia.core.series;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import mx.glucurvia.core.model.ReadingType;
import org.junit.jupiter.api.Test;

class SeriesTest {
  private static final Instant T0 = Instant.parse("2026-09-13T12:00:00Z");

  private static Series everyFiveMin(double... values) {
    var list = new java.util.ArrayList<Reading>();
    for (int i = 0; i < values.length; i++) {
      list.add(Reading.of(T0.plus(Duration.ofMinutes(5L * i)), values[i], ReadingType.REALTIME));
    }
    return Series.of(list);
  }

  @Test
  void ordenaPorTiempoYRecortaInclusive() {
    Series s =
        Series.of(
            List.of(
                Reading.of(T0.plusSeconds(600), 110, ReadingType.HISTORIC),
                Reading.of(T0, 100, ReadingType.HISTORIC),
                Reading.of(T0.plusSeconds(300), 105, ReadingType.HISTORIC)));
    assertThat(s.first().orElseThrow().mgdl()).isEqualTo(100);
    assertThat(s.between(T0, T0.plusSeconds(300)).size()).isEqualTo(2);
  }

  @Test
  void medianaRangoYHuecos() {
    Series s = everyFiveMin(100, 120, 110, 130);
    assertThat(s.median().orElseThrow()).isEqualTo(115);
    assertThat(s.range()).isEqualTo(30);
    assertThat(s.medianGapSec().orElseThrow()).isEqualTo(300);
    assertThat(s.maxGapSec()).isEqualTo(300);
  }

  @Test
  void pendienteTheilSenEnMgdlPorMinuto() {
    Series s = everyFiveMin(100, 105, 110, 115);
    assertThat(s.theilSenSlopePerMin()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
  }

  @Test
  void excluyeRecortadasYFiltraPorTipo() {
    Series s =
        Series.of(
            new Reading(T0, mx.glucurvia.core.model.Mgdl.of(40), ReadingType.HISTORIC, true),
            Reading.of(T0.plusSeconds(60), 90, ReadingType.SCAN));
    assertThat(s.excludingClipped().size()).isEqualTo(1);
    assertThat(s.ofType(ReadingType.SCAN).size()).isEqualTo(1);
  }
}
