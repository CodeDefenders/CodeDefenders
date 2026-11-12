import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankRemainingCapacityTest {
  @Test
  public void remainingCapacityTracksLevel() {
    WaterTank tank = new WaterTank(10);
    tank.fill(4);
    assertEquals(6, tank.remainingCapacity());
  }
}
