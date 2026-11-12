import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DiscountBasketSameDiscountTest {
  @Test
  public void unchangedDiscountReportsFalse() {
    assertFalse(new DiscountBasket().setDiscountPercent(0));
  }
}
