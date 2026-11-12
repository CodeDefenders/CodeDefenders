import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TokenBucketRefillTest {
  @Test
  public void addsPositiveTokens() {
    TokenBucket bucket = new TokenBucket(10);
    assertTrue(bucket.refill(3));
    assertEquals(3, bucket.tokens());
  }
}
