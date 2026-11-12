import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WaterTankFillExactCapacityTest {
  @Test
  public void exactCapacityCanBeFilled() {
    WaterTank tank = new WaterTank(10);
    assertTrue(tank.fill(10));
    assertTrue(tank.isFull());
  }
}
