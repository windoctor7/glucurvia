package mx.glucurvia.testing.fake;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mx.glucurvia.core.adapter.GlucoseSeriesReader;
import mx.glucurvia.core.math.Stats;
import mx.glucurvia.core.model.PersonalRange;
import mx.glucurvia.core.model.ReadingType;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.series.Reading;
import mx.glucurvia.core.series.Series;

/** Lecturas en memoria. Dedup por (ts, type) como la PK natural. */
public final class InMemoryGlucoseSeries implements GlucoseSeriesReader {
  private final Map<UserId, Map<String, Reading>> data = new HashMap<>();

  public InMemoryGlucoseSeries add(UserId user, Reading... readings) {
    return add(user, List.of(readings));
  }

  public InMemoryGlucoseSeries add(UserId user, List<Reading> readings) {
    Map<String, Reading> m = data.computeIfAbsent(user, k -> new HashMap<>());
    for (Reading r : readings) {
      m.put(r.ts() + "|" + r.type(), r);
    }
    return this;
  }

  public InMemoryGlucoseSeries add(UserId user, Series series) {
    return add(user, series.readings());
  }

  private List<Reading> all(UserId user) {
    return new ArrayList<>(data.getOrDefault(user, Map.of()).values());
  }

  @Override
  public Series between(UserId user, Instant from, Instant to) {
    return Series.of(all(user)).between(from, to);
  }

  @Override
  public Optional<Reading> latest(UserId user) {
    return all(user).stream().max(Comparator.comparing(Reading::ts));
  }

  @Override
  public PersonalRange personalRange(UserId user, LocalDate asOf, int windowDays) {
    Instant end = asOf.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant start = end.minusSeconds(86_400L * windowDays);
    Series window = between(user, start, end).excludingClipped();
    ReadingType primary =
        window.ofType(ReadingType.REALTIME).isEmpty() ? ReadingType.HISTORIC : ReadingType.REALTIME;
    Series s = window.ofType(primary);
    if (s.isEmpty()) {
      return new PersonalRange(null, null, null, asOf, windowDays, 0);
    }
    double[] v = s.values();
    return new PersonalRange(
        Stats.percentile(v, 0.10),
        Stats.percentile(v, 0.50),
        Stats.percentile(v, 0.90),
        asOf,
        windowDays,
        v.length);
  }
}
