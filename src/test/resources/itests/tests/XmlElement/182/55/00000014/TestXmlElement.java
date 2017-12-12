/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		// test here!
		XmlElement data = new XmlElement("kill");
		assertEquals(0,data.getAttributes().size());
	}
}