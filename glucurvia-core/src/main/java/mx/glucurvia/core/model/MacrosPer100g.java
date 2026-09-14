package mx.glucurvia.core.model;

/**
 * Composición por 100 g (o 100 ml). carbsAvailable ya sin fibra: la convención de la fuente se
 * resuelve en el catálogo (diseño 9.2), nunca aquí.
 */
public record MacrosPer100g(
    double carbsAvailable, double fiber, double protein, double fat, double kcal) {
  public MacrosPer100g {
    if (carbsAvailable < 0 || fiber < 0 || protein < 0 || fat < 0 || kcal < 0) {
      throw new IllegalArgumentException("macros negativos");
    }
  }

  public Nutrients forGrams(double grams) {
    double f = grams / 100.0;
    return new Nutrients(carbsAvailable * f, fiber * f, protein * f, fat * f, kcal * f);
  }
}
