package mx.glucurvia.core.adapter;

import mx.glucurvia.core.model.CorrectedItem;
import mx.glucurvia.core.model.EstimatedMeal;
import mx.glucurvia.core.model.ExtractedMeal;
import mx.glucurvia.core.model.UserId;

/** Estimación de nutrientes (diseño 9). Dueño: nutrition. Consumidor: assistant (que orquesta). */
public interface NutrientEstimator {
  EstimatedMeal estimate(ExtractedMeal meal, UserId user);

  /** Aprende un alias del usuario a partir de una corrección (diseño 10.4). */
  void rememberCorrection(UserId user, CorrectedItem correction);
}
