import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PasswordPolicyMinimumLengthTest {
  @Test
  public void minimumLengthIsRetained() {
    assertEquals(8, new PasswordPolicy(8).minimumLength());
  }
}
