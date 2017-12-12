/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement tester = new XmlElement("TestName", "TestData");
		XmlElement subElement = new XmlElement("subElementName", "subElementData");
		//XmlElement subElement2 = new XmlElement("subElementName2", "subElementData2");

		tester.insertElement(subElement, 1);
		assertTrue(subElement.equals(tester.getElement(1)));
	}
}