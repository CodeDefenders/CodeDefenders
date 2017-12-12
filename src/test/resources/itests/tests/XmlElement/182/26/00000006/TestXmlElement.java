/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement tester = new XmlElement("TestName", "TestData");
		XmlElement tester2 = new XmlElement("TestName2", "TestData2");
		assertFalse(tester.equals(tester2));
	}
}