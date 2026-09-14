package mx.glucurvia.core.series;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Rejilla de paso fijo por interpolación lineal (diseño 6.1, paso 3). Solo interpola entre lecturas
 * separadas como máximo {@code maxGap}; los puntos sin pareja válida se omiten, no se extrapolan.
 */
public final class Resampler {
  private Resampler() {}

  /**
   * @param observed hay una lectura real a como máximo {@code observedTolerance} del punto
   */
  public record GridPoint(Instant ts, double value, boolean observed) {}

  public static List<GridPoint> linear(
      Series series,
      Instant start,
      Instant end,
      Duration step,
      Duration maxGap,
      Duration observedTolerance) {
    if (step.isZero() || step.isNegative()) {
      throw new IllegalArgumentException("step debe ser positivo");
    }
    List<Reading> rs = series.readings();
    List<GridPoint> grid = new ArrayList<>();
    if (rs.isEmpty()) {
      return grid;
    }
    int i = 0;
    for (Instant t = start; !t.isAfter(end); t = t.plus(step)) {
      while (i < rs.size() - 1 && rs.get(i + 1).ts().compareTo(t) <= 0) {
        i++;
      }
      Reading prev = rs.get(i).ts().compareTo(t) <= 0 ? rs.get(i) : null;
      Reading next = null;
      for (int j = i; j < rs.size(); j++) {
        if (!rs.get(j).ts().isBefore(t)) {
          next = rs.get(j);
          break;
        }
      }
      Double value = null;
      if (prev != null && prev.ts().equals(t)) {
        value = prev.mgdl();
      } else if (prev != null && next != null) {
        long span = Duration.between(prev.ts(), next.ts()).toSeconds();
        if (span <= maxGap.toSeconds() && span > 0) {
          long offset = Duration.between(prev.ts(), t).toSeconds();
          value = prev.mgdl() + (next.mgdl() - prev.mgdl()) * ((double) offset / span);
        }
      }
      if (value != null) {
        boolean observed =
            (prev != null && Duration.between(prev.ts(), t).abs().compareTo(observedTolerance) <= 0)
                || (next != null
                    && Duration.between(t, next.ts()).abs().compareTo(observedTolerance) <= 0);
        grid.add(new GridPoint(t, value, observed));
      }
    }
    return grid;
  }
}
