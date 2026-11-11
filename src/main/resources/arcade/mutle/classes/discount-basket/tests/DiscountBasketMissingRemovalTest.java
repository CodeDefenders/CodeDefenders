import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DiscountBasketMissingRemovalTest {
  @Test
  public void missingItemCannotBeRemoved() {
    assertFalse(new DiscountBasket().removeItem("pen"));
  }
}
