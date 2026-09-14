package mx.glucurvia.testing.builders;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import mx.glucurvia.core.model.ReadingType;
import mx.glucurvia.core.series.Reading;
import mx.glucurvia.core.series.Series;

public final class Readings {
  private Readings() {}

  /** Una lectura cada {@code stepMin} minutos desde start, con los valores dados. */
  public static Series every(Instant start, int stepMin, ReadingType type, double... values) {
    List<Reading> list = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      list.add(Reading.of(start.plus(Duration.ofMinutes((long) stepMin * i)), values[i], type));
    }
    return Series.of(list);
  }

  public static Series merge(Series a, Series b) {
    List<Reading> all = new ArrayList<>(a.readings());
    all.addAll(b.readings());
    return Series.of(all);
  }
}
