import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankFillRaisesLevelTest {
  @Test
  public void fillRaisesLevel() {
    WaterTank tank = new WaterTank(10);
    tank.fill(4);
    assertEquals(4, tank.level());
  }
}
