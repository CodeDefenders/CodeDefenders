/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		XmlElement se = new XmlElement();
		se.addAttribute("everything", "42");
		
		e.addElement(se);
		e.addElement(new XmlElement());
		assertEquals(e.getElement(0), se);
	}
}