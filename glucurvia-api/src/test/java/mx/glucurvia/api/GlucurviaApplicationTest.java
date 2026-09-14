package mx.glucurvia.api;

import static org.assertj.core.api.Assertions.assertThat;

import mx.glucurvia.testing.db.PostgresSchemaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** La aplicación arranca contra un Postgres real con el esquema completo aplicado. */
@ExtendWith(PostgresSchemaExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlucurviaApplicationTest {

  @Autowired TestRestTemplate rest;

  @Test
  void laAplicacionArrancaYElHealthRespondeUp() {
    String body = rest.getForObject("/actuator/health", String.class);
    assertThat(body).contains("\"status\":\"UP\"");
  }
}
