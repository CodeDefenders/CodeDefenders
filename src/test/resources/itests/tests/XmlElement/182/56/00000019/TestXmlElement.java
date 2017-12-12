/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		assertEquals(e.hashCode(), 80);
		
		XmlElement se = new XmlElement();
		e.addElement(se);
		assertEquals(e.hashCode(), 741);
	}
}