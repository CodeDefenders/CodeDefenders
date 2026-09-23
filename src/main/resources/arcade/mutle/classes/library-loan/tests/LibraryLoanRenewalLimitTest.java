import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanRenewalLimitTest {
  @Test
  public void renewalLimitIsEnforced() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    loan.renew();
    assertFalse(loan.renew());
  }
}
