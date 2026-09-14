package mx.glucurvia.core.model;

/** Nutrientes absolutos de un ítem o una comida. carbsG son hidratos disponibles (sin fibra). */
public record Nutrients(double carbsG, double fiberG, double proteinG, double fatG, double kcal) {
  public static final Nutrients ZERO = new Nutrients(0, 0, 0, 0, 0);

  public Nutrients plus(Nutrients o) {
    return new Nutrients(
        carbsG + o.carbsG, fiberG + o.fiberG, proteinG + o.proteinG, fatG + o.fatG, kcal + o.kcal);
  }

  public Nutrients scale(double factor) {
    return new Nutrients(
        carbsG * factor, fiberG * factor, proteinG * factor, fatG * factor, kcal * factor);
  }
}
