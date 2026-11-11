import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TokenBucketNonpositiveTest {
  @Test
  public void rejectsNonpositiveAmounts() {
    TokenBucket bucket = new TokenBucket(10);
    assertFalse(bucket.refill(0));
    assertFalse(bucket.spend(0));
  }
}
