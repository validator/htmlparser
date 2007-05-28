/*
 * Copyright (c) 2005 Henri Sivonen
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

package fi.iki.hsivonen.htmlparser;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import fi.iki.hsivonen.xml.ContentHandlerFilter;

/**
 * @version $Id$
 * @author hsivonen
 */
public final class EmptyElementFilter extends ContentHandlerFilter {
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    /**
     * HTML 4.01 Strict elements which don't have an end tag
     */
    private static final String[] EMPTY_ELEMENTS = { "area", "base",
            "basefont", "br", "col", "command",
            "embed", "event-source", "frame", "hr", "img", "input",
            "isindex", "link", "meta", "param" };

    // should we include things like <spacer> and <image>?
    
    static final boolean isEmpty(String name) {
        return (Arrays.binarySearch(EMPTY_ELEMENTS, name) >= 0);
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String local, String qName)
            throws SAXException {
        if (XHTML_NS.equals(uri)) {
            if (isEmpty(local)) {
                fatal("End tag \u201C"
                        + local
                        + "\u201D seen even though the element is an empty element.");
            }
        }
        super.endElement(uri, local, qName);
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String local, String qName,
            Attributes attrs) throws SAXException {
        // FIXME just dropping base for now
        boolean drop = "base".equals(local);
        if (!drop) {
            super.startElement(uri, local, qName, attrs);
        }
        if (XHTML_NS.equals(uri)) {
            if (!drop && isEmpty(local)) {
                super.endElement(uri, local, qName);
            }
        }
    }
}
