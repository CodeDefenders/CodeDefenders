import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LibraryLoanCheckoutStateTest {
  @Test
  public void checkoutMarksLoanActive() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    assertTrue(loan.isCheckedOut());
  }
}
