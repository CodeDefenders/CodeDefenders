import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WaterTankStartsEmptyTest {
  @Test
  public void tankStartsEmpty() {
    WaterTank tank = new WaterTank(10);
    assertTrue(tank.isEmpty());
    assertEquals(0, tank.level());
  }
}
