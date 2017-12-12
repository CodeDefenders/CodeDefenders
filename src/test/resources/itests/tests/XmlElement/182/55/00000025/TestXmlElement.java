/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		// test here!
		java.util.Hashtable<String, String> hash = new java.util.Hashtable<String, String>(10);
		hash.put("a","d");
		XmlElement data = new XmlElement("kill",hash);
		assertEquals(hash,data.getAttributes());

	}
}