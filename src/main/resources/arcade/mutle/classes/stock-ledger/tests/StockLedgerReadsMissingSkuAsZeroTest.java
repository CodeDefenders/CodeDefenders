import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StockLedgerReadsMissingSkuAsZeroTest {
  @Test
  public void readsMissingSkuAsZero() {
    assertEquals(0, new StockLedger().getQuantity("A"));
  }
}
