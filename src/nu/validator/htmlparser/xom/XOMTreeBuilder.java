/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2008 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.htmlparser.xom;

import nu.validator.htmlparser.common.DocumentMode;
import nu.validator.htmlparser.impl.CoalescingTreeBuilder;
import nu.validator.htmlparser.impl.HtmlAttributes;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.XMLException;

import org.xml.sax.SAXException;

class XOMTreeBuilder extends CoalescingTreeBuilder<Element> {

    private final SimpleNodeFactory nodeFactory;

    private Document document;

    protected XOMTreeBuilder(SimpleNodeFactory nodeFactory) {
        super();
        this.nodeFactory = nodeFactory;
    }

    @Override
    protected void addAttributesToElement(Element element, HtmlAttributes attributes)
            throws SAXException {
        try {
            for (int i = 0; i < attributes.getLength(); i++) {
                String localName = attributes.getLocalName(i);
                String uri = attributes.getURI(i);
                if (element.getAttribute(localName, uri) == null) {
                    element.addAttribute(nodeFactory.makeAttribute(localName,
                            uri, attributes.getValue(i),
                            attributes.getType(i) == "ID" ? Attribute.Type.ID
                                    : Attribute.Type.CDATA));
                }
            }
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendCharacters(Element parent, String text) throws SAXException {
        try {
            parent.appendChild(nodeFactory.makeText(text));
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendChildrenToNewParent(Element oldParent,
            Element newParent) throws SAXException {
        try {
            Nodes children = oldParent.removeChildren();
            for (int i = 0; i < children.size(); i++) {
                newParent.appendChild(children.get(i));
            }
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendComment(Element parent, String comment) throws SAXException {
        try {
            parent.appendChild(nodeFactory.makeComment(comment));
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendCommentToDocument(String comment)
            throws SAXException {
        try {
            Element root = document.getRootElement();
            if ("http://www.xom.nu/fakeRoot".equals(root.getNamespaceURI())) {
                document.insertChild(nodeFactory.makeComment(comment), document.indexOf(root));
            } else {
                document.appendChild(nodeFactory.makeComment(comment));
            }
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected Element createElement(String ns, String name, HtmlAttributes attributes)
            throws SAXException {
        try {
            Element rv = nodeFactory.makeElement(name,
                    ns);
            for (int i = 0; i < attributes.getLength(); i++) {
                rv.addAttribute(nodeFactory.makeAttribute(
                        attributes.getLocalName(i), attributes.getURI(i),
                        attributes.getValue(i),
                        attributes.getType(i) == "ID" ? Attribute.Type.ID
                                : Attribute.Type.CDATA));
            }
            return rv;
        } catch (XMLException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected Element createHtmlElementSetAsRoot(HtmlAttributes attributes)
            throws SAXException {
        try {
            Element rv = nodeFactory.makeElement("html",
                    "http://www.w3.org/1999/xhtml");
            for (int i = 0; i < attributes.getLength(); i++) {
                rv.addAttribute(nodeFactory.makeAttribute(
                        attributes.getLocalName(i), attributes.getURI(i),
                        attributes.getValue(i),
                        attributes.getType(i) == "ID" ? Attribute.Type.ID
                                : Attribute.Type.CDATA));
            }
            document.setRootElement(rv);
            return rv;
        } catch (XMLException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected void detachFromParent(Element element) throws SAXException {
        try {
            element.detach();
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(Element child,
            Element newParent) throws SAXException {
        try {
            child.detach();
            newParent.appendChild(child);
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected boolean hasChildren(Element element) throws SAXException {
        try {
            return element.getChildCount() != 0;
        } catch (XMLException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected void insertBefore(Element child, Element sibling, Element parent)
            throws SAXException {
        try {
            parent.insertChild(child, parent.indexOf(sibling));
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected void insertCharactersBefore(String text,
            Element sibling, Element parent) throws SAXException {
        try {
            parent.insertChild(nodeFactory.makeText(text), parent.indexOf(sibling));
        } catch (XMLException e) {
            fatal(e);
        }
    }

    @Override
    protected Element parentElementFor(Element child) throws SAXException {
        try {
            ParentNode parent = child.getParent();
            if (parent != null && parent instanceof Element) {
                return (Element) parent;
            } else {
                return null;
            }
        } catch (XMLException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected Element shallowClone(Element element) throws SAXException {
        try {
            Element rv = nodeFactory.makeElement(element.getLocalName(),
                    element.getNamespaceURI());
            for (int i = 0; i < element.getAttributeCount(); i++) {
                Attribute attribute = element.getAttribute(i);
                rv.addAttribute(nodeFactory.makeAttribute(
                        attribute.getLocalName(), attribute.getNamespaceURI(),
                        attribute.getValue(), attribute.getType()));
            }
            return rv;
        } catch (XMLException e) {
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

    Nodes getDocumentFragment() {
        Element rootElt = document.getRootElement();
        Nodes rv = rootElt.removeChildren();
        document = null;
        return rv;
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#createElement(String,
     *      java.lang.String, org.xml.sax.Attributes, java.lang.Object)
     */
    @Override
    protected Element createElement(String ns, String name,
            HtmlAttributes attributes, Element form) throws SAXException {
        try {
            Element rv = nodeFactory.makeElement(name,
                    ns, form);
            for (int i = 0; i < attributes.getLength(); i++) {
                rv.addAttribute(nodeFactory.makeAttribute(
                        attributes.getLocalName(i), attributes.getURI(i),
                        attributes.getValue(i),
                        attributes.getType(i) == "ID" ? Attribute.Type.ID
                                : Attribute.Type.CDATA));
            }
            return rv;
        } catch (XMLException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#start()
     */
    @Override
    protected void start(boolean fragment) throws SAXException {
        document = nodeFactory.makeDocument();
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#documentMode(nu.validator.htmlparser.common.DocumentMode,
     *      java.lang.String, java.lang.String, boolean)
     */
    @Override
    protected void documentMode(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks)
            throws SAXException {
        if (document instanceof Mode) {
            Mode modal = (Mode) document;
            modal.setMode(mode);
        }
    }
}
