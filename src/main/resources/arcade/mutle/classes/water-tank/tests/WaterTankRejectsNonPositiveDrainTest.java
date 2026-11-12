import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class WaterTankRejectsNonPositiveDrainTest {
  @Test
  public void nonPositiveDrainIsRejected() {
    assertFalse(new WaterTank(10).drain(0));
  }
}
