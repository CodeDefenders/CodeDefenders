import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StockLedgerReceiveReportsSuccessTest {
  @Test
  public void receiptReportsSuccess() {
    assertTrue(new StockLedger().receive("A", 2));
  }
}
