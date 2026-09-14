package mx.glucurvia.core.model;

import java.util.List;

/**
 * Comparación de dos grupos (diseño 6.4). Devuelve números y su calidad, nunca conclusiones.
 *
 * @param withinMealNoiseMgdl mediana de |Δ delta_peak| entre repeticiones de la misma comida; null
 *     si no hay repeticiones
 */
public record Comparison(
    GroupStats a,
    GroupStats b,
    boolean sufficientEvidence,
    boolean notableDifference,
    Double withinMealNoiseMgdl,
    List<String> caveats) {
  public static final int MIN_N = 5;
  public static final double MIN_DELTA_PEAK_DIFF_MGDL = 20.0;
  public static final double MIN_IAUC_RELATIVE_DIFF = 0.30;
  public static final double MAX_CARBS_RELATIVE_DIFF = 0.20;

  public Comparison {
    caveats = caveats == null ? List.of() : List.copyOf(caveats);
  }
}
