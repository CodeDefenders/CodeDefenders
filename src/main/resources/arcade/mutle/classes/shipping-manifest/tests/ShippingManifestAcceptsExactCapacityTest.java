import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShippingManifestAcceptsExactCapacityTest {
  @Test
  public void acceptsExactCapacity() {
    assertTrue(new ShippingManifest(5).addPackage("box", 5));
  }
}
