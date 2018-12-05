import static org.junit.Assert.*;
import org.junit.*;

// Pay attention to the test convention: Test+Alias
public class TestClassWithPrivateInnerClass{

	@Test(timeout = 4000)
	public void test() throws Throwable {
		ClassWithPrivateInnerClass x = new ClassWithPrivateInnerClass();
		assertEquals(10, x.foo(10));
	}

}
