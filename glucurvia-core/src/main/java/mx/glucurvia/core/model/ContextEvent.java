package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento de contexto (actividad, sueño, estrés...) para etiquetar confusores (diseño 6.1, paso 12).
 */
public record ContextEvent(
    EventId id,
    EventType type,
    Instant startedAt,
    Instant endedAt,
    Map<String, Object> attributes) {
  public ContextEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(startedAt, "startedAt");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
