import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class StockLedgerRejectsNegativeReceiptTest {
  @Test
  public void rejectsNegativeReceipt() {
    assertFalse(new StockLedger().receive("A", -1));
  }
}
