package mx.glucurvia.core.event;

import java.time.Instant;
import mx.glucurvia.core.model.EventId;
import mx.glucurvia.core.model.EventType;
import mx.glucurvia.core.model.UserId;

/** Publica journal al registrar un evento. Los listeners son @Async y tras commit (plan 2). */
public record EventLogged(UserId userId, EventId eventId, EventType type, Instant startedAt) {}
