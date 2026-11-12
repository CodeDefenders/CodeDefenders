import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanAllowsZeroRenewalsTest {
  @Test
  public void zeroRenewalAllowanceIsValid() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 0);
    loan.checkout();

    assertFalse(loan.renew());
  }
}
