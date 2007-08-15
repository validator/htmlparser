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

package nu.validator.htmlparser.impl;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * <code>Tokenizer</code> reports tokens through this interface.
 * 
 * @version $Id$
 * @author hsivonen
 */
public interface TokenHandler {

    /**
     * This method is called at the start of tokenization before any other 
     * methods on this interface are called. Implementations should hold 
     * the reference to the <code>Tokenizer</code> in order to set the 
     * content model flag and in order to be able to query for 
     * <code>Locator</code> data.
     * 
     * @param self the <code>Tokenizer</code>.
     * @throws SAXException if something went wrong
     */
    public void start(Tokenizer self) throws SAXException;

    /**
     * If this handler implementation cares about comments, return <code>true</code>.
     * If not, return <code>false</code>.
     * 
     * @return whether this handler wants comments
     * @throws SAXException if something went wrong
     */
    public boolean wantsComments() throws SAXException;
    
    /**
     * Receive a doctype token.
     * 
     * @param name the name
     * @param publicIdentifier the public id
     * @param systemIdentifier the system id
     * @param correct whether the token is correct
     * @throws SAXException if something went wrong
     */
    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean correct) throws SAXException;

    /**
     * Receive a start tag token.
     * 
     * @param name the tag name
     * @param attributes the attributes
     * @throws SAXException if something went wrong
     */
    public void startTag(String name, Attributes attributes) throws SAXException;
    
    /**
     * Receive an end tag token.
     * 
     * @param name the tag name
     * @param attributes the attributes
     * @throws SAXException if something went wrong
     */
    public void endTag(String name, Attributes attributes) throws SAXException;
    
    /**
     * Receive a comment token. The data is junk if the <code>wantsComments()</code> 
     * returned <code>false</code>.
     * 
     * @param buf a buffer holding the data
     * @param length the number of code units to read
     * @throws SAXException if something went wrong
     */
    public void comment(char[] buf, int length) throws SAXException;
    
    /**
     * Receive character tokens. This method has the same semantics as 
     * the SAX method of the same name.
     * 
     * @param buf a buffer holding the data
     * @param start offset into the buffer
     * @param length the number of code units to read
     * @throws SAXException if something went wrong
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] buf, int start, int length) throws SAXException;
    
    /**
     * The end-of-file token.
     * 
     * @throws SAXException if something went wrong
     */
    public void eof() throws SAXException;
    
}
