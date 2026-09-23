import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WaterTankFullAndEmptyStateTest {
  @Test
  public void fullAndEmptyStatesFollowOperations() {
    WaterTank tank = new WaterTank(2);
    assertTrue(tank.isEmpty());
    tank.fill(2);
    assertTrue(tank.isFull());
    tank.drain(2);
    assertFalse(tank.isFull());
    assertTrue(tank.isEmpty());
  }
}
