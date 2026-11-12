import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DiscountBasketRoundingTest {
  @Test
  public void totalRoundsDownAfterDiscount() {
    DiscountBasket basket = new DiscountBasket();
    basket.addItem("pen", 19);
    basket.setDiscountPercent(50);
    assertEquals(9, basket.totalCents());
  }
}
