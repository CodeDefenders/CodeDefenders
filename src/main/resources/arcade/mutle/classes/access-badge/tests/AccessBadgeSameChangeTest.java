import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AccessBadgeSameChangeTest {
  @Test
  public void unchangedClearanceReportsNoChange() {
    assertFalse(new AccessBadge(2).changeClearance(2));
  }
}
