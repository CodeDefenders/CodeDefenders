/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestLift {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		Lift e = new Lift(4, 2);
		e.getCurrentFloor();
		assertEquals(4, e.getTopFloor() );
	}
}