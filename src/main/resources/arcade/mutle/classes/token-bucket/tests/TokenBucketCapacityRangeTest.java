import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class TokenBucketCapacityRangeTest {
  @Test
  public void rejectsNonpositiveCapacity() {
    assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0));
  }
}
