import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShippingManifestContainsAddedPackageTest {
  @Test
  public void findsAddedPackage() {
    ShippingManifest manifest = new ShippingManifest(10);
    manifest.addPackage("box", 2);
    assertTrue(manifest.containsPackage("box"));
  }
}
