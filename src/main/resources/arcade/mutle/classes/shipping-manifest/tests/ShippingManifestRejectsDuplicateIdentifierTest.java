import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShippingManifestRejectsDuplicateIdentifierTest {
  @Test
  public void rejectsDuplicateIdentifier() {
    ShippingManifest manifest = new ShippingManifest(10);
    assertTrue(manifest.addPackage("box", 2));
    assertFalse(manifest.addPackage("box", 2));
  }
}
