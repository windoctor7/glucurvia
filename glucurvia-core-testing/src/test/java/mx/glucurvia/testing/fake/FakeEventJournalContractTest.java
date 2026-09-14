package mx.glucurvia.testing.fake;

import mx.glucurvia.core.adapter.EventJournal;
import mx.glucurvia.testing.contract.AbstractEventJournalContract;

class FakeEventJournalContractTest extends AbstractEventJournalContract {
  @Override
  protected EventJournal subject() {
    return new FakeEventJournal();
  }
}
