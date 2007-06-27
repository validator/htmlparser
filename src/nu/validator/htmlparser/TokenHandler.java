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

package nu.validator.htmlparser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface TokenHandler {
    public void start(Tokenizer self) throws SAXException;

    public boolean wantsComments() throws SAXException;
    
    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean correct) throws SAXException;
    
    public void startTag(String name, Attributes attributes) throws SAXException;
    
    public void endTag(String name, Attributes attributes) throws SAXException;
    
    public void comment(char[] buf, int length) throws SAXException;
    
    public void characters(char[] buf, int start, int length) throws SAXException;
    
    public void eof() throws SAXException;
    
}
