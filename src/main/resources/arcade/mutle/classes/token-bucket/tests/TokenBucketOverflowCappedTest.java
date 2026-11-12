import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TokenBucketOverflowCappedTest {
  @Test
  public void overflowStopsAtCapacity() {
    TokenBucket bucket = new TokenBucket(10);
    bucket.refill(7);
    assertTrue(bucket.refill(7));
    assertEquals(10, bucket.tokens());
    assertTrue(bucket.refill(Integer.MAX_VALUE));
    assertEquals(10, bucket.tokens());
  }
}
