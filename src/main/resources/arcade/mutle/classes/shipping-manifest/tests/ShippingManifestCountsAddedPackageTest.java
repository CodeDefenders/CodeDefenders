import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShippingManifestCountsAddedPackageTest {
  @Test
  public void countsAddedPackage() {
    ShippingManifest manifest = new ShippingManifest(10);
    manifest.addPackage("box", 2);
    assertEquals(1, manifest.getPackageCount());
  }
}
