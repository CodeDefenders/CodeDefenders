import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketCapacityTest {
  @Test
  public void acceptsEighthButRejectsNinthItem() {
    DiscountBasket basket = new DiscountBasket();
    for (int i = 0; i < 8; i++) {
      assertTrue(basket.addItem("i" + i, i));
    }
    assertFalse(basket.addItem("extra", 1));
  }
}
