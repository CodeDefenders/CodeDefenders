import org.junit.*;
import static org.junit.Assert.*;

public class TestWithTooManyAssertions {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		assertTrue(true);
		assertFalse(false);
		assertEquals(1,1);
	}
}
