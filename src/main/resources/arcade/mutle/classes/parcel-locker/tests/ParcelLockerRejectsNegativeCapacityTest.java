import org.junit.Test;

public class ParcelLockerRejectsNegativeCapacityTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNegativeCapacity() {
    new ParcelLocker(-1);
  }
}
