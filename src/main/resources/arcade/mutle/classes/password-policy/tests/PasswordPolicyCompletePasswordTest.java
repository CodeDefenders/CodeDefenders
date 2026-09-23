import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordPolicyCompletePasswordTest {
  @Test
  public void passwordWithBothRequirementsIsAccepted() {
    assertTrue(new PasswordPolicy(8).accepts("Secure1A"));
  }
}
