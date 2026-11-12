import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibraryLoanRetainsTitleTest {
  @Test
  public void titleIsRetained() {
    assertEquals("Dune", new LibraryLoan("Dune", 14, 1).title());
  }
}
