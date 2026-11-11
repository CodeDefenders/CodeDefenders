import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class StockLedgerUsesStrictReorderThresholdTest {
  @Test
  public void usesStrictReorderThreshold() {
    StockLedger ledger = new StockLedger();
    ledger.receive("A", 2);
    assertFalse(ledger.isBelow("A", 2));
  }
}
