import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibraryLoanReturnResetsRenewalsTest {
  @Test
  public void returnRestoresRenewalAllowance() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 2);
    loan.checkout();
    loan.renew();

    loan.returnBook();

    assertEquals(2, loan.renewalsRemaining());
  }
}
