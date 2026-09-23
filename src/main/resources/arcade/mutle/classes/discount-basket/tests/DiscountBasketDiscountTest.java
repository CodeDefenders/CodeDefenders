import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketDiscountTest {
  @Test
  public void discountReducesSubtotal() {
    DiscountBasket basket = new DiscountBasket();
    basket.addItem("pen", 101);
    assertTrue(basket.setDiscountPercent(10));
    assertEquals(90, basket.totalCents());
  }
}
