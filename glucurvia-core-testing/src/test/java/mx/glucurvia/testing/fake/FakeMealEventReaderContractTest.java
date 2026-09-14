package mx.glucurvia.testing.fake;

import java.util.List;
import mx.glucurvia.core.adapter.MealEventReader;
import mx.glucurvia.core.model.NewEvent;
import mx.glucurvia.testing.contract.AbstractMealEventReaderContract;

class FakeMealEventReaderContractTest extends AbstractMealEventReaderContract {
  @Override
  protected MealEventReader subjectWith(List<NewEvent> events) {
    FakeEventJournal j = new FakeEventJournal();
    events.forEach(j::log);
    return j;
  }
}
