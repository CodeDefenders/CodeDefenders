import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DiscountBasketZeroPriceTest {
  @Test
  public void acceptsZeroPrice() {
    DiscountBasket basket = new DiscountBasket();
    basket.addItem("note", 0);
    assertEquals(0, basket.totalCents());
  }
}
