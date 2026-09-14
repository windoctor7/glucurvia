package mx.glucurvia.core.adapter;

import java.util.List;
import java.util.Optional;
import mx.glucurvia.core.model.Event;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.EventPatch;
import mx.glucurvia.core.model.EventQuery;
import mx.glucurvia.core.model.NewEvent;

/** Registro del timeline (diseño 5). Dueño: journal. Consumidor: assistant. */
public interface EventJournal {
  Event log(NewEvent event);

  Event update(EventId id, EventPatch patch);

  Optional<Event> find(EventId id);

  /** Ordenados por startedAt ascendente; excluye borrados. */
  List<Event> search(EventQuery query);

  /** Borrado lógico. */
  void delete(EventId id);
}
