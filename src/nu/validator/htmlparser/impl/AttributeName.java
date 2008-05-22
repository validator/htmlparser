/*
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

public class AttributeName {
    
    public static final int HTML = 0;

    public static final int HTML_LANG = 1;

    public static final int MATHML = 2;
    
    public static final int SVG = 3;
    
    private final String type;
    
    private final String[] uri;
    
    private final String[] local;
    
    private final String[] qName;
    
    private final boolean[] ncname;
    
    private final boolean xmlns;
    
    public String getType(int mode) {
        return type;
    }
    
    public String getUri(int mode) {
        return uri[mode];
    }
    
    public String getLocal(int mode) {
        return local[mode];
    }
    
    public String getQName(int mode) {
        return qName[mode];
    }
    
    public boolean isXmlns() {
        return xmlns;
    }
    
    private AttributeName(String camelCaseQname, boolean builtIn) {
        this.xmlns = false;
        this.ncname = new boolean[4];
        this.uri = new String[4];
        this.local = new String[4];
        this.qName = new String[4];
        this.type = ("id".equals(camelCaseQname)) ? "ID" : "CDATA";

        String lowerCaseQname = camelCaseQname.toLowerCase().intern();
        
        for (int i = 0; i < 4; i++) {
            ncname[i] = true;
        }
        for (int i = 0; i < 4; i++) {
            uri[i] = "";
        }
        this.local[HTML] = lowerCaseQname;
        this.local[HTML_LANG] = lowerCaseQname;
        this.local[MATHML] = lowerCaseQname;
        this.local[SVG] = camelCaseQname;
        this.qName[HTML] = lowerCaseQname;
        this.qName[HTML_LANG] = lowerCaseQname;
        this.qName[MATHML] = lowerCaseQname;
        this.qName[SVG] = camelCaseQname;
    }
    
    private AttributeName() {
        this.xmlns = false;
        this.ncname = new boolean[4];
        this.uri = new String[4];
        this.local = new String[4];
        this.qName = new String[4];
        this.type = "CDATA";
        
        for (int i = 0; i < 4; i++) {
            ncname[i] = true;
        }
        
        this.uri[HTML] = "";
        this.local[HTML_LANG] = "http://www.w3.org/XML/1998/namespace";
        this.uri[MATHML] = "";
        this.uri[SVG] = "";
        this.local[HTML] = "lang";
        this.local[HTML_LANG] = "lang";
        this.local[MATHML] = "lang";
        this.local[SVG] = "lang";
        this.qName[HTML] = "lang";
        this.qName[HTML_LANG] = "lang";
        this.qName[MATHML] = "lang";
        this.qName[SVG] = "lang";
    }
    
    private AttributeName(String qualified, String prefix, String local, String ns) {
        this.ncname = new boolean[4];
        this.uri = new String[4];
        this.local = new String[4];
        this.qName = new String[4];
        this.type = "CDATA";
        this.xmlns = ("xmlns" == prefix || "xmlns" == local);
        
        for (int i = 0; i < 4; i++) {
            qName[i] = qualified;
        }
        this.uri[HTML] = "";
        this.uri[HTML_LANG] = "";
        this.uri[MATHML] = ns;
        this.uri[SVG] = ns;
        this.local[HTML] = qualified;
        this.local[HTML_LANG] = qualified;
        this.local[MATHML] = local;
        this.local[SVG] = local;        
    }
}
