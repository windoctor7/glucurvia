package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Filtro de comidas para get_meals y compare (diseño 8.2). Todos los campos salvo usuario y rango
 * pueden ser null o vacíos = sin filtro.
 */
public record MealFilter(
    UserId userId,
    Instant from,
    Instant to,
    MealType mealType,
    Set<String> containsAll,
    Set<String> excludes,
    Set<String> tagsAny,
    Quality minQuality,
    Confidence minConfidence) {
  public MealFilter {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    containsAll = containsAll == null ? Set.of() : Set.copyOf(containsAll);
    excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
    tagsAny = tagsAny == null ? Set.of() : Set.copyOf(tagsAny);
  }

  public static MealFilter between(UserId userId, Instant from, Instant to) {
    return new MealFilter(userId, from, to, null, null, null, null, null, null);
  }
}
