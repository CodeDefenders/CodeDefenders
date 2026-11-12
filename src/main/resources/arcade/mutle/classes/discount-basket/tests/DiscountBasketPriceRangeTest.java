import org.junit.Test;

public class DiscountBasketPriceRangeTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNegativePrice() {
    new DiscountBasket().addItem("pen", -1);
  }
}
