import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankUnderflowKeepsLevelTest {
  @Test
  public void rejectedUnderflowKeepsLevel() {
    WaterTank tank = new WaterTank(10);
    tank.fill(2);
    tank.drain(3);
    assertEquals(2, tank.level());
  }
}
