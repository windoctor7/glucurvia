package mx.glucurvia.core.model;

import java.util.Objects;
import java.util.UUID;

/** Identificador de usuario. Hay una sola fila en users, pero user_id viaja en todo (plan 1.3). */
public record UserId(UUID value) {
  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId of(String uuid) {
    return new UserId(UUID.fromString(uuid));
  }

  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
