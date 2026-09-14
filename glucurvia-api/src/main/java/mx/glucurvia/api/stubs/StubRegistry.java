package mx.glucurvia.api.stubs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Qué adaptadores de core siguen servidos por un stub en esta instancia. */
public final class StubRegistry {
  private final List<String> stubbed = new ArrayList<>();

  void register(String port) {
    stubbed.add(port);
  }

  public List<String> stubbed() {
    return Collections.unmodifiableList(stubbed);
  }

  public boolean isEmpty() {
    return stubbed.isEmpty();
  }
}
