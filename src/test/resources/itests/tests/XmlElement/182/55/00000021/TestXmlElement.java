/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		// test here!
		XmlElement data = new XmlElement("a", "d");
		XmlElement data1 = new XmlElement("b", "c");
		data.addElement(data1);
		data1.removeFromParent();
		assertEquals(null,data1.getParent());
	}
}