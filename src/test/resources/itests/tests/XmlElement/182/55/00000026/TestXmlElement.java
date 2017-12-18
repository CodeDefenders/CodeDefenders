/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		// test here!
		XmlElement data = new XmlElement();
		XmlElement data1 = new XmlElement();
		data1.addAttribute("kill", "alive");
		data.addElement(data1);
		assertEquals(data.getElements().size(), 1);
		data1.removeFromParent();
		assertEquals(data.getElements().size(), 0);
	}
}