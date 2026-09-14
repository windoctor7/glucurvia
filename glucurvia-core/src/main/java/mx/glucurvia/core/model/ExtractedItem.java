package mx.glucurvia.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Un ítem tal como lo extrajo el LLM (diseño 8.4). Es la frontera entre el LLM y el estimador: el
 * LLM propone alimento, preparación, cantidad y gramos con rango; los nutrientes y la confianza los
 * pone Java.
 *
 * @param raw texto original del usuario ("4 cucharadas de avena")
 * @param food alimento canónico en español de México ("avena en hojuelas")
 * @param preparation cruda, cocida, frita, horneada...; puede ser null
 * @param brand marca si la dijo el usuario; puede ser null
 * @param quantity cantidad en la unidad dada; puede ser null
 * @param unit unidad casera ("cucharada copeteada", "pieza", "ml"); puede ser null
 * @param grams gramos estimados con rango; null si el ítem viene en mililitros
 * @param volumeMl mililitros para líquidos; null si no aplica
 * @param portionEaten fracción comida (1.0 todo; 0.5 "dejé la mitad")
 * @param recipe subítems y rendimiento para platos caseros; null si no aplica
 * @param assumptions supuestos que el LLM declaró
 * @param selfReportedConfidence autoevaluación del LLM; solo se registra, nunca decide; puede ser
 *     null
 * @param fallbackPer100g macros propuestos por el LLM, usados solo si el catálogo no tiene el
 *     alimento; puede ser null
 */
public record ExtractedItem(
    String raw,
    String food,
    String preparation,
    String brand,
    Double quantity,
    String unit,
    Range grams,
    Double volumeMl,
    double portionEaten,
    Recipe recipe,
    List<String> assumptions,
    Double selfReportedConfidence,
    MacrosPer100g fallbackPer100g) {
  public ExtractedItem {
    Objects.requireNonNull(raw, "raw");
    Objects.requireNonNull(food, "food");
    assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    if (portionEaten <= 0 || portionEaten > 1) {
      throw new IllegalArgumentException("portionEaten fuera de (0,1]: " + portionEaten);
    }
    if (grams == null && volumeMl == null && recipe == null) {
      throw new IllegalArgumentException("un ítem necesita gramos, mililitros o receta: " + raw);
    }
  }

  /** Receta casera: rendimiento total y subítems (diseño 8.4). */
  public record Recipe(double yieldG, List<ExtractedItem> subitems) {
    public Recipe {
      if (yieldG <= 0) {
        throw new IllegalArgumentException("yieldG debe ser positivo");
      }
      subitems = List.copyOf(Objects.requireNonNull(subitems, "subitems"));
    }
  }
}
