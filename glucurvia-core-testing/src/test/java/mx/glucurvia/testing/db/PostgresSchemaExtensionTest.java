package mx.glucurvia.testing.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PostgresSchemaExtension.class)
class PostgresSchemaExtensionTest {
  @Test
  void levantaPostgresYAplicaFlyway() throws Exception {
    try (Connection c = PostgresSchemaExtension.connection();
        ResultSet rs =
            c.createStatement().executeQuery("select count(*) from flyway_schema_history")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(0);
    }
    assertThat(System.getProperty("spring.datasource.url")).startsWith("jdbc:postgresql://");
  }
}
