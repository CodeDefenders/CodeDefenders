// @@ -17,7 +17,7 @@
//  
//      private List<XmlElement> subElements;
//  
// -    private XmlElement parent;
// +    private XmlElement parent = new XmlElement("parent");
//  
//      public XmlElement() {
//          subElements = new Vector<XmlElement>();
//
/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestXmlElement {
	@Test(timeout = 4000)
	public void test() throws Throwable {
		XmlElement el = new XmlElement("hello_world");
		assertNotNull(el.getParent());
	}
}