package mx.glucurvia.testing.fake;

import java.time.Duration;
import java.time.Instant;
import mx.glucurvia.core.adapter.CgmSyncAdapter;
import mx.glucurvia.core.model.SyncResult;
import mx.glucurvia.core.model.UserId;

/** Pull simulado: configurable como alcanzable o no; cuenta llamadas. */
public final class FakeCgmSync implements CgmSyncAdapter {
  private boolean reachable = true;
  private int newReadingsPerCall = 0;
  private Instant latest;
  private int calls;

  public FakeCgmSync reachable(boolean value) {
    this.reachable = value;
    return this;
  }

  public FakeCgmSync newReadingsPerCall(int n) {
    this.newReadingsPerCall = n;
    return this;
  }

  public FakeCgmSync latest(Instant ts) {
    this.latest = ts;
    return this;
  }

  public int calls() {
    return calls;
  }

  @Override
  public SyncResult syncNow(UserId user, Duration timeout) {
    calls++;
    if (!reachable) {
      return SyncResult.unreachable(Duration.ofMillis(1), latest);
    }
    return new SyncResult(true, newReadingsPerCall, latest, Duration.ofMillis(1));
  }
}
