import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceQueueCancelTest {
  @Test
  public void cancellationRemovesQueuedRequest() {
    ServiceQueue queue = new ServiceQueue(2);
    queue.add("wash");
    assertTrue(queue.cancel("wash"));
    assertEquals(0, queue.size());
  }
}
