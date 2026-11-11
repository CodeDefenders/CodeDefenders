import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerReserveReportsSuccessTest {
  @Test
  public void reserveReportsSuccess() {
    assertTrue(new ParcelLocker(2).reserve(1));
  }
}
