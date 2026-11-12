import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibraryLoanRenewAddsDaysTest {
  @Test
  public void renewalAddsAnotherLoanPeriod() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    loan.renew();
    assertEquals(28, loan.daysRemaining());
  }
}
