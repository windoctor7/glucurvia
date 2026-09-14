package mx.glucurvia.core.nutrition;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.MacroSource;

/**
 * Política de aclaración (diseño 10.3), determinista y sin LLM. Preguntar solo si se cumplen las
 * tres: rango de HC del ítem > 15 g y > 40 % de su punto; aporte > 30 % de los HC de la comida; sin
 * alias ni etiqueta. Si varios califican, solo el de mayor aporte.
 */
public final class ClarificationPolicy {
  public static final double MIN_ABSOLUTE_WIDTH_G = 15.0;
  public static final double MIN_RELATIVE_WIDTH = 0.40;
  public static final double MIN_SHARE = 0.30;

  private ClarificationPolicy() {}

  public static Optional<EstimatedItem> itemToAskAbout(List<EstimatedItem> items) {
    double totalCarbs = items.stream().mapToDouble(i -> i.nutrients().carbsG()).sum();
    if (totalCarbs <= 0) {
      return Optional.empty();
    }
    return items.stream()
        .filter(
            i -> i.macroSource() != MacroSource.USER_ALIAS && i.macroSource() != MacroSource.LABEL)
        .filter(i -> i.carbsRange().width() > MIN_ABSOLUTE_WIDTH_G)
        .filter(i -> i.carbsRange().relativeWidth() > MIN_RELATIVE_WIDTH)
        .filter(i -> i.nutrients().carbsG() / totalCarbs > MIN_SHARE)
        .max(Comparator.comparingDouble(i -> i.nutrients().carbsG()));
  }
}
