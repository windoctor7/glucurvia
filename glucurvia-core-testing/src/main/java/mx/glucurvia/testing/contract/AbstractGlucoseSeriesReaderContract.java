package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import mx.glucurvia.core.adapter.GlucoseSeriesReader;
import mx.glucurvia.core.model.ReadingType;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.series.Reading;
import org.junit.jupiter.api.Test;

/** Comportamiento que deben cumplir el fake y la implementación real de GlucoseSeriesReader. */
public abstract class AbstractGlucoseSeriesReaderContract {
  protected static final Instant T0 = Instant.parse("2026-09-13T12:00:00Z");

  /** Devuelve un lector que ya contiene las lecturas dadas para el usuario. */
  protected abstract GlucoseSeriesReader subjectWith(UserId user, List<Reading> readings);

  protected UserId user() {
    return UserId.random();
  }

  @Test
  void betweenDevuelveOrdenadoEInclusive() {
    UserId u = user();
    GlucoseSeriesReader r =
        subjectWith(
            u,
            List.of(
                Reading.of(T0.plusSeconds(600), 110, ReadingType.HISTORIC),
                Reading.of(T0, 100, ReadingType.HISTORIC),
                Reading.of(T0.plusSeconds(300), 105, ReadingType.SCAN)));
    var s = r.between(u, T0, T0.plusSeconds(600));
    assertThat(s.readings()).extracting(Reading::mgdl).containsExactly(100.0, 105.0, 110.0);
    assertThat(r.between(u, T0.plusSeconds(1), T0.plusSeconds(599)).size()).isEqualTo(1);
  }

  @Test
  void latestEsLaMasRecienteYVacioSinLecturas() {
    UserId u = user();
    GlucoseSeriesReader r =
        subjectWith(
            u,
            List.of(
                Reading.of(T0, 100, ReadingType.REALTIME),
                Reading.of(T0.plusSeconds(60), 101, ReadingType.REALTIME)));
    assertThat(r.latest(u)).map(Reading::mgdl).contains(101.0);
    assertThat(r.latest(user())).isEmpty();
  }

  @Test
  void rangoPersonalUsaLaFamiliaPrimariaYExcluyeRecortadas() {
    UserId u = user();
    var list = new java.util.ArrayList<Reading>();
    for (int i = 0; i < 100; i++) {
      list.add(Reading.of(T0.plus(Duration.ofMinutes(i)), 90 + i, ReadingType.REALTIME));
    }
    list.add(
        new Reading(
            T0.plus(Duration.ofMinutes(200)),
            mx.glucurvia.core.model.Mgdl.of(500),
            ReadingType.REALTIME,
            true));
    list.add(Reading.of(T0.plus(Duration.ofMinutes(300)), 400, ReadingType.HISTORIC));
    GlucoseSeriesReader r = subjectWith(u, list);
    var pr = r.personalRange(u, LocalDate.ofInstant(T0, ZoneOffset.UTC), 14);
    assertThat(pr.readingsCount()).isEqualTo(100);
    assertThat(pr.p10Mgdl()).isLessThan(pr.p50Mgdl());
    assertThat(pr.p50Mgdl()).isLessThan(pr.p90Mgdl());
    assertThat(pr.p90Mgdl()).isLessThan(200);
  }
}
