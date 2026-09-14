package mx.glucurvia.core.event;

import java.time.Instant;
import mx.glucurvia.core.model.UserId;

/** Publica cgm tras insertar lecturas nuevas; glycemic recomputa las comidas de la ventana. */
public record ReadingsIngested(UserId userId, Instant from, Instant to, String source) {}
