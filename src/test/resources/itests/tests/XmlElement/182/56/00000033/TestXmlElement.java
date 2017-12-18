/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		e.addSubElement("one.two");
		XmlElement se = new XmlElement("one");
		se.addSubElement("two");
		assertEquals(e.getElements().get(0), se);
	}
}