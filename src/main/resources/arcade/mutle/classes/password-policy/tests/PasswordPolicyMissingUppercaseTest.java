import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PasswordPolicyMissingUppercaseTest {
  @Test
  public void passwordWithoutUppercaseIsRejected() {
    assertFalse(new PasswordPolicy(8).accepts("secure1a"));
  }
}
