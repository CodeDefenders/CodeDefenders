import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceQueueFullTest {
  @Test
  public void rejectsRequestAtCapacity() {
    ServiceQueue queue = new ServiceQueue(1);
    assertTrue(queue.add("wash"));
    assertTrue(queue.isFull());
    assertFalse(queue.add("dry"));
  }
}
