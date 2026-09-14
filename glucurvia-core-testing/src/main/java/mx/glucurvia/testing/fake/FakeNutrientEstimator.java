package mx.glucurvia.testing.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mx.glucurvia.core.adapter.NutrientEstimator;
import mx.glucurvia.core.model.Confidence;
import mx.glucurvia.core.model.CorrectedItem;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.EstimatedMeal;
import mx.glucurvia.core.model.ExtractedItem;
import mx.glucurvia.core.model.ExtractedMeal;
import mx.glucurvia.core.model.MacroSource;
import mx.glucurvia.core.model.MacrosPer100g;
import mx.glucurvia.core.model.Nutrients;
import mx.glucurvia.core.model.Range;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.nutrition.ClarificationPolicy;
import mx.glucurvia.core.nutrition.NutrientMath;

/**
 * Estimador determinista sin LLM ni base de datos. Catálogo diminuto en memoria; aplica las mismas
 * reglas puras de core (banda, confianza ponderada, política de aclaración) que la implementación
 * real.
 */
public final class FakeNutrientEstimator implements NutrientEstimator {
  public static final String VERSION = "fake-1";
  private static final Set<String> EXPLICIT_UNITS = Set.of("g", "gr", "gramos", "ml", "mililitros");
  private static final MacrosPer100g DEFAULT = new MacrosPer100g(10, 1, 2, 1, 60);
  private static final Map<String, MacrosPer100g> CATALOG =
      Map.of(
          "avena", new MacrosPer100g(57.6, 10.1, 13.2, 6.5, 379),
          "manzana", new MacrosPer100g(11.4, 2.4, 0.3, 0.2, 52),
          "tostada", new MacrosPer100g(62.0, 6.0, 8.0, 3.0, 300),
          "tortilla", new MacrosPer100g(42.0, 4.0, 5.7, 2.5, 218),
          "bebida de avena", new MacrosPer100g(7.0, 0.8, 1.0, 1.5, 45));

  private final Map<UserId, Map<String, CorrectedItem>> aliases = new HashMap<>();

  @Override
  public EstimatedMeal estimate(ExtractedMeal meal, UserId user) {
    List<EstimatedItem> items = new ArrayList<>();
    for (ExtractedItem it : meal.items()) {
      items.add(estimateItem(it, user));
    }
    Optional<EstimatedItem> ask = ClarificationPolicy.itemToAskAbout(items);
    String question = null;
    if (ask.isPresent()) {
      EstimatedItem a = ask.get();
      int idx = items.indexOf(a);
      items.set(idx, withClarification(a));
      question =
          "¿Cuánto era exactamente de "
              + a.foodName()
              + "? Asumo "
              + Math.round(a.grams().point())
              + " g si no me dices.";
    }
    Nutrients totals =
        items.stream().map(EstimatedItem::nutrients).reduce(Nutrients.ZERO, Nutrients::plus);
    List<Range> carbRanges = items.stream().map(EstimatedItem::carbsRange).toList();
    return new EstimatedMeal(
        meal.mealType(),
        meal.time(),
        items,
        totals,
        NutrientMath.mealBand(carbRanges),
        Range.sumWorstCase(carbRanges),
        NutrientMath.weightedByCarbs(items),
        VERSION,
        question);
  }

  private EstimatedItem estimateItem(ExtractedItem it, UserId user) {
    String key = (it.raw() + " " + it.food()).toLowerCase(Locale.ROOT);
    Optional<CorrectedItem> alias =
        aliases.getOrDefault(user, Map.of()).values().stream()
            .filter(a -> key.contains(a.alias().toLowerCase(Locale.ROOT)))
            .findFirst();

    Range grams =
        it.grams() != null
            ? it.grams()
            : it.volumeMl() != null
                ? Range.exact(it.volumeMl())
                : Range.exact(it.recipe().yieldG());
    MacrosPer100g per100;
    MacroSource source;
    double confidence;
    if (alias.isPresent()) {
      CorrectedItem a = alias.get();
      if (a.grams() != null) {
        grams = Range.exact(a.grams());
      }
      per100 = a.per100g() != null ? a.per100g() : catalogFor(it.food()).orElse(DEFAULT);
      source = MacroSource.USER_ALIAS;
      confidence = 0.9;
    } else if (it.fallbackPer100g() != null && catalogFor(it.food()).isEmpty()) {
      per100 = it.fallbackPer100g();
      source = MacroSource.LLM_FALLBACK;
      confidence = 0.45;
    } else {
      Optional<MacrosPer100g> cat = catalogFor(it.food());
      per100 = cat.orElse(DEFAULT);
      source = cat.isPresent() ? MacroSource.CATALOG : MacroSource.LLM_FALLBACK;
      boolean explicit =
          it.unit() != null && EXPLICIT_UNITS.contains(it.unit().toLowerCase(Locale.ROOT));
      confidence = explicit ? 0.85 : (grams.relativeWidth() <= 0.5 ? 0.6 : 0.4);
      if (cat.isEmpty()) {
        confidence = Math.min(confidence, 0.4);
      }
    }
    double portion = it.portionEaten();
    Nutrients n = per100.forGrams(grams.point() * portion);
    Range carbs = grams.scale(per100.carbsAvailable() / 100.0 * portion);
    return new EstimatedItem(
        it.raw(),
        it.food(),
        it.preparation(),
        null,
        it.quantity(),
        it.unit(),
        grams,
        it.volumeMl(),
        portion,
        n,
        carbs,
        Confidence.of(confidence),
        it.selfReportedConfidence(),
        source,
        it.assumptions(),
        false);
  }

  private static Optional<MacrosPer100g> catalogFor(String food) {
    String f = food.toLowerCase(Locale.ROOT);
    return CATALOG.entrySet().stream()
        .filter(e -> f.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private static EstimatedItem withClarification(EstimatedItem i) {
    return new EstimatedItem(
        i.raw(),
        i.foodName(),
        i.preparation(),
        i.foodRefId(),
        i.quantity(),
        i.unit(),
        i.grams(),
        i.volumeMl(),
        i.portionEaten(),
        i.nutrients(),
        i.carbsRange(),
        i.confidence(),
        i.llmConfidence(),
        i.macroSource(),
        i.assumptions(),
        true);
  }

  @Override
  public void rememberCorrection(UserId user, CorrectedItem correction) {
    aliases
        .computeIfAbsent(user, k -> new HashMap<>())
        .put(correction.alias().toLowerCase(Locale.ROOT), correction);
  }
}
