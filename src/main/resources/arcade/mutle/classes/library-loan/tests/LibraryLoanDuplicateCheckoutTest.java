import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanDuplicateCheckoutTest {
  @Test
  public void activeLoanCannotBeCheckedOutTwice() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    assertFalse(loan.checkout());
  }
}
