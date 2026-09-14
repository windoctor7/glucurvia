package mx.glucurvia.core.model;

/**
 * Confianza 0–1 derivada en Java de señales observables, nunca autoinformada por el LLM (diseño
 * 10.1).
 */
public record Confidence(double value) implements Comparable<Confidence> {
  public Confidence {
    if (Double.isNaN(value) || value < 0 || value > 1) {
      throw new IllegalArgumentException("confianza fuera de [0,1]: " + value);
    }
  }

  public static Confidence of(double value) {
    return new Confidence(value);
  }

  /** Bandas del diseño 10.1: >= 0,75 alta; 0,50 <= c < 0,75 media; < 0,50 baja. */
  public ConfidenceBand band() {
    if (value >= 0.75) {
      return ConfidenceBand.HIGH;
    }
    return value >= 0.5 ? ConfidenceBand.MEDIUM : ConfidenceBand.LOW;
  }

  @Override
  public int compareTo(Confidence other) {
    return Double.compare(value, other.value);
  }
}
