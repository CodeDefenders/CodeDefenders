import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibraryLoanCheckoutDueDaysTest {
  @Test
  public void checkoutSetsDuePeriod() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    assertEquals(14, loan.daysRemaining());
  }
}
