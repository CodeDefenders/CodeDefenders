/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		// test here!
		XmlElement kill= new XmlElement("mutant");
		java.util.Hashtable<String, String> hash = new java.util.Hashtable<String, String>(10);
		assertEquals(hash.put("a","d"),kill.addAttribute("a","d"));
		
	}
}