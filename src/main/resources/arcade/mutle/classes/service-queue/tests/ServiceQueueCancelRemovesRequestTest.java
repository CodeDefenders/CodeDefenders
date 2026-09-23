import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceQueueCancelRemovesRequestTest {
  @Test
  public void cancellationRemovesRequest() {
    ServiceQueue queue = new ServiceQueue(1);
    queue.add("wash");
    queue.cancel("wash");
    assertEquals(0, queue.size());
  }
}
