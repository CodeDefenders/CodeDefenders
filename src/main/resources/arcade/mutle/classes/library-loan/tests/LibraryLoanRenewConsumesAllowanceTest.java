import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibraryLoanRenewConsumesAllowanceTest {
  @Test
  public void renewalConsumesOneAllowance() {
    LibraryLoan loan = new LibraryLoan("Dune", 14, 2);
    loan.checkout();
    loan.renew();
    assertEquals(1, loan.renewalsRemaining());
  }
}
