package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import mx.glucurvia.core.adapter.GlycemicResponseReader;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.GlycemicResponse;
import mx.glucurvia.core.model.Quality;
import mx.glucurvia.core.model.SeriesSource;
import org.junit.jupiter.api.Test;

public abstract class AbstractGlycemicResponseReaderContract {
  protected abstract GlycemicResponseReader subjectWith(List<GlycemicResponse> responses);

  public static GlycemicResponse sample(EventId meal, Quality q, Double deltaPeak) {
    Instant now = Instant.parse("2026-09-13T22:00:00Z");
    return new GlycemicResponse(
        meal,
        "test-1",
        now,
        SeriesSource.REALTIME,
        60,
        0,
        q,
        95,
        5,
        100.0,
        5,
        false,
        deltaPeak == null ? null : 100 + deltaPeak,
        now,
        45,
        deltaPeak,
        null,
        null,
        now,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        now,
        Set.of(),
        Set.of(),
        Set.of());
  }

  @Test
  void devuelveLaRespuestaDeLaComidaYVacioSiNoExiste() {
    EventId a = EventId.random();
    GlycemicResponseReader r = subjectWith(List.of(sample(a, Quality.GOOD, 55.0)));
    assertThat(r.forMeal(a)).map(GlycemicResponse::deltaPeakMgdl).contains(55.0);
    assertThat(r.forMeal(EventId.random())).isEmpty();
  }
}
