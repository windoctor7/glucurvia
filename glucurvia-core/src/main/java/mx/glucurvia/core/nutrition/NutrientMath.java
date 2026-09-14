package mx.glucurvia.core.nutrition;

import java.util.List;
import mx.glucurvia.core.model.Confidence;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.Range;

/** Reglas puras de agregación de la incertidumbre nutricional (diseño 10.2). */
public final class NutrientMath {
  public static final double BAND_FACTOR = 1.3;

  private NutrientMath() {}

  /**
   * Banda de la comida: punto = suma de puntos; semianchura = max(1,3 × sqrt(Σ semianchura_i²),
   * max_i semianchura_i). Conservadora sin exigir independencia.
   */
  public static Range mealBand(List<Range> items) {
    double point = 0;
    double sumSq = 0;
    double maxHw = 0;
    for (Range r : items) {
      point += r.point();
      double hw = r.halfWidth();
      sumSq += hw * hw;
      maxHw = Math.max(maxHw, hw);
    }
    double hw = Math.max(BAND_FACTOR * Math.sqrt(sumSq), maxHw);
    return new Range(point, Math.max(0, point - hw), point + hw);
  }

  /** Confianza de la comida: media ponderada por aporte de hidratos. Sin ítems, 0. */
  public static Confidence weightedByCarbs(List<EstimatedItem> items) {
    double carbs = 0;
    double weighted = 0;
    for (EstimatedItem it : items) {
      double c = Math.max(0, it.nutrients().carbsG());
      carbs += c;
      weighted += c * it.confidence().value();
    }
    if (carbs == 0) {
      return items.isEmpty()
          ? Confidence.of(0)
          : Confidence.of(
              items.stream().mapToDouble(i -> i.confidence().value()).average().orElse(0));
    }
    return Confidence.of(weighted / carbs);
  }
}
