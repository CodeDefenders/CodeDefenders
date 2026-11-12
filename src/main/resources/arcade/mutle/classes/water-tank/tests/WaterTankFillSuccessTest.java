import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WaterTankFillSuccessTest {
  @Test
  public void fillReportsSuccess() {
    assertTrue(new WaterTank(10).fill(4));
  }
}
