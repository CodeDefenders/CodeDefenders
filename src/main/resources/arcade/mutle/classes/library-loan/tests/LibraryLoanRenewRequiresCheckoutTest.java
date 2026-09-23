import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanRenewRequiresCheckoutTest {
  @Test
  public void renewalRequiresCheckout() {
    assertFalse(new LibraryLoan("Dune", 14, 1).renew());
  }
}
