import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SignalLampRedUnsafeTest {
  @Test
  public void redDoesNotPermitCrossing() {
    assertFalse(new SignalLamp("red").safeToCross());
  }
}
