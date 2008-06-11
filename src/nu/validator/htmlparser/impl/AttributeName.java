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

import java.util.Arrays;

import nu.validator.htmlparser.annotation.IdType;
import nu.validator.htmlparser.annotation.Local;
import nu.validator.htmlparser.annotation.NsUri;
import nu.validator.htmlparser.annotation.QName;

public final class AttributeName implements Comparable<AttributeName> {
    
    private static final @NsUri String[] ALL_NO_NS = {"", "", "", ""};

    private static final boolean [] ALL_NCNAME = {true, true, true, true};

    private static @NsUri String[] NAMESPACE(@Local String ns) {
        return new String[]{"", ns, ns, ""};
    }

    private static @Local String[] CAMEL_CASE_LOCAL(@Local String name, @Local String camel) {
        return new String[]{name, name, camel, name};
    }

    private static @Local String[] COLONIFIED_LOCAL(@Local String name, @Local String suffix) {
        return new String[]{name, suffix, suffix, name};
    }
    
    private static @Local String[] SAME_LOWER_CASE_LOCAL(@Local String name) {
        return new String[]{name, name, name, name};
    }

    private static @QName String[] SAME_LOWER_CASE_QNAME(@Local String name) {
        return new String[]{name, name, name, name};
    }
    
    static AttributeName elementNameByBuffer(char[] buf, int length) {
        int hash = bufToHash(buf, length);
        int index = Arrays.binarySearch(ATTRIBUTE_HASHES, hash);
        if (index < 0) {
            return create(StringUtil.localNameFromBuffer(buf, length));
        } else {
            AttributeName rv = ATTRIBUTE_NAMES[index];
            @Local String name = rv.getQName(HTML);
            if (name.length() != length) {
                return create(StringUtil.localNameFromBuffer(buf,
                        length));
            }
            for (int i = 0; i < length; i++) {
                if (name.charAt(i) != buf[i]) {
                    return create(StringUtil.localNameFromBuffer(buf,
                            length));
                }
            }
            return rv;
        }
    }
    
    /**
     * This method has to return a unique integer for each well-known
     * lower-cased attribute name.
     * 
     * @param buf
     * @param len
     * @return
     */
    private static int bufToHash(char[] buf, int len) {
        int hash = len;
        hash <<= 5;
        hash += buf[0] - 0x60;
        int j = len;
        for (int i = 0; i < 4 && j > 0; i++) {
            j--;
            hash <<= 5;
            hash += buf[j] - 0x60;
        }
        int hash2 = 0;
        for (int i = 1; i < len && i < 5; i++) {
            hash2 <<= 6;
            hash2 += buf[i] - 0x5F;            
        }
        return hash ^ hash2;
    }
    
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

    private AttributeName(@NsUri String[] uri, @Local String[] local,
            @QName String[] qName, boolean[] ncname, boolean xmlns) {
        this.type = "CDATA";
        this.uri = uri;
        this.local = local;
        this.qName = qName;
        this.ncname = ncname;
        this.xmlns = xmlns;
    }
    
