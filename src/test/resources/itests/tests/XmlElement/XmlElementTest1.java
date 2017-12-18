public class XmlElementTest {

    @Test(timeout = 4000)
    public void test() throws Throwable {
        XmlElement x = new XmlElement('Test');
        assertNotNull(x.getData());
    }
}
