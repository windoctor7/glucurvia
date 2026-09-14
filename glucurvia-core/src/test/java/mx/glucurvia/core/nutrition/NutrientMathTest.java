package mx.glucurvia.core.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import mx.glucurvia.core.model.Range;
import org.junit.jupiter.api.Test;

class NutrientMathTest {
  @Test
  void bandaDeLaCenaDelDiseno() {
    // avena 23 (17–29), manzana 10 (8–13), tostada 8 (6–11), bebida 9 (5–13): diseño 9.3 → ≈50
    // (40–60)
    Range band =
        NutrientMath.mealBand(
            List.of(
                Range.of(23, 17, 29), Range.of(10, 8, 13), Range.of(8, 6, 11), Range.of(9, 5, 13)));
    assertThat(band.point()).isEqualTo(50);
    assertThat(band.low()).isCloseTo(39.6, within(0.1));
    assertThat(band.high()).isCloseTo(60.4, within(0.1));
    assertThat(
            Range.sumWorstCase(
                List.of(
                    Range.of(23, 17, 29),
                    Range.of(10, 8, 13),
                    Range.of(8, 6, 11),
                    Range.of(9, 5, 13))))
        .isEqualTo(Range.of(50, 36, 66));
  }

  @Test
  void laBandaNuncaEsMasEstrechaQueElItemMasIncierto() {
    Range band = NutrientMath.mealBand(List.of(Range.of(30, 10, 50), Range.exact(5)));
    assertThat(band.halfWidth()).isGreaterThanOrEqualTo(20);
  }
}
