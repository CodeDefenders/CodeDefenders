import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SignalLampGreenToAmberTest {
  @Test
  public void greenAdvancesToAmber() {
    SignalLamp lamp = new SignalLamp("green");
    assertTrue(lamp.advance());
    assertEquals("amber", lamp.colour());
  }
}
