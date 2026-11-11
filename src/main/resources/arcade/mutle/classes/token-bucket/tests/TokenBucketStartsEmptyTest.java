import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TokenBucketStartsEmptyTest {
  @Test
  public void startsWithoutTokens() {
    TokenBucket bucket = new TokenBucket(10);
    assertTrue(bucket.empty());
    assertEquals(0, bucket.tokens());
  }
}
