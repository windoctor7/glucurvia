package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento a registrar (diseño 5). Para MEAL, {@code meal} lleva la comida ya estimada por nutrition:
 * journal guarda lo que recibe y no llama al estimador (plan 4.1).
 *
 * @param endedAt fin del evento (sueño, actividad); puede ser null
 * @param rawText texto original del usuario; puede ser null si el origen no es el chat
 * @param messageId mensaje de chat que lo originó; puede ser null
 * @param attributes datos por tipo (diseño 5.2)
 * @param meal solo para MEAL; null en el resto
 */
public record NewEvent(
    UserId userId,
    EventType type,
    EventTime time,
    Instant endedAt,
    String source,
    String rawText,
    UUID messageId,
    Map<String, Object> attributes,
    EstimatedMeal meal) {
  public NewEvent {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(source, "source");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    if (type == EventType.MEAL && meal == null) {
      throw new IllegalArgumentException("un evento MEAL necesita la comida estimada");
    }
    if (type != EventType.MEAL && meal != null) {
      throw new IllegalArgumentException("solo MEAL lleva comida");
    }
  }
}
