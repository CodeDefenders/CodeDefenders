/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement tester = new XmlElement("TestName", "TestData");
		XmlElement subElement = new XmlElement("subElementName", "subElementData");
		//tester.addSubElement(subElement);
		assertEquals(0, tester.count());
	}
}