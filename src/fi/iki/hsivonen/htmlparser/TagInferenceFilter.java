/*
 * Copyright (c) 2005, 2006 Henri Sivonen
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
import fi.iki.hsivonen.xml.EmptyAttributes;

/**
 * @version $Id$
 * @author hsivonen
 */
public final class TagInferenceFilter extends ContentHandlerFilter {
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    private static final String[][] END_CAUSING_STARTS = {
            /* body */{},
            /* colgroup */{ "colgroup", "tbody", "tfoot", "thead", "tr" },
            /* dd */{ "dd", "dt" },
            /* dt */{ "dd", "dt" },
            /* head */{}, // handled separately
            /* html */{},
            /* li */{ "li" },
            /* option */{"optgroup", "option"},
            /* p */{ "address", "blockquote", "center", "dd", "div", "dl",
                    "dt", "fieldset", "form", "h1", "h2", "h3", "h4", "h5",
                    "h6", "hr", "isindex", "li", "noframes", "noscript", "ol",
                    "p", "pre", "table", "tbody", "td", "tfoot", "th", "tr",
                    "ul" },
            /* tbody */{ "tbody" },
            /* td */{ "tbody", "td", "tfoot", "th", "tr" },
            /* tfoot */{ "tbody" },
            /* th */{ "tbody", "td", "tfoot", "th", "tr" },
            /* thead */{ "tbody", "tfoot" },
            /* tr */{ "tbody", "tfoot", "tr" } };

    private static final String[] OPTIONAL_END = { "body", "colgroup", "dd",
            "dt", "head", "html", "li", "option", "p", "tbody", "td", "tfoot", "th",
            "thead", "tr" };

    private static final String[] HEAD_CHILDREN = { "base", "bgsound",
            "isindex", "link", "meta", "object", "script", "style", "title" };

    private String[] stack = new String[48];

    private int stackIndex = 0;

    private HtmlParser parser;

    private boolean headClosed;

    private static boolean isOptionalEnd(String name) {
        return Arrays.binarySearch(OPTIONAL_END, name) > -1;
    }

    private static boolean isHeadChild(String name) {
        return Arrays.binarySearch(HEAD_CHILDREN, name) > -1;
    }

    private static boolean startImpliesEnd(String start, String top) {
        if (top == null) {
            return false;
        }
        if ("head".equals(top)) {
            return !isHeadChild(start);
        }
        int i = Arrays.binarySearch(OPTIONAL_END, top);
        if (i < 0) {
            return false;
        }
        return Arrays.binarySearch(END_CAUSING_STARTS[i], start) > -1;
    }

    private void push(String str) {
        stackIndex++;
        if (stackIndex == stack.length) {
            String[] newStack = new String[stack.length + 16];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[stackIndex] = str;
    }

    private String pop() {
        String rv = stack[stackIndex];
        if (stackIndex > 0) {
            stackIndex--;
        }
        return rv;
    }

    private String peek() {
        return stack[stackIndex];
    }

    private boolean isEmpty() {
        return stackIndex == 0;
    }

    /**
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void flushStack() throws SAXException {
        for (;;) {
            String top = pop();
            if (top == null) {
                return;
            }
            if (isOptionalEnd(top)) {
                endElement(top);
            } else {
                fatal("Document ended but there were unclosed elements.");
            }
        }
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String local, String qName)
            throws SAXException {
        for (;;) {
            String top = pop();
            if (top == null) {
                fatal("End tag for an element that was not open.");
            } else if (top.equals(local)) {
                endElement(top);
                break;
            } else {
                if (isOptionalEnd(top)) {
                    endElement(top);
                } else {
                    fatal("Stray end tag: " + local);
                }
            }
        }
        if (isEmpty()) {
            super.endPrefixMapping("");
            parser.setNonWhiteSpaceAllowed(false);
        }
    }

    /**
     * @param uri
     * @param name
     * @throws SAXException
     */
    private void endElement(String name) throws SAXException {
        super.endElement(XHTML_NS, name, name);
        if ("head".equals(name)) {
            headClosed = true;
        }
    }

    /**
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        headClosed = false;
        stackIndex = 0;
        stack[0] = null;
        super.startDocument();
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String local, String qName,
            Attributes attrs) throws SAXException {
        if (isEmpty()) {
            super.startPrefixMapping("", XHTML_NS);
        }
        boolean wereInferences;
        String top = null;
        do {
            wereInferences = false;
            for (;;) {
                top = peek();
                if (startImpliesEnd(local, top)) {
                    pop();
                    endElement(top);
                    wereInferences = true;
                } else {
                    break;
                }
            }
            top = peek();
            if ("table".equals(top) && "tr".equals(local)) {
                startElement("tbody");
                wereInferences = true;
            } else if (top == null && !"html".equals(local)) {
                startElement("html");
                wereInferences = true;
            } else if ("html".equals(top) && !"head".equals(local)
                    && !headClosed) {
                startElement("head");
                wereInferences = true;
            } else if ("html".equals(top) && !"body".equals(local)
                    && headClosed) {
                startElement("body");
                wereInferences = true;
            }
        } while (wereInferences);

        push(local);
        super.startElement(uri, local, qName, attrs);
        parser.setNonWhiteSpaceAllowed(true);
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void startElement(String name) throws SAXException {
        push(name);
        super.startElement(XHTML_NS, name, name,
                EmptyAttributes.EMPTY_ATTRIBUTES);
        parser.setNonWhiteSpaceAllowed(true);
    }

    /**
     * @param parser
     */
    public TagInferenceFilter(HtmlParser parser) {
        this.parser = parser;
    }
}
