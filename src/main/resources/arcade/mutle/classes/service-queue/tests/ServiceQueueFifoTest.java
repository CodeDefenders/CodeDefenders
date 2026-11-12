import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceQueueFifoTest {
  @Test
  public void servesRequestsInInsertionOrder() {
    ServiceQueue queue = new ServiceQueue(2);
    queue.add("wash");
    queue.add("dry");
    assertEquals("wash", queue.serveNext());
  }
}
