package mx.glucurvia.core.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import mx.glucurvia.core.model.ContextEvent;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.MealEvent;
import mx.glucurvia.core.model.UserId;

/**
 * Comidas y contexto para el algoritmo y las consultas. Dueño: journal. Consumidores: glycemic,
 * insights.
 */
public interface MealEventReader {
  List<MealEvent> mealsBetween(UserId user, Instant from, Instant to);

  /** Eventos no MEAL de los tipos dados (vacío = todos) con startedAt en [from, to]. */
  List<ContextEvent> contextBetween(UserId user, Instant from, Instant to, Set<EventType> types);
}
