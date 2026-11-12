import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShippingManifestRemainingCapacityTest {
  @Test
  public void tracksRemainingCapacity() {
    ShippingManifest manifest = new ShippingManifest(10);
    manifest.addPackage("box", 2);
    assertEquals(8, manifest.getRemainingCapacity());
  }
}
