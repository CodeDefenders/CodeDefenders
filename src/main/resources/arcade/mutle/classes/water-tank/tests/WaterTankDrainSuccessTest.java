import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WaterTankDrainSuccessTest {
  @Test
  public void drainReportsSuccess() {
    WaterTank tank = new WaterTank(10);
    tank.fill(4);
    assertTrue(tank.drain(2));
  }
}
