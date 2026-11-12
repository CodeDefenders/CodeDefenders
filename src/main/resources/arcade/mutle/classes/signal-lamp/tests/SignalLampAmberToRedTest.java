import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SignalLampAmberToRedTest {
  @Test
  public void amberAdvancesToRed() {
    SignalLamp lamp = new SignalLamp("amber");
    assertTrue(lamp.advance());
    assertEquals("red", lamp.colour());
  }
}
