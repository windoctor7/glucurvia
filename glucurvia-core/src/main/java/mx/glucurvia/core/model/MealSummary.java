package mx.glucurvia.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Comida con su respuesta resumida, para listar y narrar (diseño 8.2). Los campos de respuesta son
 * nulos si aún no se calculó.
 */
public record MealSummary(
    EventId eventId,
    Instant startedAt,
    MealType mealType,
    double carbsG,
    Confidence confidence,
    Set<String> foods,
    Set<String> tags,
    Quality quality,
    SeriesSource seriesSource,
    Double deltaPeakMgdl,
    Double iauc0to120,
    Integer timeToPeakMin,
    Integer minutesAbovePersonalP90,
    boolean inProgress) {
  public MealSummary {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(confidence, "confidence");
    foods = foods == null ? Set.of() : Set.copyOf(foods);
    tags = tags == null ? Set.of() : Set.copyOf(tags);
  }

  public Double metric(Metric m) {
    return switch (m) {
      case DELTA_PEAK -> deltaPeakMgdl;
      case IAUC_0_120 -> iauc0to120;
      case TIME_TO_PEAK -> timeToPeakMin == null ? null : timeToPeakMin.doubleValue();
      case MINUTES_ABOVE_P90 ->
          minutesAbovePersonalP90 == null ? null : minutesAbovePersonalP90.doubleValue();
    };
  }
}
