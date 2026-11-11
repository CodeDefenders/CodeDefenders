import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShippingManifestRemovalFreesCapacityTest {
  @Test
  public void removalFreesCapacity() {
    ShippingManifest manifest = new ShippingManifest(10);
    assertTrue(manifest.addPackage("first", 4));
    assertTrue(manifest.removePackage("first"));
    assertTrue(manifest.addPackage("second", 6));
  }
}
