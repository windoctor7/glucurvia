package mx.glucurvia.testing.fake;

import java.util.List;
import mx.glucurvia.core.adapter.GlucoseSeriesReader;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.core.series.Reading;
import mx.glucurvia.testing.contract.AbstractGlucoseSeriesReaderContract;

class InMemoryGlucoseSeriesContractTest extends AbstractGlucoseSeriesReaderContract {
  @Override
  protected GlucoseSeriesReader subjectWith(UserId user, List<Reading> readings) {
    return new InMemoryGlucoseSeries().add(user, readings);
  }
}
