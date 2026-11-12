import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class StockLedgerRejectsOverShipmentTest {
  @Test
  public void rejectsOverShipment() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 2);
    assertFalse(ledger.ship("A", 3));
  }
}
