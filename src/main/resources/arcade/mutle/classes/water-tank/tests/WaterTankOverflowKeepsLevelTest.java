import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankOverflowKeepsLevelTest {
  @Test
  public void rejectedOverflowKeepsLevel() {
    WaterTank tank = new WaterTank(10);
    tank.fill(9);
    tank.fill(2);
    assertEquals(9, tank.level());
  }
}
