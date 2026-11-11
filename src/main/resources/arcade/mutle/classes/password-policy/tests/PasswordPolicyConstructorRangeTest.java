import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class PasswordPolicyConstructorRangeTest {
  @Test
  public void constructorRejectsInvalidLength() {
    assertThrows(IllegalArgumentException.class, () -> new PasswordPolicy(3));
    assertThrows(IllegalArgumentException.class, () -> new PasswordPolicy(31));
  }
}
