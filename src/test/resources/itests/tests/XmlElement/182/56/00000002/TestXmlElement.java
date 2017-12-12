/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		e.addAttribute("abc", "cba");
		assertEquals(e.getAttribute("abc"), "cba");
		assertEquals(e.getAttribute("notThere", "deff"), "deff");
	}
}