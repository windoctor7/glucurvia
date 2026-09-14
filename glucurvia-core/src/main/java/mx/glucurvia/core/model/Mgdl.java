package mx.glucurvia.core.model;

/**
 * Glucosa en mg/dL, la unidad canónica de todo el sistema (diseño 2.3). mmol/L solo al presentar.
 */
public record Mgdl(double value) implements Comparable<Mgdl> {
  public static final double MMOL_FACTOR = 18.0182;

  public Mgdl {
    if (Double.isNaN(value) || value < 0) {
      throw new IllegalArgumentException("mg/dL inválido: " + value);
    }
  }

  public static Mgdl of(double value) {
    return new Mgdl(value);
  }

  public static Mgdl fromMmolL(double mmolL) {
    return new Mgdl(mmolL * MMOL_FACTOR);
  }

  public double toMmolL() {
    return value / MMOL_FACTOR;
  }

  @Override
  public int compareTo(Mgdl other) {
    return Double.compare(value, other.value);
  }
}
