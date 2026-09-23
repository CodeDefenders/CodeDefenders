import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DiscountBasketEmptyTotalTest {
  @Test
  public void emptyBasketTotalsZero() {
    assertEquals(0, new DiscountBasket().totalCents());
  }
}
