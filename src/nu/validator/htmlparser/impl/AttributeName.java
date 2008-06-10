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

import nu.validator.htmlparser.annotation.IdType;
import nu.validator.htmlparser.annotation.Local;
import nu.validator.htmlparser.annotation.NsUri;
import nu.validator.htmlparser.annotation.QName;

public final class AttributeName {
    
    private static final @NsUri String[] ALL_NO_NS = {"", "", "", ""};

    private static final boolean [] ALL_NCNAME = {true, true, true, true};

    private static @Local String[] SAME_LOWER_CASE_LOCAL(@Local String name) {
        @Local String[] rv = new String[4];
        rv[0] = name;
        rv[1] = name;
        rv[2] = name;
        rv[3] = name;
        return rv;
    }

    private static @QName String[] SAME_LOWER_CASE_QNAME(@Local String name) {
        @QName String[] rv = new String[4];
        rv[0] = name;
        rv[1] = name;
        rv[2] = name;
        rv[3] = name;
        return rv;
    }
    
    public static final AttributeName ID = new AttributeName("ID", ALL_NO_NS, SAME_LOWER_CASE_LOCAL("id"), SAME_LOWER_CASE_QNAME("id"), ALL_NCNAME, false);
    
    
    
    public static final int HTML = 0;

    public static final int MATHML = 1;
    
    public static final int SVG = 2;

    public static final int HTML_LANG = 3;
    
    private final @IdType String type;
    
    private final @NsUri String[] uri;
    
    private final @Local String[] local;
    
    private final @QName String[] qName;
    
    private final boolean[] ncname;
    
    private final boolean xmlns;
    
    /**
     * @param type
     * @param uri
     * @param local
     * @param name
     * @param ncname
     * @param xmlns
     */
    private AttributeName(@IdType String type, @NsUri String[] uri, @Local String[] local,
            @QName String[] qName, boolean[] ncname, boolean xmlns) {
        this.type = type;
        this.uri = uri;
        this.local = local;
        this.qName = qName;
        this.ncname = ncname;
        this.xmlns = xmlns;
    }
    
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

    public boolean isNcName(int mode) {
        return ncname[mode];
    }
    
    public boolean isXmlns() {
        return xmlns;
    }
    
}
