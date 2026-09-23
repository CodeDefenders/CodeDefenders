import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class WaterTankRejectsUnderflowTest {
  @Test
  public void underflowDrainIsRejected() {
    WaterTank tank = new WaterTank(10);
    tank.fill(2);
    assertFalse(tank.drain(3));
  }
}
