import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TokenBucketExactRefillTest {
  @Test
  public void refillCanReachCapacity() {
    TokenBucket bucket = new TokenBucket(10);
    assertTrue(bucket.refill(10));
    assertEquals(10, bucket.tokens());
  }
}
