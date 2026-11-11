import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StockLedgerRemovesDepletedSkuTest {
  @Test
  public void removesDepletedSku() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 2);
    ledger.ship("A", 2);
    assertEquals(0, ledger.getSkuCount());
  }
}
