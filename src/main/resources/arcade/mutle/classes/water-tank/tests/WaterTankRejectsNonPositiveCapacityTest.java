import org.junit.Test;

public class WaterTankRejectsNonPositiveCapacityTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNonPositiveCapacity() {
    new WaterTank(0);
  }
}
