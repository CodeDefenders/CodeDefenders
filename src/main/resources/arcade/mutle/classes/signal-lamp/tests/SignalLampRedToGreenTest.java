import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SignalLampRedToGreenTest {
  @Test
  public void redAdvancesToGreen() {
    SignalLamp lamp = new SignalLamp("red");
    assertTrue(lamp.advance());
    assertEquals("green", lamp.colour());
  }
}
