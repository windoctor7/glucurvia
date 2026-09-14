package mx.glucurvia.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import mx.glucurvia.testing.db.PostgresSchemaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * Deriva del contrato REST (plan, sección 3): todo endpoint implementado bajo /api/v1 tiene que
 * estar en docs/api/openapi.yaml. Lo que el contrato promete y aún no existe solo se informa.
 */
@ExtendWith(PostgresSchemaExtension.class)
@SpringBootTest
class OpenApiDriftTest {
  private static final Logger log = LoggerFactory.getLogger(OpenApiDriftTest.class);
  private static final String PREFIX = "/api/v1";

  // El actuator registra otro RequestMappingHandlerMapping; se quiere el de los controladores de la
  // aplicación.
  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  @Test
  @SuppressWarnings("unchecked")
  void todoEndpointImplementadoEstaEnElContrato() throws Exception {
    Set<String> spec = new TreeSet<>();
    try (InputStream in = getClass().getResourceAsStream("/openapi.yaml")) {
      assertThat(in).as("docs/api/openapi.yaml en el classpath de test").isNotNull();
      Map<String, Object> doc = new Yaml().load(in);
      Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) doc.get("paths");
      paths.forEach(
          (path, ops) ->
              ops.keySet().forEach(m -> spec.add(m.toUpperCase() + " " + normalize(path))));
    }

    Set<String> implemented = new TreeSet<>();
    mappings
        .getHandlerMethods()
        .forEach(
            (info, method) -> {
              if (info.getPathPatternsCondition() == null) {
                return;
              }
              info.getPathPatternsCondition().getPatternValues().stream()
                  .filter(p -> p.startsWith(PREFIX))
                  .forEach(
                      p ->
                          info.getMethodsCondition()
                              .getMethods()
                              .forEach(
                                  m ->
                                      implemented.add(
                                          m.name()
                                              + " "
                                              + normalize(p.substring(PREFIX.length())))));
            });

    Set<String> undocumented = new TreeSet<>(implemented);
    undocumented.removeAll(spec);
    Set<String> pending = new TreeSet<>(spec);
    pending.removeAll(implemented);
    log.info(
        "Contrato REST: {} endpoints; implementados {}; pendientes {}: {}",
        spec.size(),
        implemented.size(),
        pending.size(),
        pending);
    assertThat(undocumented)
        .as("endpoints implementados que no están en docs/api/openapi.yaml")
        .isEmpty();
  }

  /** {eventId} y {id} son el mismo hueco: se comparan solo la forma de la ruta y el método. */
  private static String normalize(String path) {
    return path.replaceAll("\\{[^}]+}", "{}").replaceAll("/+$", "");
  }
}
