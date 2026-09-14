package mx.glucurvia.core.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Vista de una comida para glycemic e insights (plan 4.1). Lleva lo que insights necesita para el
 * ruido propio: hidratos y alimento dominante.
 *
 * @param dominantFoodRefId fila del catálogo del ítem con más hidratos; puede ser null
 * @param foods nombres canónicos de los ítems, para filtros "contiene"
 */
public record MealEvent(
    EventId id,
    UserId userId,
    Instant startedAt,
    ZoneId localTz,
    TimeConfidence timeConfidence,
    int uncertaintyMin,
    MealType mealType,
    double carbsG,
    Confidence confidence,
    UUID dominantFoodRefId,
    Set<String> foods,
    Set<String> tags) {
  public MealEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(localTz, "localTz");
    Objects.requireNonNull(timeConfidence, "timeConfidence");
    Objects.requireNonNull(confidence, "confidence");
    foods = foods == null ? Set.of() : Set.copyOf(foods);
    tags = tags == null ? Set.of() : Set.copyOf(tags);
  }
}
