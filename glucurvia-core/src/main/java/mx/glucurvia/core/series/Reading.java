package mx.glucurvia.core.series;

import java.time.Instant;
import java.util.Objects;
import mx.glucurvia.core.model.Mgdl;
import mx.glucurvia.core.model.ReadingType;

/** Una lectura del sensor (diseño 3, glucose_readings). */
public record Reading(Instant ts, Mgdl value, ReadingType type, boolean clipped) {
  public Reading {
    Objects.requireNonNull(ts, "ts");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(type, "type");
  }

  public static Reading of(Instant ts, double mgdl, ReadingType type) {
    return new Reading(ts, Mgdl.of(mgdl), type, false);
  }

  public double mgdl() {
    return value.value();
  }
}
