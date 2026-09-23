import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PasswordPolicyShortPasswordTest {
  @Test
  public void shortPasswordIsRejected() {
    assertFalse(new PasswordPolicy(8).accepts("Short1A"));
  }
}
