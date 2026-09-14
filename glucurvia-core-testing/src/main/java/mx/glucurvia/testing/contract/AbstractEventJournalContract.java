package mx.glucurvia.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import mx.glucurvia.core.adapter.EventJournal;
import mx.glucurvia.core.model.Event;
import mx.glucurvia.core.model.EventPatch;
import mx.glucurvia.core.model.EventQuery;
import mx.glucurvia.core.model.EventTime;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.testing.builders.Meals;
import org.junit.jupiter.api.Test;

public abstract class AbstractEventJournalContract {
  protected static final Instant T0 = Instant.parse("2026-09-13T20:00:00Z");

  protected abstract EventJournal subject();

  @Test
  void registraYRecupera() {
    EventJournal j = subject();
    UserId u = UserId.random();
    Event e = j.log(Meals.newMeal(u, T0, 50, "avena", "manzana"));
    assertThat(j.find(e.id())).isPresent();
    assertThat(j.find(e.id()).orElseThrow().meal().totals().carbsG()).isEqualTo(50);
    assertThat(j.find(e.id()).orElseThrow().userId()).isEqualTo(u);
  }

  @Test
  void corrigeLaHoraYConservaLoDemas() {
    EventJournal j = subject();
    UserId u = UserId.random();
    Event e = j.log(Meals.newMeal(u, T0, 50, "avena"));
    EventTime later = EventTime.exact(T0.plus(Duration.ofMinutes(30)), Meals.CDMX);
    Event updated = j.update(e.id(), EventPatch.ofTime(later));
    assertThat(updated.startedAt()).isEqualTo(T0.plus(Duration.ofMinutes(30)));
    assertThat(updated.meal().totals().carbsG()).isEqualTo(50);
    assertThat(updated.rawText()).isEqualTo(e.rawText());
  }

  @Test
  void buscaPorRangoTipoYTextoYExcluyeBorrados() {
    EventJournal j = subject();
    UserId u = UserId.random();
    Event meal = j.log(Meals.newMeal(u, T0, 40, "tortilla"));
    j.log(
        Meals.newContext(
            u, EventType.ACTIVITY, T0.plus(Duration.ofMinutes(20)), Map.of("kind", "walk")));
    j.log(Meals.newMeal(UserId.random(), T0, 40, "tortilla"));
    var all =
        j.search(
            new EventQuery(u, T0.minusSeconds(1), T0.plus(Duration.ofHours(1)), Set.of(), null));
    assertThat(all).hasSize(2);
    assertThat(
            j.search(
                new EventQuery(
                    u,
                    T0.minusSeconds(1),
                    T0.plus(Duration.ofHours(1)),
                    Set.of(EventType.ACTIVITY),
                    null)))
        .hasSize(1);
    assertThat(
            j.search(
                new EventQuery(
                    u, T0.minusSeconds(1), T0.plus(Duration.ofHours(1)), Set.of(), "TORTILLA")))
        .hasSize(1);
    j.delete(meal.id());
    assertThat(j.find(meal.id())).isEmpty();
    assertThat(
            j.search(
                new EventQuery(
                    u,
                    T0.minusSeconds(1),
                    T0.plus(Duration.ofHours(1)),
                    Set.of(EventType.MEAL),
                    null)))
        .isEmpty();
  }
}
