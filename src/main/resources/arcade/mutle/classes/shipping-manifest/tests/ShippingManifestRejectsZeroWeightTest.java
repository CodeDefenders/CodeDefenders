import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShippingManifestRejectsZeroWeightTest {
  @Test
  public void rejectsZeroWeight() {
    assertFalse(new ShippingManifest(10).addPackage("box", 0));
  }
}
