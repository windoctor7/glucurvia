package mx.glucurvia.core.adapter;

import java.time.Duration;
import mx.glucurvia.core.model.SyncResult;
import mx.glucurvia.core.model.UserId;

/**
 * Pull inmediato a la fuente CGM (diseño 4.4). Dueño: cgm. Consumidor: assistant. Adaptador de
 * core.
 */
public interface CgmSyncAdapter {
  /** Nunca lanza por fallo de la fuente: devuelve sourceReachable=false dentro del timeout. */
  SyncResult syncNow(UserId user, Duration timeout);
}
