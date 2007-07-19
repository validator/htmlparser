package nu.validator.htmlparser.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;

class DOMTreeBuilder extends TreeBuilder<Element> {

    private DOMImplementation implementation;

    private Document document;

    protected DOMTreeBuilder(DOMImplementation implementation) {
        super(XmlViolationPolicy.ALLOW, true);
        this.implementation = implementation;
    }

    @Override
    protected void addAttributesToElement(Element element, Attributes attributes)
            throws SAXException {
        try {
            for (int i = 0; i < attributes.getLength(); i++) {
                String localName = attributes.getLocalName(i);
                String uri = attributes.getURI(i);
                if (!element.hasAttributeNS(uri, localName)) {
                    element.setAttributeNS(uri, localName,
                            attributes.getValue(i));
                }
            }
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendCharacters(Element parent, char[] buf, int start,
            int length) throws SAXException {
        try {
            parent.appendChild(document.createTextNode(new String(buf, start,
                    length)));
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendChildrenToNewParent(Element oldParent,
            Element newParent) throws SAXException {
        try {
            while (oldParent.hasChildNodes()) {
                newParent.appendChild(oldParent.getFirstChild());
            }
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendComment(Element parent, char[] buf, int start,
            int length) throws SAXException {
        try {
            parent.appendChild(document.createComment(new String(buf, start,
                    length)));
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendCommentToDocument(char[] buf, int start, int length)
            throws SAXException {
        try {
            document.appendChild(document.createComment(new String(buf, start,
                    length)));
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected Element createElement(String name, Attributes attributes)
            throws SAXException {
        try {
            Element rv = document.createElementNS(
                    "http://www.w3.org/1999/xhtml", name);
            for (int i = 0; i < attributes.getLength(); i++) {
                rv.setAttributeNS(attributes.getURI(i),
                        attributes.getLocalName(i), attributes.getValue(i));
            }
            return rv;
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected Element createHtmlElementSetAsRoot(Attributes attributes)
            throws SAXException {
        try {
            Element rv = document.createElementNS(
                    "http://www.w3.org/1999/xhtml", "html");
            for (int i = 0; i < attributes.getLength(); i++) {
                rv.setAttributeNS(attributes.getURI(i),
                        attributes.getLocalName(i), attributes.getValue(i));
            }
            document.appendChild(rv);
            return rv;
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected void detachFromParent(Element element) throws SAXException {
        try {
            Node parent = element.getParentNode();
            if (parent != null) {
                parent.removeChild(element);
            }
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(Element child,
            Element newParent) throws SAXException {
        try {
            newParent.appendChild(child);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected boolean hasChildren(Element element) throws SAXException {
        try {
            return element.hasChildNodes();
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected void insertBefore(Element child, Element sibling, Element parent)
            throws SAXException {
        try {
            parent.insertBefore(child, sibling);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected void insertCharactersBefore(char[] buf, int start, int length,
            Element sibling, Element parent) throws SAXException {
        try {
            parent.insertBefore(document.createTextNode(new String(buf, start, length)), sibling);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    @Override
    protected Element parentElementFor(Element child) throws SAXException {
        try {
            Node parent = child.getParentNode();
            if (parent != null || parent.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) parent;
            } else {
                return null;
            }
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected Element shallowClone(Element element) throws SAXException {
        try {
            return (Element) element.cloneNode(false);
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * Returns the document.
     * 
     * @return the document
     */
    Document getDocument() {
        Document rv = document;
        document = null;
        return rv;
    }

    DocumentFragment getDocumentFragment() {
        DocumentFragment rv = document.createDocumentFragment();
        Node rootElt = document.getFirstChild();
        while (rootElt.hasChildNodes()) {
            rv.appendChild(rootElt.getFirstChild());
        }
        document = null;
        return rv;
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#createElement(java.lang.String, org.xml.sax.Attributes, java.lang.Object)
     */
    @Override
    protected Element createElement(String name, Attributes attributes,
            Element form) throws SAXException {
        try {
            Element rv = createElement(name, attributes);
            
            return rv;
        } catch (DOMException e) {
            fatal(e);
            return null;
        }
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#start()
     */
    @Override
    protected void start(boolean fragment) throws SAXException {
        document = implementation.createDocument(null, null, null);
    }
}
