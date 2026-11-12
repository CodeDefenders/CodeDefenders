import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestRemoveMissingPackageTest {
  @Test
  public void cannotRemoveMissingPackage() {
    assertFalse(new ShippingManifest(10).removePackage("box"));
  }
}
