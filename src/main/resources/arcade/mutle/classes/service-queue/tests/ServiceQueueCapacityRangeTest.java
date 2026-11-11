import org.junit.Test;

public class ServiceQueueCapacityRangeTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsZeroCapacity() {
    new ServiceQueue(0);
  }
}
