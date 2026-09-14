package mx.glucurvia.api.stubs;

import mx.glucurvia.core.adapter.CgmSyncAdapter;
import mx.glucurvia.core.adapter.EventJournal;
import mx.glucurvia.core.adapter.GlucoseSeriesReader;
import mx.glucurvia.core.adapter.GlycemicResponseReader;
import mx.glucurvia.core.adapter.InsightsQueries;
import mx.glucurvia.core.adapter.MealEventReader;
import mx.glucurvia.core.adapter.NutrientEstimator;
import mx.glucurvia.testing.fake.FakeCgmSync;
import mx.glucurvia.testing.fake.FakeEventJournal;
import mx.glucurvia.testing.fake.FakeGlycemicResponses;
import mx.glucurvia.testing.fake.FakeInsights;
import mx.glucurvia.testing.fake.FakeNutrientEstimator;
import mx.glucurvia.testing.fake.InMemoryGlucoseSeries;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Un stub por adaptador de core, envolviendo los fakes de core-testing (plan, sección 2). Es una
 * autoconfiguración, no una @Configuration normal: solo así Spring la evalúa después de
 * los @Service reales de los módulos y @ConditionalOnMissingBean se retira cuando llega la
 * implementación. Registrada en
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 */
@AutoConfiguration
public class StubAdaptersAutoConfiguration {

  private final FakeEventJournal journal = new FakeEventJournal();

  @Bean
  public StubRegistry stubRegistry() {
    return new StubRegistry();
  }

  @Bean
  @ConditionalOnMissingBean(GlucoseSeriesReader.class)
  public GlucoseSeriesReader glucoseSeriesReaderStub(StubRegistry registry) {
    registry.register("GlucoseSeriesReader");
    return new InMemoryGlucoseSeries();
  }

  @Bean
  @ConditionalOnMissingBean(CgmSyncAdapter.class)
  public CgmSyncAdapter cgmSyncPortStub(StubRegistry registry) {
    registry.register("CgmSyncAdapter");
    return new FakeCgmSync().reachable(false);
  }

  // El fake implementa los dos adaptadores de journal; se expone cada uno con un delegado que solo
  // implementa su interfaz, para que Spring no vea dos candidatos del mismo tipo.
  @Bean
  @ConditionalOnMissingBean(EventJournal.class)
  public EventJournal eventJournalStub(StubRegistry registry) {
    registry.register("EventJournal");
    return new EventJournalDelegate(journal);
  }

  @Bean
  @ConditionalOnMissingBean(MealEventReader.class)
  public MealEventReader mealEventReaderStub(StubRegistry registry) {
    registry.register("MealEventReader");
    return new MealEventReaderDelegate(journal);
  }

  private record EventJournalDelegate(FakeEventJournal target) implements EventJournal {
    @Override
    public mx.glucurvia.core.model.Event log(mx.glucurvia.core.model.NewEvent event) {
      return target.log(event);
    }

    @Override
    public mx.glucurvia.core.model.Event update(
        mx.glucurvia.core.model.EventId id, mx.glucurvia.core.model.EventPatch patch) {
      return target.update(id, patch);
    }

    @Override
    public java.util.Optional<mx.glucurvia.core.model.Event> find(
        mx.glucurvia.core.model.EventId id) {
      return target.find(id);
    }

    @Override
    public java.util.List<mx.glucurvia.core.model.Event> search(
        mx.glucurvia.core.model.EventQuery query) {
      return target.search(query);
    }

    @Override
    public void delete(mx.glucurvia.core.model.EventId id) {
      target.delete(id);
    }
  }

  private record MealEventReaderDelegate(FakeEventJournal target) implements MealEventReader {
    @Override
    public java.util.List<mx.glucurvia.core.model.MealEvent> mealsBetween(
        mx.glucurvia.core.model.UserId user, java.time.Instant from, java.time.Instant to) {
      return target.mealsBetween(user, from, to);
    }

    @Override
    public java.util.List<mx.glucurvia.core.model.ContextEvent> contextBetween(
        mx.glucurvia.core.model.UserId user,
        java.time.Instant from,
        java.time.Instant to,
        java.util.Set<mx.glucurvia.core.model.EventType> types) {
      return target.contextBetween(user, from, to, types);
    }
  }

  @Bean
  @ConditionalOnMissingBean(NutrientEstimator.class)
  public NutrientEstimator nutrientEstimatorStub(StubRegistry registry) {
    registry.register("NutrientEstimator");
    return new FakeNutrientEstimator();
  }

  @Bean
  @ConditionalOnMissingBean(GlycemicResponseReader.class)
  public GlycemicResponseReader glycemicResponseReaderStub(StubRegistry registry) {
    registry.register("GlycemicResponseReader");
    return new FakeGlycemicResponses();
  }

  @Bean
  @ConditionalOnMissingBean(InsightsQueries.class)
  public InsightsQueries insightsQueriesStub(StubRegistry registry) {
    registry.register("InsightsQueries");
    return new FakeInsights();
  }
}
