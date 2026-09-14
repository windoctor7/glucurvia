package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Map;

/**
 * Corrección parcial de un evento (diseño 5.4). Todo puede ser null = "no cambia".
 *
 * @param clearEndedAt true para borrar endedAt
 */
public record EventPatch(
    EventTime time,
    Instant endedAt,
    boolean clearEndedAt,
    Map<String, Object> attributes,
    EstimatedMeal meal) {
  public static EventPatch ofTime(EventTime time) {
    return new EventPatch(time, null, false, null, null);
  }

  public static EventPatch ofMeal(EstimatedMeal meal) {
    return new EventPatch(null, null, false, null, meal);
  }
}
