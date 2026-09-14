package mx.glucurvia.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Comida tal como la extrajo el LLM (diseño 8.4), antes de estimar nutrientes.
 *
 * @param repeatOfEventRef "lo mismo que ayer"; texto libre que Java resuelve (diseño 5.4); puede
 *     ser null
 * @param clarification pregunta que el LLM propone hacer; Java decide si se hace (diseño 10.3);
 *     puede ser null
 */
public record ExtractedMeal(
    EventTime time,
    MealType mealType,
    String repeatOfEventRef,
    List<ExtractedItem> items,
    String clarification) {
  public ExtractedMeal {
    Objects.requireNonNull(time, "time");
    items = items == null ? List.of() : List.copyOf(items);
  }
}
