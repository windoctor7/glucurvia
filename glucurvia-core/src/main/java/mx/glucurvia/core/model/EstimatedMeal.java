package mx.glucurvia.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Comida estimada: totales derivados de los ítems, nunca editados a mano (diseño 2.3).
 *
 * @param carbsBand banda de hidratos de la comida por raíz de suma de cuadrados × 1,3 (diseño 10.2)
 * @param carbsWorstCase suma de extremos, se muestra como "peor caso"
 * @param clarificationQuestion la única pregunta permitida, si la política la exige; puede ser null
 */
public record EstimatedMeal(
    MealType mealType,
    EventTime time,
    List<EstimatedItem> items,
    Nutrients totals,
    Range carbsBand,
    Range carbsWorstCase,
    Confidence confidence,
    String estimatorVersion,
    String clarificationQuestion) {
  public EstimatedMeal {
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(totals, "totals");
    Objects.requireNonNull(carbsBand, "carbsBand");
    Objects.requireNonNull(carbsWorstCase, "carbsWorstCase");
    Objects.requireNonNull(confidence, "confidence");
    Objects.requireNonNull(estimatorVersion, "estimatorVersion");
    items = items == null ? List.of() : List.copyOf(items);
  }
}
