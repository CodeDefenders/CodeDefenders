import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PasswordPolicyNullPasswordTest {
  @Test
  public void nullPasswordIsRejected() {
    assertFalse(new PasswordPolicy(8).accepts(null));
  }
}
