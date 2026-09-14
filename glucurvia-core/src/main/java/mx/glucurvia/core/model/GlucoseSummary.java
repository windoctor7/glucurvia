package mx.glucurvia.core.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Resumen por días para la pantalla de inicio y la tool get_glucose_summary (diseño 7 y 8.2). */
public record GlucoseSummary(
    UserId userId,
    LocalDate from,
    LocalDate to,
    PersonalRange personalRange,
    List<DailyStats> days) {
  public GlucoseSummary {
    Objects.requireNonNull(userId, "userId");
    days = days == null ? List.of() : List.copyOf(days);
  }

  public record DailyStats(
      LocalDate day,
      Double meanMgdl,
      Double p10Mgdl,
      Double p50Mgdl,
      Double p90Mgdl,
      int minutesAbovePersonalP90,
      double coveragePct,
      int mealsLogged) {}
}
