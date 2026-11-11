import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StockLedgerShipmentReducesQuantityTest {
  @Test
  public void shipmentReducesQuantity() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 3);
    ledger.ship("A", 1);
    assertEquals(2, ledger.getQuantity("A"));
  }
}