    private static AttributeName create(@IdType String type) {
        return null;
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
    
    // START CODE ONLY USED FOR GENERATING CODE

    // (@IdType String type, @NsUri String[] uri, @Local String[] local,
//    @QName String[] qName, boolean[] ncname, boolean xmlns)
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override public String toString() {
        return "(" + ("ID" == type ? "\"ID\", ": "") + formatNs() + ", " + formatLocal() + ", " + formatQname() + ", " + formatNcname() + ", " + (xmlns ? "true" : "false") + ")";
    }

    private String formatQname() {
        for (int i = 1; i < qName.length; i++) {
            if(qName[0] != qName[i]) {
                return "new String[]{\"" + qName[0] + "\", \"" + qName[1] + "\", \"" + qName[2] + "\", \"" + qName[3] + "\"}";
            }
        }
        return "SAME_LOWER_CASE_QNAME(\"" + qName[0] + "\")";
    }

    private String formatLocal() {
        if (local[0] == local[1] && local[0] == local[3] && local[0] != local[2]) {
            return "CAMEL_CASE_LOCAL(\"" + local[0] + "\", \"" + local[2] + "\")";
        }
        if (local[0] == local[3] && local[1] == local[2] && local[0] != local[1]) {
            return "COLONIFIED_LOCAL(\"" + local[0] + "\", \"" + local[1] + "\")";
        }
        for (int i = 1; i < local.length; i++) {
            if(local[0] != local[i]) {
                return "new String[]{\"" + local[0] + "\", \"" + local[1] + "\", \"" + local[2] + "\", \"" + local[3] + "\"}";
            }
        }
        return "SAME_LOWER_CASE_LOCAL(\"" + local[0] + "\")";
    }

    private String formatNs() {
        if (uri[1] != "" && uri[0] == "" && uri[3] == "" && uri[1] == uri[2]) {
            return "NAMESPACE(\"" + uri[1] + "\")";
        }
        for (int i = 0; i < uri.length; i++) {
            if("" != uri[i]) {
                return "new String[]{\"" + uri[0] + "\", \"" + uri[1] + "\", \"" + uri[2] + "\", \"" + uri[3] + "\"}";
            }
        }
        return "ALL_NO_NS";
    }

    private String formatNcname() {
        for (int i = 0; i < ncname.length; i++) {
            if(!ncname[i]) {
                return "new boolean[]{" + ncname[0] + ", " + ncname[1] + ", " + ncname[2] + ", " + ncname[3] + "}";
            }
        }
        return "ALL_NCNAME";
    }
    
    private String constName() {
        String name = getLocal(HTML);
        char[] buf = new char[name.length()];
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == ':') {
                buf[i] = '_';
            } else if (c >= '0' && c <= '9') {
                buf[i] = c;
            } else {
                buf[i] = (char) (c - 0x20);
            }
        }
        return new String(buf);
    }

    private int hash() {
        String name = getLocal(HTML);
        return bufToHash(name.toCharArray(), name.length());
    }

    public int compareTo(AttributeName other) {
        int thisHash = this.hash();
        int otherHash = other.hash();
        if (thisHash < otherHash) {
            return -1;
        } else if (thisHash == otherHash) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Regenerate self
     * 
     * @param args
     */
    public static void main(String[] args) {
        Arrays.sort(ATTRIBUTE_NAMES);
        for (int i = 1; i < ATTRIBUTE_NAMES.length; i++) {
            if (ATTRIBUTE_NAMES[i].hash() == ATTRIBUTE_NAMES[i - 1].hash()) {
                System.err.println("Hash collision: " + ATTRIBUTE_NAMES[i].getLocal(HTML)
                        + ", " + ATTRIBUTE_NAMES[i - 1].getLocal(HTML));
                return;
            }
        }
        for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
            AttributeName att = ATTRIBUTE_NAMES[i];
            System.out.println("public static final AttributeName "
                    + att.constName() + " = new AttributeName" + att.toString()
                    + ";");
        }
        System.out.println("private final static AttributeName[] ATTRIBUTE_NAMES = {");
        for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
            AttributeName att = ATTRIBUTE_NAMES[i];
            System.out.println(att.constName() + ",");
        }
        System.out.println("};");
        System.out.println("private final static int[] ATTRIBUTE_HASHES = {");
        for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
            AttributeName att = ATTRIBUTE_NAMES[i];
            System.out.println(Integer.toString(att.hash()) + ",");
        }
        System.out.println("};");
    }

    // START GENERATED CODE
    public static final AttributeName V_MATHEMATICAL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("v-mathematical"), SAME_LOWER_CASE_QNAME("v-mathematical"), ALL_NCNAME, false);
    public static final AttributeName V_IDEOGRAPHIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("v-ideographic"), SAME_LOWER_CASE_QNAME("v-ideographic"), ALL_NCNAME, false);
    public static final AttributeName V_ALPHABETIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("v-alphabetic"), SAME_LOWER_CASE_QNAME("v-alphabetic"), ALL_NCNAME, false);
    public static final AttributeName V_HANGING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("v-hanging"), SAME_LOWER_CASE_QNAME("v-hanging"), ALL_NCNAME, false);
    public static final AttributeName X_HEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("x-height"), SAME_LOWER_CASE_QNAME("x-height"), ALL_NCNAME, false);
    public static final AttributeName Y2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("y2"), SAME_LOWER_CASE_QNAME("y2"), ALL_NCNAME, false);
    public static final AttributeName Y1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("y1"), SAME_LOWER_CASE_QNAME("y1"), ALL_NCNAME, false);
    public static final AttributeName X2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("x2"), SAME_LOWER_CASE_QNAME("x2"), ALL_NCNAME, false);
    public static final AttributeName X1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("x1"), SAME_LOWER_CASE_QNAME("x1"), ALL_NCNAME, false);
    public static final AttributeName U2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("u2"), SAME_LOWER_CASE_QNAME("u2"), ALL_NCNAME, false);
    public static final AttributeName U1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("u1"), SAME_LOWER_CASE_QNAME("u1"), ALL_NCNAME, false);
    public static final AttributeName K4 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("k4"), SAME_LOWER_CASE_QNAME("k4"), ALL_NCNAME, false);
    public static final AttributeName K2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("k2"), SAME_LOWER_CASE_QNAME("k2"), ALL_NCNAME, false);
    public static final AttributeName K3 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("k3"), SAME_LOWER_CASE_QNAME("k3"), ALL_NCNAME, false);
    public static final AttributeName K1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("k1"), SAME_LOWER_CASE_QNAME("k1"), ALL_NCNAME, false);
    public static final AttributeName G2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("g2"), SAME_LOWER_CASE_QNAME("g2"), ALL_NCNAME, false);
    public static final AttributeName G1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("g1"), SAME_LOWER_CASE_QNAME("g1"), ALL_NCNAME, false);
    public static final AttributeName D = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("d"), SAME_LOWER_CASE_QNAME("d"), ALL_NCNAME, false);
    public static final AttributeName K = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("k"), SAME_LOWER_CASE_QNAME("k"), ALL_NCNAME, false);
    public static final AttributeName R = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("r"), SAME_LOWER_CASE_QNAME("r"), ALL_NCNAME, false);
    public static final AttributeName X = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("x"), SAME_LOWER_CASE_QNAME("x"), ALL_NCNAME, false);
    public static final AttributeName Y = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("y"), SAME_LOWER_CASE_QNAME("y"), ALL_NCNAME, false);
    public static final AttributeName Z = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("z"), SAME_LOWER_CASE_QNAME("z"), ALL_NCNAME, false);
    public static final AttributeName BY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("by"), SAME_LOWER_CASE_QNAME("by"), ALL_NCNAME, false);
    public static final AttributeName CX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cx"), SAME_LOWER_CASE_QNAME("cx"), ALL_NCNAME, false);
    public static final AttributeName CY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cy"), SAME_LOWER_CASE_QNAME("cy"), ALL_NCNAME, false);
    public static final AttributeName DX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dx"), SAME_LOWER_CASE_QNAME("dx"), ALL_NCNAME, false);
    public static final AttributeName DY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dy"), SAME_LOWER_CASE_QNAME("dy"), ALL_NCNAME, false);
    public static final AttributeName FX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fx"), SAME_LOWER_CASE_QNAME("fx"), ALL_NCNAME, false);
    public static final AttributeName FY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fy"), SAME_LOWER_CASE_QNAME("fy"), ALL_NCNAME, false);
    public static final AttributeName ID = new AttributeName("ID", ALL_NO_NS, SAME_LOWER_CASE_LOCAL("id"), SAME_LOWER_CASE_QNAME("id"), ALL_NCNAME, false);
    public static final AttributeName IN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("in"), SAME_LOWER_CASE_QNAME("in"), ALL_NCNAME, false);
    public static final AttributeName RT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rt"), SAME_LOWER_CASE_QNAME("rt"), ALL_NCNAME, false);
    public static final AttributeName RX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rx"), SAME_LOWER_CASE_QNAME("rx"), ALL_NCNAME, false);
    public static final AttributeName RY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ry"), SAME_LOWER_CASE_QNAME("ry"), ALL_NCNAME, false);
    public static final AttributeName TO = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("to"), SAME_LOWER_CASE_QNAME("to"), ALL_NCNAME, false);
    public static final AttributeName ALT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alt"), SAME_LOWER_CASE_QNAME("alt"), ALL_NCNAME, false);
    public static final AttributeName DIR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dir"), SAME_LOWER_CASE_QNAME("dir"), ALL_NCNAME, false);
    public static final AttributeName DUR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dur"), SAME_LOWER_CASE_QNAME("dur"), ALL_NCNAME, false);
    public static final AttributeName END = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("end"), SAME_LOWER_CASE_QNAME("end"), ALL_NCNAME, false);
    public static final AttributeName FOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("for"), SAME_LOWER_CASE_QNAME("for"), ALL_NCNAME, false);
    public static final AttributeName IN2 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("in2"), SAME_LOWER_CASE_QNAME("in2"), ALL_NCNAME, false);
    public static final AttributeName LOW = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("low"), SAME_LOWER_CASE_QNAME("low"), ALL_NCNAME, false);
    public static final AttributeName MIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("min"), SAME_LOWER_CASE_QNAME("min"), ALL_NCNAME, false);
    public static final AttributeName MAX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("max"), SAME_LOWER_CASE_QNAME("max"), ALL_NCNAME, false);
    public static final AttributeName REL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rel"), SAME_LOWER_CASE_QNAME("rel"), ALL_NCNAME, false);
    public static final AttributeName REV = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rev"), SAME_LOWER_CASE_QNAME("rev"), ALL_NCNAME, false);
    public static final AttributeName SRC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("src"), SAME_LOWER_CASE_QNAME("src"), ALL_NCNAME, false);
    public static final AttributeName AXIS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("axis"), SAME_LOWER_CASE_QNAME("axis"), ALL_NCNAME, false);
    public static final AttributeName ABBR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("abbr"), SAME_LOWER_CASE_QNAME("abbr"), ALL_NCNAME, false);
    public static final AttributeName BASE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("base"), SAME_LOWER_CASE_QNAME("base"), ALL_NCNAME, false);
    public static final AttributeName BIAS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("bias"), SAME_LOWER_CASE_QNAME("bias"), ALL_NCNAME, false);
    public static final AttributeName BBOX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("bbox"), SAME_LOWER_CASE_QNAME("bbox"), ALL_NCNAME, false);
    public static final AttributeName END  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("end "), SAME_LOWER_CASE_QNAME("end "), ALL_NCNAME, false);
    public static final AttributeName CITE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cite"), SAME_LOWER_CASE_QNAME("cite"), ALL_NCNAME, false);
    public static final AttributeName CODE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("code"), SAME_LOWER_CASE_QNAME("code"), ALL_NCNAME, false);
    public static final AttributeName COLS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cols"), SAME_LOWER_CASE_QNAME("cols"), ALL_NCNAME, false);
    public static final AttributeName CLIP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("clip"), SAME_LOWER_CASE_QNAME("clip"), ALL_NCNAME, false);
    public static final AttributeName CHAR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("char"), SAME_LOWER_CASE_QNAME("char"), ALL_NCNAME, false);
    public static final AttributeName DATA = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("data"), SAME_LOWER_CASE_QNAME("data"), ALL_NCNAME, false);
    public static final AttributeName EDGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("edge"), SAME_LOWER_CASE_QNAME("edge"), ALL_NCNAME, false);
    public static final AttributeName FACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("face"), SAME_LOWER_CASE_QNAME("face"), ALL_NCNAME, false);
    public static final AttributeName FILL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fill"), SAME_LOWER_CASE_QNAME("fill"), ALL_NCNAME, false);
    public static final AttributeName FROM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("from"), SAME_LOWER_CASE_QNAME("from"), ALL_NCNAME, false);
    public static final AttributeName FORM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("form"), SAME_LOWER_CASE_QNAME("form"), ALL_NCNAME, false);
    public static final AttributeName HREF = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("href"), SAME_LOWER_CASE_QNAME("href"), ALL_NCNAME, false);
    public static final AttributeName HIGH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("high"), SAME_LOWER_CASE_QNAME("high"), ALL_NCNAME, false);
    public static final AttributeName ICON = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("icon"), SAME_LOWER_CASE_QNAME("icon"), ALL_NCNAME, false);
    public static final AttributeName LANG = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("lang"), SAME_LOWER_CASE_QNAME("lang"), ALL_NCNAME, false);
    public static final AttributeName LINK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("link"), SAME_LOWER_CASE_QNAME("link"), ALL_NCNAME, false);
    public static final AttributeName LIST = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("list"), SAME_LOWER_CASE_QNAME("list"), ALL_NCNAME, false);
    public static final AttributeName MODE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mode"), SAME_LOWER_CASE_QNAME("mode"), ALL_NCNAME, false);
    public static final AttributeName MASK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mask"), SAME_LOWER_CASE_QNAME("mask"), ALL_NCNAME, false);
    public static final AttributeName NAME = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("name"), SAME_LOWER_CASE_QNAME("name"), ALL_NCNAME, false);
    public static final AttributeName OPEN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("open"), SAME_LOWER_CASE_QNAME("open"), ALL_NCNAME, false);
    public static final AttributeName PING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ping"), SAME_LOWER_CASE_QNAME("ping"), ALL_NCNAME, false);
    public static final AttributeName PATH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("path"), SAME_LOWER_CASE_QNAME("path"), ALL_NCNAME, false);
    public static final AttributeName ROLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("role"), SAME_LOWER_CASE_QNAME("role"), ALL_NCNAME, false);
    public static final AttributeName ROWS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rows"), SAME_LOWER_CASE_QNAME("rows"), ALL_NCNAME, false);
    public static final AttributeName REFX = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("refx", "refX"), SAME_LOWER_CASE_QNAME("refX"), ALL_NCNAME, false);
    public static final AttributeName REFY = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("refy", "refY"), SAME_LOWER_CASE_QNAME("refY"), ALL_NCNAME, false);
    public static final AttributeName SIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("size"), SAME_LOWER_CASE_QNAME("size"), ALL_NCNAME, false);
    public static final AttributeName SEED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("seed"), SAME_LOWER_CASE_QNAME("seed"), ALL_NCNAME, false);
    public static final AttributeName SPAN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("span"), SAME_LOWER_CASE_QNAME("span"), ALL_NCNAME, false);
    public static final AttributeName STEP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("step"), SAME_LOWER_CASE_QNAME("step"), ALL_NCNAME, false);
    public static final AttributeName TYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("type"), SAME_LOWER_CASE_QNAME("type"), ALL_NCNAME, false);
    public static final AttributeName TEXT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("text"), SAME_LOWER_CASE_QNAME("text"), ALL_NCNAME, false);
    public static final AttributeName WHEN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("when"), SAME_LOWER_CASE_QNAME("when"), ALL_NCNAME, false);
    public static final AttributeName WRAP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("wrap"), SAME_LOWER_CASE_QNAME("wrap"), ALL_NCNAME, false);
    public static final AttributeName XREF = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("xref"), SAME_LOWER_CASE_QNAME("xref"), ALL_NCNAME, false);
    public static final AttributeName CLOSE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("close"), SAME_LOWER_CASE_QNAME("close"), ALL_NCNAME, false);
    public static final AttributeName CLEAR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("clear"), SAME_LOWER_CASE_QNAME("clear"), ALL_NCNAME, false);
    public static final AttributeName CLASS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("class"), SAME_LOWER_CASE_QNAME("class"), ALL_NCNAME, false);
    public static final AttributeName ALINK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alink"), SAME_LOWER_CASE_QNAME("alink"), ALL_NCNAME, false);
    public static final AttributeName ALIGN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("align"), SAME_LOWER_CASE_QNAME("align"), ALL_NCNAME, false);
    public static final AttributeName FRAME = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("frame"), SAME_LOWER_CASE_QNAME("frame"), ALL_NCNAME, false);
    public static final AttributeName BEGIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("begin"), SAME_LOWER_CASE_QNAME("begin"), ALL_NCNAME, false);
    public static final AttributeName ASYNC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("async"), SAME_LOWER_CASE_QNAME("async"), ALL_NCNAME, false);
    public static final AttributeName DEFER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("defer"), SAME_LOWER_CASE_QNAME("defer"), ALL_NCNAME, false);
    public static final AttributeName DEPTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("depth"), SAME_LOWER_CASE_QNAME("depth"), ALL_NCNAME, false);
    public static final AttributeName COLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("color"), SAME_LOWER_CASE_QNAME("color"), ALL_NCNAME, false);
    public static final AttributeName FENCE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fence"), SAME_LOWER_CASE_QNAME("fence"), ALL_NCNAME, false);
    public static final AttributeName LOCAL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("local"), SAME_LOWER_CASE_QNAME("local"), ALL_NCNAME, false);
    public static final AttributeName INDEX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("index"), SAME_LOWER_CASE_QNAME("index"), ALL_NCNAME, false);
    public static final AttributeName OTHER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("other"), SAME_LOWER_CASE_QNAME("other"), ALL_NCNAME, false);
    public static final AttributeName ORDER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("order"), SAME_LOWER_CASE_QNAME("order"), ALL_NCNAME, false);
    public static final AttributeName ONCUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oncut"), SAME_LOWER_CASE_QNAME("oncut"), ALL_NCNAME, false);
    public static final AttributeName ISMAP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ismap"), SAME_LOWER_CASE_QNAME("ismap"), ALL_NCNAME, false);
    public static final AttributeName MEDIA = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("media"), SAME_LOWER_CASE_QNAME("media"), ALL_NCNAME, false);
    public static final AttributeName LABEL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("label"), SAME_LOWER_CASE_QNAME("label"), ALL_NCNAME, false);
    public static final AttributeName ONEND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onend"), SAME_LOWER_CASE_QNAME("onend"), ALL_NCNAME, false);
    public static final AttributeName NARGS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("nargs"), SAME_LOWER_CASE_QNAME("nargs"), ALL_NCNAME, false);
    public static final AttributeName SLOPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("slope"), SAME_LOWER_CASE_QNAME("slope"), ALL_NCNAME, false);
    public static final AttributeName SHAPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("shape"), SAME_LOWER_CASE_QNAME("shape"), ALL_NCNAME, false);
    public static final AttributeName SCALE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scale"), SAME_LOWER_CASE_QNAME("scale"), ALL_NCNAME, false);
    public static final AttributeName SCOPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scope"), SAME_LOWER_CASE_QNAME("scope"), ALL_NCNAME, false);
    public static final AttributeName VLINK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("vlink"), SAME_LOWER_CASE_QNAME("vlink"), ALL_NCNAME, false);
    public static final AttributeName WIDTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("width"), SAME_LOWER_CASE_QNAME("width"), ALL_NCNAME, false);
    public static final AttributeName STEMH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stemh"), SAME_LOWER_CASE_QNAME("stemh"), ALL_NCNAME, false);
    public static final AttributeName STYLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("style"), SAME_LOWER_CASE_QNAME("style"), ALL_NCNAME, false);
    public static final AttributeName VALUE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("value"), SAME_LOWER_CASE_QNAME("value"), ALL_NCNAME, false);
    public static final AttributeName TITLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("title"), SAME_LOWER_CASE_QNAME("title"), ALL_NCNAME, false);
    public static final AttributeName START = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("start"), SAME_LOWER_CASE_QNAME("start"), ALL_NCNAME, false);
    public static final AttributeName STEMV = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stemv"), SAME_LOWER_CASE_QNAME("stemv"), ALL_NCNAME, false);
    public static final AttributeName RULES = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rules"), SAME_LOWER_CASE_QNAME("rules"), ALL_NCNAME, false);
    public static final AttributeName SPEED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("speed"), SAME_LOWER_CASE_QNAME("speed"), ALL_NCNAME, false);
    public static final AttributeName XMLNS = new AttributeName(NAMESPACE("http://www.w3.org/2000/xmlns/"), SAME_LOWER_CASE_LOCAL("xmlns"), SAME_LOWER_CASE_QNAME("xmlns"), new boolean[]{false, false, false, false}, true);
    public static final AttributeName ACTION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("action"), SAME_LOWER_CASE_QNAME("action"), ALL_NCNAME, false);
    public static final AttributeName ACCEPT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accept"), SAME_LOWER_CASE_QNAME("accept"), ALL_NCNAME, false);
    public static final AttributeName ACCENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accent"), SAME_LOWER_CASE_QNAME("accent"), ALL_NCNAME, false);
    public static final AttributeName ALTIMG = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("altimg"), SAME_LOWER_CASE_QNAME("altimg"), ALL_NCNAME, false);
    public static final AttributeName FORMAT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("format"), SAME_LOWER_CASE_QNAME("format"), ALL_NCNAME, false);
    public static final AttributeName FILTER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("filter"), SAME_LOWER_CASE_QNAME("filter"), ALL_NCNAME, false);
    public static final AttributeName ASCENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ascent"), SAME_LOWER_CASE_QNAME("ascent"), ALL_NCNAME, false);
    public static final AttributeName CURSOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cursor"), SAME_LOWER_CASE_QNAME("cursor"), ALL_NCNAME, false);
    public static final AttributeName BORDER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("border"), SAME_LOWER_CASE_QNAME("border"), ALL_NCNAME, false);
    public static final AttributeName COORDS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("coords"), SAME_LOWER_CASE_QNAME("coords"), ALL_NCNAME, false);
    public static final AttributeName LOWSRC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("lowsrc"), SAME_LOWER_CASE_QNAME("lowsrc"), ALL_NCNAME, false);
    public static final AttributeName LQUOTE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("lquote"), SAME_LOWER_CASE_QNAME("lquote"), ALL_NCNAME, false);
    public static final AttributeName HEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("height"), SAME_LOWER_CASE_QNAME("height"), ALL_NCNAME, false);
    public static final AttributeName LSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("lspace"), SAME_LOWER_CASE_QNAME("lspace"), ALL_NCNAME, false);
    public static final AttributeName NOHREF = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("nohref"), SAME_LOWER_CASE_QNAME("nohref"), ALL_NCNAME, false);
    public static final AttributeName NOWRAP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("nowrap"), SAME_LOWER_CASE_QNAME("nowrap"), ALL_NCNAME, false);
    public static final AttributeName HIDDEN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("hidden"), SAME_LOWER_CASE_QNAME("hidden"), ALL_NCNAME, false);
    public static final AttributeName ORIENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("orient"), SAME_LOWER_CASE_QNAME("orient"), ALL_NCNAME, false);
    public static final AttributeName ORIGIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("origin"), SAME_LOWER_CASE_QNAME("origin"), ALL_NCNAME, false);
    public static final AttributeName ONCOPY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oncopy"), SAME_LOWER_CASE_QNAME("oncopy"), ALL_NCNAME, false);
    public static final AttributeName ONDROP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondrop"), SAME_LOWER_CASE_QNAME("ondrop"), ALL_NCNAME, false);
    public static final AttributeName ONHELP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onhelp"), SAME_LOWER_CASE_QNAME("onhelp"), ALL_NCNAME, false);
    public static final AttributeName ONBLUR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onblur"), SAME_LOWER_CASE_QNAME("onblur"), ALL_NCNAME, false);
    public static final AttributeName ONSTOP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onstop"), SAME_LOWER_CASE_QNAME("onstop"), ALL_NCNAME, false);
    public static final AttributeName ONZOOM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onzoom"), SAME_LOWER_CASE_QNAME("onzoom"), ALL_NCNAME, false);
    public static final AttributeName METHOD = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("method"), SAME_LOWER_CASE_QNAME("method"), ALL_NCNAME, false);
    public static final AttributeName ONMOVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmove"), SAME_LOWER_CASE_QNAME("onmove"), ALL_NCNAME, false);
    public static final AttributeName ONLOAD = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onload"), SAME_LOWER_CASE_QNAME("onload"), ALL_NCNAME, false);
    public static final AttributeName ONDRAG = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondrag"), SAME_LOWER_CASE_QNAME("ondrag"), ALL_NCNAME, false);
    public static final AttributeName MACROS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("macros"), SAME_LOWER_CASE_QNAME("macros"), ALL_NCNAME, false);
    public static final AttributeName HSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("hspace"), SAME_LOWER_CASE_QNAME("hspace"), ALL_NCNAME, false);
    public static final AttributeName OFFSET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("offset"), SAME_LOWER_CASE_QNAME("offset"), ALL_NCNAME, false);
    public static final AttributeName OBJECT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("object"), SAME_LOWER_CASE_QNAME("object"), ALL_NCNAME, false);
    public static final AttributeName USEMAP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("usemap"), SAME_LOWER_CASE_QNAME("usemap"), ALL_NCNAME, false);
    public static final AttributeName RADIUS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("radius"), SAME_LOWER_CASE_QNAME("radius"), ALL_NCNAME, false);
    public static final AttributeName SCHEME = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scheme"), SAME_LOWER_CASE_QNAME("scheme"), ALL_NCNAME, false);
    public static final AttributeName SCOPED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scoped"), SAME_LOWER_CASE_QNAME("scoped"), ALL_NCNAME, false);
    public static final AttributeName REPEAT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("repeat"), SAME_LOWER_CASE_QNAME("repeat"), ALL_NCNAME, false);
    public static final AttributeName RESULT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("result"), SAME_LOWER_CASE_QNAME("result"), ALL_NCNAME, false);
    public static final AttributeName VSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("vspace"), SAME_LOWER_CASE_QNAME("vspace"), ALL_NCNAME, false);
    public static final AttributeName TARGET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("target"), SAME_LOWER_CASE_QNAME("target"), ALL_NCNAME, false);
    public static final AttributeName VALUE_ = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("value:"), SAME_LOWER_CASE_QNAME("value:"), ALL_NCNAME, false);
    public static final AttributeName PROMPT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("prompt"), SAME_LOWER_CASE_QNAME("prompt"), ALL_NCNAME, false);
    public static final AttributeName POSTER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("poster"), SAME_LOWER_CASE_QNAME("poster"), ALL_NCNAME, false);
    public static final AttributeName POINTS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("points"), SAME_LOWER_CASE_QNAME("points"), ALL_NCNAME, false);
    public static final AttributeName WIDTHS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("widths"), SAME_LOWER_CASE_QNAME("widths"), ALL_NCNAME, false);
    public static final AttributeName VALUES = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("values"), SAME_LOWER_CASE_QNAME("values"), ALL_NCNAME, false);
    public static final AttributeName ROTATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rotate"), SAME_LOWER_CASE_QNAME("rotate"), ALL_NCNAME, false);
    public static final AttributeName STRING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("string"), SAME_LOWER_CASE_QNAME("string"), ALL_NCNAME, false);
    public static final AttributeName STROKE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke"), SAME_LOWER_CASE_QNAME("stroke"), ALL_NCNAME, false);
    public static final AttributeName RQUOTE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rquote"), SAME_LOWER_CASE_QNAME("rquote"), ALL_NCNAME, false);
    public static final AttributeName VALIGN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("valign"), SAME_LOWER_CASE_QNAME("valign"), ALL_NCNAME, false);
    public static final AttributeName RSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rspace"), SAME_LOWER_CASE_QNAME("rspace"), ALL_NCNAME, false);
    public static final AttributeName CLASSID = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("classid"), SAME_LOWER_CASE_QNAME("classid"), ALL_NCNAME, false);
    public static final AttributeName CLOSURE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("closure"), SAME_LOWER_CASE_QNAME("closure"), ALL_NCNAME, false);
    public static final AttributeName BGCOLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("bgcolor"), SAME_LOWER_CASE_QNAME("bgcolor"), ALL_NCNAME, false);
    public static final AttributeName CHECKED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("checked"), SAME_LOWER_CASE_QNAME("checked"), ALL_NCNAME, false);
    public static final AttributeName CHAROFF = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("charoff"), SAME_LOWER_CASE_QNAME("charoff"), ALL_NCNAME, false);
    public static final AttributeName CHARSET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("charset"), SAME_LOWER_CASE_QNAME("charset"), ALL_NCNAME, false);
    public static final AttributeName ALTTEXT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alttext"), SAME_LOWER_CASE_QNAME("alttext"), ALL_NCNAME, false);
    public static final AttributeName DATASRC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("datasrc"), SAME_LOWER_CASE_QNAME("datasrc"), ALL_NCNAME, false);
    public static final AttributeName DATAFLD = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("datafld"), SAME_LOWER_CASE_QNAME("datafld"), ALL_NCNAME, false);
    public static final AttributeName DEFAULT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("default"), SAME_LOWER_CASE_QNAME("default"), ALL_NCNAME, false);
    public static final AttributeName DESCENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("descent"), SAME_LOWER_CASE_QNAME("descent"), ALL_NCNAME, false);
    public static final AttributeName DECLARE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("declare"), SAME_LOWER_CASE_QNAME("declare"), ALL_NCNAME, false);
    public static final AttributeName ARCHIVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("archive"), SAME_LOWER_CASE_QNAME("archive"), ALL_NCNAME, false);
    public static final AttributeName DIVISOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("divisor"), SAME_LOWER_CASE_QNAME("divisor"), ALL_NCNAME, false);
    public static final AttributeName DISPLAY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("display"), SAME_LOWER_CASE_QNAME("display"), ALL_NCNAME, false);
    public static final AttributeName ENCTYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("enctype"), SAME_LOWER_CASE_QNAME("enctype"), ALL_NCNAME, false);
    public static final AttributeName COLSPAN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("colspan"), SAME_LOWER_CASE_QNAME("colspan"), ALL_NCNAME, false);
    public static final AttributeName AZIMUTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("azimuth"), SAME_LOWER_CASE_QNAME("azimuth"), ALL_NCNAME, false);
    public static final AttributeName CONTENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("content"), SAME_LOWER_CASE_QNAME("content"), ALL_NCNAME, false);
    public static final AttributeName COMPACT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("compact"), SAME_LOWER_CASE_QNAME("compact"), ALL_NCNAME, false);
    public static final AttributeName HANGING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("hanging"), SAME_LOWER_CASE_QNAME("hanging"), ALL_NCNAME, false);
    public static final AttributeName HEADERS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("headers"), SAME_LOWER_CASE_QNAME("headers"), ALL_NCNAME, false);
    public static final AttributeName NOSHADE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("noshade"), SAME_LOWER_CASE_QNAME("noshade"), ALL_NCNAME, false);
    public static final AttributeName KERNING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("kerning"), SAME_LOWER_CASE_QNAME("kerning"), ALL_NCNAME, false);
    public static final AttributeName OPTIMUM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("optimum"), SAME_LOWER_CASE_QNAME("optimum"), ALL_NCNAME, false);
    public static final AttributeName OPACITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("opacity"), SAME_LOWER_CASE_QNAME("opacity"), ALL_NCNAME, false);
    public static final AttributeName LARGEOP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("largeop"), SAME_LOWER_CASE_QNAME("largeop"), ALL_NCNAME, false);
    public static final AttributeName ONKEYUP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onkeyup"), SAME_LOWER_CASE_QNAME("onkeyup"), ALL_NCNAME, false);
    public static final AttributeName ONERROR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onerror"), SAME_LOWER_CASE_QNAME("onerror"), ALL_NCNAME, false);
    public static final AttributeName ONFOCUS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onfocus"), SAME_LOWER_CASE_QNAME("onfocus"), ALL_NCNAME, false);
    public static final AttributeName ONABORT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onabort"), SAME_LOWER_CASE_QNAME("onabort"), ALL_NCNAME, false);
    public static final AttributeName ONINPUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oninput"), SAME_LOWER_CASE_QNAME("oninput"), ALL_NCNAME, false);
    public static final AttributeName ONSTART = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onstart"), SAME_LOWER_CASE_QNAME("onstart"), ALL_NCNAME, false);
    public static final AttributeName ONRESET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onreset"), SAME_LOWER_CASE_QNAME("onreset"), ALL_NCNAME, false);
    public static final AttributeName ONCLICK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onclick"), SAME_LOWER_CASE_QNAME("onclick"), ALL_NCNAME, false);
    public static final AttributeName ONBEGIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbegin"), SAME_LOWER_CASE_QNAME("onbegin"), ALL_NCNAME, false);
    public static final AttributeName ONPASTE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onpaste"), SAME_LOWER_CASE_QNAME("onpaste"), ALL_NCNAME, false);
    public static final AttributeName MAXSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("maxsize"), SAME_LOWER_CASE_QNAME("maxsize"), ALL_NCNAME, false);
    public static final AttributeName MINSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("minsize"), SAME_LOWER_CASE_QNAME("minsize"), ALL_NCNAME, false);
    public static final AttributeName PATTERN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("pattern"), SAME_LOWER_CASE_QNAME("pattern"), ALL_NCNAME, false);
    public static final AttributeName RESTART = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("restart"), SAME_LOWER_CASE_QNAME("restart"), ALL_NCNAME, false);
    public static final AttributeName SANDBOX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("sandbox"), SAME_LOWER_CASE_QNAME("sandbox"), ALL_NCNAME, false);
    public static final AttributeName REPLACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("replace"), SAME_LOWER_CASE_QNAME("replace"), ALL_NCNAME, false);
    public static final AttributeName VIEWBOX = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("viewbox", "viewBox"), SAME_LOWER_CASE_QNAME("viewBox"), ALL_NCNAME, false);
    public static final AttributeName TARGETX = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("targetx", "targetX"), SAME_LOWER_CASE_QNAME("targetX"), ALL_NCNAME, false);
    public static final AttributeName TARGETY = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("targety", "targetY"), SAME_LOWER_CASE_QNAME("targetY"), ALL_NCNAME, false);
    public static final AttributeName PROFILE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("profile"), SAME_LOWER_CASE_QNAME("profile"), ALL_NCNAME, false);
    public static final AttributeName START   = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("start  "), SAME_LOWER_CASE_QNAME("start  "), ALL_NCNAME, false);
    public static final AttributeName SUMMARY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("summary"), SAME_LOWER_CASE_QNAME("summary"), ALL_NCNAME, false);
    public static final AttributeName ROWSPAN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rowspan"), SAME_LOWER_CASE_QNAME("rowspan"), ALL_NCNAME, false);
    public static final AttributeName STANDBY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("standby"), SAME_LOWER_CASE_QNAME("standby"), ALL_NCNAME, false);
    public static final AttributeName UNICODE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("unicode"), SAME_LOWER_CASE_QNAME("unicode"), ALL_NCNAME, false);
    public static final AttributeName SPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("spacing"), SAME_LOWER_CASE_QNAME("spacing"), ALL_NCNAME, false);
    public static final AttributeName VERSION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("version"), SAME_LOWER_CASE_QNAME("version"), ALL_NCNAME, false);
    public static final AttributeName ADDITIVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("additive"), SAME_LOWER_CASE_QNAME("additive"), ALL_NCNAME, false);
    public static final AttributeName FONTSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fontsize"), SAME_LOWER_CASE_QNAME("fontsize"), ALL_NCNAME, false);
    public static final AttributeName BASELINE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("baseline"), SAME_LOWER_CASE_QNAME("baseline"), ALL_NCNAME, false);
    public static final AttributeName CALCMODE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("calcmode", "calcMode"), SAME_LOWER_CASE_QNAME("calcMode"), ALL_NCNAME, false);
    public static final AttributeName BEVELLED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("bevelled"), SAME_LOWER_CASE_QNAME("bevelled"), ALL_NCNAME, false);
    public static final AttributeName EXPONENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("exponent"), SAME_LOWER_CASE_QNAME("exponent"), ALL_NCNAME, false);
    public static final AttributeName EDGEMODE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("edgemode", "edgeMode"), SAME_LOWER_CASE_QNAME("edgeMode"), ALL_NCNAME, false);
    public static final AttributeName GLYPHREF = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("glyphref", "glyphRef"), SAME_LOWER_CASE_QNAME("glyphRef"), ALL_NCNAME, false);
    public static final AttributeName DATETIME = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("datetime"), SAME_LOWER_CASE_QNAME("datetime"), ALL_NCNAME, false);
    public static final AttributeName DISABLED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("disabled"), SAME_LOWER_CASE_QNAME("disabled"), ALL_NCNAME, false);
    public static final AttributeName ENCODING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("encoding"), SAME_LOWER_CASE_QNAME("encoding"), ALL_NCNAME, false);
    public static final AttributeName CODETYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("codetype"), SAME_LOWER_CASE_QNAME("codetype"), ALL_NCNAME, false);
    public static final AttributeName CODEBASE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("codebase"), SAME_LOWER_CASE_QNAME("codebase"), ALL_NCNAME, false);
    public static final AttributeName LONGDESC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("longdesc"), SAME_LOWER_CASE_QNAME("longdesc"), ALL_NCNAME, false);
    public static final AttributeName MULTIPLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("multiple"), SAME_LOWER_CASE_QNAME("multiple"), ALL_NCNAME, false);
    public static final AttributeName KEYTIMES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("keytimes", "keyTimes"), SAME_LOWER_CASE_QNAME("keyTimes"), ALL_NCNAME, false);
    public static final AttributeName NORESIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("noresize"), SAME_LOWER_CASE_QNAME("noresize"), ALL_NCNAME, false);
    public static final AttributeName NOTATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("notation"), SAME_LOWER_CASE_QNAME("notation"), ALL_NCNAME, false);
    public static final AttributeName OVERFLOW = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("overflow"), SAME_LOWER_CASE_QNAME("overflow"), ALL_NCNAME, false);
    public static final AttributeName OPERATOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("operator"), SAME_LOWER_CASE_QNAME("operator"), ALL_NCNAME, false);
    public static final AttributeName ONREPEAT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onrepeat"), SAME_LOWER_CASE_QNAME("onrepeat"), ALL_NCNAME, false);
    public static final AttributeName ONSELECT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onselect"), SAME_LOWER_CASE_QNAME("onselect"), ALL_NCNAME, false);
    public static final AttributeName ONSUBMIT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onsubmit"), SAME_LOWER_CASE_QNAME("onsubmit"), ALL_NCNAME, false);
    public static final AttributeName ONFINISH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onfinish"), SAME_LOWER_CASE_QNAME("onfinish"), ALL_NCNAME, false);
    public static final AttributeName LANGUAGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("language"), SAME_LOWER_CASE_QNAME("language"), ALL_NCNAME, false);
    public static final AttributeName ONSCROLL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onscroll"), SAME_LOWER_CASE_QNAME("onscroll"), ALL_NCNAME, false);
    public static final AttributeName ONBOUNCE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbounce"), SAME_LOWER_CASE_QNAME("onbounce"), ALL_NCNAME, false);
    public static final AttributeName ONCHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onchange"), SAME_LOWER_CASE_QNAME("onchange"), ALL_NCNAME, false);
    public static final AttributeName ONUNLOAD = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onunload"), SAME_LOWER_CASE_QNAME("onunload"), ALL_NCNAME, false);
    public static final AttributeName ONRESIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onresize"), SAME_LOWER_CASE_QNAME("onresize"), ALL_NCNAME, false);
    public static final AttributeName HREFLANG = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("hreflang"), SAME_LOWER_CASE_QNAME("hreflang"), ALL_NCNAME, false);
    public static final AttributeName MANIFEST = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("manifest"), SAME_LOWER_CASE_QNAME("manifest"), ALL_NCNAME, false);
    public static final AttributeName MATHSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mathsize"), SAME_LOWER_CASE_QNAME("mathsize"), ALL_NCNAME, false);
    public static final AttributeName LOOPEND  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("loopend "), SAME_LOWER_CASE_QNAME("loopend "), ALL_NCNAME, false);
    public static final AttributeName PANOSE_1 = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("panose-1"), SAME_LOWER_CASE_QNAME("panose-1"), ALL_NCNAME, false);
    public static final AttributeName SEAMLESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("seamless"), SAME_LOWER_CASE_QNAME("seamless"), ALL_NCNAME, false);
    public static final AttributeName SELECTED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("selected"), SAME_LOWER_CASE_QNAME("selected"), ALL_NCNAME, false);
    public static final AttributeName READONLY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("readonly"), SAME_LOWER_CASE_QNAME("readonly"), ALL_NCNAME, false);
    public static final AttributeName REQUIRED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("required"), SAME_LOWER_CASE_QNAME("required"), ALL_NCNAME, false);
    public static final AttributeName TABINDEX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("tabindex"), SAME_LOWER_CASE_QNAME("tabindex"), ALL_NCNAME, false);
    public static final AttributeName TEMPLATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("template"), SAME_LOWER_CASE_QNAME("template"), ALL_NCNAME, false);
    public static final AttributeName ROWALIGN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rowalign"), SAME_LOWER_CASE_QNAME("rowalign"), ALL_NCNAME, false);
    public static final AttributeName ROWLINES = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rowlines"), SAME_LOWER_CASE_QNAME("rowlines"), ALL_NCNAME, false);
    public static final AttributeName STRETCHY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stretchy"), SAME_LOWER_CASE_QNAME("stretchy"), ALL_NCNAME, false);
    public static final AttributeName XML_BASE = new AttributeName(NAMESPACE("http://www.w3.org/XML/1998/namespace"), COLONIFIED_LOCAL("xml:base", "base"), SAME_LOWER_CASE_QNAME("xml:base"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName XML_LANG = new AttributeName(NAMESPACE("http://www.w3.org/XML/1998/namespace"), COLONIFIED_LOCAL("xml:lang", "lang"), SAME_LOWER_CASE_QNAME("xml:lang"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName CLIP_PATH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("clip-path"), SAME_LOWER_CASE_QNAME("clip-path"), ALL_NCNAME, false);
    public static final AttributeName CLIP_RULE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("clip-rule"), SAME_LOWER_CASE_QNAME("clip-rule"), ALL_NCNAME, false);
    public static final AttributeName ACCESSKEY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accesskey"), SAME_LOWER_CASE_QNAME("accesskey"), ALL_NCNAME, false);
    public static final AttributeName DRAGGABLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("draggable"), SAME_LOWER_CASE_QNAME("draggable"), ALL_NCNAME, false);
    public static final AttributeName EQUALROWS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("equalrows"), SAME_LOWER_CASE_QNAME("equalrows"), ALL_NCNAME, false);
    public static final AttributeName FONT_SIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-size"), SAME_LOWER_CASE_QNAME("font-size"), ALL_NCNAME, false);
    public static final AttributeName FONTSTYLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fontstyle"), SAME_LOWER_CASE_QNAME("fontstyle"), ALL_NCNAME, false);
    public static final AttributeName AMPLITUDE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("amplitude"), SAME_LOWER_CASE_QNAME("amplitude"), ALL_NCNAME, false);
    public static final AttributeName AUTOFOCUS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("autofocus"), SAME_LOWER_CASE_QNAME("autofocus"), ALL_NCNAME, false);
    public static final AttributeName FILTERRES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("filterres", "filterRes"), SAME_LOWER_CASE_QNAME("filterRes"), ALL_NCNAME, false);
    public static final AttributeName FILL_RULE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fill-rule"), SAME_LOWER_CASE_QNAME("fill-rule"), ALL_NCNAME, false);
    public static final AttributeName CONTROLS  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("controls "), SAME_LOWER_CASE_QNAME("controls "), ALL_NCNAME, false);
    public static final AttributeName ARIA_OWNS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-owns"), SAME_LOWER_CASE_QNAME("aria-owns"), ALL_NCNAME, false);
    public static final AttributeName ARIA_SORT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-sort"), SAME_LOWER_CASE_QNAME("aria-sort"), ALL_NCNAME, false);
    public static final AttributeName ARIA_LIVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-live"), SAME_LOWER_CASE_QNAME("aria-live"), ALL_NCNAME, false);
    public static final AttributeName ELEVATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("elevation"), SAME_LOWER_CASE_QNAME("elevation"), ALL_NCNAME, false);
    public static final AttributeName DIRECTION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("direction"), SAME_LOWER_CASE_QNAME("direction"), ALL_NCNAME, false);
    public static final AttributeName KEYPOINTS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("keypoints", "keyPoints"), SAME_LOWER_CASE_QNAME("keyPoints"), ALL_NCNAME, false);
    public static final AttributeName HIDEFOCUS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("hidefocus"), SAME_LOWER_CASE_QNAME("hidefocus"), ALL_NCNAME, false);
    public static final AttributeName INTERCEPT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("intercept"), SAME_LOWER_CASE_QNAME("intercept"), ALL_NCNAME, false);
    public static final AttributeName INPUTMODE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("inputmode"), SAME_LOWER_CASE_QNAME("inputmode"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEUP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmouseup"), SAME_LOWER_CASE_QNAME("onmouseup"), ALL_NCNAME, false);
    public static final AttributeName ONROWEXIT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onrowexit"), SAME_LOWER_CASE_QNAME("onrowexit"), ALL_NCNAME, false);
    public static final AttributeName ONFOCUSIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onfocusin"), SAME_LOWER_CASE_QNAME("onfocusin"), ALL_NCNAME, false);
    public static final AttributeName ONKEYDOWN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onkeydown"), SAME_LOWER_CASE_QNAME("onkeydown"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGEND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragend"), SAME_LOWER_CASE_QNAME("ondragend"), ALL_NCNAME, false);
    public static final AttributeName ONMESSAGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmessage"), SAME_LOWER_CASE_QNAME("onmessage"), ALL_NCNAME, false);
    public static final AttributeName ONINVALID = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oninvalid"), SAME_LOWER_CASE_QNAME("oninvalid"), ALL_NCNAME, false);
    public static final AttributeName ONMOVEEND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmoveend"), SAME_LOWER_CASE_QNAME("onmoveend"), ALL_NCNAME, false);
    public static final AttributeName MATHCOLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mathcolor"), SAME_LOWER_CASE_QNAME("mathcolor"), ALL_NCNAME, false);
    public static final AttributeName MASKUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("maskunits", "maskUnits"), SAME_LOWER_CASE_QNAME("maskUnits"), ALL_NCNAME, false);
    public static final AttributeName MAXLENGTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("maxlength"), SAME_LOWER_CASE_QNAME("maxlength"), ALL_NCNAME, false);
    public static final AttributeName LINEBREAK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("linebreak"), SAME_LOWER_CASE_QNAME("linebreak"), ALL_NCNAME, false);
    public static final AttributeName TRANSFORM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("transform"), SAME_LOWER_CASE_QNAME("transform"), ALL_NCNAME, false);
    public static final AttributeName SEPARATOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("separator"), SAME_LOWER_CASE_QNAME("separator"), ALL_NCNAME, false);
    public static final AttributeName SCROLLING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scrolling"), SAME_LOWER_CASE_QNAME("scrolling"), ALL_NCNAME, false);
    public static final AttributeName SELECTION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("selection"), SAME_LOWER_CASE_QNAME("selection"), ALL_NCNAME, false);
    public static final AttributeName REPEATDUR = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("repeatdur", "repeatDur"), SAME_LOWER_CASE_QNAME("repeatDur"), ALL_NCNAME, false);
    public static final AttributeName POINTSATY = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("pointsaty", "pointsAtY"), SAME_LOWER_CASE_QNAME("pointsAtY"), ALL_NCNAME, false);
    public static final AttributeName POINTSATX = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("pointsatx", "pointsAtX"), SAME_LOWER_CASE_QNAME("pointsAtX"), ALL_NCNAME, false);
    public static final AttributeName POINTSATZ = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("pointsatz", "pointsAtZ"), SAME_LOWER_CASE_QNAME("pointsAtZ"), ALL_NCNAME, false);
    public static final AttributeName SYMMETRIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("symmetric"), SAME_LOWER_CASE_QNAME("symmetric"), ALL_NCNAME, false);
    public static final AttributeName VALUETYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("valuetype"), SAME_LOWER_CASE_QNAME("valuetype"), ALL_NCNAME, false);
    public static final AttributeName AUTOPLAY   = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("autoplay  "), SAME_LOWER_CASE_QNAME("autoplay  "), ALL_NCNAME, false);
    public static final AttributeName XML_SPACE = new AttributeName(NAMESPACE("http://www.w3.org/XML/1998/namespace"), COLONIFIED_LOCAL("xml:space", "space"), SAME_LOWER_CASE_QNAME("xml:space"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName ARIA_GRAB  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-grab "), SAME_LOWER_CASE_QNAME("aria-grab "), ALL_NCNAME, false);
    public static final AttributeName ARIA_BUSY  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-busy "), SAME_LOWER_CASE_QNAME("aria-busy "), ALL_NCNAME, false);
    public static final AttributeName ACCUMULATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accumulate"), SAME_LOWER_CASE_QNAME("accumulate"), ALL_NCNAME, false);
    public static final AttributeName ACTIONTYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("actiontype"), SAME_LOWER_CASE_QNAME("actiontype"), ALL_NCNAME, false);
    public static final AttributeName FONT_STYLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-style"), SAME_LOWER_CASE_QNAME("font-style"), ALL_NCNAME, false);
    public static final AttributeName ALPHABETIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alphabetic"), SAME_LOWER_CASE_QNAME("alphabetic"), ALL_NCNAME, false);
    public static final AttributeName BACKGROUND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("background"), SAME_LOWER_CASE_QNAME("background"), ALL_NCNAME, false);
    public static final AttributeName FONTWEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fontweight"), SAME_LOWER_CASE_QNAME("fontweight"), ALL_NCNAME, false);
    public static final AttributeName FONTFAMILY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fontfamily"), SAME_LOWER_CASE_QNAME("fontfamily"), ALL_NCNAME, false);
    public static final AttributeName CAP_HEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cap-height"), SAME_LOWER_CASE_QNAME("cap-height"), ALL_NCNAME, false);
    public static final AttributeName GROUPALIGN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("groupalign"), SAME_LOWER_CASE_QNAME("groupalign"), ALL_NCNAME, false);
    public static final AttributeName AUTOSUBMIT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("autosubmit"), SAME_LOWER_CASE_QNAME("autosubmit"), ALL_NCNAME, false);
    public static final AttributeName GLYPH_NAME = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("glyph-name"), SAME_LOWER_CASE_QNAME("glyph-name"), ALL_NCNAME, false);
    public static final AttributeName ARIA_LEVEL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-level"), SAME_LOWER_CASE_QNAME("aria-level"), ALL_NCNAME, false);
    public static final AttributeName COLUMNSPAN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("columnspan"), SAME_LOWER_CASE_QNAME("columnspan"), ALL_NCNAME, false);
    public static final AttributeName KEYSPLINES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("keysplines", "keySplines"), SAME_LOWER_CASE_QNAME("keySplines"), ALL_NCNAME, false);
    public static final AttributeName NUMOCTAVES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("numoctaves", "numOctaves"), SAME_LOWER_CASE_QNAME("numOctaves"), ALL_NCNAME, false);
    public static final AttributeName ONROWENTER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onrowenter"), SAME_LOWER_CASE_QNAME("onrowenter"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGDROP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragdrop"), SAME_LOWER_CASE_QNAME("ondragdrop"), ALL_NCNAME, false);
    public static final AttributeName ONKEYPRESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onkeypress"), SAME_LOWER_CASE_QNAME("onkeypress"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGOVER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragover"), SAME_LOWER_CASE_QNAME("ondragover"), ALL_NCNAME, false);
    public static final AttributeName ONFOCUSOUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onfocusout"), SAME_LOWER_CASE_QNAME("onfocusout"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEOUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmouseout"), SAME_LOWER_CASE_QNAME("onmouseout"), ALL_NCNAME, false);
    public static final AttributeName ONDBLCLICK = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondblclick"), SAME_LOWER_CASE_QNAME("ondblclick"), ALL_NCNAME, false);
    public static final AttributeName ONACTIVATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onactivate"), SAME_LOWER_CASE_QNAME("onactivate"), ALL_NCNAME, false);
    public static final AttributeName PLAYCOUNT  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("playcount "), SAME_LOWER_CASE_QNAME("playcount "), ALL_NCNAME, false);
    public static final AttributeName IRRELEVANT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("irrelevant"), SAME_LOWER_CASE_QNAME("irrelevant"), ALL_NCNAME, false);
    public static final AttributeName MARKER_END = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("marker-end"), SAME_LOWER_CASE_QNAME("marker-end"), ALL_NCNAME, false);
    public static final AttributeName MARKER_MID = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("marker-mid"), SAME_LOWER_CASE_QNAME("marker-mid"), ALL_NCNAME, false);
    public static final AttributeName HTTP_EQUIV = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("http-equiv"), SAME_LOWER_CASE_QNAME("http-equiv"), ALL_NCNAME, false);
    public static final AttributeName LOOPSTART  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("loopstart "), SAME_LOWER_CASE_QNAME("loopstart "), ALL_NCNAME, false);
    public static final AttributeName OCCURRENCE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("occurrence"), SAME_LOWER_CASE_QNAME("occurrence"), ALL_NCNAME, false);
    public static final AttributeName PATHLENGTH = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("pathlength", "pathLength"), SAME_LOWER_CASE_QNAME("pathLength"), ALL_NCNAME, false);
    public static final AttributeName RADIOGROUP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("radiogroup"), SAME_LOWER_CASE_QNAME("radiogroup"), ALL_NCNAME, false);
    public static final AttributeName SEPARATORS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("separators"), SAME_LOWER_CASE_QNAME("separators"), ALL_NCNAME, false);
    public static final AttributeName REPEAT_MAX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("repeat-max"), SAME_LOWER_CASE_QNAME("repeat-max"), ALL_NCNAME, false);
    public static final AttributeName REPEAT_MIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("repeat-min"), SAME_LOWER_CASE_QNAME("repeat-min"), ALL_NCNAME, false);
    public static final AttributeName VIEWTARGET = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("viewtarget", "viewTarget"), SAME_LOWER_CASE_QNAME("viewTarget"), ALL_NCNAME, false);
    public static final AttributeName VISIBILITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("visibility"), SAME_LOWER_CASE_QNAME("visibility"), ALL_NCNAME, false);
    public static final AttributeName TEXTLENGTH = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("textlength", "textLength"), SAME_LOWER_CASE_QNAME("textLength"), ALL_NCNAME, false);
    public static final AttributeName ROWSPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rowspacing"), SAME_LOWER_CASE_QNAME("rowspacing"), ALL_NCNAME, false);
    public static final AttributeName STOP_COLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stop-color"), SAME_LOWER_CASE_QNAME("stop-color"), ALL_NCNAME, false);
    public static final AttributeName VERT_ADV_Y = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("vert-adv-y"), SAME_LOWER_CASE_QNAME("vert-adv-y"), ALL_NCNAME, false);
    public static final AttributeName XLINK_ROLE = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:role", "role"), SAME_LOWER_CASE_QNAME("xlink:role"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName XLINK_TYPE = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:type", "type"), SAME_LOWER_CASE_QNAME("xlink:type"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName XLINK_HREF = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:href", "href"), SAME_LOWER_CASE_QNAME("xlink:href"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName XLINK_SHOW = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:show", "show"), SAME_LOWER_CASE_QNAME("xlink:show"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName ZOOMANDPAN = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("zoomandpan", "zoomAndPan"), SAME_LOWER_CASE_QNAME("zoomAndPan"), ALL_NCNAME, false);
    public static final AttributeName ACCENTUNDER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accentunder"), SAME_LOWER_CASE_QNAME("accentunder"), ALL_NCNAME, false);
    public static final AttributeName FRAMEBORDER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("frameborder"), SAME_LOWER_CASE_QNAME("frameborder"), ALL_NCNAME, false);
    public static final AttributeName FONT_WEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-weight"), SAME_LOWER_CASE_QNAME("font-weight"), ALL_NCNAME, false);
    public static final AttributeName CELLPADDING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cellpadding"), SAME_LOWER_CASE_QNAME("cellpadding"), ALL_NCNAME, false);
    public static final AttributeName CELLSPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("cellspacing"), SAME_LOWER_CASE_QNAME("cellspacing"), ALL_NCNAME, false);
    public static final AttributeName BASEPROFILE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("baseprofile", "baseProfile"), SAME_LOWER_CASE_QNAME("baseProfile"), ALL_NCNAME, false);
    public static final AttributeName FONT_FAMILY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-family"), SAME_LOWER_CASE_QNAME("font-family"), ALL_NCNAME, false);
    public static final AttributeName FILTERUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("filterunits", "filterUnits"), SAME_LOWER_CASE_QNAME("filterUnits"), ALL_NCNAME, false);
    public static final AttributeName ARIA_SECRET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-secret"), SAME_LOWER_CASE_QNAME("aria-secret"), ALL_NCNAME, false);
    public static final AttributeName ARABIC_FORM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("arabic-form"), SAME_LOWER_CASE_QNAME("arabic-form"), ALL_NCNAME, false);
    public static final AttributeName ARIA_FLOWTO = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-flowto"), SAME_LOWER_CASE_QNAME("aria-flowto"), ALL_NCNAME, false);
    public static final AttributeName FLOOD_COLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("flood-color"), SAME_LOWER_CASE_QNAME("flood-color"), ALL_NCNAME, false);
    public static final AttributeName ARIA_ATOMIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-atomic"), SAME_LOWER_CASE_QNAME("aria-atomic"), ALL_NCNAME, false);
    public static final AttributeName COLUMNWIDTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("columnwidth"), SAME_LOWER_CASE_QNAME("columnwidth"), ALL_NCNAME, false);
    public static final AttributeName COLUMNALIGN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("columnalign"), SAME_LOWER_CASE_QNAME("columnalign"), ALL_NCNAME, false);
    public static final AttributeName COLUMNLINES = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("columnlines"), SAME_LOWER_CASE_QNAME("columnlines"), ALL_NCNAME, false);
    public static final AttributeName CONTEXTMENU = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("contextmenu"), SAME_LOWER_CASE_QNAME("contextmenu"), ALL_NCNAME, false);
    public static final AttributeName IDEOGRAPHIC = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ideographic"), SAME_LOWER_CASE_QNAME("ideographic"), ALL_NCNAME, false);
    public static final AttributeName ORIENTATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("orientation"), SAME_LOWER_CASE_QNAME("orientation"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGENTER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragenter"), SAME_LOWER_CASE_QNAME("ondragenter"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEOVER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmouseover"), SAME_LOWER_CASE_QNAME("onmouseover"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGSTART = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragstart"), SAME_LOWER_CASE_QNAME("ondragstart"), ALL_NCNAME, false);
    public static final AttributeName ONFORMINPUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onforminput"), SAME_LOWER_CASE_QNAME("onforminput"), ALL_NCNAME, false);
    public static final AttributeName ONBEFORECUT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforecut"), SAME_LOWER_CASE_QNAME("onbeforecut"), ALL_NCNAME, false);
    public static final AttributeName ONMOVESTART = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmovestart"), SAME_LOWER_CASE_QNAME("onmovestart"), ALL_NCNAME, false);
    public static final AttributeName HORIZ_ADV_X = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("horiz-adv-x"), SAME_LOWER_CASE_QNAME("horiz-adv-x"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEDOWN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmousedown"), SAME_LOWER_CASE_QNAME("onmousedown"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEMOVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmousemove"), SAME_LOWER_CASE_QNAME("onmousemove"), ALL_NCNAME, false);
    public static final AttributeName ONDRAGLEAVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondragleave"), SAME_LOWER_CASE_QNAME("ondragleave"), ALL_NCNAME, false);
    public static final AttributeName MARKERUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("markerunits", "markerUnits"), SAME_LOWER_CASE_QNAME("markerUnits"), ALL_NCNAME, false);
    public static final AttributeName MATHVARIANT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mathvariant"), SAME_LOWER_CASE_QNAME("mathvariant"), ALL_NCNAME, false);
    public static final AttributeName MARGINWIDTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("marginwidth"), SAME_LOWER_CASE_QNAME("marginwidth"), ALL_NCNAME, false);
    public static final AttributeName MARKERWIDTH = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("markerwidth", "markerWidth"), SAME_LOWER_CASE_QNAME("markerWidth"), ALL_NCNAME, false);
    public static final AttributeName SCRIPTLEVEL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scriptlevel"), SAME_LOWER_CASE_QNAME("scriptlevel"), ALL_NCNAME, false);
    public static final AttributeName SCROLLDELAY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scrolldelay"), SAME_LOWER_CASE_QNAME("scrolldelay"), ALL_NCNAME, false);
    public static final AttributeName REPEATCOUNT = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("repeatcount", "repeatCount"), SAME_LOWER_CASE_QNAME("repeatCount"), ALL_NCNAME, false);
    public static final AttributeName TABLEVALUES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("tablevalues", "tableValues"), SAME_LOWER_CASE_QNAME("tableValues"), ALL_NCNAME, false);
    public static final AttributeName TEXT_ANCHOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("text-anchor"), SAME_LOWER_CASE_QNAME("text-anchor"), ALL_NCNAME, false);
    public static final AttributeName STITCHTILES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("stitchtiles", "stitchTiles"), SAME_LOWER_CASE_QNAME("stitchTiles"), ALL_NCNAME, false);
    public static final AttributeName STARTOFFSET = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("startoffset", "startOffset"), SAME_LOWER_CASE_QNAME("startOffset"), ALL_NCNAME, false);
    public static final AttributeName XLINK_TITLE = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:title", "title"), SAME_LOWER_CASE_QNAME("xlink:title"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName ARIA_HIDDEN  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-hidden "), SAME_LOWER_CASE_QNAME("aria-hidden "), ALL_NCNAME, false);
    public static final AttributeName XMLNS_XLINK = new AttributeName(NAMESPACE("http://www.w3.org/2000/xmlns/"), COLONIFIED_LOCAL("xmlns:xlink", "xlink"), SAME_LOWER_CASE_QNAME("xmlns:xlink"), new boolean[]{false, false, false, false}, true);
    public static final AttributeName EQUALCOLUMNS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("equalcolumns"), SAME_LOWER_CASE_QNAME("equalcolumns"), ALL_NCNAME, false);
    public static final AttributeName FONT_STRETCH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-stretch"), SAME_LOWER_CASE_QNAME("font-stretch"), ALL_NCNAME, false);
    public static final AttributeName FONT_VARIANT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-variant"), SAME_LOWER_CASE_QNAME("font-variant"), ALL_NCNAME, false);
    public static final AttributeName FRAMESPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("framespacing"), SAME_LOWER_CASE_QNAME("framespacing"), ALL_NCNAME, false);
    public static final AttributeName DATAFORMATAS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dataformatas"), SAME_LOWER_CASE_QNAME("dataformatas"), ALL_NCNAME, false);
    public static final AttributeName FILL_OPACITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("fill-opacity"), SAME_LOWER_CASE_QNAME("fill-opacity"), ALL_NCNAME, false);
    public static final AttributeName AUTOCOMPLETE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("autocomplete"), SAME_LOWER_CASE_QNAME("autocomplete"), ALL_NCNAME, false);
    public static final AttributeName ARIA_CHANNEL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-channel"), SAME_LOWER_CASE_QNAME("aria-channel"), ALL_NCNAME, false);
    public static final AttributeName ARIA_SETSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-setsize"), SAME_LOWER_CASE_QNAME("aria-setsize"), ALL_NCNAME, false);
    public static final AttributeName DISPLAYSTYLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("displaystyle"), SAME_LOWER_CASE_QNAME("displaystyle"), ALL_NCNAME, false);
    public static final AttributeName KERNELMATRIX = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("kernelmatrix", "kernelMatrix"), SAME_LOWER_CASE_QNAME("kernelMatrix"), ALL_NCNAME, false);
    public static final AttributeName ONBEFORECOPY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforecopy"), SAME_LOWER_CASE_QNAME("onbeforecopy"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEENTER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmouseenter"), SAME_LOWER_CASE_QNAME("onmouseenter"), ALL_NCNAME, false);
    public static final AttributeName ONAFTERPRINT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onafterprint"), SAME_LOWER_CASE_QNAME("onafterprint"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSEWHEEL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmousewheel"), SAME_LOWER_CASE_QNAME("onmousewheel"), ALL_NCNAME, false);
    public static final AttributeName ONMOUSELEAVE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onmouseleave"), SAME_LOWER_CASE_QNAME("onmouseleave"), ALL_NCNAME, false);
    public static final AttributeName ONDEACTIVATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondeactivate"), SAME_LOWER_CASE_QNAME("ondeactivate"), ALL_NCNAME, false);
    public static final AttributeName ONCELLCHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oncellchange"), SAME_LOWER_CASE_QNAME("oncellchange"), ALL_NCNAME, false);
    public static final AttributeName ONFORMCHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onformchange"), SAME_LOWER_CASE_QNAME("onformchange"), ALL_NCNAME, false);
    public static final AttributeName ONROWSDELETE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onrowsdelete"), SAME_LOWER_CASE_QNAME("onrowsdelete"), ALL_NCNAME, false);
    public static final AttributeName LENGTHADJUST = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("lengthadjust", "lengthAdjust"), SAME_LOWER_CASE_QNAME("lengthAdjust"), ALL_NCNAME, false);
    public static final AttributeName MARGINHEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("marginheight"), SAME_LOWER_CASE_QNAME("marginheight"), ALL_NCNAME, false);
    public static final AttributeName MARKERHEIGHT = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("markerheight", "markerHeight"), SAME_LOWER_CASE_QNAME("markerHeight"), ALL_NCNAME, false);
    public static final AttributeName MARKER_START = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("marker-start"), SAME_LOWER_CASE_QNAME("marker-start"), ALL_NCNAME, false);
    public static final AttributeName MATHEMATICAL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mathematical"), SAME_LOWER_CASE_QNAME("mathematical"), ALL_NCNAME, false);
    public static final AttributeName PATTERNUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("patternunits", "patternUnits"), SAME_LOWER_CASE_QNAME("patternUnits"), ALL_NCNAME, false);
    public static final AttributeName WORD_SPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("word-spacing"), SAME_LOWER_CASE_QNAME("word-spacing"), ALL_NCNAME, false);
    public static final AttributeName REPEAT_START = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("repeat-start"), SAME_LOWER_CASE_QNAME("repeat-start"), ALL_NCNAME, false);
    public static final AttributeName WRITING_MODE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("writing-mode"), SAME_LOWER_CASE_QNAME("writing-mode"), ALL_NCNAME, false);
    public static final AttributeName STROKE_WIDTH = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-width"), SAME_LOWER_CASE_QNAME("stroke-width"), ALL_NCNAME, false);
    public static final AttributeName STDDEVIATION = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("stddeviation", "stdDeviation"), SAME_LOWER_CASE_QNAME("stdDeviation"), ALL_NCNAME, false);
    public static final AttributeName UNICODE_BIDI = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("unicode-bidi"), SAME_LOWER_CASE_QNAME("unicode-bidi"), ALL_NCNAME, false);
    public static final AttributeName STOP_OPACITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stop-opacity"), SAME_LOWER_CASE_QNAME("stop-opacity"), ALL_NCNAME, false);
    public static final AttributeName UNITS_PER_EM = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("units-per-em"), SAME_LOWER_CASE_QNAME("units-per-em"), ALL_NCNAME, false);
    public static final AttributeName SURFACESCALE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("surfacescale", "surfaceScale"), SAME_LOWER_CASE_QNAME("surfaceScale"), ALL_NCNAME, false);
    public static final AttributeName UNSELECTABLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("unselectable"), SAME_LOWER_CASE_QNAME("unselectable"), ALL_NCNAME, false);
    public static final AttributeName SPREADMETHOD = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("spreadmethod", "spreadMethod"), SAME_LOWER_CASE_QNAME("spreadMethod"), ALL_NCNAME, false);
    public static final AttributeName ARIA_CHECKED  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-checked "), SAME_LOWER_CASE_QNAME("aria-checked "), ALL_NCNAME, false);
    public static final AttributeName ARIA_PRESSED  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-pressed "), SAME_LOWER_CASE_QNAME("aria-pressed "), ALL_NCNAME, false);
    public static final AttributeName ARIA_INVALID  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-invalid "), SAME_LOWER_CASE_QNAME("aria-invalid "), ALL_NCNAME, false);
    public static final AttributeName ACCENT_HEIGHT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accent-height"), SAME_LOWER_CASE_QNAME("accent-height"), ALL_NCNAME, false);
    public static final AttributeName CLIPPATHUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("clippathunits", "clipPathUnits"), SAME_LOWER_CASE_QNAME("clipPathUnits"), ALL_NCNAME, false);
    public static final AttributeName BASEFREQUENCY = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("basefrequency", "baseFrequency"), SAME_LOWER_CASE_QNAME("baseFrequency"), ALL_NCNAME, false);
    public static final AttributeName GRADIENTUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("gradientunits", "gradientUnits"), SAME_LOWER_CASE_QNAME("gradientUnits"), ALL_NCNAME, false);
    public static final AttributeName ATTRIBUTETYPE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("attributetype", "attributeType"), SAME_LOWER_CASE_QNAME("attributeType"), ALL_NCNAME, false);
    public static final AttributeName ATTRIBUTENAME = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("attributename", "attributeName"), SAME_LOWER_CASE_QNAME("attributeName"), ALL_NCNAME, false);
    public static final AttributeName ARIA_READONLY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-readonly"), SAME_LOWER_CASE_QNAME("aria-readonly"), ALL_NCNAME, false);
    public static final AttributeName ARIA_VALUEMAX = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-valuemax"), SAME_LOWER_CASE_QNAME("aria-valuemax"), ALL_NCNAME, false);
    public static final AttributeName ARIA_HASPOPUP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-haspopup"), SAME_LOWER_CASE_QNAME("aria-haspopup"), ALL_NCNAME, false);
    public static final AttributeName ARIA_CONTROLS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-controls"), SAME_LOWER_CASE_QNAME("aria-controls"), ALL_NCNAME, false);
    public static final AttributeName ARIA_RELEVANT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-relevant"), SAME_LOWER_CASE_QNAME("aria-relevant"), ALL_NCNAME, false);
    public static final AttributeName ARIA_POSINSET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-posinset"), SAME_LOWER_CASE_QNAME("aria-posinset"), ALL_NCNAME, false);
    public static final AttributeName ARIA_VALUENOW = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-valuenow"), SAME_LOWER_CASE_QNAME("aria-valuenow"), ALL_NCNAME, false);
    public static final AttributeName FLOOD_OPACITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("flood-opacity"), SAME_LOWER_CASE_QNAME("flood-opacity"), ALL_NCNAME, false);
    public static final AttributeName ARIA_VALUEMIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-valuemin"), SAME_LOWER_CASE_QNAME("aria-valuemin"), ALL_NCNAME, false);
    public static final AttributeName DEFINITIONURL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("definitionurl"), SAME_LOWER_CASE_QNAME("definitionurl"), ALL_NCNAME, false);
    public static final AttributeName ARIA_DATATYPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-datatype"), SAME_LOWER_CASE_QNAME("aria-datatype"), ALL_NCNAME, false);
    public static final AttributeName ARIA_REQUIRED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-required"), SAME_LOWER_CASE_QNAME("aria-required"), ALL_NCNAME, false);
    public static final AttributeName COLOR_PROFILE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("color-profile"), SAME_LOWER_CASE_QNAME("color-profile"), ALL_NCNAME, false);
    public static final AttributeName COLUMNSPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("columnspacing"), SAME_LOWER_CASE_QNAME("columnspacing"), ALL_NCNAME, false);
    public static final AttributeName MOVABLELIMITS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("movablelimits"), SAME_LOWER_CASE_QNAME("movablelimits"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREPRINT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforeprint"), SAME_LOWER_CASE_QNAME("onbeforeprint"), ALL_NCNAME, false);
    public static final AttributeName ONCONTEXTMENU = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oncontextmenu"), SAME_LOWER_CASE_QNAME("oncontextmenu"), ALL_NCNAME, false);
    public static final AttributeName ONSELECTSTART = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onselectstart"), SAME_LOWER_CASE_QNAME("onselectstart"), ALL_NCNAME, false);
    public static final AttributeName ONLOSECAPTURE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onlosecapture"), SAME_LOWER_CASE_QNAME("onlosecapture"), ALL_NCNAME, false);
    public static final AttributeName ONERRORUPDATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onerrorupdate"), SAME_LOWER_CASE_QNAME("onerrorupdate"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREPASTE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforepaste"), SAME_LOWER_CASE_QNAME("onbeforepaste"), ALL_NCNAME, false);
    public static final AttributeName ONAFTERUPDATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onafterupdate"), SAME_LOWER_CASE_QNAME("onafterupdate"), ALL_NCNAME, false);
    public static final AttributeName LINETHICKNESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("linethickness"), SAME_LOWER_CASE_QNAME("linethickness"), ALL_NCNAME, false);
    public static final AttributeName SCRIPTMINSIZE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scriptminsize"), SAME_LOWER_CASE_QNAME("scriptminsize"), ALL_NCNAME, false);
    public static final AttributeName PRESERVEALPHA = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("preservealpha", "preserveAlpha"), SAME_LOWER_CASE_QNAME("preserveAlpha"), ALL_NCNAME, false);
    public static final AttributeName THINMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("thinmathspace"), SAME_LOWER_CASE_QNAME("thinmathspace"), ALL_NCNAME, false);
    public static final AttributeName UNICODE_RANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("unicode-range"), SAME_LOWER_CASE_QNAME("unicode-range"), ALL_NCNAME, false);
    public static final AttributeName VERT_ORIGIN_X = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("vert-origin-x"), SAME_LOWER_CASE_QNAME("vert-origin-x"), ALL_NCNAME, false);
    public static final AttributeName VERT_ORIGIN_Y = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("vert-origin-y"), SAME_LOWER_CASE_QNAME("vert-origin-y"), ALL_NCNAME, false);
    public static final AttributeName SPECIFICATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("specification"), SAME_LOWER_CASE_QNAME("specification"), ALL_NCNAME, false);
    public static final AttributeName XLINK_ARCROLE = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:arcrole", "arcrole"), SAME_LOWER_CASE_QNAME("xlink:arcrole"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName XLINK_ACTUATE = new AttributeName(NAMESPACE("http://www.w3.org/1999/xlink"), COLONIFIED_LOCAL("xlink:actuate", "actuate"), SAME_LOWER_CASE_QNAME("xlink:actuate"), new boolean[]{false, true, true, false}, false);
    public static final AttributeName ARIA_DISABLED  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-disabled "), SAME_LOWER_CASE_QNAME("aria-disabled "), ALL_NCNAME, false);
    public static final AttributeName ARIA_EXPANDED  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-expanded "), SAME_LOWER_CASE_QNAME("aria-expanded "), ALL_NCNAME, false);
    public static final AttributeName ARIA_SELECTED  = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-selected "), SAME_LOWER_CASE_QNAME("aria-selected "), ALL_NCNAME, false);
    public static final AttributeName ACCEPT_CHARSET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("accept-charset"), SAME_LOWER_CASE_QNAME("accept-charset"), ALL_NCNAME, false);
    public static final AttributeName BASELINE_SHIFT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("baseline-shift"), SAME_LOWER_CASE_QNAME("baseline-shift"), ALL_NCNAME, false);
    public static final AttributeName ALIGNMENTSCOPE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alignmentscope"), SAME_LOWER_CASE_QNAME("alignmentscope"), ALL_NCNAME, false);
    public static final AttributeName ARIA_MULTILINE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-multiline"), SAME_LOWER_CASE_QNAME("aria-multiline"), ALL_NCNAME, false);
    public static final AttributeName HORIZ_ORIGIN_X = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("horiz-origin-x"), SAME_LOWER_CASE_QNAME("horiz-origin-x"), ALL_NCNAME, false);
    public static final AttributeName HORIZ_ORIGIN_Y = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("horiz-origin-y"), SAME_LOWER_CASE_QNAME("horiz-origin-y"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREUNLOAD = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforeunload"), SAME_LOWER_CASE_QNAME("onbeforeunload"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREUPDATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforeupdate"), SAME_LOWER_CASE_QNAME("onbeforeupdate"), ALL_NCNAME, false);
    public static final AttributeName ONFILTERCHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onfilterchange"), SAME_LOWER_CASE_QNAME("onfilterchange"), ALL_NCNAME, false);
    public static final AttributeName ONROWSINSERTED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onrowsinserted"), SAME_LOWER_CASE_QNAME("onrowsinserted"), ALL_NCNAME, false);
    public static final AttributeName LETTER_SPACING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("letter-spacing"), SAME_LOWER_CASE_QNAME("letter-spacing"), ALL_NCNAME, false);
    public static final AttributeName MATHBACKGROUND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mathbackground"), SAME_LOWER_CASE_QNAME("mathbackground"), ALL_NCNAME, false);
    public static final AttributeName LIGHTING_COLOR = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("lighting-color"), SAME_LOWER_CASE_QNAME("lighting-color"), ALL_NCNAME, false);
    public static final AttributeName PRIMITIVEUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("primitiveunits", "primitiveUnits"), SAME_LOWER_CASE_QNAME("primitiveUnits"), ALL_NCNAME, false);
    public static final AttributeName POINTER_EVENTS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("pointer-events"), SAME_LOWER_CASE_QNAME("pointer-events"), ALL_NCNAME, false);
    public static final AttributeName TEXT_RENDERING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("text-rendering"), SAME_LOWER_CASE_QNAME("text-rendering"), ALL_NCNAME, false);
    public static final AttributeName SYSTEMLANGUAGE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("systemlanguage", "systemLanguage"), SAME_LOWER_CASE_QNAME("systemLanguage"), ALL_NCNAME, false);
    public static final AttributeName SUBSCRIPTSHIFT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("subscriptshift"), SAME_LOWER_CASE_QNAME("subscriptshift"), ALL_NCNAME, false);
    public static final AttributeName THICKMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("thickmathspace"), SAME_LOWER_CASE_QNAME("thickmathspace"), ALL_NCNAME, false);
    public static final AttributeName STROKE_OPACITY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-opacity"), SAME_LOWER_CASE_QNAME("stroke-opacity"), ALL_NCNAME, false);
    public static final AttributeName STROKE_LINECAP = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-linecap"), SAME_LOWER_CASE_QNAME("stroke-linecap"), ALL_NCNAME, false);
    public static final AttributeName ARIA_LABELLEDBY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-labelledby"), SAME_LOWER_CASE_QNAME("aria-labelledby"), ALL_NCNAME, false);
    public static final AttributeName ARIA_DROPEFFECT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-dropeffect"), SAME_LOWER_CASE_QNAME("aria-dropeffect"), ALL_NCNAME, false);
    public static final AttributeName ARIA_TEMPLATEID = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-templateid"), SAME_LOWER_CASE_QNAME("aria-templateid"), ALL_NCNAME, false);
    public static final AttributeName DIFFUSECONSTANT = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("diffuseconstant", "diffuseConstant"), SAME_LOWER_CASE_QNAME("diffuseConstant"), ALL_NCNAME, false);
    public static final AttributeName CONTENTEDITABLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("contenteditable"), SAME_LOWER_CASE_QNAME("contenteditable"), ALL_NCNAME, false);
    public static final AttributeName COLOR_RENDERING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("color-rendering"), SAME_LOWER_CASE_QNAME("color-rendering"), ALL_NCNAME, false);
    public static final AttributeName IMAGE_RENDERING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("image-rendering"), SAME_LOWER_CASE_QNAME("image-rendering"), ALL_NCNAME, false);
    public static final AttributeName ONCONTROLSELECT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("oncontrolselect"), SAME_LOWER_CASE_QNAME("oncontrolselect"), ALL_NCNAME, false);
    public static final AttributeName MEDIUMMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("mediummathspace"), SAME_LOWER_CASE_QNAME("mediummathspace"), ALL_NCNAME, false);
    public static final AttributeName ONDATAAVAILABLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondataavailable"), SAME_LOWER_CASE_QNAME("ondataavailable"), ALL_NCNAME, false);
    public static final AttributeName SHAPE_RENDERING = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("shape-rendering"), SAME_LOWER_CASE_QNAME("shape-rendering"), ALL_NCNAME, false);
    public static final AttributeName REPEAT_TEMPLATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("repeat-template"), SAME_LOWER_CASE_QNAME("repeat-template"), ALL_NCNAME, false);
    public static final AttributeName TEXT_DECORATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("text-decoration"), SAME_LOWER_CASE_QNAME("text-decoration"), ALL_NCNAME, false);
    public static final AttributeName STROKE_LINEJOIN = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-linejoin"), SAME_LOWER_CASE_QNAME("stroke-linejoin"), ALL_NCNAME, false);
    public static final AttributeName FONT_SIZE_ADJUST = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("font-size-adjust"), SAME_LOWER_CASE_QNAME("font-size-adjust"), ALL_NCNAME, false);
    public static final AttributeName ARIA_DESCRIBEDBY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-describedby"), SAME_LOWER_CASE_QNAME("aria-describedby"), ALL_NCNAME, false);
    public static final AttributeName CONTENTSTYLETYPE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("contentstyletype", "contentStyleType"), SAME_LOWER_CASE_QNAME("contentStyleType"), ALL_NCNAME, false);
    public static final AttributeName KERNELUNITLENGTH = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("kernelunitlength", "kernelUnitLength"), SAME_LOWER_CASE_QNAME("kernelUnitLength"), ALL_NCNAME, false);
    public static final AttributeName ONDATASETCHANGED = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondatasetchanged"), SAME_LOWER_CASE_QNAME("ondatasetchanged"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREACTIVATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforeactivate"), SAME_LOWER_CASE_QNAME("onbeforeactivate"), ALL_NCNAME, false);
    public static final AttributeName ONPROPERTYCHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onpropertychange"), SAME_LOWER_CASE_QNAME("onpropertychange"), ALL_NCNAME, false);
    public static final AttributeName MASKCONTENTUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("maskcontentunits", "maskContentUnits"), SAME_LOWER_CASE_QNAME("maskContentUnits"), ALL_NCNAME, false);
    public static final AttributeName PATTERNTRANSFORM = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("patterntransform", "patternTransform"), SAME_LOWER_CASE_QNAME("patternTransform"), ALL_NCNAME, false);
    public static final AttributeName REQUIREDFEATURES = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("requiredfeatures", "requiredFeatures"), SAME_LOWER_CASE_QNAME("requiredFeatures"), ALL_NCNAME, false);
    public static final AttributeName RENDERING_INTENT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("rendering-intent"), SAME_LOWER_CASE_QNAME("rendering-intent"), ALL_NCNAME, false);
    public static final AttributeName SUPERSCRIPTSHIFT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("superscriptshift"), SAME_LOWER_CASE_QNAME("superscriptshift"), ALL_NCNAME, false);
    public static final AttributeName STROKE_DASHARRAY = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-dasharray"), SAME_LOWER_CASE_QNAME("stroke-dasharray"), ALL_NCNAME, false);
    public static final AttributeName SPECULARCONSTANT = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("specularconstant", "specularConstant"), SAME_LOWER_CASE_QNAME("specularConstant"), ALL_NCNAME, false);
    public static final AttributeName SPECULAREXPONENT = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("specularexponent", "specularExponent"), SAME_LOWER_CASE_QNAME("specularExponent"), ALL_NCNAME, false);
    public static final AttributeName YCHANNELSELECTOR = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("ychannelselector", "yChannelSelector"), SAME_LOWER_CASE_QNAME("yChannelSelector"), ALL_NCNAME, false);
    public static final AttributeName XCHANNELSELECTOR = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("xchannelselector", "xChannelSelector"), SAME_LOWER_CASE_QNAME("xChannelSelector"), ALL_NCNAME, false);
    public static final AttributeName DOMINANT_BASELINE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("dominant-baseline"), SAME_LOWER_CASE_QNAME("dominant-baseline"), ALL_NCNAME, false);
    public static final AttributeName GRADIENTTRANSFORM = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("gradienttransform", "gradientTransform"), SAME_LOWER_CASE_QNAME("gradientTransform"), ALL_NCNAME, false);
    public static final AttributeName ARIA_AUTOCOMPLETE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-autocomplete"), SAME_LOWER_CASE_QNAME("aria-autocomplete"), ALL_NCNAME, false);
    public static final AttributeName ENABLE_BACKGROUND = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("enable-background"), SAME_LOWER_CASE_QNAME("enable-background"), ALL_NCNAME, false);
    public static final AttributeName CONTENTSCRIPTTYPE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("contentscripttype", "contentScriptType"), SAME_LOWER_CASE_QNAME("contentScriptType"), ALL_NCNAME, false);
    public static final AttributeName OVERLINE_POSITION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("overline-position"), SAME_LOWER_CASE_QNAME("overline-position"), ALL_NCNAME, false);
    public static final AttributeName ONBEFOREEDITFOCUS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbeforeeditfocus"), SAME_LOWER_CASE_QNAME("onbeforeeditfocus"), ALL_NCNAME, false);
    public static final AttributeName ONDATASETCOMPLETE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("ondatasetcomplete"), SAME_LOWER_CASE_QNAME("ondatasetcomplete"), ALL_NCNAME, false);
    public static final AttributeName ONBEFORDEACTIVATE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onbefordeactivate"), SAME_LOWER_CASE_QNAME("onbefordeactivate"), ALL_NCNAME, false);
    public static final AttributeName LIMITINGCONEANGLE = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("limitingconeangle", "limitingConeAngle"), SAME_LOWER_CASE_QNAME("limitingConeAngle"), ALL_NCNAME, false);
    public static final AttributeName STROKE_MITERLIMIT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-miterlimit"), SAME_LOWER_CASE_QNAME("stroke-miterlimit"), ALL_NCNAME, false);
    public static final AttributeName STROKE_DASHOFFSET = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("stroke-dashoffset"), SAME_LOWER_CASE_QNAME("stroke-dashoffset"), ALL_NCNAME, false);
    public static final AttributeName VERYTHINMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("verythinmathspace"), SAME_LOWER_CASE_QNAME("verythinmathspace"), ALL_NCNAME, false);
    public static final AttributeName ALIGNMENT_BASELINE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("alignment-baseline"), SAME_LOWER_CASE_QNAME("alignment-baseline"), ALL_NCNAME, false);
    public static final AttributeName OVERLINE_THICKNESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("overline-thickness"), SAME_LOWER_CASE_QNAME("overline-thickness"), ALL_NCNAME, false);
    public static final AttributeName ONREADYSTATECHANGE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("onreadystatechange"), SAME_LOWER_CASE_QNAME("onreadystatechange"), ALL_NCNAME, false);
    public static final AttributeName REQUIREDEXTENSIONS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("requiredextensions", "requiredExtensions"), SAME_LOWER_CASE_QNAME("requiredExtensions"), ALL_NCNAME, false);
    public static final AttributeName UNDERLINE_POSITION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("underline-position"), SAME_LOWER_CASE_QNAME("underline-position"), ALL_NCNAME, false);
    public static final AttributeName VERYTHICKMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("verythickmathspace"), SAME_LOWER_CASE_QNAME("verythickmathspace"), ALL_NCNAME, false);
    public static final AttributeName COLOR_INTERPOLATION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("color-interpolation"), SAME_LOWER_CASE_QNAME("color-interpolation"), ALL_NCNAME, false);
    public static final AttributeName PATTERNCONTENTUNITS = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("patterncontentunits", "patternContentUnits"), SAME_LOWER_CASE_QNAME("patternContentUnits"), ALL_NCNAME, false);
    public static final AttributeName PRESERVEASPECTRATIO = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("preserveaspectratio", "preserveAspectRatio"), SAME_LOWER_CASE_QNAME("preserveAspectRatio"), ALL_NCNAME, false);
    public static final AttributeName UNDERLINE_THICKNESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("underline-thickness"), SAME_LOWER_CASE_QNAME("underline-thickness"), ALL_NCNAME, false);
    public static final AttributeName ARIA_MULTISELECTABLE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-multiselectable"), SAME_LOWER_CASE_QNAME("aria-multiselectable"), ALL_NCNAME, false);
    public static final AttributeName SCRIPTSIZEMULTIPLIER = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("scriptsizemultiplier"), SAME_LOWER_CASE_QNAME("scriptsizemultiplier"), ALL_NCNAME, false);
    public static final AttributeName ARIA_ACTIVEDESCENDANT = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("aria-activedescendant"), SAME_LOWER_CASE_QNAME("aria-activedescendant"), ALL_NCNAME, false);
    public static final AttributeName VERYVERYTHINMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("veryverythinmathspace"), SAME_LOWER_CASE_QNAME("veryverythinmathspace"), ALL_NCNAME, false);
    public static final AttributeName STRIKETHROUGH_POSITION = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("strikethrough-position"), SAME_LOWER_CASE_QNAME("strikethrough-position"), ALL_NCNAME, false);
    public static final AttributeName VERYVERYTHICKMATHSPACE = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("veryverythickmathspace"), SAME_LOWER_CASE_QNAME("veryverythickmathspace"), ALL_NCNAME, false);
    public static final AttributeName STRIKETHROUGH_THICKNESS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("strikethrough-thickness"), SAME_LOWER_CASE_QNAME("strikethrough-thickness"), ALL_NCNAME, false);
    public static final AttributeName EXTERNALRESOURCESREQUIRED = new AttributeName(ALL_NO_NS, CAMEL_CASE_LOCAL("externalresourcesrequired", "externalResourcesRequired"), SAME_LOWER_CASE_QNAME("externalResourcesRequired"), ALL_NCNAME, false);
    public static final AttributeName GLYPH_ORIENTATION_VERTICAL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("glyph-orientation-vertical"), SAME_LOWER_CASE_QNAME("glyph-orientation-vertical"), ALL_NCNAME, false);
    public static final AttributeName COLOR_INTERPOLATION_FILTERS = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("color-interpolation-filters"), SAME_LOWER_CASE_QNAME("color-interpolation-filters"), ALL_NCNAME, false);
    public static final AttributeName GLYPH_ORIENTATION_HORIZONTAL = new AttributeName(ALL_NO_NS, SAME_LOWER_CASE_LOCAL("glyph-orientation-horizontal"), SAME_LOWER_CASE_QNAME("glyph-orientation-horizontal"), ALL_NCNAME, false);
    private final static AttributeName[] ATTRIBUTE_NAMES = {
    V_MATHEMATICAL,
    V_IDEOGRAPHIC,
    V_ALPHABETIC,
    V_HANGING,
    X_HEIGHT,
    Y2,
    Y1,
    X2,
    X1,
    U2,
    U1,
    K4,
    K2,
    K3,
    K1,
    G2,
    G1,
    D,
    K,
    R,
    X,
    Y,
    Z,
    BY,
    CX,
    CY,
    DX,
    DY,
    FX,
    FY,
    ID,
    IN,
    RT,
    RX,
    RY,
    TO,
    ALT,
    DIR,
    DUR,
    END,
    FOR,
    IN2,
    LOW,
    MIN,
    MAX,
    REL,
    REV,
    SRC,
    AXIS,
    ABBR,
    BASE,
    BIAS,
    BBOX,
    END ,
    CITE,
    CODE,
    COLS,
    CLIP,
    CHAR,
    DATA,
    EDGE,
    FACE,
    FILL,
    FROM,
    FORM,
    HREF,
    HIGH,
    ICON,
    LANG,
    LINK,
    LIST,
    MODE,
    MASK,
    NAME,
    OPEN,
    PING,
    PATH,
    ROLE,
    ROWS,
    REFX,
    REFY,
    SIZE,
    SEED,
    SPAN,
    STEP,
    TYPE,
    TEXT,
    WHEN,
    WRAP,
    XREF,
    CLOSE,
    CLEAR,
    CLASS,
    ALINK,
    ALIGN,
    FRAME,
    BEGIN,
    ASYNC,
    DEFER,
    DEPTH,
    COLOR,
    FENCE,
    LOCAL,
    INDEX,
    OTHER,
    ORDER,
    ONCUT,
    ISMAP,
    MEDIA,
    LABEL,
    ONEND,
    NARGS,
    SLOPE,
    SHAPE,
    SCALE,
    SCOPE,
    VLINK,
    WIDTH,
    STEMH,
    STYLE,
    VALUE,
    TITLE,
    START,
    STEMV,
    RULES,
    SPEED,
    XMLNS,
    ACTION,
    ACCEPT,
    ACCENT,
    ALTIMG,
    FORMAT,
    FILTER,
    ASCENT,
    CURSOR,
    BORDER,
    COORDS,
    LOWSRC,
    LQUOTE,
    HEIGHT,
    LSPACE,
    NOHREF,
    NOWRAP,
    HIDDEN,
    ORIENT,
    ORIGIN,
    ONCOPY,
    ONDROP,
    ONHELP,
    ONBLUR,
    ONSTOP,
    ONZOOM,
    METHOD,
    ONMOVE,
    ONLOAD,
    ONDRAG,
    MACROS,
    HSPACE,
    OFFSET,
    OBJECT,
    USEMAP,
    RADIUS,
    SCHEME,
    SCOPED,
    REPEAT,
    RESULT,
    VSPACE,
    TARGET,
    VALUE_,
    PROMPT,
    POSTER,
    POINTS,
    WIDTHS,
    VALUES,
    ROTATE,
    STRING,
    STROKE,
    RQUOTE,
    VALIGN,
    RSPACE,
    CLASSID,
    CLOSURE,
    BGCOLOR,
    CHECKED,
    CHAROFF,
    CHARSET,
    ALTTEXT,
    DATASRC,
    DATAFLD,
    DEFAULT,
    DESCENT,
    DECLARE,
    ARCHIVE,
    DIVISOR,
    DISPLAY,
    ENCTYPE,
    COLSPAN,
    AZIMUTH,
    CONTENT,
    COMPACT,
    HANGING,
    HEADERS,
    NOSHADE,
    KERNING,
    OPTIMUM,
    OPACITY,
    LARGEOP,
    ONKEYUP,
    ONERROR,
    ONFOCUS,
    ONABORT,
    ONINPUT,
    ONSTART,
    ONRESET,
    ONCLICK,
    ONBEGIN,
    ONPASTE,
    MAXSIZE,
    MINSIZE,
    PATTERN,
    RESTART,
    SANDBOX,
    REPLACE,
    VIEWBOX,
    TARGETX,
    TARGETY,
    PROFILE,
    START  ,
    SUMMARY,
    ROWSPAN,
    STANDBY,
    UNICODE,
    SPACING,
    VERSION,
    ADDITIVE,
    FONTSIZE,
    BASELINE,
    CALCMODE,
    BEVELLED,
    EXPONENT,
    EDGEMODE,
    GLYPHREF,
    DATETIME,
    DISABLED,
    ENCODING,
    CODETYPE,
    CODEBASE,
    LONGDESC,
    MULTIPLE,
    KEYTIMES,
    NORESIZE,
    NOTATION,
    OVERFLOW,
    OPERATOR,
    ONREPEAT,
    ONSELECT,
    ONSUBMIT,
    ONFINISH,
    LANGUAGE,
    ONSCROLL,
    ONBOUNCE,
    ONCHANGE,
    ONUNLOAD,
    ONRESIZE,
    HREFLANG,
    MANIFEST,
    MATHSIZE,
    LOOPEND ,
    PANOSE_1,
    SEAMLESS,
    SELECTED,
    READONLY,
    REQUIRED,
    TABINDEX,
    TEMPLATE,
    ROWALIGN,
    ROWLINES,
    STRETCHY,
    XML_BASE,
    XML_LANG,
    CLIP_PATH,
    CLIP_RULE,
    ACCESSKEY,
    DRAGGABLE,
    EQUALROWS,
    FONT_SIZE,
    FONTSTYLE,
    AMPLITUDE,
    AUTOFOCUS,
    FILTERRES,
    FILL_RULE,
    CONTROLS ,
    ARIA_OWNS,
    ARIA_SORT,
    ARIA_LIVE,
    ELEVATION,
    DIRECTION,
    KEYPOINTS,
    HIDEFOCUS,
    INTERCEPT,
    INPUTMODE,
    ONMOUSEUP,
    ONROWEXIT,
    ONFOCUSIN,
    ONKEYDOWN,
    ONDRAGEND,
    ONMESSAGE,
    ONINVALID,
    ONMOVEEND,
    MATHCOLOR,
    MASKUNITS,
    MAXLENGTH,
    LINEBREAK,
    TRANSFORM,
    SEPARATOR,
    SCROLLING,
    SELECTION,
    REPEATDUR,
    POINTSATY,
    POINTSATX,
    POINTSATZ,
    SYMMETRIC,
    VALUETYPE,
    AUTOPLAY  ,
    XML_SPACE,
    ARIA_GRAB ,
    ARIA_BUSY ,
    ACCUMULATE,
    ACTIONTYPE,
    FONT_STYLE,
    ALPHABETIC,
    BACKGROUND,
    FONTWEIGHT,
    FONTFAMILY,
    CAP_HEIGHT,
    GROUPALIGN,
    AUTOSUBMIT,
    GLYPH_NAME,
    ARIA_LEVEL,
    COLUMNSPAN,
    KEYSPLINES,
    NUMOCTAVES,
    ONROWENTER,
    ONDRAGDROP,
    ONKEYPRESS,
    ONDRAGOVER,
    ONFOCUSOUT,
    ONMOUSEOUT,
    ONDBLCLICK,
    ONACTIVATE,
    PLAYCOUNT ,
    IRRELEVANT,
    MARKER_END,
    MARKER_MID,
    HTTP_EQUIV,
    LOOPSTART ,
    OCCURRENCE,
    PATHLENGTH,
    RADIOGROUP,
    SEPARATORS,
    REPEAT_MAX,
    REPEAT_MIN,
    VIEWTARGET,
    VISIBILITY,
    TEXTLENGTH,
    ROWSPACING,
    STOP_COLOR,
    VERT_ADV_Y,
    XLINK_ROLE,
    XLINK_TYPE,
    XLINK_HREF,
    XLINK_SHOW,
    ZOOMANDPAN,
    ACCENTUNDER,
    FRAMEBORDER,
    FONT_WEIGHT,
    CELLPADDING,
    CELLSPACING,
    BASEPROFILE,
    FONT_FAMILY,
    FILTERUNITS,
    ARIA_SECRET,
    ARABIC_FORM,
    ARIA_FLOWTO,
    FLOOD_COLOR,
    ARIA_ATOMIC,
    COLUMNWIDTH,
    COLUMNALIGN,
    COLUMNLINES,
    CONTEXTMENU,
    IDEOGRAPHIC,
    ORIENTATION,
    ONDRAGENTER,
    ONMOUSEOVER,
    ONDRAGSTART,
    ONFORMINPUT,
    ONBEFORECUT,
    ONMOVESTART,
    HORIZ_ADV_X,
    ONMOUSEDOWN,
    ONMOUSEMOVE,
    ONDRAGLEAVE,
    MARKERUNITS,
    MATHVARIANT,
    MARGINWIDTH,
    MARKERWIDTH,
    SCRIPTLEVEL,
    SCROLLDELAY,
    REPEATCOUNT,
    TABLEVALUES,
    TEXT_ANCHOR,
    STITCHTILES,
    STARTOFFSET,
    XLINK_TITLE,
    ARIA_HIDDEN ,
    XMLNS_XLINK,
    EQUALCOLUMNS,
    FONT_STRETCH,
    FONT_VARIANT,
    FRAMESPACING,
    DATAFORMATAS,
    FILL_OPACITY,
    AUTOCOMPLETE,
    ARIA_CHANNEL,
    ARIA_SETSIZE,
    DISPLAYSTYLE,
    KERNELMATRIX,
    ONBEFORECOPY,
    ONMOUSEENTER,
    ONAFTERPRINT,
    ONMOUSEWHEEL,
    ONMOUSELEAVE,
    ONDEACTIVATE,
    ONCELLCHANGE,
    ONFORMCHANGE,
    ONROWSDELETE,
    LENGTHADJUST,
    MARGINHEIGHT,
    MARKERHEIGHT,
    MARKER_START,
    MATHEMATICAL,
    PATTERNUNITS,
    WORD_SPACING,
    REPEAT_START,
    WRITING_MODE,
    STROKE_WIDTH,
    STDDEVIATION,
    UNICODE_BIDI,
    STOP_OPACITY,
    UNITS_PER_EM,
    SURFACESCALE,
    UNSELECTABLE,
    SPREADMETHOD,
    ARIA_CHECKED ,
    ARIA_PRESSED ,
    ARIA_INVALID ,
    ACCENT_HEIGHT,
    CLIPPATHUNITS,
    BASEFREQUENCY,
    GRADIENTUNITS,
    ATTRIBUTETYPE,
    ATTRIBUTENAME,
    ARIA_READONLY,
    ARIA_VALUEMAX,
    ARIA_HASPOPUP,
    ARIA_CONTROLS,
    ARIA_RELEVANT,
    ARIA_POSINSET,
    ARIA_VALUENOW,
    FLOOD_OPACITY,
    ARIA_VALUEMIN,
    DEFINITIONURL,
    ARIA_DATATYPE,
    ARIA_REQUIRED,
    COLOR_PROFILE,
    COLUMNSPACING,
    MOVABLELIMITS,
    ONBEFOREPRINT,
    ONCONTEXTMENU,
    ONSELECTSTART,
    ONLOSECAPTURE,
    ONERRORUPDATE,
    ONBEFOREPASTE,
    ONAFTERUPDATE,
    LINETHICKNESS,
    SCRIPTMINSIZE,
    PRESERVEALPHA,
    THINMATHSPACE,
    UNICODE_RANGE,
    VERT_ORIGIN_X,
    VERT_ORIGIN_Y,
    SPECIFICATION,
    XLINK_ARCROLE,
    XLINK_ACTUATE,
    ARIA_DISABLED ,
    ARIA_EXPANDED ,
    ARIA_SELECTED ,
    ACCEPT_CHARSET,
    BASELINE_SHIFT,
    ALIGNMENTSCOPE,
    ARIA_MULTILINE,
    HORIZ_ORIGIN_X,
    HORIZ_ORIGIN_Y,
    ONBEFOREUNLOAD,
    ONBEFOREUPDATE,
    ONFILTERCHANGE,
    ONROWSINSERTED,
    LETTER_SPACING,
    MATHBACKGROUND,
    LIGHTING_COLOR,
    PRIMITIVEUNITS,
    POINTER_EVENTS,
    TEXT_RENDERING,
    SYSTEMLANGUAGE,
    SUBSCRIPTSHIFT,
    THICKMATHSPACE,
    STROKE_OPACITY,
    STROKE_LINECAP,
    ARIA_LABELLEDBY,
    ARIA_DROPEFFECT,
    ARIA_TEMPLATEID,
    DIFFUSECONSTANT,
    CONTENTEDITABLE,
    COLOR_RENDERING,
    IMAGE_RENDERING,
    ONCONTROLSELECT,
    MEDIUMMATHSPACE,
    ONDATAAVAILABLE,
    SHAPE_RENDERING,
    REPEAT_TEMPLATE,
    TEXT_DECORATION,
    STROKE_LINEJOIN,
    FONT_SIZE_ADJUST,
    ARIA_DESCRIBEDBY,
    CONTENTSTYLETYPE,
    KERNELUNITLENGTH,
    ONDATASETCHANGED,
    ONBEFOREACTIVATE,
    ONPROPERTYCHANGE,
    MASKCONTENTUNITS,
    PATTERNTRANSFORM,
    REQUIREDFEATURES,
    RENDERING_INTENT,
    SUPERSCRIPTSHIFT,
    STROKE_DASHARRAY,
    SPECULARCONSTANT,
    SPECULAREXPONENT,
    YCHANNELSELECTOR,
    XCHANNELSELECTOR,
    DOMINANT_BASELINE,
    GRADIENTTRANSFORM,
    ARIA_AUTOCOMPLETE,
    ENABLE_BACKGROUND,
    CONTENTSCRIPTTYPE,
    OVERLINE_POSITION,
    ONBEFOREEDITFOCUS,
    ONDATASETCOMPLETE,
    ONBEFORDEACTIVATE,
    LIMITINGCONEANGLE,
    STROKE_MITERLIMIT,
    STROKE_DASHOFFSET,
    VERYTHINMATHSPACE,
    ALIGNMENT_BASELINE,
    OVERLINE_THICKNESS,
    ONREADYSTATECHANGE,
    REQUIREDEXTENSIONS,
    UNDERLINE_POSITION,
    VERYTHICKMATHSPACE,
    COLOR_INTERPOLATION,
    PATTERNCONTENTUNITS,
    PRESERVEASPECTRATIO,
    UNDERLINE_THICKNESS,
    ARIA_MULTISELECTABLE,
    SCRIPTSIZEMULTIPLIER,
    ARIA_ACTIVEDESCENDANT,
    VERYVERYTHINMATHSPACE,
    STRIKETHROUGH_POSITION,
    VERYVERYTHICKMATHSPACE,
    STRIKETHROUGH_THICKNESS,
    EXTERNALRESOURCESREQUIRED,
    GLYPH_ORIENTATION_VERTICAL,
    COLOR_INTERPOLATION_FILTERS,
    GLYPH_ORIENTATION_HORIZONTAL,
    };
    private final static int[] ATTRIBUTE_HASHES = {
    -497097476,
    -463928234,
    -430340652,
    -329569880,
    -290279069,
    -89718,
    -89621,
    -88693,
    -88598,
    -85626,
    -85529,
    -75426,
    -75368,
    -75329,
    -75271,
    -71276,
    -71179,
    1156,
    1387,
    1618,
    1816,
    1849,
    1882,
    68408,
    69402,
    69433,
    70429,
    70462,
    72479,
    72508,
    74892,
    75206,
    84615,
    84747,
    84776,
    86500,
    3199700,
    3296183,
    3297079,
    3314176,
    3362293,
    3394138,
    3561972,
    3586978,
    3596468,
    3748159,
    3758373,
    3771031,
    135804309,
    135870610,
    136505636,
    136914358,
    137103451,
    137420996,
    137524325,
    137597093,
    137933495,
    137950994,
    137991568,
    138474854,
    139644547,
    140684576,
    140939883,
    141003336,
    141020456,
    142747599,
    142917409,
    144144486,
    147037156,
    147135200,
    147515449,
    148082859,
    148236577,
    149067688,
    150341504,
    151198456,
    151287161,
    153334452,
    153672678,
    153909611,
    153942376,
    154292213,
    154301750,
    154539676,
    154748674,
    155412338,
    155846369,
    158827672,
    158938822,
    159524831,
    168282346,
    168649791,
    168683832,
    169941728,
    170114851,
    170825652,
    171943530,
    171972855,
    173106518,
    173885260,
    175761820,
    175799491,
    176571618,
    178275607,
    179143815,
    179651841,
    180752891,
    180938018,
    180942343,
    181282252,
    181295979,
    182496341,
    185058730,
    186049646,
    186815333,
    186893733,
    189864672,
    190580160,
    190863165,
    191303794,
    191496199,
    191619535,
    191786209,
    191846179,
    191972353,
    192312629,
    196176473,
    201748388,
    201982258,
    202012972,
    203876282,
    204158512,
    205636554,
    206207276,
    207620962,
    208152020,
    209229450,
    209735524,
    210482656,
    210928352,
    211000500,
    212042382,
    212435605,
    212813250,
    213293350,
    213616355,
    213943794,
    214198932,
    214212901,
    214263508,
    214268867,
    214632938,
    214658884,
    214842874,
    214880750,
    214946438,
    215087763,
    215194804,
    216425312,
    217496878,
    218654247,
    220301234,
    220341542,
    220402249,
    221451570,
    221476670,
    221486260,
    222504692,
    222675754,
    222773822,
    222843861,
    222917148,
    223475661,
    224477994,
    224624801,
    224824253,
    224893438,
    225162720,
    225431460,
    225680564,
    235275111,
    235392933,
    235501954,
    236352623,
    236404002,
    236860070,
    237975026,
    239639285,
    239820870,
    240271927,
    240351653,
    240814958,
    241081186,
    241192189,
    241549789,
    242124398,
    242733826,
    242807099,
    242929138,
    242935922,
    244009773,
    244443618,
    245617258,
    246057700,
    246646567,
    247001129,
    247533217,
    247764671,
    247814785,
    247832683,
    247884082,
    247920095,
    247926134,
    247932913,
    248106598,
    248190317,
    248496885,
    249265209,
    251272249,
    252583410,
    255004022,
    255118599,
    255565678,
    256137812,
    256205473,
    256238241,
    256880876,
    257631815,
    258223023,
    258376450,
    258517835,
    258879731,
    259495977,
    259917881,
    268864417,
    270670951,
    271317153,
    272253155,
    272327681,
    272575649,
    273027171,
    273068545,
    273409185,
    275469569,
    275774753,
    275944097,
    275946913,
    276905633,
    277505859,
    278933731,
    279173287,
    279342497,
    279435585,
    280844867,
    281490721,
    281496865,
    281501729,
    281558945,
    281701923,
    281768161,
    281983427,
    281993091,
    282027553,
    282056871,
    282057185,
    282246689,
    282835815,
    283186563,
    284115111,
    287436577,
    288016135,
    288657567,
    289092547,
    289678849,
    291124321,
    291937697,
    292034179,
    292131297,
    297405153,
    297500385,
    302052414,
    302388924,
    302830823,
    302944841,
    303086975,
    304225341,
    304236128,
    304841214,
    306217064,
    306266900,
    306864572,
    307280092,
    307567265,
    307685821,
    308181346,
    308500726,
    309202096,
    312505753,
    312575464,
    312938800,
    313492600,
    314880165,
    315036445,
    315314801,
    315333758,
    315518053,
    315522471,
    315524694,
    315546802,
    315649995,
    315658808,
    316523432,
    317551921,
    319482418,
    320876050,
    321031457,
    321907888,
    321930518,
    323777510,
    323810278,
    323875814,
    324617682,
    325719730,
    330716976,
    330975972,
    331131004,
    331138619,
    335713698,
    335778212,
    337790522,
    337949895,
    338327975,
    338351537,
    338445418,
    338895712,
    339351741,
    339964342,
    340201575,
    341489291,
    343397277,
    346042584,
    347173573,
    348398230,
    348416646,
    348458280,
    348471821,
    348529143,
    348566003,
    348773857,
    349106467,
    349468497,
    349637051,
    349899627,
    349902443,
    350122687,
    350311957,
    350406742,
    353174179,
    354420578,
    354474343,
    355800303,
    356398319,
    356676327,
    356881327,
    358466979,
    358759474,
    359414145,
    359826378,
    364253758,
    364273912,
    364361092,
    364846815,
    367396230,
    369710337,
    371537684,
    371906023,
    371944053,
    371944055,
    371978423,
    371999779,
    373359720,
    374781453,
    375056684,
    375091873,
    375142794,
    375195105,
    376734983,
    376948898,
    377045063,
    377114091,
    377864472,
    381394107,
    382026316,
    382071513,
    382082294,
    382083613,
    382100962,
    382118947,
    382338143,
    382450162,
    382615035,
    382635239,
    382788136,
    382954110,
    383607427,
    383607695,
    388441172,
    388870536,
    389229357,
    390178282,
    391161869,
    393031885,
    393097907,
    397807941,
    398235882,
    398289656,
    403757368,
    405076875,
    405458215,
    405772965,
    406902278,
    407110189,
    407602344,
    408597888,
    408832381,
    409728889,
    413472146,
    415297636,
    415625880,
    415635687,
    415953086,
    416169011,
    416186804,
    416209996,
    416213458,
    416277684,
    416464063,
    416486115,
    416486383,
    416512818,
    417289775,
    419464296,
    422742061,
    422795702,
    423507111,
    425813125,
    425946226,
    426258482,
    426366253,
    426387686,
    426476001,
    426766796,
    427232406,
    431796453,
    431796477,
    431796578,
    436887910,
    437089663,
    438684738,
    439742564,
    440899582,
    440918244,
    441454977,
    441492971,
    441775681,
    441782716,
    441882744,
    441890336,
    441916811,
    442094886,
    442205675,
    442382369,
    442393466,
    442414599,
    443704629,
    443772077,
    446210478,
    449185973,
    449248418,
    449251769,
    449715872,
    449754359,
    449766374,
    449769969,
    450968891,
    455334818,
    458016522,
    459681790,
    460209361,
    460456135,
    460488903,
    460545086,
    464917054,
    464942073,
    465350882,
    465350890,
    465350906,
    470439907,
    472078501,
    472277996,
    475928930,
    483002194,
    483034962,
    483275883,
    483320227,
    483323724,
    483336842,
    484109413,
    484141292,
    484556762,
    491090596,
    491352592,
    492448828,
    492558707,
    492973100,
    493235516,
    493479215,
    493695081,
    508569803,
    508996840,
    509510906,
    509757922,
    510805255,
    510881057,
    514562868,
    516311203,
    516611750,
    516874452,
    521641332,
    524009902,
    526297146,
    526520806,
    539671460,
    542124235,
    544355954,
    548234024,
    550388859,
    550429105,
    550471953,
    550539818,
    554671264,
    556839903,
    556976626,
    560138427,
    560575070,
    561928482,
    561928632,
    562671116,
    563719692,
    570579875,
    574285996,
    576614626,
    577641058,
    577910386,
    581655033,
    583394792,
    583958585,
    583983537,
    585781883,
    594481573,
    594485866,
    595311269,
    606477091,
    614836323,
    617589827,
    623942755,
    627797159,
    628865701,
    645392679,
    654345320,
    659276679,
    660970813,
    677253135,
    690497469,
    710318186,
    729528999,
    761401272,
    763083431,
    795672098,
    842483674,
    876847136,
    913906855,
    943956679,
    };

}
