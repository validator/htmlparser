/*
 * Copyright (c) 2007 Henri Sivonen
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

package nu.validator.htmlparser.tools;

import nu.validator.htmlparser.impl.NCName;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Quick and dirty hack to work around Xalan xmlns weirdness.
 * 
 * @version $Id: XmlnsDropper.java 254 2008-05-29 08:56:11Z hsivonen $
 * @author hsivonen
 */
class EventFixups implements ContentHandler {

    private final ContentHandler delegate;

    /**
     * @param delegate
     */
    public EventFixups(final ContentHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        delegate.characters(ch, start, length);
    }

    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        delegate.endDocument();
    }

    /**
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!NCName.isNCName(localName)) {
            localName = "__bogus__";
        }
        delegate.endElement(uri, localName, localName);
        if ((uri == "http://www.w3.org/2000/svg" && localName == "svg") || (uri == "http://www.w3.org/1998/Math/MathML" && localName == "math")) {
            delegate.startPrefixMapping("xlink", "http://www.w3.org/1999/xlink");
        }
    }

    /**
     * @param prefix
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        delegate.endPrefixMapping(prefix);
    }

    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        delegate.ignorableWhitespace(ch, start, length);
    }

    /**
     * @param target
     * @param data
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws SAXException {
        delegate.processingInstruction(target, data);
    }

    /**
     * @param locator
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        delegate.setDocumentLocator(locator);
    }

    /**
     * @param name
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name) throws SAXException {
        delegate.skippedEntity(name);
    }

    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        delegate.startDocument();
    }

    /**
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (!NCName.isNCName(localName)) {
            localName = "__bogus__";
        }
        AttributesImpl ai = new AttributesImpl();
        for (int i = 0; i < atts.getLength(); i++) {
            String u = atts.getURI(i);
            String t = atts.getType(i);
            String v = atts.getValue(i);
            String n = atts.getLocalName(i);
            if (!NCName.isNCName(n) || u == "http://www.w3.org/2000/xmlns/") {
                n = "__bogus_" + i + "__";
            }
            String q = n;
            if (u == "http://www.w3.org/XML/1998/namespace") {
                q = "xml:" + n;
            } else if (u == "http://www.w3.org/1999/xlink") {
                q = "xlink:" + n;
            }
            ai.addAttribute(u, n, q, t, v); 
        }
        if (uri == "http://www.w3.org/2000/svg" && localName == "svg") {
            delegate.startPrefixMapping("xlink", "http://www.w3.org/1999/xlink");
        }
        delegate.startElement(uri, localName, localName, ai);
    }

    /**
     * @param prefix
     * @param uri
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegate.startPrefixMapping(prefix, uri);
    }
    
}
