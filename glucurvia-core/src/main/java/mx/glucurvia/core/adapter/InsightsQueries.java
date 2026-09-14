package mx.glucurvia.core.adapter;

import java.time.LocalDate;
import java.util.List;
import mx.glucurvia.core.model.Comparison;
import mx.glucurvia.core.model.GlucoseSummary;
import mx.glucurvia.core.model.MealFilter;
import mx.glucurvia.core.model.MealSummary;
import mx.glucurvia.core.model.Metric;
import mx.glucurvia.core.model.UserId;

/**
 * Consultas analíticas (diseño 6.4 y 8.2). Dueño: insights. Consumidor: assistant. Nunca
 * conclusiones.
 */
public interface InsightsQueries {
  /** Ordenadas por startedAt descendente. */
  List<MealSummary> meals(MealFilter filter);

  Comparison compare(MealFilter a, MealFilter b, Metric metric);

  GlucoseSummary summary(UserId user, LocalDate from, LocalDate to);
}
