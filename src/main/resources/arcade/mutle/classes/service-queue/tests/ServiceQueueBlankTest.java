import org.junit.Test;

public class ServiceQueueBlankTest {
  @Test(expected = IllegalArgumentException.class)
  public void blankRequestIsInvalid() {
    new ServiceQueue(1).add(" ");
  }
}
