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

package nu.validator.htmlparser.legacy;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.TokenHandler;
import nu.validator.htmlparser.Tokenizer;

public class LegacyTokenHandler implements TokenHandler {

    private ContentHandler contentHandler;
    
    private Tokenizer tokenizer;
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        contentHandler.characters(buf, start, length);
    }

    public void comment(char[] buf, int length) throws SAXException {

    }

    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean inError) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void endTag(String name, Attributes attributes) throws SAXException {
        contentHandler.endElement("http://www.w3.org/1999/xhtml", name, name);
    }

    public void eof() throws SAXException {
        // TODO flush tag inference
        contentHandler.endDocument();
    }

    public void start(Tokenizer self) throws SAXException {
        tokenizer = self;
        contentHandler.setDocumentLocator(self);
        contentHandler.startDocument();
    }

    public void startTag(String name, Attributes attributes)
            throws SAXException {
        contentHandler.startElement("http://www.w3.org/1999/xhtml", name, name, attributes);
    }

    public boolean wantsComments() throws SAXException {
        return false;
    }

    /**
     * @param contentHandler
     */
    public LegacyTokenHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public void setNonWhiteSpaceAllowed(boolean b) {
        // TODO Auto-generated method stub
        
    }

}
