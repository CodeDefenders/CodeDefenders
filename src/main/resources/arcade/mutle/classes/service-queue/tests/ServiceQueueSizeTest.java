import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceQueueSizeTest {
  @Test
  public void sizeTracksCurrentRequests() {
    ServiceQueue queue = new ServiceQueue(3);
    queue.add("wash");
    assertEquals(1, queue.size());
  }
}
