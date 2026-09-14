package mx.glucurvia.core.model;

import java.util.Collection;

/**
 * Estimación con incertidumbre: punto más intervalo plausible (~80 %), nunca un número solo (diseño
 * 10.1). Invariante: low <= point <= high.
 */
public record Range(double point, double low, double high) {
  public Range {
    if (Double.isNaN(point)
        || Double.isNaN(low)
        || Double.isNaN(high)
        || !(low <= point && point <= high)) {
      throw new IllegalArgumentException("rango inválido: " + low + " <= " + point + " <= " + high);
    }
  }

  public static Range of(double point, double low, double high) {
    return new Range(point, low, high);
  }

  public static Range exact(double value) {
    return new Range(value, value, value);
  }

  public double width() {
    return high - low;
  }

  public double halfWidth() {
    return width() / 2;
  }

  /** Anchura relativa al punto; infinita si el punto es cero. */
  public double relativeWidth() {
    return point == 0 ? Double.POSITIVE_INFINITY : width() / point;
  }

  public Range scale(double factor) {
    if (factor < 0) {
      throw new IllegalArgumentException("factor negativo");
    }
    return new Range(point * factor, low * factor, high * factor);
  }

  /** Suma de extremos: el peor caso (diseño 10.2). Para la banda de comida usar NutrientMath. */
  public Range plus(Range other) {
    return new Range(point + other.point, low + other.low, high + other.high);
  }

  public static Range sumWorstCase(Collection<Range> ranges) {
    return ranges.stream().reduce(Range.exact(0), Range::plus);
  }
}
