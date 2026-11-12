import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StockLedgerAccumulatesReceiptsTest {
  @Test
  public void accumulatesReceipts() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 2);
    ledger.receive("A", 3);
    assertEquals(5, ledger.getQuantity("A"));
  }
}
