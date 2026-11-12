import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ServiceQueueEmptyTest {
  @Test
  public void servingEmptyQueueReturnsNull() {
    assertNull(new ServiceQueue(1).serveNext());
  }
}
