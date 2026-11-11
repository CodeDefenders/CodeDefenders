import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TokenBucketOverspendTest {
  @Test
  public void rejectsMoreThanAvailable() {
    TokenBucket bucket = new TokenBucket(10);
    bucket.refill(2);
    assertFalse(bucket.spend(3));
    assertEquals(2, bucket.tokens());
  }
}
