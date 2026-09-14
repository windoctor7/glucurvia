package mx.glucurvia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.glucurvia.api.stubs.StubRegistry;
import mx.glucurvia.core.adapter.CgmSyncAdapter;
import mx.glucurvia.core.adapter.EventJournal;
import mx.glucurvia.core.adapter.GlucoseSeriesReader;
import mx.glucurvia.core.adapter.GlycemicResponseReader;
import mx.glucurvia.core.adapter.InsightsQueries;
import mx.glucurvia.core.adapter.MealEventReader;
import mx.glucurvia.core.adapter.NutrientEstimator;
import mx.glucurvia.testing.db.PostgresSchemaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@ExtendWith(PostgresSchemaExtension.class)
@SpringBootTest
class StubAdaptersTest {

  @Autowired ApplicationContext ctx;
  @Autowired StubRegistry registry;

  @Test
  void sinImplementacionesRealesTodosLosAdaptadoresEstanEnStub() {
    for (Class<?> port :
        new Class<?>[] {
          GlucoseSeriesReader.class,
          CgmSyncAdapter.class,
          EventJournal.class,
          MealEventReader.class,
          NutrientEstimator.class,
          GlycemicResponseReader.class,
          InsightsQueries.class
        }) {
      assertThat(ctx.getBeanProvider(port).getIfAvailable()).as(port.getSimpleName()).isNotNull();
    }
    assertThat(registry.stubbed()).hasSize(7);
    // Los dos stubs de journal comparten estado: lo que se registra por uno se lee por el otro.
    var user = mx.glucurvia.core.model.UserId.random();
    var at = java.time.Instant.parse("2026-09-13T20:00:00Z");
    ctx.getBean(EventJournal.class)
        .log(mx.glucurvia.testing.builders.Meals.newMeal(user, at, 40, "tortilla"));
    assertThat(
            ctx.getBean(MealEventReader.class)
                .mealsBetween(user, at.minusSeconds(1), at.plusSeconds(1)))
        .hasSize(1);
  }

  @Test
  void conPerfilProdLaAplicacionSeNiegaAArrancarConStubs() {
    assertThatThrownBy(
            () ->
                SpringApplication.run(
                    GlucurviaApplication.class,
                    "--spring.profiles.active=prod",
                    "--server.port=0",
                    "--spring.main.web-application-type=none"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no se arranca en prod con stubs");
  }
}
