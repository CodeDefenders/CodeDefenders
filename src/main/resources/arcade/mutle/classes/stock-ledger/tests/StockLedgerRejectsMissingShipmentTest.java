import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class StockLedgerRejectsMissingShipmentTest {
  @Test
  public void rejectsMissingShipment() {
    assertFalse(new StockLedger().ship("A", 1));
  }
}
