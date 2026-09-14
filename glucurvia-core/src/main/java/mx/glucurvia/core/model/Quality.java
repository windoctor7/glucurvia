package mx.glucurvia.core.model;

/** Calidad de una respuesta glucémica (diseño 6.1, paso 13). */
public enum Quality {
  GOOD,
  PARTIAL,
  CONFOUNDED,
  INSUFFICIENT;

  /** Las que cuentan para comparaciones (diseño 6.4). */
  public boolean usableForComparison() {
    return this == GOOD || this == PARTIAL;
  }
}
