package mx.glucurvia.testing.fake;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mx.glucurvia.core.adapter.InsightsQueries;
import mx.glucurvia.core.math.Stats;
import mx.glucurvia.core.model.Comparison;
import mx.glucurvia.core.model.GlucoseSummary;
import mx.glucurvia.core.model.GroupStats;
import mx.glucurvia.core.model.MealFilter;
import mx.glucurvia.core.model.MealSummary;
import mx.glucurvia.core.model.Metric;
import mx.glucurvia.core.model.SeriesSource;
import mx.glucurvia.core.model.UserId;

/**
 * Consultas en memoria sobre resúmenes añadidos a mano. Aplica las reglas de comparación del diseño
 * 6.4 exactamente como debe hacerlo la implementación real (y el test de contrato lo exige).
 */
public final class FakeInsights implements InsightsQueries {
  private final Map<UserId, List<MealSummary>> meals = new HashMap<>();
  private Double withinMealNoise;
  private GlucoseSummary summary;

  public FakeInsights add(UserId user, MealSummary... summaries) {
    meals.computeIfAbsent(user, k -> new ArrayList<>()).addAll(List.of(summaries));
    return this;
  }

  public FakeInsights withinMealNoise(Double mgdl) {
    this.withinMealNoise = mgdl;
    return this;
  }

  public FakeInsights summary(GlucoseSummary s) {
    this.summary = s;
    return this;
  }

  @Override
  public List<MealSummary> meals(MealFilter f) {
    return meals.getOrDefault(f.userId(), List.of()).stream()
        .filter(m -> !m.startedAt().isBefore(f.from()) && !m.startedAt().isAfter(f.to()))
        .filter(m -> f.mealType() == null || f.mealType() == m.mealType())
        .filter(m -> m.foods().containsAll(f.containsAll()))
        .filter(m -> f.excludes().stream().noneMatch(m.foods()::contains))
        .filter(m -> f.tagsAny().isEmpty() || f.tagsAny().stream().anyMatch(m.tags()::contains))
        .filter(
            m ->
                f.minQuality() == null
                    || (m.quality() != null && m.quality().ordinal() <= f.minQuality().ordinal()))
        .filter(m -> f.minConfidence() == null || m.confidence().compareTo(f.minConfidence()) >= 0)
        .sorted(Comparator.comparing(MealSummary::startedAt).reversed())
        .toList();
  }

  @Override
  public Comparison compare(MealFilter a, MealFilter b, Metric metric) {
    List<MealSummary> ga = usable(meals(a), metric);
    List<MealSummary> gb = usable(meals(b), metric);
    GroupStats sa = stats(ga);
    GroupStats sb = stats(gb);
    List<String> caveats = new ArrayList<>();
    boolean sameSource = sa.seriesSource() != null && sa.seriesSource() == sb.seriesSource();
    boolean sufficient = sa.n() >= Comparison.MIN_N && sb.n() >= Comparison.MIN_N && sameSource;
    if (sa.n() < Comparison.MIN_N || sb.n() < Comparison.MIN_N) {
      caveats.add("pocas comidas: se necesitan al menos " + Comparison.MIN_N + " por grupo");
    }
    if (!sameSource && sa.n() > 0 && sb.n() > 0) {
      caveats.add("fuentes de lecturas distintas: no comparables");
    }
    boolean notable = false;
    if (sufficient) {
      double ma = median(ga, metric);
      double mb = median(gb, metric);
      double diff = Math.abs(ma - mb);
      double iqrMax = Math.max(Stats.iqr(values(ga, metric)), Stats.iqr(values(gb, metric)));
      boolean bigEnough =
          switch (metric) {
            case DELTA_PEAK -> diff >= Comparison.MIN_DELTA_PEAK_DIFF_MGDL;
            case IAUC_0_120 ->
                diff >= Comparison.MIN_IAUC_RELATIVE_DIFF * Math.max(Math.abs(ma), Math.abs(mb));
            default -> diff > 0;
          };
      boolean beyondSpread = diff >= 0.5 * iqrMax;
      double ca = sa.medianCarbsG();
      double cb = sb.medianCarbsG();
      boolean carbsComparable =
          Math.abs(ca - cb) <= Comparison.MAX_CARBS_RELATIVE_DIFF * Math.max(ca, cb);
      if (!carbsComparable) {
        caveats.add(
            "la carga de hidratos difiere más del 20 %: se compara cantidad, no composición");
      }
      boolean beyondNoise =
          withinMealNoise == null || metric != Metric.DELTA_PEAK || diff > withinMealNoise;
      if (!beyondNoise) {
        caveats.add("la diferencia no supera el ruido entre repeticiones de la misma comida");
      }
      notable = bigEnough && beyondSpread && carbsComparable && beyondNoise;
    }
    return new Comparison(sa, sb, sufficient, notable, withinMealNoise, caveats);
  }

  @Override
  public GlucoseSummary summary(UserId user, LocalDate from, LocalDate to) {
    return summary != null ? summary : new GlucoseSummary(user, from, to, null, List.of());
  }

  private static List<MealSummary> usable(List<MealSummary> in, Metric metric) {
    return in.stream()
        .filter(m -> m.quality() != null && m.quality().usableForComparison())
        .filter(m -> m.metric(metric) != null)
        .toList();
  }

  private static double[] values(List<MealSummary> g, Metric metric) {
    return g.stream().mapToDouble(m -> m.metric(metric)).toArray();
  }

  private static double median(List<MealSummary> g, Metric metric) {
    return Stats.median(values(g, metric));
  }

  private static GroupStats stats(List<MealSummary> g) {
    if (g.isEmpty()) {
      return new GroupStats(0, 0, null, null, null, null, null, null, null, Map.of(), List.of());
    }
    SeriesSource source =
        g.stream()
                .map(MealSummary::seriesSource)
                .filter(Objects::nonNull)
                .allMatch(s -> s == g.get(0).seriesSource())
            ? g.get(0).seriesSource()
            : null;
    Map<String, Integer> tagCounts = new HashMap<>();
    g.forEach(m -> m.tags().forEach(t -> tagCounts.merge(t, 1, Integer::sum)));
    return new GroupStats(
        g.size(),
        (int) g.stream().filter(m -> m.quality() == mx.glucurvia.core.model.Quality.GOOD).count(),
        source,
        med(g, Metric.DELTA_PEAK),
        iqr(g, Metric.DELTA_PEAK),
        med(g, Metric.IAUC_0_120),
        med(g, Metric.TIME_TO_PEAK),
        med(g, Metric.MINUTES_ABOVE_P90),
        Stats.median(g.stream().mapToDouble(MealSummary::carbsG).toArray()),
        tagCounts,
        g.stream().map(MealSummary::eventId).toList());
  }

  private static Double med(List<MealSummary> g, Metric m) {
    double[] v =
        g.stream()
            .map(x -> x.metric(m))
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .toArray();
    return v.length == 0 ? null : Stats.median(v);
  }

  private static Double iqr(List<MealSummary> g, Metric m) {
    double[] v =
        g.stream()
            .map(x -> x.metric(m))
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .toArray();
    return v.length == 0 ? null : Stats.iqr(v);
  }
}
