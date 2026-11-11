import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class WaterTankRejectsOverflowTest {
  @Test
  public void overflowFillIsRejected() {
    WaterTank tank = new WaterTank(10);
    tank.fill(9);
    assertFalse(tank.fill(2));
  }
}
