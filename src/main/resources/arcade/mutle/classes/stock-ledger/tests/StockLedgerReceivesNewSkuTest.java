import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StockLedgerReceivesNewSkuTest {
  @Test
  public void receivesNewSku() {
    StockLedger ledger = new StockLedger();
    assertTrue(ledger.receive("A", 3));
    assertEquals(3, ledger.getQuantity("A"));
  }
}
