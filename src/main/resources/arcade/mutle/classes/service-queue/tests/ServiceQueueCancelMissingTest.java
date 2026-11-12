import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ServiceQueueCancelMissingTest {
  @Test
  public void missingCancellationReportsFalse() {
    assertFalse(new ServiceQueue(1).cancel("wash"));
  }
}
