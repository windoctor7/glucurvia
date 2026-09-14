package mx.glucurvia.core.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Instante de un evento con su zona local vigente y la confianza en la hora (diseño 5.3).
 *
 * @param basis explicación de cómo se dedujo la hora ("dijo 'cené' a las 21:52"); puede ser null
 */
public record EventTime(
    Instant at, ZoneId localTz, TimeConfidence confidence, int uncertaintyMin, String basis) {
  public EventTime {
    Objects.requireNonNull(at, "at");
    Objects.requireNonNull(localTz, "localTz");
    Objects.requireNonNull(confidence, "confidence");
    if (uncertaintyMin < 0) {
      throw new IllegalArgumentException("uncertaintyMin negativo");
    }
  }

  public static EventTime exact(Instant at, ZoneId localTz) {
    return new EventTime(at, localTz, TimeConfidence.EXACT, 5, null);
  }

  public static EventTime approx(Instant at, ZoneId localTz, int uncertaintyMin, String basis) {
    return new EventTime(at, localTz, TimeConfidence.APPROX, uncertaintyMin, basis);
  }

  public static EventTime inferred(Instant at, ZoneId localTz, int uncertaintyMin, String basis) {
    return new EventTime(at, localTz, TimeConfidence.INFERRED, uncertaintyMin, basis);
  }
}
