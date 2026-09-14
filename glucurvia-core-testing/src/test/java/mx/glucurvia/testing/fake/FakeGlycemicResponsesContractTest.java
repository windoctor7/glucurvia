package mx.glucurvia.testing.fake;

import java.util.List;
import mx.glucurvia.core.adapter.GlycemicResponseReader;
import mx.glucurvia.core.model.GlycemicResponse;
import mx.glucurvia.testing.contract.AbstractGlycemicResponseReaderContract;

class FakeGlycemicResponsesContractTest extends AbstractGlycemicResponseReaderContract {
  @Override
  protected GlycemicResponseReader subjectWith(List<GlycemicResponse> responses) {
    FakeGlycemicResponses f = new FakeGlycemicResponses();
    responses.forEach(f::put);
    return f;
  }
}
