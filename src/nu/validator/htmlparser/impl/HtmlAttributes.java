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

package nu.validator.htmlparser.impl;

import nu.validator.htmlparser.annotation.IdType;
import nu.validator.htmlparser.annotation.Local;
import nu.validator.htmlparser.annotation.NsUri;
import nu.validator.htmlparser.annotation.QName;

import org.xml.sax.Attributes;

/**
 * Be careful with this class. QName is the name in from HTML tokenization.
 * Otherwise, please refer to the interface doc.
 * 
 * @version $Id: AttributesImpl.java 206 2008-03-20 14:09:29Z hsivonen $
 * @author hsivonen
 */
public final class HtmlAttributes implements Attributes {

    private int mode;

    private int length;

    private AttributeName[] names;

    private String[] values;
    
    private String idValue;

    public HtmlAttributes(int mode) {
        this.mode = mode;
        this.length = 0;
        this.names = new AttributeName[5]; // covers 98.3% of elements according to
        // Hixie
        this.values = new String[5];
        this.idValue = null;
    }

    public int getIndex(@QName String qName) {
        for (int i = 0; i < length; i++) {
            if (names[i].getQName(mode).equals(qName)) {
                return i;
            }
        }
        return -1;
    }

    public int getIndex(@NsUri String uri, @Local String localName) {
        for (int i = 0; i < length; i++) {
            if (names[i].getLocal(mode).equals(localName)
                    && names[i].getUri(mode).equals(uri)) {
                return i;
            }
        }
        return -1;
    }

    public int getLength() {
        return length;
    }

    public @Local String getLocalName(int index) {
        if (index < length) {
            return names[index].getLocal(mode);
        } else {
            return null;
        }
    }

    public @QName String getQName(int index) {
        if (index < length) {
            return names[index].getQName(mode);
        } else {
            return null;
        }
    }

    public @IdType String getType(int index) {
        if (index < length) {
            return names[index].getType(mode);
        } else {
            return null;
        }
    }

    public @IdType String getType(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getType(index);
        }
    }

    public @IdType String getType(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        } else {
            return getType(index);
        }
    }

    public @NsUri String getURI(int index) {
        if (index < length) {
            return names[index].getUri(mode);
        } else {
            return null;
        }
    }

    public String getValue(int index) {
        if (index < length) {
            return values[index];
        } else {
            return null;
        }
    }

    public final String getValue(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getValue(index);
        }
    }

    public String getValue(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        } else {
            return getValue(index);
        }
    }
    
    public String getId() {
        return idValue;
    }

    void addAttribute(AttributeName name, String value) {
        if (name == AttributeName.ID) {
            idValue = value;
        }
        if (names.length == length) {
            int newLen = names.length + 10; // The first growth covers virtually
            // 100% of elements according to
            // Hixie
            AttributeName[] newNames = new AttributeName[newLen];
            System.arraycopy(names, 0, newNames, 0, names.length);
            names = newNames;
            String[] newValues = new String[newLen];
            System.arraycopy(values, 0, newValues, 0, values.length);
            values = newValues;
        }
        names[length] = name;
        values[length] = value;
        length++;
    }

    void clear() {
        for (int i = 0; i < length; i++) {
            names[i] = null;
            values[i] = null;
        }
        length = 0;
        idValue = null;
    }
    
    void adjustForMath() {
        mode = AttributeName.MATHML;
    }
    
    void adjustForSvg() {
        mode = AttributeName.SVG;
    }
}
