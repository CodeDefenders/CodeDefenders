import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankRepeatedFillsAccumulateTest {
  @Test
  public void repeatedFillsAccumulate() {
    WaterTank tank = new WaterTank(10);
    tank.fill(3);
    tank.fill(4);

    assertEquals(7, tank.level());
  }
}
