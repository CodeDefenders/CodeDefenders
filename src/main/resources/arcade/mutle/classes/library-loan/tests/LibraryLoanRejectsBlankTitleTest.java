import org.junit.Test;

public class LibraryLoanRejectsBlankTitleTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsBlankTitle() {
    new LibraryLoan(" ", 14, 1);
  }
}
