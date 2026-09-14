package mx.glucurvia.core.math;

import java.util.Arrays;

/** Estadísticos puros. Percentil con interpolación lineal, como percentile_cont de PostgreSQL. */
public final class Stats {
  private Stats() {}

  public static double median(double[] values) {
    return percentile(values, 0.5);
  }

  public static double percentile(double[] values, double p) {
    if (values.length == 0) {
      throw new IllegalArgumentException("sin valores");
    }
    if (p < 0 || p > 1) {
      throw new IllegalArgumentException("p fuera de [0,1]");
    }
    double[] s = values.clone();
    Arrays.sort(s);
    double pos = p * (s.length - 1);
    int lo = (int) Math.floor(pos);
    int hi = (int) Math.ceil(pos);
    if (lo == hi) {
      return s[lo];
    }
    return s[lo] + (s[hi] - s[lo]) * (pos - lo);
  }

  public static double iqr(double[] values) {
    return percentile(values, 0.75) - percentile(values, 0.25);
  }

  public static double mean(double[] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("sin valores");
    }
    double sum = 0;
    for (double v : values) {
      sum += v;
    }
    return sum / values.length;
  }

  /** Mediana de las pendientes entre todos los pares (Theil–Sen). 0 con menos de dos puntos. */
  public static double theilSenSlope(double[] x, double[] y) {
    if (x.length != y.length) {
      throw new IllegalArgumentException("x e y de distinta longitud");
    }
    if (x.length < 2) {
      return 0;
    }
    double[] slopes = new double[x.length * (x.length - 1) / 2];
    int k = 0;
    for (int i = 0; i < x.length; i++) {
      for (int j = i + 1; j < x.length; j++) {
        double dx = x[j] - x[i];
        if (dx != 0) {
          slopes[k++] = (y[j] - y[i]) / dx;
        }
      }
    }
    return k == 0 ? 0 : median(Arrays.copyOf(slopes, k));
  }
}
