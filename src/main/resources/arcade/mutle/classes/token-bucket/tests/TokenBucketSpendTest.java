import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TokenBucketSpendTest {
  @Test
  public void spendsAvailableTokens() {
    TokenBucket bucket = new TokenBucket(10);
    bucket.refill(5);
    assertTrue(bucket.spend(2));
    assertEquals(3, bucket.tokens());
  }
}
