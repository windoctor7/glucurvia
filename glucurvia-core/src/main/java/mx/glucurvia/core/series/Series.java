package mx.glucurvia.core.series;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import mx.glucurvia.core.math.Stats;
import mx.glucurvia.core.model.ReadingType;

/**
 * Lecturas ordenadas por tiempo, inmutables. Utilidad pura compartida por cgm, glycemic e insights.
 */
public final class Series {
  private static final Series EMPTY = new Series(List.of());

  private final List<Reading> readings;

  private Series(List<Reading> sorted) {
    this.readings = sorted;
  }

  public static Series of(Collection<Reading> readings) {
    List<Reading> sorted = new ArrayList<>(readings);
    sorted.sort(Comparator.comparing(Reading::ts));
    return new Series(List.copyOf(sorted));
  }

  public static Series of(Reading... readings) {
    return of(List.of(readings));
  }

  public static Series empty() {
    return EMPTY;
  }

  public List<Reading> readings() {
    return readings;
  }

  public boolean isEmpty() {
    return readings.isEmpty();
  }

  public int size() {
    return readings.size();
  }

  public Optional<Reading> first() {
    return readings.isEmpty() ? Optional.empty() : Optional.of(readings.get(0));
  }

  public Optional<Reading> last() {
    return readings.isEmpty() ? Optional.empty() : Optional.of(readings.get(readings.size() - 1));
  }

  /** Lecturas con ts en [from, to], ambos inclusive. */
  public Series between(Instant from, Instant to) {
    return new Series(
        readings.stream().filter(r -> !r.ts().isBefore(from) && !r.ts().isAfter(to)).toList());
  }

  public Series ofTypes(Set<ReadingType> types) {
    return new Series(readings.stream().filter(r -> types.contains(r.type())).toList());
  }

  public Series ofType(ReadingType type) {
    return ofTypes(Set.of(type));
  }

  public Series excludingClipped() {
    return new Series(readings.stream().filter(r -> !r.clipped()).toList());
  }

  public Optional<Reading> lastAtOrBefore(Instant t) {
    Reading found = null;
    for (Reading r : readings) {
      if (r.ts().isAfter(t)) {
        break;
      }
      found = r;
    }
    return Optional.ofNullable(found);
  }

  public Optional<Reading> firstAtOrAfter(Instant t) {
    for (Reading r : readings) {
      if (!r.ts().isBefore(t)) {
        return Optional.of(r);
      }
    }
    return Optional.empty();
  }

  /** Última lectura dentro de [t - window, t]. */
  public Optional<Reading> lastWithin(Instant t, Duration window) {
    return between(t.minus(window), t).last();
  }

  public double[] values() {
    return readings.stream().mapToDouble(Reading::mgdl).toArray();
  }

  public OptionalDouble median() {
    return readings.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(Stats.median(values()));
  }

  /** máx − mín; 0 si hay menos de dos lecturas. */
  public double range() {
    if (readings.size() < 2) {
      return 0;
    }
    double[] v = values();
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (double x : v) {
      min = Math.min(min, x);
      max = Math.max(max, x);
    }
    return max - min;
  }

  public Optional<Reading> max() {
    return readings.stream().max(Comparator.comparingDouble(Reading::mgdl));
  }

  /**
   * Mediana del intervalo entre lecturas consecutivas, en segundos. Vacío con menos de dos
   * lecturas.
   */
  public OptionalInt medianGapSec() {
    if (readings.size() < 2) {
      return OptionalInt.empty();
    }
    double[] gaps = new double[readings.size() - 1];
    for (int i = 1; i < readings.size(); i++) {
      gaps[i - 1] = Duration.between(readings.get(i - 1).ts(), readings.get(i).ts()).toSeconds();
    }
    return OptionalInt.of((int) Math.round(Stats.median(gaps)));
  }

  /** Mayor hueco entre lecturas consecutivas, en segundos; 0 con menos de dos lecturas. */
  public int maxGapSec() {
    long max = 0;
    for (int i = 1; i < readings.size(); i++) {
      max =
          Math.max(
              max, Duration.between(readings.get(i - 1).ts(), readings.get(i).ts()).toSeconds());
    }
    return (int) max;
  }

  /** Pendiente robusta (Theil–Sen) en mg/dL por minuto; 0 con menos de dos lecturas. */
  public double theilSenSlopePerMin() {
    if (readings.size() < 2) {
      return 0;
    }
    Instant t0 = readings.get(0).ts();
    double[] x = new double[readings.size()];
    double[] y = new double[readings.size()];
    for (int i = 0; i < readings.size(); i++) {
      x[i] = Duration.between(t0, readings.get(i).ts()).toSeconds() / 60.0;
      y[i] = readings.get(i).mgdl();
    }
    return Stats.theilSenSlope(x, y);
  }
}
