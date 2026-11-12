import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketDiscountReportsChangeTest {
  @Test
  public void changedDiscountReportsSuccess() {
    assertTrue(new DiscountBasket().setDiscountPercent(10));
  }
}
