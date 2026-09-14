package mx.glucurvia.testing.fake;

import java.util.List;
import mx.glucurvia.core.adapter.InsightsQueries;
import mx.glucurvia.core.model.MealSummary;
import mx.glucurvia.core.model.UserId;
import mx.glucurvia.testing.contract.AbstractInsightsQueriesContract;

class FakeInsightsContractTest extends AbstractInsightsQueriesContract {
  @Override
  protected InsightsQueries subjectWith(UserId user, List<MealSummary> meals) {
    return new FakeInsights().add(user, meals.toArray(MealSummary[]::new));
  }
}
