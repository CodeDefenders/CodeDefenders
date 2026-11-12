import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestRejectsEmptyIdentifierTest {
  @Test
  public void rejectsEmptyIdentifier() {
    assertFalse(new ShippingManifest(10).addPackage("", 1));
  }
}
