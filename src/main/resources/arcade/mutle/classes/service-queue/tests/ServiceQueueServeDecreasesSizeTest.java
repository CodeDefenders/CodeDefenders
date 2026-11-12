import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceQueueServeDecreasesSizeTest {
  @Test
  public void servingDecreasesSize() {
    ServiceQueue queue = new ServiceQueue(1);
    queue.add("A");
    queue.serveNext();
    assertEquals(0, queue.size());
  }
}
