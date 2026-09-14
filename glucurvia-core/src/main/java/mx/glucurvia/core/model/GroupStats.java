package mx.glucurvia.core.model;

import java.util.List;
import java.util.Map;

/** Estadísticos de un grupo de comidas (diseño 6.4). Medianas nulas si el grupo está vacío. */
public record GroupStats(
    int n,
    int nGood,
    SeriesSource seriesSource,
    Double medianDeltaPeak,
    Double iqrDeltaPeak,
    Double medianIauc120,
    Double medianTimeToPeak,
    Double medianMinutesAbove,
    Double medianCarbsG,
    Map<String, Integer> contextTagCounts,
    List<EventId> mealIds) {
  public GroupStats {
    contextTagCounts = contextTagCounts == null ? Map.of() : Map.copyOf(contextTagCounts);
    mealIds = mealIds == null ? List.of() : List.copyOf(mealIds);
  }
}
