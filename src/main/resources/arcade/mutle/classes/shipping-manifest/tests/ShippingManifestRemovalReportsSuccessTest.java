import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShippingManifestRemovalReportsSuccessTest {
  @Test
  public void removalReportsSuccess() {
    ShippingManifest manifest = new ShippingManifest(10);
    manifest.addPackage("box", 2);
    assertTrue(manifest.removePackage("box"));
  }
}
