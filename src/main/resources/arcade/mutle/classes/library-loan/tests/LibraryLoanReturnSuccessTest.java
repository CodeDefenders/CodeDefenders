import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LibraryLoanReturnSuccessTest {
  @Test
  public void returnReportsSuccess() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 1);
    loan.checkout();
    assertTrue(loan.returnBook());
  }
}
