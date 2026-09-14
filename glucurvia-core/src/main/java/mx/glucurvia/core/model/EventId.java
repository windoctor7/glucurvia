package mx.glucurvia.core.model;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un evento del timeline (comida, actividad, sueño...). */
public record EventId(UUID value) {
  public EventId {
    Objects.requireNonNull(value, "value");
  }

  public static EventId of(String uuid) {
    return new EventId(UUID.fromString(uuid));
  }

  public static EventId random() {
    return new EventId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
