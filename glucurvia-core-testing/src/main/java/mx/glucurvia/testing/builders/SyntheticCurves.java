package mx.glucurvia.testing.builders;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import mx.glucurvia.core.model.Mgdl;
import mx.glucurvia.core.model.ReadingType;
import mx.glucurvia.core.series.Reading;
import mx.glucurvia.core.series.Series;

/**
 * Entradas de las curvas sintéticas del diseño 6.8. Solo entradas: los valores esperados de cada
 * métrica los produce y justifica el módulo glycemic en sus propios tests (plan 7, gly-2).
 */
public final class SyntheticCurves {
  public static final Instant MEAL_AT = Instant.parse("2026-09-13T14:00:00Z");

  private SyntheticCurves() {}

  /**
   * @param mealAt instante de la comida; la serie cubre [-60 min, +240 min] salvo que se indique
   */
  public record Curve(String name, Series series, Instant mealAt) {}

  public static List<Curve> all() {
    return List.of(
        flat(),
        classicPeak15(),
        classicPeak5(),
        truncatedAt50(),
        gap40(),
        peakAtEdge(),
        clippedHigh(),
        unstableBaseline(),
        isolatedScanAbove());
  }

  /** Respuesta plana: delta < 20 → NO_EXCURSION. */
  public static Curve flat() {
    return new Curve(
        "plana", shape(15, ReadingType.HISTORIC, t -> 100 + 4 * Math.sin(t / 30.0)), MEAL_AT);
  }

  /** Pico clásico +55 a los 45 min, vuelve al basal hacia +150; histórica cada 15 min. */
  public static Curve classicPeak15() {
    return new Curve(
        "pico clasico 15 min", shape(15, ReadingType.HISTORIC, SyntheticCurves::classic), MEAL_AT);
  }

  /** Misma forma, tiempo real cada 5 min: las métricas deben coincidir salvo la pendiente. */
  public static Curve classicPeak5() {
    return new Curve(
        "pico clasico 5 min", shape(5, ReadingType.REALTIME, SyntheticCurves::classic), MEAL_AT);
  }

  /** Las lecturas terminan en +50 min: PEAK_AT_EDGE y calidad degradada. */
  public static Curve truncatedAt50() {
    Series s =
        shape(5, ReadingType.REALTIME, SyntheticCurves::classic)
            .between(MEAL_AT.minus(Duration.ofMinutes(60)), MEAL_AT.plus(Duration.ofMinutes(50)));
    return new Curve("truncada en +50", s, MEAL_AT);
  }

  /** Hueco de 40 min entre +30 y +70: max_gap alto, sin interpolar a través. */
  public static Curve gap40() {
    Series full = shape(5, ReadingType.REALTIME, SyntheticCurves::classic);
    List<Reading> kept = new ArrayList<>();
    for (Reading r : full.readings()) {
      Duration d = Duration.between(MEAL_AT, r.ts());
      if (d.toMinutes() > 30 && d.toMinutes() < 70) {
        continue;
      }
      kept.add(r);
    }
    return new Curve("hueco de 40 min", Series.of(kept), MEAL_AT);
  }

  /** Sigue subiendo en +180: el máximo cae en el borde. */
  public static Curve peakAtEdge() {
    return new Curve(
        "pico en el borde",
        shape(5, ReadingType.REALTIME, t -> t < 0 ? 100 : 100 + 0.4 * t),
        MEAL_AT);
  }

  /** Valor recortado (HI) en el pico: no puede ser el máximo. */
  public static Curve clippedHigh() {
    Series base = shape(5, ReadingType.REALTIME, SyntheticCurves::classic);
    List<Reading> out = new ArrayList<>();
    for (Reading r : base.readings()) {
      long m = Duration.between(MEAL_AT, r.ts()).toMinutes();
      out.add(m == 45 ? new Reading(r.ts(), Mgdl.of(500), r.type(), true) : r);
    }
    return new Curve("recortada HI", Series.of(out), MEAL_AT);
  }

  /** Basal bajando 1 mg/dL·min antes de la comida: baseline_unstable → CONFOUNDED. */
  public static Curve unstableBaseline() {
    return new Curve(
        "basal inestable",
        shape(5, ReadingType.REALTIME, t -> t < 0 ? 160 + t : classic(t)),
        MEAL_AT);
  }

  /**
   * Histórica plana con un escaneo aislado 40 mg/dL por encima: el escaneo no puede ser el pico.
   */
  public static Curve isolatedScanAbove() {
    Series hist = shape(15, ReadingType.HISTORIC, t -> 105);
    Series scan =
        Series.of(Reading.of(MEAL_AT.plus(Duration.ofMinutes(40)), 145, ReadingType.SCAN));
    return new Curve("escaneo aislado", Readings.merge(hist, scan), MEAL_AT);
  }

  /** Curva de referencia: basal 100, sube hasta +55 en 45 min, baja a 100 hacia +150. */
  static double classic(double t) {
    if (t < 0) {
      return 100;
    }
    if (t <= 45) {
      return 100 + 55 * (t / 45.0);
    }
    if (t <= 150) {
      return 155 - 55 * ((t - 45) / 105.0);
    }
    return 100;
  }

  private static Series shape(
      int stepMin, ReadingType type, java.util.function.DoubleUnaryOperator f) {
    List<Reading> list = new ArrayList<>();
    for (int t = -60; t <= 240; t += stepMin) {
      list.add(Reading.of(MEAL_AT.plus(Duration.ofMinutes(t)), f.applyAsDouble(t), type));
    }
    return Series.of(list);
  }
}
