package mx.glucurvia.core.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.glucurvia.core.model.Confidence;
import mx.glucurvia.core.model.EstimatedItem;
import mx.glucurvia.core.model.MacroSource;
import mx.glucurvia.core.model.Nutrients;
import mx.glucurvia.core.model.Range;
import org.junit.jupiter.api.Test;

class ClarificationPolicyTest {
  private static EstimatedItem item(String name, Range carbs, MacroSource source) {
    return new EstimatedItem(
        name,
        name,
        null,
        null,
        null,
        null,
        Range.exact(50),
        null,
        1.0,
        new Nutrients(carbs.point(), 0, 0, 0, 0),
        carbs,
        Confidence.of(0.6),
        null,
        source,
        List.of(),
        false);
  }

  @Test
  void laCenaDelDisenoNoPregunta() {
    var items =
        List.of(
            item("avena", Range.of(23, 17, 29), MacroSource.CATALOG),
            item("manzana", Range.of(10, 8, 13), MacroSource.CATALOG),
            item("tostada", Range.of(8, 6, 11), MacroSource.CATALOG),
            item("bebida", Range.of(9, 5, 13), MacroSource.CATALOG));
    assertThat(ClarificationPolicy.itemToAskAbout(items)).isEmpty();
  }

  @Test
  void preguntaSoloPorElItemAnchoYDominanteSinAlias() {
    var items =
        List.of(
            item("arroz", Range.of(45, 20, 70), MacroSource.CATALOG),
            item("pollo", Range.exact(0), MacroSource.CATALOG));
    assertThat(ClarificationPolicy.itemToAskAbout(items))
        .map(EstimatedItem::foodName)
        .contains("arroz");
    var withAlias = List.of(item("arroz", Range.of(45, 20, 70), MacroSource.USER_ALIAS));
    assertThat(ClarificationPolicy.itemToAskAbout(withAlias)).isEmpty();
  }
}
