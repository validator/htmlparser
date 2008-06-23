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

package nu.validator.htmlparser.gwt;

import nu.validator.htmlparser.common.DocumentMode;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.impl.HtmlAttributes;
import nu.validator.htmlparser.impl.TreeBuilder;

import org.xml.sax.SAXException;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

class BrowserTreeBuilder extends TreeBuilder<JavaScriptObject> {

    private JavaScriptObject document;
    
    private JavaScriptObject script;
    
    private JavaScriptObject placeholder;

    protected BrowserTreeBuilder(JavaScriptObject document) {
        super(XmlViolationPolicy.ALLOW, true);
        this.document = document;
    }

    private static native boolean hasAttributeNS(JavaScriptObject element, String uri, String localName) /*-{
        return element.hasAttributeNS(uri, localName); 
    }-*/;

    private static native void setAttributeNS(JavaScriptObject element, String uri, String localName, String value) /*-{
        element.setAttributeNS(uri, localName, value); 
    }-*/;
    
    
    @Override
    protected void addAttributesToElement(JavaScriptObject element, HtmlAttributes attributes)
            throws SAXException {
        try {
            for (int i = 0; i < attributes.getLength(); i++) {
                String localName = attributes.getLocalName(i);
                String uri = attributes.getURI(i);
                if (!hasAttributeNS(element, uri, localName)) {
                    setAttributeNS(element, uri, localName,
                            attributes.getValue(i));
                }
            }
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    private static native void appendChild(JavaScriptObject parent, JavaScriptObject child) /*-{
        parent.appendChild(child); 
    }-*/;

    private static native JavaScriptObject createTextNode(JavaScriptObject doc, String text) /*-{
        return doc.createTextNode(text); 
    }-*/;
    
    @Override
    protected void appendCharacters(JavaScriptObject parent, char[] buf, int start,
            int length) throws SAXException {
        try {
            if (parent == placeholder) {
                appendChild(script, createTextNode(document, new String(buf, start,
                        length)));
                
            } else {
                appendChild(parent, createTextNode(document, new String(buf, start,
                    length)));
            }
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    private static native boolean hasChildNodes(JavaScriptObject element) /*-{
        return element.hasChildNodes(); 
    }-*/;
    
    private static native JavaScriptObject getFirstChild(JavaScriptObject element) /*-{
        return element.firstChild; 
    }-*/;
    
    @Override
    protected void appendChildrenToNewParent(JavaScriptObject oldParent,
            JavaScriptObject newParent) throws SAXException {
        try {
            while (hasChildNodes(oldParent)) {
                appendChild(newParent, getFirstChild(oldParent));
            }
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    private static native JavaScriptObject createComment(JavaScriptObject doc, String text) /*-{
        return doc.createComment(text); 
    }-*/;
    
    @Override
    protected void appendComment(JavaScriptObject parent, char[] buf, int start,
            int length) throws SAXException {
        try {
            appendChild(parent, createComment(document, new String(buf, start,
                    length)));
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    @Override
    protected void appendCommentToDocument(char[] buf, int start, int length)
            throws SAXException {
        try {
            appendChild(document, createComment(document, new String(buf, start,
                    length)));
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    private static native JavaScriptObject createElementNS(JavaScriptObject doc, String ns, String local) /*-{
        return doc.createElementNS(ns, local); 
    }-*/;
    
    @Override
    protected JavaScriptObject createElement(String ns, String name, HtmlAttributes attributes)
            throws SAXException {
        try {
            JavaScriptObject rv = createElementNS(document,
                    ns, name);
            for (int i = 0; i < attributes.getLength(); i++) {
                setAttributeNS(rv, attributes.getURI(i),
                        attributes.getLocalName(i), attributes.getValue(i));
            }
            
            if ("script" == name) {
                script = rv;
                placeholder = createElementNS(document, "http://n.validator.nu/placeholder/", "__placeholder__");
                rv = placeholder;
            }
            
            return rv;
        } catch (JavaScriptException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    protected JavaScriptObject createHtmlElementSetAsRoot(HtmlAttributes attributes)
            throws SAXException {
        try {
            JavaScriptObject rv = createElementNS(document,
                    "http://www.w3.org/1999/xhtml", "html");
            for (int i = 0; i < attributes.getLength(); i++) {
                setAttributeNS(rv, attributes.getURI(i),
                        attributes.getLocalName(i), attributes.getValue(i));
            }
            appendChild(document, rv);
            return rv;
        } catch (JavaScriptException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    private static native JavaScriptObject getParentNode(JavaScriptObject element) /*-{
        return element.parentNode; 
    }-*/;
    
    private static native void removeChild(JavaScriptObject parent, JavaScriptObject child) /*-{
        parent.removeChild(child); 
    }-*/;
    
    
    @Override
    protected void detachFromParent(JavaScriptObject element) throws SAXException {
        try {
            JavaScriptObject parent = getParentNode(element);
            if (parent != null) {
                removeChild(parent, element);
            }
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(JavaScriptObject child,
            JavaScriptObject newParent) throws SAXException {
        try {
            appendChild(newParent, child);
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    @Override
    protected boolean hasChildren(JavaScriptObject element) throws SAXException {
        try {
            return hasChildNodes(element);
        } catch (JavaScriptException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    private static native void insertBeforeNative(JavaScriptObject parent, JavaScriptObject child, JavaScriptObject sibling) /*-{
        parent.insertBefore(child, sibling);
    }-*/;
    
    @Override
    protected void insertBefore(JavaScriptObject child, JavaScriptObject sibling, JavaScriptObject parent)
            throws SAXException {
        try {
            insertBeforeNative(parent, child, sibling);
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    @Override
    protected void insertCharactersBefore(char[] buf, int start, int length,
            JavaScriptObject sibling, JavaScriptObject parent) throws SAXException {
        try {
            insertBeforeNative(parent, createTextNode(document, new String(buf, start, length)), sibling);
        } catch (JavaScriptException e) {
            fatal(e);
        }
    }

    private static native int getNodeType(JavaScriptObject node) /*-{
        return node.nodeType;
    }-*/;
    
    @Override
    protected JavaScriptObject parentElementFor(JavaScriptObject child) throws SAXException {
        try {
            JavaScriptObject parent = getParentNode(child);
            if (parent != null && getNodeType(parent) == 1 /* ELEMENT_NODE */) {
                return parent;
            } else {
                return null;
            }
        } catch (JavaScriptException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    private static native JavaScriptObject cloneNode(JavaScriptObject node) /*-{
        return node.cloneNode(false);
    }-*/;
    
    @Override
    protected JavaScriptObject shallowClone(JavaScriptObject element) throws SAXException {
        try {
            return cloneNode(element);
        } catch (JavaScriptException e) {
            fatal(e);
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * Returns the document.
     * 
     * @return the document
     */
    JavaScriptObject getDocument() {
        JavaScriptObject rv = document;
        document = null;
        return rv;
    }

    private static native JavaScriptObject createDocumentFragment(JavaScriptObject doc) /*-{
        return doc.createDocumentFragment(); 
    }-*/;

    JavaScriptObject getDocumentFragment() {
        JavaScriptObject rv = createDocumentFragment(document);
        JavaScriptObject rootElt = getFirstChild(document);
        while (hasChildNodes(rootElt)) {
            appendChild(rv, getFirstChild(rootElt));
        }
        document = null;
        return rv;
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#createJavaScriptObject(String, java.lang.String, org.xml.sax.Attributes, java.lang.Object)
     */
    @Override
    protected JavaScriptObject createElement(String ns, String name,
            HtmlAttributes attributes, JavaScriptObject form) throws SAXException {
        try {
            JavaScriptObject rv = createElement(ns, name, attributes);
//            rv.setUserData("nu.validator.form-pointer", form, null);
            return rv;
        } catch (JavaScriptException e) {
            fatal(e);
            return null;
        }
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#start()
     */
    @Override
    protected void start(boolean fragment) throws SAXException {
        script = null;
        placeholder = null;
    }

    protected void documentMode(DocumentMode mode, String publicIdentifier, String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) throws SAXException {
//        document.setUserData("nu.validator.document-mode", mode, null);
    }

    /**
     * @see nu.validator.htmlparser.impl.TreeBuilder#elementPopped(java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override protected void elementPopped(String ns, String name,
            JavaScriptObject node) throws SAXException {
        if (node == placeholder) {
            requestSuspension();
        }
    }
    
    private static native void replace(JavaScriptObject oldNode, JavaScriptObject newNode) /*-{
        oldNode.parentNode.replaceChild(newNode, oldNode);
    }-*/;
    
    void maybeRunScript() {
        if (script != null) {
            replace(placeholder, script);
            script = null;
            placeholder = null;
        }
    }
}
