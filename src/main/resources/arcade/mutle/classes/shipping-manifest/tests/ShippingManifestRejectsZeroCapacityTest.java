import org.junit.Test;

public class ShippingManifestRejectsZeroCapacityTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsZeroCapacity() {
    new ShippingManifest(0);
  }
}
