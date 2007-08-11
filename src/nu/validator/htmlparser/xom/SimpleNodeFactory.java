package nu.validator.htmlparser.xom;

import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import nu.xom.Attribute.Type;

public class SimpleNodeFactory {

    public Attribute makeAttribute(String localName, String uri, String value, Type type) {
        return new Attribute(localName, uri, value, type);
    }

    public Text makeText(String string) {
        return new Text(string);
    }

    public Comment makeComment(String string) {
        return new Comment(string);
    }

    public Element makeElement(String name, String namespace) {
        return new Element(name, namespace);
    }

    public Element makeElement(String name, String namespace, Element form) {
        return new Element(name, namespace);
    }
    
    public Document makeDocument() {
        return new ModalDocument(new Element("root", "http://www.xom.nu/fakeRoot"));
    }
    
}
