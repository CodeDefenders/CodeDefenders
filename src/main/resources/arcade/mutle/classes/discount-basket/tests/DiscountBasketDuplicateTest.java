import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketDuplicateTest {
  @Test
  public void rejectsRepeatedName() {
    DiscountBasket basket = new DiscountBasket();
    assertTrue(basket.addItem("pen", 1));
    assertFalse(basket.addItem("pen", 2));
  }
}
