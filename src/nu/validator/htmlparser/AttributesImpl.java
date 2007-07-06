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

/**
 * Be careful with this class. QName is the name in from 
 * HTML tokenization. Otherwise, please refer to the interface doc.
 * 
 * @version $Id$
 * @author hsivonen
 */
public class AttributesImpl implements Attributes {

    private int length = 0;

    private int limit = 0;

    private String[] array = new String[10]; // covers 98.3% of elements according to Hixie

    public final int getIndex(String qName) {
        for (int i = 0; i < limit; i += 2) {
            if (array[i].equals(qName)) {
                return i / 2;
            }
        }
        return -1;
    }

    public int getIndex(String uri, String localName) {
        if ("".equals(uri)) {
            return getIndex(localName);
        } else {
            return -1;
        }
    }

    public final int getLength() {
        return length;
    }

    public String getLocalName(int index) {
        return getQName(index);
    }

    public final String getQName(int index) {
        return index < length ? array[index * 2] : null;
    }

    public final String getType(int index) {
        if (index < length) {
            if ("id".equals(getQName(index))) {
                return "ID";
            } else {
                return "CDATA";
            }
        } else {
            return null;
        }
    }

    public final String getType(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getType(index);
        }
    }

    public String getType(String uri, String localName) {
        if ("".equals(uri)) {
            return getType(localName);
        } else {
            return null;
        }
    }

    public String getURI(int index) {
        return index < length ? "" : null;
    }

    public final String getValue(int index) {
        return index < length ? array[index * 2 + 1] : null;
    }

    public String getValue(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getValue(index);
        }
    }

    public String getValue(String uri, String localName) {
        if ("".equals(uri)) {
            return getValue(localName);
        } else {
            return null;
        }
    }

    final void addAttribute(String name, String value) {
        if (array.length == limit) {
            String[] newArray = new String[array.length + 10]; // The first growth covers virtually 100% of elements according to Hixie
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
        array[limit] = name;
        array[limit + 1] = value;
        length++;
        limit += 2;
    }

    final void addAttribute(String name) {
        addAttribute(name, "");
    }

}
