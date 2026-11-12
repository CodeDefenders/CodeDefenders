import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class WaterTankRejectsNonPositiveFillTest {
  @Test
  public void nonPositiveFillIsRejected() {
    assertFalse(new WaterTank(10).fill(0));
  }
}
