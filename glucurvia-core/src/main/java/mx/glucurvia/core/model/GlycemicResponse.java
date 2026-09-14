package mx.glucurvia.core.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Respuesta glucémica de una comida (diseño 3, glycemic_responses; 6.1). Campos numéricos nulos
 * cuando no se pudieron calcular. Determinista y versionada.
 */
public record GlycemicResponse(
    EventId mealEventId,
    String algorithmVersion,
    Instant computedAt,
    SeriesSource seriesSource,
    int cadenceSec,
    double scanFraction,
    Quality quality,
    double coveragePct,
    Integer maxGapMin,
    Double baselineMgdl,
    int baselineN,
    boolean baselineUnstable,
    Double peakMgdl,
    Instant peakAt,
    Integer timeToPeakMin,
    Double deltaPeakMgdl,
    FixedPoints fixed,
    Integer returnToBaselineMin,
    Instant iaucWindowStart,
    Double iauc0to120,
    Double iauc0to180,
    Double maxRiseRate,
    Double maxFallRate,
    Integer minutesAbovePersonalP90,
    Double personalP90Mgdl,
    LocalDate personalRangePeriodEnd,
    Instant windowEnd,
    Set<String> flags,
    Set<String> contextTags,
    Set<EventId> confounderEventIds) {

  public static final String FLAG_IN_PROGRESS = "IN_PROGRESS";
  public static final String FLAG_PEAK_AT_EDGE = "PEAK_AT_EDGE";
  public static final String FLAG_TRUNCATED_BY_NEXT_MEAL = "TRUNCATED_BY_NEXT_MEAL";
  public static final String FLAG_NO_EXCURSION = "NO_EXCURSION";
  public static final String FLAG_NO_RETURN_IN_WINDOW = "NO_RETURN_IN_WINDOW";

  public GlycemicResponse {
    Objects.requireNonNull(mealEventId, "mealEventId");
    Objects.requireNonNull(algorithmVersion, "algorithmVersion");
    Objects.requireNonNull(computedAt, "computedAt");
    Objects.requireNonNull(seriesSource, "seriesSource");
    Objects.requireNonNull(quality, "quality");
    fixed = fixed == null ? FixedPoints.EMPTY : fixed;
    flags = flags == null ? Set.of() : Set.copyOf(flags);
    contextTags = contextTags == null ? Set.of() : Set.copyOf(contextTags);
    confounderEventIds = confounderEventIds == null ? Set.of() : Set.copyOf(confounderEventIds);
  }

  public boolean inProgress() {
    return flags.contains(FLAG_IN_PROGRESS);
  }

  /** Valores interpolados a tiempo fijo tras la comida; nulos si no hay lectura real cerca. */
  public record FixedPoints(Double g30, Double g60, Double g90, Double g120, Double g180) {
    public static final FixedPoints EMPTY = new FixedPoints(null, null, null, null, null);
  }
}
