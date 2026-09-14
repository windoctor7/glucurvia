package mx.glucurvia.testing.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Levanta un Postgres efímero una sola vez por JVM y aplica TODAS las migraciones de
 * glucurvia-schema (plan 5.3). Expone la conexión como propiedades de sistema para que
 * {@code @SpringBootTest} la use sin configuración: spring.datasource.url/username/password.
 *
 * <p>Uso: {@code @ExtendWith(PostgresSchemaExtension.class)} en la clase de test.
 */
public final class PostgresSchemaExtension implements BeforeAllCallback {
  public static final String IMAGE = "postgres:17-alpine";
  public static final String[] LOCATIONS = {
    "classpath:db/migration/core", "classpath:db/migration/cgm", "classpath:db/migration/journal",
    "classpath:db/migration/nutrition", "classpath:db/migration/assistant",
        "classpath:db/migration/glycemic"
  };

  private static PostgreSQLContainer<?> container;
  private static boolean migrated;

  @Override
  public void beforeAll(ExtensionContext context) {
    start();
  }

  public static synchronized void start() {
    if (container == null) {
      container =
          new PostgreSQLContainer<>(IMAGE)
              .withDatabaseName("glucurvia")
              .withUsername("glucurvia")
              .withPassword("glucurvia");
      container.start();
    }
    if (!migrated) {
      Flyway.configure()
          .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
          .locations(LOCATIONS)
          .outOfOrder(true)
          .failOnMissingLocations(false)
          .load()
          .migrate();
      migrated = true;
    }
    System.setProperty("spring.datasource.url", container.getJdbcUrl());
    System.setProperty("spring.datasource.username", container.getUsername());
    System.setProperty("spring.datasource.password", container.getPassword());
    System.setProperty("spring.flyway.enabled", "false");
  }

  public static String jdbcUrl() {
    start();
    return container.getJdbcUrl();
  }

  public static Connection connection() throws SQLException {
    start();
    return DriverManager.getConnection(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }
}
