import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankRepeatedDrainsAccumulateTest {
  @Test
  public void repeatedDrainsAccumulate() {
    WaterTank tank = new WaterTank(10);
    tank.fill(10);
    tank.drain(3);
    tank.drain(2);

    assertEquals(5, tank.level());
  }
}
