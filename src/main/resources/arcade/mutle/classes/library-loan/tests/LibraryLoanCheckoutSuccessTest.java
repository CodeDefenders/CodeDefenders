import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LibraryLoanCheckoutSuccessTest {
  @Test
  public void checkoutReportsSuccess() {
    assertTrue(new LibraryLoan("Dune", 14, 1).checkout());
  }
}
