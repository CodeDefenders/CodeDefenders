import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class StockLedgerRejectsZeroReceiptTest {
  @Test
  public void rejectsZeroReceipt() {
    assertFalse(new StockLedger().receive("A", 0));
  }
}
