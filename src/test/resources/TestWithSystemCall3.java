import org.junit.*;
import static org.junit.Assert.*;

public class TestWithSystemCall3 {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		System.currentTimeMillis();
	}
}