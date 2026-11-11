import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestReportsMissingPackageTest {
  @Test
  public void reportsMissingPackage() {
    assertFalse(new ShippingManifest(10).containsPackage("box"));
  }
}
