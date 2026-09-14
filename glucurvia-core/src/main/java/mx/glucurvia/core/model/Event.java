package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Evento persistido del timeline. {@code meal} es null salvo para MEAL. */
public record Event(
    EventId id,
    UserId userId,
    EventType type,
    EventTime time,
    Instant endedAt,
    String source,
    String rawText,
    UUID messageId,
    Map<String, Object> attributes,
    EstimatedMeal meal,
    Instant createdAt) {
  public Event {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(createdAt, "createdAt");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  public Instant startedAt() {
    return time.at();
  }
}
