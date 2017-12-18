/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement e = new XmlElement();
		XmlElement se = new XmlElement();
		e.addElement(se);
		se.append(new XmlElement());
		
		assertNull(se.getParent());
	}
}