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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import fi.iki.hsivonen.xml.ContentHandlerFilter;

/**
 * @version $Id$
 * @author hsivonen
 */
public final class CharacterEncodingDeclarationFilter extends ContentHandlerFilter {
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    private static final int DOC_STARTED = 0;
    private static final int HTML_OPEN = 1;
    private static final int HEAD_OPEN = 2;
    private static final int SITUATION_OVER = 3;
    
    // XXX should white space and case-insensitivity be allowed
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("^[ \t\r\n]*Content-Type[ \t\r\n]*$", Pattern.CASE_INSENSITIVE);

    // XXX should white space and case-insensitivity be allowed
    // charset name pattern based on RFC 2978
    private static final Pattern CONTENT_PATTERN = Pattern.compile("^[ \t\r\n]*text/html[ \t\r\n]*;[ \t\r\n]*charset[ \t\r\n]*=[ \t\r\n]*([a-zA-Z0-9!#$%&\'+^_`{}~-]+)[ \t\r\n]*$", Pattern.CASE_INSENSITIVE);
    
    private int state = DOC_STARTED;

    private HtmlParser parser;

    private boolean swallowEnd;
    
    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String local, String qName)
            throws SAXException {
        if(swallowEnd) {
            swallowEnd = false;
            return;
        }
        if (state != SITUATION_OVER) {
            if(XHTML_NS.equals(uri)) {
                if("head".equals(local) || "html".equals(local)) {
                    state = SITUATION_OVER;
                    parser.setEncoding(null);
                }
            }
        }
        super.endElement(uri, local, qName);
    }
    /**
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        state = DOC_STARTED;
        swallowEnd = false;
        super.startDocument();
    }
    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String local, String qName,
            Attributes attrs) throws SAXException {
        if(XHTML_NS.equals(uri)) {
            if(state == DOC_STARTED) {
                if("html".equals(local)) {
                    state = HTML_OPEN;
                } else {
                    state = SITUATION_OVER;
                    parser.setEncoding(null);
                }
            } else if (state == HTML_OPEN){
                if("head".equals(local)) {
                    state = HEAD_OPEN;
                } else {
                    state = SITUATION_OVER;
                    parser.setEncoding(null);
                }
            } else if (state == HEAD_OPEN) {
                if("meta".equals(local)) {
                    String charset = attrs.getValue("charset");                 
                    if (charset != null) {
                        parser.setEncoding(charset);                        
                    } else {
                    String httpEquiv = attrs.getValue("http-equiv");
                    if(httpEquiv != null) {
                        Matcher m = CONTENT_TYPE_PATTERN.matcher(httpEquiv);
                        if(m.matches()) {
                            if (attrs.getLength() == 2) {
                                String content = attrs.getValue("content");
                                if (content != null) {
                                    m = CONTENT_PATTERN.matcher(content);
                                    if (m.matches()) {
                                        parser.setEncoding(m.group(1));
//                                        swallowEnd = true;
//                                        return;
                                    } else {
                                        // from WA1
                                        err("The \u201Ccontent\u201D attribute of the \u201Cmeta\u201D element did not contain the string \u201Ctext/html; charset=\u201D followed by an IANA character encoding name.");
                                    }
                                } else {
                                    err("There was no \u201Ccontent\u201D attribute on the \u201Cmeta\u201D element.");
                                }
                            } else {
                                // from WA1
                                err("When the element \u201Cmeta\u201D is used for declaring the character encoding, it must have exactly two attributes: \u201Chttp-equiv\u201D and \u201Ccontent\u201D.");
                            }
                        } else {
                            // from WA1
                            err("The element \u201Cmeta\u201D with the attribute \u201Chttp-equiv\u201D is only allowed when it is used for declaring the character encoding.");
                        }
                    } else {
                        state = SITUATION_OVER;                                            
                        parser.setEncoding(null);
                    }
                    }
                } else {
                    state = SITUATION_OVER;                    
                    parser.setEncoding(null);
                }
            }
        }
        super.startElement(uri, local, qName, attrs);
    }
    /**
     * @param parser
     */
    public CharacterEncodingDeclarationFilter(HtmlParser parser) {
        this.parser = parser;
    }
}
