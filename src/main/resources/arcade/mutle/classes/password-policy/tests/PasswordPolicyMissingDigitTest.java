import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PasswordPolicyMissingDigitTest {
  @Test
  public void passwordWithoutDigitIsRejected() {
    assertFalse(new PasswordPolicy(8).accepts("SecureAA"));
  }
}
