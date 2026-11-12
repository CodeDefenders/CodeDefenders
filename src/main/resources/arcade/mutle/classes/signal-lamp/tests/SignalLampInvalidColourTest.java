import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SignalLampInvalidColourTest {
  @Test
  public void rejectsUnknownColour() {
    assertThrows(IllegalArgumentException.class, () -> new SignalLamp("blue"));
  }
}
