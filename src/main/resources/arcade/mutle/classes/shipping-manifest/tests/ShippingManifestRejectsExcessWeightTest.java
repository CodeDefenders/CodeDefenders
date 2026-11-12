import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestRejectsExcessWeightTest {
  @Test
  public void rejectsExcessWeight() {
    assertFalse(new ShippingManifest(5).addPackage("box", 6));
  }
}
