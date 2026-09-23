import org.junit.Test;

public class LibraryLoanRejectsNegativeRenewalsTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNegativeRenewals() {
    new LibraryLoan("Dune", 14, -1);
  }
}
