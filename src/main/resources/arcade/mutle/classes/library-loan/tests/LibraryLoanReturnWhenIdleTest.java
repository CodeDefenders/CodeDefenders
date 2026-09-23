import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LibraryLoanReturnWhenIdleTest {
  @Test
  public void idleLoanCannotBeReturned() {
    assertFalse(new LibraryLoan("Dune", 14, 1).returnBook());
  }
}
