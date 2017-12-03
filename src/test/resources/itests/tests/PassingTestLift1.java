/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestLift {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		Lift lift = new Lift(2);
		assertEquals(2, lift.getTopFloor());
	}
}