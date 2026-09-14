package mx.glucurvia.core.event;

import java.time.Instant;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.UserId;

/** Publica journal al corregir o borrar un evento. */
public record EventUpdated(
    UserId userId, EventId eventId, EventType type, Instant startedAt, boolean deleted) {}
