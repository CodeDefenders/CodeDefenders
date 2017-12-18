/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		
		XmlElement tester = new XmlElement("TestName", "TestData");
		assertEquals("TestName", tester.getName());
	}
}