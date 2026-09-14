package mx.glucurvia.core.model;

/** Peso en gramos. */
public record Grams(double value) {
  public Grams {
    if (Double.isNaN(value) || value < 0) {
      throw new IllegalArgumentException("gramos inválidos: " + value);
    }
  }

  public static Grams of(double value) {
    return new Grams(value);
  }
}
