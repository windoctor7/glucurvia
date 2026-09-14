package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mx.glucurvia.core.adapter.InsightsQueries;
import mx.glucurvia.core.model.Comparison;
import mx.glucurvia.core.model.MealFilter;
import mx.glucurvia.core.model.MealSummary;
import mx.glucurvia.core.model.Metric;
import mx.glucurvia.core.model.Quality;
import mx.glucurvia.core.model.SeriesSource;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.testing.builders.Meals;
import org.junit.jupiter.api.Test;

/** Las reglas de comparación del diseño 6.4, que fake y real deben aplicar igual. */
public abstract class AbstractInsightsQueriesContract {
  protected static final Instant T0 = Instant.parse("2026-09-01T14:00:00Z");

  /** Devuelve consultas que ya contienen esos resúmenes para el usuario. */
  protected abstract InsightsQueries subjectWith(UserId user, List<MealSummary> meals);

  private static List<MealSummary> group(
      String food, int n, double carbs, double deltaPeak, SeriesSource src, Quality q) {
    List<MealSummary> out = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      out.add(
          Meals.summary(
              T0.plus(Duration.ofDays(i)),
              carbs,
              Set.of("avena", food),
              Set.of(),
              q,
              src,
              deltaPeak + (i % 2 == 0 ? 3 : -3),
              1000.0,
              45,
              20));
    }
    return out;
  }

  private static MealFilter with(UserId u, String food) {
    return new MealFilter(
        u,
        T0.minus(Duration.ofDays(1)),
        T0.plus(Duration.ofDays(30)),
        null,
        Set.of(food),
        Set.of(),
        Set.of(),
        null,
        null);
  }

  @Test
  void conMenosDeCincoPorGrupoNoHayEvidenciaSuficiente() {
    UserId u = UserId.random();
    List<MealSummary> all =
        new ArrayList<>(group("manzana", 3, 35, 38, SeriesSource.REALTIME, Quality.GOOD));
    all.addAll(group("tortilla", 6, 36, 70, SeriesSource.REALTIME, Quality.GOOD));
    Comparison c =
        subjectWith(u, all).compare(with(u, "manzana"), with(u, "tortilla"), Metric.DELTA_PEAK);
    assertThat(c.sufficientEvidence()).isFalse();
    assertThat(c.notableDifference()).isFalse();
    assertThat(c.a().n()).isEqualTo(3);
    assertThat(c.caveats()).isNotEmpty();
  }

  @Test
  void diferenciaGrandeConCargaComparableEsApreciable() {
    UserId u = UserId.random();
    List<MealSummary> all =
        new ArrayList<>(group("manzana", 6, 35, 38, SeriesSource.REALTIME, Quality.GOOD));
    all.addAll(group("tortilla", 6, 37, 70, SeriesSource.REALTIME, Quality.GOOD));
    Comparison c =
        subjectWith(u, all).compare(with(u, "manzana"), with(u, "tortilla"), Metric.DELTA_PEAK);
    assertThat(c.sufficientEvidence()).isTrue();
    assertThat(c.notableDifference()).isTrue();
    assertThat(c.a().medianDeltaPeak()).isLessThan(c.b().medianDeltaPeak());
  }

  @Test
  void cargaDeHidratosMuyDistintaNoEsApreciableYLoDice() {
    UserId u = UserId.random();
    List<MealSummary> all =
        new ArrayList<>(group("manzana", 6, 35, 38, SeriesSource.REALTIME, Quality.GOOD));
    all.addAll(group("tortilla", 6, 60, 70, SeriesSource.REALTIME, Quality.GOOD));
    Comparison c =
        subjectWith(u, all).compare(with(u, "manzana"), with(u, "tortilla"), Metric.DELTA_PEAK);
    assertThat(c.sufficientEvidence()).isTrue();
    assertThat(c.notableDifference()).isFalse();
    assertThat(c.caveats()).anyMatch(s -> s.toLowerCase().contains("hidratos"));
  }

  @Test
  void fuentesDistintasNoSeComparanYLasNoUsablesSeExcluyen() {
    UserId u = UserId.random();
    List<MealSummary> all =
        new ArrayList<>(group("manzana", 6, 35, 38, SeriesSource.HISTORIC, Quality.GOOD));
    all.addAll(group("tortilla", 6, 36, 70, SeriesSource.REALTIME, Quality.GOOD));
    all.addAll(group("tortilla", 3, 36, 70, SeriesSource.REALTIME, Quality.INSUFFICIENT));
    Comparison c =
        subjectWith(u, all).compare(with(u, "manzana"), with(u, "tortilla"), Metric.DELTA_PEAK);
    assertThat(c.sufficientEvidence()).isFalse();
    assertThat(c.b().n()).isEqualTo(6);
  }

  @Test
  void mealsFiltraPorAlimentosYOrdenaDescendente() {
    UserId u = UserId.random();
    List<MealSummary> all =
        new ArrayList<>(group("manzana", 2, 35, 38, SeriesSource.REALTIME, Quality.GOOD));
    all.addAll(group("tortilla", 2, 36, 70, SeriesSource.REALTIME, Quality.GOOD));
    var r =
        subjectWith(u, all)
            .meals(
                new MealFilter(
                    u,
                    T0.minus(Duration.ofDays(1)),
                    T0.plus(Duration.ofDays(30)),
                    null,
                    Set.of("avena"),
                    Set.of("tortilla"),
                    Set.of(),
                    null,
                    null));
    assertThat(r).hasSize(2);
    assertThat(r.get(0).startedAt()).isAfter(r.get(1).startedAt());
  }
}
