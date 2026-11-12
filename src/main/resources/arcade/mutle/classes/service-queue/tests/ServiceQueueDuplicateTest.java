import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceQueueDuplicateTest {
  @Test
  public void rejectsDuplicateAwayFromQueueFront() {
    ServiceQueue queue = new ServiceQueue(3);
    assertTrue(queue.add("wash"));
    assertTrue(queue.add("dry"));
    assertFalse(queue.add("dry"));
  }
}
