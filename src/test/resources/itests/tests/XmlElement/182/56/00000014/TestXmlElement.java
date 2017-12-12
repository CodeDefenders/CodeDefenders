/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		e.addSubElement("nam", "dat");
		assertEquals(e.getElements().size(), 1);
		e.removeElement(0);
		assertEquals(e.getElements().size(), 0);
	}
}