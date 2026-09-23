import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SignalLampGreenSafeTest {
  @Test
  public void greenPermitsCrossing() {
    assertTrue(new SignalLamp("green").safeToCross());
  }
}
