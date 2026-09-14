package mx.glucurvia.core.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Resultado de un pull a la fuente CGM (diseño 4.4).
 *
 * @param latestReadingTs última lectura conocida tras el pull; puede ser null si no hay ninguna
 */
public record SyncResult(
    boolean sourceReachable, int newReadings, Instant latestReadingTs, Duration elapsed) {
  public static SyncResult unreachable(Duration elapsed, Instant latestKnown) {
    return new SyncResult(false, 0, latestKnown, elapsed);
  }
}
