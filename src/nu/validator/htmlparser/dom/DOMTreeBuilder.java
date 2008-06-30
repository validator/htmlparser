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

package nu.validator.htmlparser.dom;

import nu.validator.htmlparser.common.DocumentMode;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.impl.HtmlAttributes;
import nu.validator.htmlparser.impl.TreeBuilder;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The tree builder glue for building a tree through the public DOM APIs.
 * 
 * @version $Id$
 * @author hsivonen
 */
class DOMTreeBuilder extends TreeBuilder<Element> {

    /**
     * The DOM impl.
     */
    private DOMImplementation implementation;

    /**
     * The current doc.
     */
    private Document document;

    /**
     * The constructor.
     * @param implementation the DOM impl.
     */
    protected DOMTreeBuilder(DOMImplementation implementation) {
        super(XmlViolationPolicy.ALLOW, true);
        this.implementation = implementation;
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#addAttributesToElement(java.lang.Object, nu.validator.htmlparser.impl.HtmlAttributes)
     */
    @Override
    protected void addAttributesToElement(Element element, HtmlAttributes attributes)
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#appendCharacters(java.lang.Object, char[], int, int)
     */
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#appendChildrenToNewParent(java.lang.Object, java.lang.Object)
     */
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#appendComment(java.lang.Object, char[], int, int)
     */
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#appendCommentToDocument(char[], int, int)
     */
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#createElement(java.lang.String, java.lang.String, nu.validator.htmlparser.impl.HtmlAttributes)
     */
    @Override
    protected Element createElement(String ns, String name, HtmlAttributes attributes)
            throws SAXException {
        try {
            Element rv = document.createElementNS(
                    ns, name);
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#createHtmlElementSetAsRoot(nu.validator.htmlparser.impl.HtmlAttributes)
     */
    @Override
    protected Element createHtmlElementSetAsRoot(HtmlAttributes attributes)
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#detachFromParent(java.lang.Object)
     */
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

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#detachFromParentAndAppendToNewParent(java.lang.Object, java.lang.Object)
     */
    @Override
    protected void detachFromParentAndAppendToNewParent(Element child,
            Element newParent) throws SAXException {
        try {
            newParent.appendChild(child);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#hasChildren(java.lang.Object)
     */
    @Override
    protected boolean hasChildren(Element element) throws SAXException {
        try {
            return element.hasChildNodes();
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#insertBefore(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    protected void insertBefore(Element child, Element sibling, Element parent)
            throws SAXException {
        try {
            parent.insertBefore(child, sibling);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#insertCharactersBefore(char[], int, int, java.lang.Object, java.lang.Object)
     */
    @Override
    protected void insertCharactersBefore(char[] buf, int start, int length,
            Element sibling, Element parent) throws SAXException {
        try {
            parent.insertBefore(document.createTextNode(new String(buf, start, length)), sibling);
        } catch (DOMException e) {
            fatal(e);
        }
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#parentElementFor(java.lang.Object)
     */
    @Override
    protected Element parentElementFor(Element child) throws SAXException {
        try {
            Node parent = child.getParentNode();
            if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) parent;
            } else {
                return null;
            }
        } catch (DOMException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#shallowClone(java.lang.Object)
     */
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
     * @see nu.validator.htmlparser.impl.TreeBuilder#createElement(String, java.lang.String, org.xml.sax.Attributes, java.lang.Object)
     */
    @Override
    protected Element createElement(String ns, String name,
            HtmlAttributes attributes, Element form) throws SAXException {
        try {
            Element rv = createElement(ns, name, attributes);
            rv.setUserData("nu.validator.form-pointer", form, null);
            return rv;
        } catch (DOMException e) {
            fatal(e);
            return null;
        }
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#start()
     */
    @Override
    protected void start(boolean fragment) throws SAXException {
        document = implementation.createDocument(null, null, null);
    }

    /**
     * 
     * @see nu.validator.htmlparser.impl.TreeBuilder#documentMode(nu.validator.htmlparser.common.DocumentMode, java.lang.String, java.lang.String, boolean)
     */
    protected void documentMode(DocumentMode mode, String publicIdentifier, String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) throws SAXException {
        document.setUserData("nu.validator.document-mode", mode, null);
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

    /**
     * Return the document fragment.
     * 
     * @return the document fragment
     */
    DocumentFragment getDocumentFragment() {
        DocumentFragment rv = document.createDocumentFragment();
        Node rootElt = document.getFirstChild();
        while (rootElt.hasChildNodes()) {
            rv.appendChild(rootElt.getFirstChild());
        }
        document = null;
        return rv;
    }
}
