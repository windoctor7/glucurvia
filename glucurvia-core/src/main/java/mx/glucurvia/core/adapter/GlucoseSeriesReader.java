package mx.glucurvia.core.adapter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import mx.glucurvia.core.model.PersonalRange;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.series.Reading;
import mx.glucurvia.core.series.Series;

/** Lecturas de glucosa. Dueño: cgm. Consumidores: glycemic, insights, assistant. */
public interface GlucoseSeriesReader {
  /** Todas las lecturas (todos los tipos) con ts en [from, to]. */
  Series between(UserId user, Instant from, Instant to);

  Optional<Reading> latest(UserId user);

  /**
   * Percentiles de la familia primaria del usuario en los últimos {@code windowDays} hasta asOf.
   */
  PersonalRange personalRange(UserId user, LocalDate asOf, int windowDays);
}
