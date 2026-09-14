package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Búsqueda de eventos; sirve para localizar "la cena de ayer" antes de corregirla (diseño 5.4). */
public record EventQuery(
    UserId userId, Instant from, Instant to, Set<EventType> types, String textContains) {
  public EventQuery {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    types = types == null ? Set.of() : Set.copyOf(types);
  }

  public boolean matchesType(EventType t) {
    return types.isEmpty() || types.contains(t);
  }
}
