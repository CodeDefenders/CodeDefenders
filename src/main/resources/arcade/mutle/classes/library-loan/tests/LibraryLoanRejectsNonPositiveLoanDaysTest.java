import org.junit.Test;

public class LibraryLoanRejectsNonPositiveLoanDaysTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNonPositiveLoanDays() {
    new LibraryLoan("Dune", 0, 1);
  }
}
