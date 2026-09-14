package mx.glucurvia.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Corrección de un ítem que se convierte en alias del usuario (diseño 10.4).
 *
 * @param alias texto con el que el usuario nombra el alimento ("mi tostada")
 * @param foodRefId fila del catálogo a la que apunta; puede ser null si viene con per100g
 * @param grams gramos de la porción del usuario; puede ser null
 * @param per100g composición de un producto concreto con etiqueta; puede ser null
 * @param learnedFrom evento del que salió la corrección; puede ser null
 */
public record CorrectedItem(
    String alias, UUID foodRefId, Double grams, MacrosPer100g per100g, EventId learnedFrom) {
  public CorrectedItem {
    Objects.requireNonNull(alias, "alias");
    if (foodRefId == null && per100g == null && grams == null) {
      throw new IllegalArgumentException("una corrección necesita catálogo, macros o gramos");
    }
  }
}
