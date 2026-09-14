package mx.glucurvia.core.adapter;

import java.util.Optional;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.GlycemicResponse;

/**
 * Respuesta glucémica calculada de una comida. Dueño: glycemic. Consumidores: insights, assistant.
 */
public interface GlycemicResponseReader {
  Optional<GlycemicResponse> forMeal(EventId mealEventId);
}
