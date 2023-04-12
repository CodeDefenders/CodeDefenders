public class XmlElementTest {
    public static void main(String[] args) {
        XmlElement a = new XmlElement("a");
        XmlElement b = new XmlElement("b");
        XmlElement c = new XmlElement("c");

        b.addElement(c);
        a.addElement(b);

        a.getElement("b");
        a.addSubElement("b.c.d");

        a.equals(null);
        a.hashCode();
    }
}
