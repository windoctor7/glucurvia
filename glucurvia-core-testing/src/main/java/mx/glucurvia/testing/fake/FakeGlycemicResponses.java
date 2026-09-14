package mx.glucurvia.testing.fake;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mx.glucurvia.core.adapter.GlycemicResponseReader;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.GlycemicResponse;

public final class FakeGlycemicResponses implements GlycemicResponseReader {
  private final Map<EventId, GlycemicResponse> responses = new HashMap<>();

  public FakeGlycemicResponses put(GlycemicResponse r) {
    responses.put(r.mealEventId(), r);
    return this;
  }

  @Override
  public Optional<GlycemicResponse> forMeal(EventId mealEventId) {
    return Optional.ofNullable(responses.get(mealEventId));
  }
}
