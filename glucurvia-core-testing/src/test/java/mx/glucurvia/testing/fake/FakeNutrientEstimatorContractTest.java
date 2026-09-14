package mx.glucurvia.testing.fake;

import mx.glucurvia.core.adapter.NutrientEstimator;
import mx.glucurvia.testing.contract.AbstractNutrientEstimatorContract;

class FakeNutrientEstimatorContractTest extends AbstractNutrientEstimatorContract {
  @Override
  protected NutrientEstimator subject() {
    return new FakeNutrientEstimator();
  }
}
