import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestRejectsNullIdentifierTest {
  @Test
  public void rejectsNullIdentifier() {
    assertFalse(new ShippingManifest(10).addPackage(null, 1));
  }
}
