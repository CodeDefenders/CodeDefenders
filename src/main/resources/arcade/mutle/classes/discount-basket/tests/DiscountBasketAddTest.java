import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscountBasketAddTest {
  @Test
  public void acceptsNamedItem() {
    assertTrue(new DiscountBasket().addItem("pen", 125));
  }
}
