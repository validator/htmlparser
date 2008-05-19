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

package nu.validator.htmlparser.test;

import nu.validator.htmlparser.impl.ContentModelFlag;
import nu.validator.htmlparser.impl.TokenHandler;
import nu.validator.htmlparser.impl.Tokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONBoolean;
import com.sdicons.json.model.JSONNull;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;

public class JSONArrayTokenHandler implements TokenHandler, ErrorHandler {

    private static final JSONString DOCTYPE = new JSONString("DOCTYPE");

    private static final JSONString START_TAG = new JSONString("StartTag");

    private static final JSONString END_TAG = new JSONString("EndTag");

    private static final JSONString COMMENT = new JSONString("Comment");

    private static final JSONString CHARACTER = new JSONString("Character");

    private static final JSONString PARSE_ERROR = new JSONString("ParseError");

    private final StringBuilder builder = new StringBuilder();

    private JSONArray array = null;

    private ContentModelFlag contentModelFlag;

    private String contentModelElement;
    
    public void setContentModelFlag(ContentModelFlag contentModelFlag, String contentModelElement) {
        this.contentModelFlag = contentModelFlag;
        this.contentModelElement = contentModelElement;
    }

    public void characters(char[] buf, int start, int length)
            throws SAXException {
        builder.append(buf, start, length);
    }

    private void flushCharacters() {
        if (builder.length() > 0) {
            JSONArray token = new JSONArray();
            token.getValue().add(CHARACTER);
            token.getValue().add(new JSONString(builder.toString()));
            array.getValue().add(token);
            builder.setLength(0);
        }
    }

    public void comment(char[] buf, int length) throws SAXException {
        flushCharacters();
        JSONArray token = new JSONArray();
        token.getValue().add(COMMENT);
        token.getValue().add(new JSONString(new String(buf, 0, length)));
        array.getValue().add(token);
    }

    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean forceQuirks) throws SAXException {
        flushCharacters();
        JSONArray token = new JSONArray();
        token.getValue().add(DOCTYPE);
        token.getValue().add(new JSONString(name));
        token.getValue().add(publicIdentifier == null ? JSONNull.NULL : new JSONString(publicIdentifier));
        token.getValue().add(systemIdentifier == null ? JSONNull.NULL : new JSONString(systemIdentifier));
        token.getValue().add(new JSONBoolean(!forceQuirks));
        array.getValue().add(token);
    }

    public void endTag(String name, Attributes attributes) throws SAXException {
        flushCharacters();
        JSONArray token = new JSONArray();
        token.getValue().add(END_TAG);
        token.getValue().add(new JSONString(name));
        array.getValue().add(token);
    }

    public void eof() throws SAXException {
        flushCharacters();
    }

    public void start(Tokenizer self) throws SAXException {
        array = new JSONArray();
        self.setContentModelFlag(contentModelFlag, contentModelElement);
    }

    public void startTag(String name, Attributes attributes, boolean selfClosing)
            throws SAXException {
        flushCharacters();
        JSONArray token = new JSONArray();
        token.getValue().add(START_TAG);
        token.getValue().add(new JSONString(name));
        JSONObject attrs = new JSONObject();
        for (int i = 0; i < attributes.getLength(); i++) {
            attrs.getValue().put(attributes.getQName(i), new JSONString(attributes.getValue(i)));
        }
        token.getValue().add(attrs);
        if (selfClosing) {
            token.getValue().add(JSONBoolean.TRUE);            
        }
        array.getValue().add(token);
    }

    public boolean wantsComments() throws SAXException {
        return true;
    }

    public void error(SAXParseException exception) throws SAXException {
        flushCharacters();
        array.getValue().add(PARSE_ERROR);
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        throw new RuntimeException("Should never happen.");
    }

    public void warning(SAXParseException exception) throws SAXException {
    }

    /**
     * Returns the array.
     * 
     * @return the array
     */
    public JSONArray getArray() {
        return array;
    }

}
