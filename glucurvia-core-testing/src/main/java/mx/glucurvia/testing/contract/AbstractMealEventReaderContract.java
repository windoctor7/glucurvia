package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mx.glucurvia.core.adapter.MealEventReader;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.NewEvent;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.testing.builders.Meals;
import org.junit.jupiter.api.Test;

public abstract class AbstractMealEventReaderContract {
  protected static final Instant T0 = Instant.parse("2026-09-13T20:00:00Z");

  /** Devuelve un lector que ya contiene los eventos dados. */
  protected abstract MealEventReader subjectWith(List<NewEvent> events);

  @Test
  void devuelveSoloComidasDelRangoConHidratosYAlimentos() {
    UserId u = UserId.random();
    MealEventReader r =
        subjectWith(
            List.of(
                Meals.newMeal(u, T0, 50, "avena", "manzana"),
                Meals.newMeal(u, T0.plus(Duration.ofHours(6)), 30, "tortilla"),
                Meals.newContext(
                    u,
                    EventType.ACTIVITY,
                    T0.plus(Duration.ofMinutes(30)),
                    Map.of("kind", "walk"))));
    var meals = r.mealsBetween(u, T0.minusSeconds(1), T0.plus(Duration.ofHours(1)));
    assertThat(meals).hasSize(1);
    assertThat(meals.get(0).carbsG()).isEqualTo(50);
    assertThat(meals.get(0).foods()).contains("avena", "manzana");
    assertThat(meals.get(0).localTz()).isEqualTo(Meals.CDMX);
  }

  @Test
  void contextoFiltraPorTipoYNuncaIncluyeComidas() {
    UserId u = UserId.random();
    MealEventReader r =
        subjectWith(
            List.of(
                Meals.newMeal(u, T0, 50, "avena"),
                Meals.newContext(
                    u, EventType.ACTIVITY, T0.plus(Duration.ofMinutes(30)), Map.of("kind", "walk")),
                Meals.newContext(
                    u, EventType.SLEEP, T0.minus(Duration.ofHours(10)), Map.of("hours", 6.5))));
    assertThat(
            r.contextBetween(
                u, T0.minus(Duration.ofHours(12)), T0.plus(Duration.ofHours(1)), Set.of()))
        .hasSize(2);
    var walks =
        r.contextBetween(
            u,
            T0.minus(Duration.ofHours(12)),
            T0.plus(Duration.ofHours(1)),
            Set.of(EventType.ACTIVITY));
    assertThat(walks).hasSize(1);
    assertThat(walks.get(0).attributes()).containsEntry("kind", "walk");
  }
}
