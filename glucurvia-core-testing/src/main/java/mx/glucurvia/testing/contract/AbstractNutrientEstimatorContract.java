package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.List;
import mx.glucurvia.core.adapter.NutrientEstimator;
import mx.glucurvia.core.model.ConfidenceBand;
import mx.glucurvia.core.model.CorrectedItem;
import mx.glucurvia.core.model.EstimatedMeal;
import mx.glucurvia.core.model.EventTime;
import mx.glucurvia.core.model.ExtractedItem;
import mx.glucurvia.core.model.ExtractedMeal;
import mx.glucurvia.core.model.MacroSource;
import mx.glucurvia.core.model.MacrosPer100g;
import mx.glucurvia.core.model.MealType;
import mx.glucurvia.core.model.Range;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.testing.builders.Meals;
import org.junit.jupiter.api.Test;

/**
 * Independiente del catálogo: usa ítems con macros de respaldo o alias, que cualquier
 * implementación debe respetar aunque no conozca el alimento.
 */
public abstract class AbstractNutrientEstimatorContract {
  protected static final EventTime AT =
      EventTime.approx(Instant.parse("2026-09-13T21:40:00Z"), Meals.CDMX, 15, "test");
  private static final MacrosPer100g CIEN_G_50_HC = new MacrosPer100g(50, 2, 4, 1, 250);

  protected abstract NutrientEstimator subject();

  private static ExtractedItem item(
      String raw, String food, String unit, Range grams, MacrosPer100g fallback) {
    return new ExtractedItem(
        raw, food, null, null, null, unit, grams, null, 1.0, null, List.of(), null, fallback);
  }

  @Test
  void losMacrosDeRespaldoSeUsanCuandoElAlimentoEsDesconocido() {
    EstimatedMeal m =
        subject()
            .estimate(
                new ExtractedMeal(
                    AT,
                    MealType.DINNER,
                    null,
                    List.of(
                        item(
                            "100 g de zzplato-inventado",
                            "zzplato-inventado",
                            "g",
                            Range.exact(100),
                            CIEN_G_50_HC)),
                    null),
                UserId.random());
    assertThat(m.items()).hasSize(1);
    assertThat(m.items().get(0).nutrients().carbsG()).isCloseTo(50, within(0.01));
    assertThat(m.items().get(0).macroSource()).isEqualTo(MacroSource.LLM_FALLBACK);
    assertThat(m.totals().carbsG()).isCloseTo(50, within(0.01));
  }

  @Test
  void losTotalesSonLaSumaDeLosItemsYLaBandaCubreElPunto() {
    EstimatedMeal m =
        subject()
            .estimate(
                new ExtractedMeal(
                    AT,
                    MealType.DINNER,
                    null,
                    List.of(
                        item("a", "zz-a", "g", Range.of(100, 80, 120), CIEN_G_50_HC),
                        item("b", "zz-b", "g", Range.of(50, 40, 60), CIEN_G_50_HC)),
                    null),
                UserId.random());
    double sum = m.items().stream().mapToDouble(i -> i.nutrients().carbsG()).sum();
    assertThat(m.totals().carbsG()).isCloseTo(sum, within(0.01));
    assertThat(m.carbsBand().low()).isLessThanOrEqualTo(m.carbsBand().point());
    assertThat(m.carbsBand().high()).isGreaterThanOrEqualTo(m.carbsBand().point());
    assertThat(m.carbsWorstCase().width()).isGreaterThanOrEqualTo(m.carbsBand().width() * 0.99);
  }

  @Test
  void laPorcionComidaEscalaLosNutrientes() {
    ExtractedItem half =
        new ExtractedItem(
            "dejé la mitad",
            "zz-c",
            null,
            null,
            null,
            "g",
            Range.exact(100),
            null,
            0.5,
            null,
            List.of(),
            null,
            CIEN_G_50_HC);
    EstimatedMeal m =
        subject()
            .estimate(
                new ExtractedMeal(AT, MealType.DINNER, null, List.of(half), null), UserId.random());
    assertThat(m.items().get(0).nutrients().carbsG()).isCloseTo(25, within(0.01));
  }

  @Test
  void unAliasAprendidoTieneConfianzaAltaYFuenteUsuario() {
    NutrientEstimator e = subject();
    UserId u = UserId.random();
    e.rememberCorrection(
        u, new CorrectedItem("mi tostada", null, 12.0, new MacrosPer100g(62, 6, 8, 3, 300), null));
    EstimatedMeal m =
        e.estimate(
            new ExtractedMeal(
                AT,
                MealType.DINNER,
                null,
                List.of(item("una de mi tostada", "tostada", "pieza", Range.of(20, 15, 30), null)),
                null),
            u);
    var it = m.items().get(0);
    assertThat(it.macroSource()).isEqualTo(MacroSource.USER_ALIAS);
    assertThat(it.confidence().band()).isEqualTo(ConfidenceBand.HIGH);
    assertThat(it.grams().point()).isEqualTo(12.0);
    assertThat(it.nutrients().carbsG()).isCloseTo(7.44, within(0.01));
  }

  @Test
  void laConfianzaDelLlmNoDecideNada() {
    ExtractedItem sure =
        new ExtractedItem(
            "un plato de zz-d",
            "zz-d",
            null,
            null,
            null,
            "plato",
            Range.of(200, 100, 400),
            null,
            1.0,
            null,
            List.of(),
            0.99,
            CIEN_G_50_HC);
    EstimatedMeal m =
        subject()
            .estimate(
                new ExtractedMeal(AT, MealType.DINNER, null, List.of(sure), null), UserId.random());
    assertThat(m.items().get(0).confidence().band()).isNotEqualTo(ConfidenceBand.HIGH);
    assertThat(m.items().get(0).llmConfidence()).isEqualTo(0.99);
  }
}
