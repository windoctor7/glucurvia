package mx.glucurvia.core.model;

import java.time.LocalDate;

/**
 * Rango habitual del propio usuario en una ventana móvil; no es un objetivo clínico (diseño 2.1).
 */
public record PersonalRange(
    Double p10Mgdl,
    Double p50Mgdl,
    Double p90Mgdl,
    LocalDate periodEnd,
    int windowDays,
    int readingsCount) {
  public boolean isEmpty() {
    return readingsCount == 0 || p90Mgdl == null;
  }
}
