/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		assertEquals(e.addAttribute(null, ""), null);
		assertEquals(e.addAttribute("", null), null);
	}
}