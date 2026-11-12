import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceQueueCancelReportsSuccessTest {
  @Test
  public void cancellationReportsSuccess() {
    ServiceQueue queue = new ServiceQueue(1);
    queue.add("wash");
    assertTrue(queue.cancel("wash"));
  }
}
