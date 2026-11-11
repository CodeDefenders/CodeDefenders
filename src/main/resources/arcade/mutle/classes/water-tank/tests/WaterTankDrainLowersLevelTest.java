import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankDrainLowersLevelTest {
  @Test
  public void drainLowersLevel() {
    WaterTank tank = new WaterTank(10);
    tank.fill(4);
    tank.drain(2);
    assertEquals(2, tank.level());
  }
}
