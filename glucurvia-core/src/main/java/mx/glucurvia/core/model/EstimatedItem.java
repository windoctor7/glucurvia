package mx.glucurvia.core.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Ítem ya estimado por nutrition (diseño 9.1). Se persiste tal cual en meal_items.
 *
 * @param foodRefId fila del catálogo usada; null si no hubo match
 * @param grams gramos con rango, ya derivados de mililitros si hacía falta
 * @param carbsRange hidratos disponibles con rango (punto = nutrients.carbsG)
 * @param llmConfidence autoevaluación del LLM, solo para evaluación; puede ser null
 */
public record EstimatedItem(
    String raw,
    String foodName,
    String preparation,
    UUID foodRefId,
    Double quantity,
    String unit,
    Range grams,
    Double volumeMl,
    double portionEaten,
    Nutrients nutrients,
    Range carbsRange,
    Confidence confidence,
    Double llmConfidence,
    MacroSource macroSource,
    List<String> assumptions,
    boolean needsClarification) {
  public EstimatedItem {
    Objects.requireNonNull(raw, "raw");
    Objects.requireNonNull(foodName, "foodName");
    Objects.requireNonNull(grams, "grams");
    Objects.requireNonNull(nutrients, "nutrients");
    Objects.requireNonNull(carbsRange, "carbsRange");
    Objects.requireNonNull(confidence, "confidence");
    Objects.requireNonNull(macroSource, "macroSource");
    assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
  }
}
