import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanReturnClearsStateTest {
  @Test
  public void returnClearsLoanState() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    loan.returnBook();
    assertFalse(loan.isCheckedOut());
    assertEquals(0, loan.daysRemaining());
  }
}
