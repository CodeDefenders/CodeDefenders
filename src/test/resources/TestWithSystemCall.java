import org.junit.*;
import static org.junit.Assert.*;

public class TestWithSystemCall {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		System.out.println("Hello world");
	}
}