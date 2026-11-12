import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceQueueAddTest {
  @Test
  public void acceptsFirstRequest() {
    assertTrue(new ServiceQueue(2).add("wash"));
  }
}
