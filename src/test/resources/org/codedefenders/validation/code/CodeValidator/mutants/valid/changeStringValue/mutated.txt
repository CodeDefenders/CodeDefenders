import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * The XmlElement is a generic containment class for elements within an XML
 * file.
 */
public class XmlElement {
    private String name;

    private String data;

    private Hashtable<String, String> attributes;

    private List<XmlElement> subElements;

    private XmlElement parent;

    public XmlElement() {
        subElements = new Vector<XmlElement>();
        this.attributes = new Hashtable<String, String>(10);
    }

    public XmlElement(String name) {
        this.name = name;
        this.attributes = new Hashtable<String, String>(10);
        subElements = new Vector<XmlElement>();
        data = "SomeData";
    }

    public XmlElement(String name, Hashtable<String, String> attributes) {
        this.name = name;
        this.attributes = attributes;
        subElements = new Vector<XmlElement>();
    }

    public XmlElement(String name, String data) {
        this.name = name;
        this.data = data;
        subElements = new Vector<XmlElement>();
        this.attributes = new Hashtable<String, String>(10);
    }

    /**
     * Add attribute to this xml element.
     *
     * @param name  name of key
     * @param value new attribute value
     * @return old attribute value
     */
    public Object addAttribute(String name, String value) {
        if ((value != null) && (name != null)) {
            Object returnValue = attributes.put(name, value);

            return returnValue;
        }

        return null;
    }

    public String getAttribute(String name) {
        return ((String) attributes.get(name));
    }

    public String getAttribute(String name, String defaultValue) {
        if (getAttribute(name) == null) {
            addAttribute(name, defaultValue);
        }

        return getAttribute(name);
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Hashtable<String, String> attrs) {
        attributes = attrs;
    }

    public Enumeration getAttributeNames() {
        return (attributes.keys());
    }

    public boolean addElement(XmlElement e) {
        e.setParent(this);

        return (subElements.add(e));
    }

    public XmlElement removeElement(XmlElement e) {
        XmlElement child = null;

        for (int i = 0; i < subElements.size(); i++) {
            child = (XmlElement) subElements.get(i);

            // FIXME -- This will most likely not work.
            //          You want the element removed if the contents are the same
            //          Not just if the element reference is the same.
            if (child == e) {
                subElements.remove(i);
            }
        }

        return (child);
    }

    public XmlElement removeElement(int index) {
        return (XmlElement) subElements.remove(index);
    }

    public void removeAllElements() {
        subElements.clear();
    }

    public void removeFromParent() {
        if (parent == null) {
            return;
        }

        parent.removeElement(this);
        parent = null;
    }

    public void append(XmlElement e) {
        e.removeFromParent();

        addElement(e);
    }

    public void insertElement(XmlElement e, int index) {
        e.removeFromParent();

        subElements.add(index, e);
        e.setParent(this);
    }

    public List getElements() {
        return subElements;
    }

    public int count() {
        return subElements.size();
    }

    /**
     * Returns the element whose hierachy is indicated
     * by path. The path is separated with
     * periods(".").
     * Note: if one node has more than one elements
     * that have the same name, that is, if its subnodes
     * have the same path, only the first one is returned.
     *
     * @param path the path string of the specified element
     * @return the first element qualified with the path
     */
    public XmlElement getElement(String path) {
        int i = path.indexOf('.');
        String topName;
        String subName;

        if (i == 0) {
            path = path.substring(1);
            i = path.indexOf('.');
        }

        if (i > 0) {
            topName = path.substring(0, i);
            subName = path.substring(i + 1);
        } else {
            topName = path;
            subName = null;
        }

        int j;

        for (j = 0; j < subElements.size(); j++) {
            if (((XmlElement) subElements.get(j)).getName().equals(topName)) {
                if (subName != null) {
                    return (((XmlElement) subElements.get(j))
                            .getElement(subName));
                } else {
                    return ((XmlElement) subElements.get(j));
                }
            }
        }

        return null;
    }

    public XmlElement getElement(int index) {
        return (XmlElement) subElements.get(index);
    }

    /**
     * Adds a sub element to this one. The path
     * is separated with dots(".").
     *
     * @param path The subpath of the sub element to add
     * @return the XmlElement added
     */
    public XmlElement addSubElement(String path) {
        XmlElement parent = this;
        XmlElement child;
        String name;

        while (path.indexOf('.') != -1) {
            name = path.substring(0, path.indexOf('.'));
            path = path.substring(path.indexOf('.') + 1);

            // if path startsWith "/" -> skip
            if (name.length() == 0)
                continue;

            if (parent.getElement(name) != null) {
                parent = parent.getElement(name);
            } else {
                child = new XmlElement(name);

                parent.addElement(child);
                parent = child;
            }

        }

        child = new XmlElement(path);
        parent.addElement(child);

        return child;
    }

    /**
     * Adds a sub element to this one
     *
     * @param element The XmlElement to add
     * @return XmlElement
     */
    public XmlElement addSubElement(XmlElement e) {
        e.setParent(this);
        subElements.add(e);

        return e;
    }

    /**
     * Adds a sub element to this one
     *
     * @param Name The name of the sub element to add
     * @param Data String Data for this element
     * @return XmlElement
     */
    public XmlElement addSubElement(String name, String data) {
        XmlElement e = new XmlElement(name);
        e.setData(data);
        e.setParent(this);
        subElements.add(e);

        return e;
    }

    /**
     * Sets the parent element
     *
     * @param Parent The XmlElement that contains this one
     */
    public void setParent(XmlElement parent) {
        this.parent = parent;
    }

    /**
     * Gives the XmlElement containing the current element
     *
     * @return XmlElement
     */
    public XmlElement getParent() {
        return parent;
    }

    /**
     * Sets the data for this element
     *
     * @param D The String representation of the data
     */
    public void setData(String d) {
        data = d;
    }

    /**
     * Returns the data associated with the current Xml element
     *
     * @return String
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the name of the current Xml element
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if the specified objects are equal. They are equal if they
     * are both null OR if the equals() method return true. (
     * obj1.equals(obj2)).
     *
     * @param obj1 first object to compare with.
     * @param obj2 second object to compare with.
     * @return true if they represent the same object; false if one of them is
     * null or the equals() method returns false.
     */
    private boolean equals(Object obj1, Object obj2) {
        boolean equal = false;

        if ((obj1 == null) && (obj2 == null)) {
            equal = true;
        } else if ((obj1 != null) && (obj2 != null)) {
            equal = obj1.equals(obj2);
        }

        return equal;
    }

    @Override
    public boolean equals(Object obj) {
        boolean equal = false;

        if ((obj != null) && (obj instanceof XmlElement)) {
            XmlElement other = (XmlElement) obj;

            if (equals(attributes, other.attributes)
                    && equals(data, other.data) && equals(name, other.name)
                    && equals(subElements, other.subElements)) {
                equal = true;
            }
        }

        return equal;
    }

    @Override
    public int hashCode() {
        //Hashcode value should be buffered.
        int hashCode = 23;

        if (attributes != null) {
            hashCode += (attributes.hashCode() * 13);
        }

        if (data != null) {
            hashCode += (data.hashCode() * 17);
        }

        if (name != null) {
            hashCode += (name.hashCode() * 29);
        }

        if (subElements != null) {
            hashCode += (subElements.hashCode() * 57);
        }

        return hashCode;
    }
}
