import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketRemoveTest {
  @Test
  public void removalUpdatesCount() {
    DiscountBasket basket = new DiscountBasket();
    basket.addItem("pen", 1);
    assertTrue(basket.removeItem("pen"));
    assertEquals(0, basket.itemCount());
  }
}
