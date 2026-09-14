package mx.glucurvia.testing.fake;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mx.glucurvia.core.adapter.EventJournal;
import mx.glucurvia.core.adapter.MealEventReader;
import mx.glucurvia.core.model.ContextEvent;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.EstimatedMeal;
import mx.glucurvia.core.model.Event;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.EventPatch;
import mx.glucurvia.core.model.EventQuery;
import mx.glucurvia.core.model.EventTime;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.MealEvent;
import mx.glucurvia.core.model.NewEvent;
import mx.glucurvia.core.model.UserId;

/**
 * Timeline en memoria. Implementa los dos adaptadores de journal para que un solo fake sirva a
 * todos.
 */
public final class FakeEventJournal implements EventJournal, MealEventReader {
  private final Clock clock;
  private final Map<EventId, Event> events = new LinkedHashMap<>();
  private final Set<EventId> deleted = new HashSet<>();

  public FakeEventJournal() {
    this(Clock.systemUTC());
  }

  public FakeEventJournal(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Event log(NewEvent e) {
    Event ev =
        new Event(
            EventId.random(),
            e.userId(),
            e.type(),
            e.time(),
            e.endedAt(),
            e.source(),
            e.rawText(),
            e.messageId(),
            e.attributes(),
            e.meal(),
            clock.instant());
    events.put(ev.id(), ev);
    return ev;
  }

  @Override
  public Event update(EventId id, EventPatch patch) {
    Event old = find(id).orElseThrow(() -> new NoSuchElementException("evento " + id));
    EventTime time = patch.time() != null ? patch.time() : old.time();
    Instant endedAt =
        patch.clearEndedAt() ? null : (patch.endedAt() != null ? patch.endedAt() : old.endedAt());
    Map<String, Object> attrs = old.attributes();
    if (patch.attributes() != null) {
      attrs = new HashMap<>(old.attributes());
      attrs.putAll(patch.attributes());
    }
    EstimatedMeal meal = patch.meal() != null ? patch.meal() : old.meal();
    Event updated =
        new Event(
            old.id(),
            old.userId(),
            old.type(),
            time,
            endedAt,
            old.source(),
            old.rawText(),
            old.messageId(),
            attrs,
            meal,
            old.createdAt());
    events.put(id, updated);
    return updated;
  }

  @Override
  public Optional<Event> find(EventId id) {
    return deleted.contains(id) ? Optional.empty() : Optional.ofNullable(events.get(id));
  }

  @Override
  public List<Event> search(EventQuery q) {
    return events.values().stream()
        .filter(e -> !deleted.contains(e.id()))
        .filter(e -> e.userId().equals(q.userId()))
        .filter(e -> !e.startedAt().isBefore(q.from()) && !e.startedAt().isAfter(q.to()))
        .filter(e -> q.matchesType(e.type()))
        .filter(
            e ->
                q.textContains() == null
                    || (e.rawText() != null
                        && e.rawText()
                            .toLowerCase(Locale.ROOT)
                            .contains(q.textContains().toLowerCase(Locale.ROOT))))
        .sorted(Comparator.comparing(Event::startedAt))
        .toList();
  }

  @Override
  public void delete(EventId id) {
    deleted.add(id);
  }

  @Override
  public List<MealEvent> mealsBetween(UserId user, Instant from, Instant to) {
    List<MealEvent> out = new ArrayList<>();
    for (Event e : search(new EventQuery(user, from, to, Set.of(EventType.MEAL), null))) {
      EstimatedMeal m = e.meal();
      UUID dominant =
          m.items().stream()
              .filter(i -> i.foodRefId() != null)
              .max(Comparator.comparingDouble(i -> i.nutrients().carbsG()))
              .map(EstimatedItem::foodRefId)
              .orElse(null);
      Set<String> foods =
          m.items().stream()
              .map(i -> i.foodName().toLowerCase(Locale.ROOT))
              .collect(Collectors.toSet());
      out.add(
          new MealEvent(
              e.id(),
              e.userId(),
              e.startedAt(),
              e.time().localTz(),
              e.time().confidence(),
              e.time().uncertaintyMin(),
              m.mealType(),
              m.totals().carbsG(),
              m.confidence(),
              dominant,
              foods,
              Set.of()));
    }
    return out;
  }

  @Override
  public List<ContextEvent> contextBetween(
      UserId user, Instant from, Instant to, Set<EventType> types) {
    return search(new EventQuery(user, from, to, types, null)).stream()
        .filter(e -> e.type() != EventType.MEAL)
        .map(e -> new ContextEvent(e.id(), e.type(), e.startedAt(), e.endedAt(), e.attributes()))
        .toList();
  }
}
