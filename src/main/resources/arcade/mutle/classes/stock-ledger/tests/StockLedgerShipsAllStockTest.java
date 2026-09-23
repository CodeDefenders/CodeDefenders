import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StockLedgerShipsAllStockTest {
  @Test
  public void shipsAllStock() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 2);
    assertTrue(ledger.ship("A", 2));
  }
}
