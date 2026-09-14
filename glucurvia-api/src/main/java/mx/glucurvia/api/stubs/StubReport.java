package mx.glucurvia.api.stubs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, escribe qué adaptadores siguen en stub. Con el perfil prod activo, si queda alguno,
 * la aplicación no arranca: un contrato se mergeó sin su implementación (guía 6.1).
 */
@Component
public class StubReport implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(StubReport.class);

  private final StubRegistry registry;
  private final Environment environment;

  public StubReport(StubRegistry registry, Environment environment) {
    this.registry = registry;
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (registry.isEmpty()) {
      log.info("Adaptadores en stub: ninguno. Todos los contratos tienen implementación real.");
      return;
    }
    boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
    String msg = "Adaptadores en stub (" + registry.stubbed().size() + "): " + registry.stubbed();
    if (prod) {
      throw new IllegalStateException(
          msg + " — no se arranca en prod con stubs; falta la implementación de un contrato");
    }
    log.warn(msg);
  }
}
