package mx.glucurvia.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import mx.glucurvia.testing.db.PostgresSchemaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** El baseline crea todas las tablas del diseño (sección 3) y siembra el usuario. */
@ExtendWith(PostgresSchemaExtension.class)
class SchemaBaselineTest {

  @Test
  void existenTodasLasTablasDelDisenoYElUsuarioSembrado() throws Exception {
    List<String> tables = new ArrayList<>();
    try (Connection c = PostgresSchemaExtension.connection();
        ResultSet rs =
            c.createStatement()
                .executeQuery(
                    "select table_name from information_schema.tables where table_schema = 'public' order by 1")) {
      while (rs.next()) {
        tables.add(rs.getString(1));
      }
    }
    assertThat(tables)
        .contains(
            "users",
            "import_batches",
            "cgm_pull_state",
            "glucose_readings",
            "conversations",
            "conversation_messages",
            "events",
            "food_references",
            "user_food_aliases",
            "meals",
            "meal_items",
            "glycemic_responses",
            "flyway_schema_history");
    try (Connection c = PostgresSchemaExtension.connection();
        ResultSet rs = c.createStatement().executeQuery("select locale, timezone from users")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString(1)).isEqualTo("es-MX");
      assertThat(rs.getString(2)).isEqualTo("America/Mexico_City");
      assertThat(rs.next()).isFalse();
    }
  }
}
