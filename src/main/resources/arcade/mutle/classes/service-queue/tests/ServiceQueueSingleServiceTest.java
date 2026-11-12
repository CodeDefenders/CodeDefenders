import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceQueueSingleServiceTest {
  @Test
  public void servesQueuedRequest() {
    ServiceQueue queue = new ServiceQueue(1);
    queue.add("A");
    assertEquals("A", queue.serveNext());
  }
}
