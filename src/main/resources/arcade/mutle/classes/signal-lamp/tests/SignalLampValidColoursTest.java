import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SignalLampValidColoursTest {
  @Test
  public void retainsKnownInitialColour() {
    assertEquals("amber", new SignalLamp("amber").colour());
  }
}
