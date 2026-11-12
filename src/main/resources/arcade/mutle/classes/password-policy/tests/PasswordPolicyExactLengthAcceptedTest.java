import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordPolicyExactLengthAcceptedTest {
  @Test
  public void exactMinimumLengthIsAccepted() {
    assertTrue(new PasswordPolicy(8).accepts("Secure1A"));
  }
}
