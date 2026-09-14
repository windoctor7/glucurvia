package mx.glucurvia.testing.builders;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mx.glucurvia.core.model.Confidence;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.EstimatedMeal;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.EventTime;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.MacroSource;
import mx.glucurvia.core.model.MealSummary;
import mx.glucurvia.core.model.MealType;
import mx.glucurvia.core.model.NewEvent;
import mx.glucurvia.core.model.Nutrients;
import mx.glucurvia.core.model.Quality;
import mx.glucurvia.core.model.Range;
import mx.glucurvia.core.model.SeriesSource;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.nutrition.NutrientMath;

/** Constructores de comidas para pruebas. Zona por defecto: la del proyecto. */
public final class Meals {
  public static final ZoneId CDMX = ZoneId.of("America/Mexico_City");

  private Meals() {}

  /** Comida estimada con un ítem por alimento, repartiendo los hidratos a partes iguales. */
  public static EstimatedMeal estimated(MealType type, Instant at, double carbsG, String... foods) {
    List<EstimatedItem> items = new ArrayList<>();
    double each = foods.length == 0 ? 0 : carbsG / foods.length;
    for (String f : foods) {
      Range carbs = Range.of(each, each * 0.8, each * 1.2);
      items.add(
          new EstimatedItem(
              f,
              f,
              null,
              null,
              null,
              null,
              Range.exact(50),
              null,
              1.0,
              new Nutrients(each, 1, 2, 1, each * 4),
              carbs,
              Confidence.of(0.7),
              null,
              MacroSource.CATALOG,
              List.of(),
              false));
    }
    List<Range> ranges = items.stream().map(EstimatedItem::carbsRange).toList();
    Nutrients totals =
        items.stream().map(EstimatedItem::nutrients).reduce(Nutrients.ZERO, Nutrients::plus);
    return new EstimatedMeal(
        type,
        EventTime.approx(at, CDMX, 15, "test"),
        items,
        totals,
        ranges.isEmpty() ? Range.exact(0) : NutrientMath.mealBand(ranges),
        Range.sumWorstCase(ranges),
        NutrientMath.weightedByCarbs(items),
        "test-1",
        null);
  }

  public static NewEvent newMeal(UserId user, Instant at, double carbsG, String... foods) {
    EstimatedMeal m = estimated(MealType.LUNCH, at, carbsG, foods);
    return new NewEvent(
        user,
        EventType.MEAL,
        m.time(),
        null,
        "TEST",
        "comí " + String.join(", ", foods),
        null,
        Map.of(),
        m);
  }

  public static NewEvent newContext(
      UserId user, EventType type, Instant at, Map<String, Object> attributes) {
    return new NewEvent(
        user, type, EventTime.exact(at, CDMX), null, "TEST", null, null, attributes, null);
  }

  /** Resumen con respuesta ya calculada, para FakeInsights y tests de contrato de insights. */
  public static MealSummary summary(
      Instant at,
      double carbsG,
      Set<String> foods,
      Set<String> tags,
      Quality quality,
      SeriesSource source,
      Double deltaPeak,
      Double iauc120,
      Integer timeToPeak,
      Integer minutesAbove) {
    return new MealSummary(
        EventId.random(),
        at,
        MealType.LUNCH,
        carbsG,
        Confidence.of(0.7),
        foods,
        tags,
        quality,
        source,
        deltaPeak,
        iauc120,
        timeToPeak,
        minutesAbove,
        false);
  }
}
