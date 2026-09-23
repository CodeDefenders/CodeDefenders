import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SignalLampAmberUnsafeTest {
  @Test
  public void amberDoesNotPermitCrossing() {
    assertFalse(new SignalLamp("amber").safeToCross());
  }
}
