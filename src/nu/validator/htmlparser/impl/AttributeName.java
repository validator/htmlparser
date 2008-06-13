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
import nu.validator.htmlparser.annotation.NoLength;
import nu.validator.htmlparser.annotation.NsUri;
import nu.validator.htmlparser.annotation.QName;

public final class AttributeName
// Uncomment to regenerate
// implements Comparable<AttributeName>
{

    private static final @NsUri String[] ALL_NO_NS = { "", "", "", "" };

    private static final boolean[] ALL_NCNAME = { true, true, true, true };

    private static final boolean[] ALL_NO_NCNAME = { false, false, false, false };

    private static @NsUri String[] NAMESPACE(@Local String ns) {
        return new String[] { "", ns, ns, "" };
    }

    private static @Local String[] CAMEL_CASE_LOCAL(@Local String name,
            @Local String camel) {
        return new String[] { name, name, camel, name };
    }

    private static @Local String[] COLONIFIED_LOCAL(@Local String name,
            @Local String suffix) {
        return new String[] { name, suffix, suffix, name };
    }

    private static @Local String[] SAME_LOWER_CASE_LOCAL(@Local String name) {
        return new String[] { name, name, name, name };
    }

    private static @QName String[] SAME_LOWER_CASE_QNAME(@Local String name) {
        return new String[] { name, name, name, name };
    }

    static AttributeName nameByBuffer(char[] buf, int length,
            boolean checkNcName) {
        int hash = AttributeName.bufToHash(buf, length);
        int index = Arrays.binarySearch(AttributeName.ATTRIBUTE_HASHES, hash);
        if (index < 0) {
            return AttributeName.create(StringUtil.localNameFromBuffer(buf,
                    length), checkNcName);
        } else {
            AttributeName rv = AttributeName.ATTRIBUTE_NAMES[index];
            @Local String name = rv.getQName(AttributeName.HTML);
            if (name.length() != length) {
                return AttributeName.create(StringUtil.localNameFromBuffer(buf,
                        length), checkNcName);
            }
            for (int i = 0; i < length; i++) {
                if (name.charAt(i) != buf[i]) {
                    return AttributeName.create(StringUtil.localNameFromBuffer(
                            buf, length), checkNcName);
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
        int hash2 = 0;
        int hash = len;
        hash <<= 5;
        hash += buf[0] - 0x60;
        int j = len;
        for (int i = 0; i < 4 && j > 0; i++) {
            j--;
            hash <<= 5;
            hash += buf[j] - 0x60;
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

    // XXX convert to bitfield
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
    private AttributeName(@IdType String type, @NsUri String[] uri,
            @Local String[] local, @QName String[] qName, boolean[] ncname,
            boolean xmlns) {
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

    private static AttributeName create(@Local String name, boolean checkNcName) {
        boolean ncName = true;
        boolean xmlns = name.startsWith("xmlns:");
        if (checkNcName) {
            if (xmlns) {
                ncName = false;
            } else {
                ncName = NCName.isNCName(name);
            }
        }
        return new AttributeName(AttributeName.ALL_NO_NS,
                AttributeName.SAME_LOWER_CASE_LOCAL(name),
                AttributeName.SAME_LOWER_CASE_QNAME(name),
                (ncName ? AttributeName.ALL_NCNAME
                        : AttributeName.ALL_NO_NCNAME), xmlns);
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

    boolean isBoolean() {
        return this == AttributeName.ACTIVE || this == AttributeName.ASYNC
                || this == AttributeName.AUTOFOCUS
                || this == AttributeName.AUTOSUBMIT
                || this == AttributeName.CHECKED
                || this == AttributeName.COMPACT
                || this == AttributeName.DECLARE
                || this == AttributeName.DEFAULT || this == AttributeName.DEFER
                || this == AttributeName.DISABLED
                || this == AttributeName.ISMAP
                || this == AttributeName.MULTIPLE
                || this == AttributeName.NOHREF
                || this == AttributeName.NORESIZE
                || this == AttributeName.NOSHADE
                || this == AttributeName.NOWRAP
                || this == AttributeName.READONLY
                || this == AttributeName.REQUIRED
                || this == AttributeName.SELECTED;
    }

    boolean equalsAnother(AttributeName another) {
        return this.getLocal(AttributeName.HTML) == another.getLocal(AttributeName.HTML);
    }

    boolean isCaseFolded() {
        return this == AttributeName.ACTIVE || this == AttributeName.ALIGN
                || this == AttributeName.ASYNC
                || this == AttributeName.AUTOCOMPLETE
                || this == AttributeName.AUTOFOCUS
                || this == AttributeName.AUTOSUBMIT
                || this == AttributeName.CHECKED || this == AttributeName.CLEAR
                || this == AttributeName.COMPACT
                || this == AttributeName.DATAFORMATAS
                || this == AttributeName.DECLARE
                || this == AttributeName.DEFAULT || this == AttributeName.DEFER
                || this == AttributeName.DIR || this == AttributeName.DISABLED
                || this == AttributeName.ENCTYPE || this == AttributeName.FRAME
                || this == AttributeName.ISMAP || this == AttributeName.METHOD
                || this == AttributeName.MULTIPLE
                || this == AttributeName.NOHREF
                || this == AttributeName.NORESIZE
                || this == AttributeName.NOSHADE
                || this == AttributeName.NOWRAP
                || this == AttributeName.READONLY
                || this == AttributeName.REPLACE
                || this == AttributeName.REQUIRED
                || this == AttributeName.RULES || this == AttributeName.SCOPE
                || this == AttributeName.SCROLLING
                || this == AttributeName.SELECTED
                || this == AttributeName.SHAPE || this == AttributeName.STEP
                || this == AttributeName.TYPE || this == AttributeName.VALIGN
                || this == AttributeName.VALUETYPE;
    }

    // START CODE ONLY USED FOR GENERATING CODE uncomment to regenerate

    //    
    // public int compareTo(AttributeName other) {
    // int thisHash = this.hash();
    // int otherHash = other.hash();
    // if (thisHash < otherHash) {
    // return -1;
    // } else if (thisHash == otherHash) {
    // return 0;
    // } else {
    // return 1;
    // }
    // }

    //
    // /**
    // * @see java.lang.Object#toString()
    // */
    // @Override public String toString() {
    // return "(" + ("ID" == type ? "\"ID\", " : "") + formatNs() + ", "
    // + formatLocal() + ", " + formatQname() + ", " + formatNcname()
    // + ", " + (xmlns ? "true" : "false") + ")";
    // }
    //
    // private String formatQname() {
    // for (int i = 1; i < qName.length; i++) {
    // if (qName[0] != qName[i]) {
    // return "new String[]{\"" + qName[0] + "\", \"" + qName[1]
    // + "\", \"" + qName[2] + "\", \"" + qName[3] + "\"}";
    // }
    // }
    // return "SAME_LOWER_CASE_QNAME(\"" + qName[0] + "\")";
    // }
    //
    // private String formatLocal() {
    // if (local[0] == local[1] && local[0] == local[3]
    // && local[0] != local[2]) {
    // return "CAMEL_CASE_LOCAL(\"" + local[0] + "\", \"" + local[2]
    // + "\")";
    // }
    // if (local[0] == local[3] && local[1] == local[2]
    // && local[0] != local[1]) {
    // return "COLONIFIED_LOCAL(\"" + local[0] + "\", \"" + local[1]
    // + "\")";
    // }
    // for (int i = 1; i < local.length; i++) {
    // if (local[0] != local[i]) {
    // return "new String[]{\"" + local[0] + "\", \"" + local[1]
    // + "\", \"" + local[2] + "\", \"" + local[3] + "\"}";
    // }
    // }
    // return "SAME_LOWER_CASE_LOCAL(\"" + local[0] + "\")";
    // }
    //
    // private String formatNs() {
    // if (uri[1] != "" && uri[0] == "" && uri[3] == "" && uri[1] == uri[2]) {
    // return "NAMESPACE(\"" + uri[1] + "\")";
    // }
    // for (int i = 0; i < uri.length; i++) {
    // if ("" != uri[i]) {
    // return "new String[]{\"" + uri[0] + "\", \"" + uri[1]
    // + "\", \"" + uri[2] + "\", \"" + uri[3] + "\"}";
    // }
    // }
    // return "ALL_NO_NS";
    // }
    //
    // private String formatNcname() {
    // for (int i = 0; i < ncname.length; i++) {
    // if (!ncname[i]) {
    // return "new boolean[]{" + ncname[0] + ", " + ncname[1] + ", "
    // + ncname[2] + ", " + ncname[3] + "}";
    // }
    // }
    // return "ALL_NCNAME";
    // }
    //
    // private String constName() {
    // String name = getLocal(HTML);
    // char[] buf = new char[name.length()];
    // for (int i = 0; i < name.length(); i++) {
    // char c = name.charAt(i);
    // if (c == '-' || c == ':') {
    // buf[i] = '_';
    // } else if (c >= '0' && c <= '9') {
    // buf[i] = c;
    // } else {
    // buf[i] = (char) (c - 0x20);
    // }
    // }
    // return new String(buf);
    // }
    //
    // private int hash() {
    // String name = getLocal(HTML);
    // return bufToHash(name.toCharArray(), name.length());
    // }
    //
    // /**
    // * Regenerate self
    // *
    // * @param args
    // */
    // public static void main(String[] args) {
    // Arrays.sort(ATTRIBUTE_NAMES);
    // for (int i = 1; i < ATTRIBUTE_NAMES.length; i++) {
    // if (ATTRIBUTE_NAMES[i].hash() == ATTRIBUTE_NAMES[i - 1].hash()) {
    // System.err.println("Hash collision: "
    // + ATTRIBUTE_NAMES[i].getLocal(HTML) + ", "
    // + ATTRIBUTE_NAMES[i - 1].getLocal(HTML));
    // return;
    // }
    // }
    // for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
    // AttributeName att = ATTRIBUTE_NAMES[i];
    // System.out.println("public static final AttributeName "
    // + att.constName() + " = new AttributeName" + att.toString()
    // + ";");
    // }
    // System.out.println("private final static @NoLength AttributeName[] ATTRIBUTE_NAMES
    // = {");
    // for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
    // AttributeName att = ATTRIBUTE_NAMES[i];
    // System.out.println(att.constName() + ",");
    // }
    // System.out.println("};");
    // System.out.println("private final static @NoLength int[] ATTRIBUTE_HASHES = {");
    // for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
    // AttributeName att = ATTRIBUTE_NAMES[i];
    // System.out.println(Integer.toString(att.hash()) + ",");
    // }
    // System.out.println("};");
    // }

    // START GENERATED CODE
    public static final AttributeName D = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("d"),
            AttributeName.SAME_LOWER_CASE_QNAME("d"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName K = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("k"),
            AttributeName.SAME_LOWER_CASE_QNAME("k"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName R = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("r"),
            AttributeName.SAME_LOWER_CASE_QNAME("r"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName X = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("x"),
            AttributeName.SAME_LOWER_CASE_QNAME("x"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName Y = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("y"),
            AttributeName.SAME_LOWER_CASE_QNAME("y"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName Z = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("z"),
            AttributeName.SAME_LOWER_CASE_QNAME("z"), AttributeName.ALL_NCNAME,
            false);

    public static final AttributeName BY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("by"),
            AttributeName.SAME_LOWER_CASE_QNAME("by"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("cx"),
            AttributeName.SAME_LOWER_CASE_QNAME("cx"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("cy"),
            AttributeName.SAME_LOWER_CASE_QNAME("cy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("dx"),
            AttributeName.SAME_LOWER_CASE_QNAME("dx"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("dy"),
            AttributeName.SAME_LOWER_CASE_QNAME("dy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName G2 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("g2"),
            AttributeName.SAME_LOWER_CASE_QNAME("g2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName G1 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("g1"),
            AttributeName.SAME_LOWER_CASE_QNAME("g1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("fx"),
            AttributeName.SAME_LOWER_CASE_QNAME("fx"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("fy"),
            AttributeName.SAME_LOWER_CASE_QNAME("fy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName K4 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("k4"),
            AttributeName.SAME_LOWER_CASE_QNAME("k4"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName K2 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("k2"),
            AttributeName.SAME_LOWER_CASE_QNAME("k2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName K3 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("k3"),
            AttributeName.SAME_LOWER_CASE_QNAME("k3"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName K1 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("k1"),
            AttributeName.SAME_LOWER_CASE_QNAME("k1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ID = new AttributeName("ID",
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("id"),
            AttributeName.SAME_LOWER_CASE_QNAME("id"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName IN = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("in"),
            AttributeName.SAME_LOWER_CASE_QNAME("in"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName U2 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("u2"),
            AttributeName.SAME_LOWER_CASE_QNAME("u2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName U1 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("u1"),
            AttributeName.SAME_LOWER_CASE_QNAME("u1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("rt"),
            AttributeName.SAME_LOWER_CASE_QNAME("rt"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("rx"),
            AttributeName.SAME_LOWER_CASE_QNAME("rx"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("ry"),
            AttributeName.SAME_LOWER_CASE_QNAME("ry"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TO = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("to"),
            AttributeName.SAME_LOWER_CASE_QNAME("to"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName Y2 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("y2"),
            AttributeName.SAME_LOWER_CASE_QNAME("y2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName Y1 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("y1"),
            AttributeName.SAME_LOWER_CASE_QNAME("y1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName X1 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("x1"),
            AttributeName.SAME_LOWER_CASE_QNAME("x1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName X2 = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.SAME_LOWER_CASE_LOCAL("x2"),
            AttributeName.SAME_LOWER_CASE_QNAME("x2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alt"),
            AttributeName.SAME_LOWER_CASE_QNAME("alt"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DIR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("dir"),
            AttributeName.SAME_LOWER_CASE_QNAME("dir"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DUR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("dur"),
            AttributeName.SAME_LOWER_CASE_QNAME("dur"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName END = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("end"),
            AttributeName.SAME_LOWER_CASE_QNAME("end"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("for"),
            AttributeName.SAME_LOWER_CASE_QNAME("for"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName IN2 = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("in2"),
            AttributeName.SAME_LOWER_CASE_QNAME("in2"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MAX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("max"),
            AttributeName.SAME_LOWER_CASE_QNAME("max"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("min"),
            AttributeName.SAME_LOWER_CASE_QNAME("min"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LOW = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("low"),
            AttributeName.SAME_LOWER_CASE_QNAME("low"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rel"),
            AttributeName.SAME_LOWER_CASE_QNAME("rel"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REV = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rev"),
            AttributeName.SAME_LOWER_CASE_QNAME("rev"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SRC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("src"),
            AttributeName.SAME_LOWER_CASE_QNAME("src"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AXIS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("axis"),
            AttributeName.SAME_LOWER_CASE_QNAME("axis"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ABBR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("abbr"),
            AttributeName.SAME_LOWER_CASE_QNAME("abbr"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BBOX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("bbox"),
            AttributeName.SAME_LOWER_CASE_QNAME("bbox"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CITE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cite"),
            AttributeName.SAME_LOWER_CASE_QNAME("cite"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CODE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("code"),
            AttributeName.SAME_LOWER_CASE_QNAME("code"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BIAS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("bias"),
            AttributeName.SAME_LOWER_CASE_QNAME("bias"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cols"),
            AttributeName.SAME_LOWER_CASE_QNAME("cols"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName END  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("end "),
            AttributeName.SAME_LOWER_CASE_QNAME("end "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLIP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("clip"),
            AttributeName.SAME_LOWER_CASE_QNAME("clip"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CHAR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("char"),
            AttributeName.SAME_LOWER_CASE_QNAME("char"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BASE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("base"),
            AttributeName.SAME_LOWER_CASE_QNAME("base"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EDGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("edge"),
            AttributeName.SAME_LOWER_CASE_QNAME("edge"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DATA = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("data"),
            AttributeName.SAME_LOWER_CASE_QNAME("data"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fill"),
            AttributeName.SAME_LOWER_CASE_QNAME("fill"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FROM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("from"),
            AttributeName.SAME_LOWER_CASE_QNAME("from"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FORM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("form"),
            AttributeName.SAME_LOWER_CASE_QNAME("form"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("face"),
            AttributeName.SAME_LOWER_CASE_QNAME("face"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HIGH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("high"),
            AttributeName.SAME_LOWER_CASE_QNAME("high"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HREF = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("href"),
            AttributeName.SAME_LOWER_CASE_QNAME("href"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OPEN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("open"),
            AttributeName.SAME_LOWER_CASE_QNAME("open"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ICON = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("icon"),
            AttributeName.SAME_LOWER_CASE_QNAME("icon"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NAME = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("name"),
            AttributeName.SAME_LOWER_CASE_QNAME("name"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MODE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mode"),
            AttributeName.SAME_LOWER_CASE_QNAME("mode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MASK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mask"),
            AttributeName.SAME_LOWER_CASE_QNAME("mask"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LINK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("link"),
            AttributeName.SAME_LOWER_CASE_QNAME("link"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LANG = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("lang"),
            AttributeName.SAME_LOWER_CASE_QNAME("lang"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LIST = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("list"),
            AttributeName.SAME_LOWER_CASE_QNAME("list"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("type"),
            AttributeName.SAME_LOWER_CASE_QNAME("type"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WHEN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("when"),
            AttributeName.SAME_LOWER_CASE_QNAME("when"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WRAP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("wrap"),
            AttributeName.SAME_LOWER_CASE_QNAME("wrap"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEXT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("text"),
            AttributeName.SAME_LOWER_CASE_QNAME("text"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("path"),
            AttributeName.SAME_LOWER_CASE_QNAME("path"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ping"),
            AttributeName.SAME_LOWER_CASE_QNAME("ping"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REFX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("refx",
                    "refX"), AttributeName.SAME_LOWER_CASE_QNAME("refX"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REFY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("refy",
                    "refY"), AttributeName.SAME_LOWER_CASE_QNAME("refY"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("size"),
            AttributeName.SAME_LOWER_CASE_QNAME("size"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SEED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("seed"),
            AttributeName.SAME_LOWER_CASE_QNAME("seed"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROWS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rows"),
            AttributeName.SAME_LOWER_CASE_QNAME("rows"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPAN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("span"),
            AttributeName.SAME_LOWER_CASE_QNAME("span"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STEP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("step"),
            AttributeName.SAME_LOWER_CASE_QNAME("step"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("role"),
            AttributeName.SAME_LOWER_CASE_QNAME("role"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XREF = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("xref"),
            AttributeName.SAME_LOWER_CASE_QNAME("xref"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ASYNC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("async"),
            AttributeName.SAME_LOWER_CASE_QNAME("async"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALINK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alink"),
            AttributeName.SAME_LOWER_CASE_QNAME("alink"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALIGN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("align"),
            AttributeName.SAME_LOWER_CASE_QNAME("align"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLOSE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("close"),
            AttributeName.SAME_LOWER_CASE_QNAME("close"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("color"),
            AttributeName.SAME_LOWER_CASE_QNAME("color"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLASS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("class"),
            AttributeName.SAME_LOWER_CASE_QNAME("class"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLEAR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("clear"),
            AttributeName.SAME_LOWER_CASE_QNAME("clear"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BEGIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("begin"),
            AttributeName.SAME_LOWER_CASE_QNAME("begin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DEPTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("depth"),
            AttributeName.SAME_LOWER_CASE_QNAME("depth"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DEFER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("defer"),
            AttributeName.SAME_LOWER_CASE_QNAME("defer"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FENCE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fence"),
            AttributeName.SAME_LOWER_CASE_QNAME("fence"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FRAME = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("frame"),
            AttributeName.SAME_LOWER_CASE_QNAME("frame"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ISMAP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ismap"),
            AttributeName.SAME_LOWER_CASE_QNAME("ismap"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONEND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onend"),
            AttributeName.SAME_LOWER_CASE_QNAME("onend"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName INDEX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("index"),
            AttributeName.SAME_LOWER_CASE_QNAME("index"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ORDER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("order"),
            AttributeName.SAME_LOWER_CASE_QNAME("order"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OTHER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("other"),
            AttributeName.SAME_LOWER_CASE_QNAME("other"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oncut"),
            AttributeName.SAME_LOWER_CASE_QNAME("oncut"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NARGS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("nargs"),
            AttributeName.SAME_LOWER_CASE_QNAME("nargs"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MEDIA = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("media"),
            AttributeName.SAME_LOWER_CASE_QNAME("media"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LABEL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("label"),
            AttributeName.SAME_LOWER_CASE_QNAME("label"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LOCAL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("local"),
            AttributeName.SAME_LOWER_CASE_QNAME("local"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WIDTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("width"),
            AttributeName.SAME_LOWER_CASE_QNAME("width"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TITLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("title"),
            AttributeName.SAME_LOWER_CASE_QNAME("title"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VLINK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("vlink"),
            AttributeName.SAME_LOWER_CASE_QNAME("vlink"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VALUE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("value"),
            AttributeName.SAME_LOWER_CASE_QNAME("value"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SLOPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("slope"),
            AttributeName.SAME_LOWER_CASE_QNAME("slope"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SHAPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("shape"),
            AttributeName.SAME_LOWER_CASE_QNAME("shape"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCOPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scope"),
            AttributeName.SAME_LOWER_CASE_QNAME("scope"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCALE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scale"),
            AttributeName.SAME_LOWER_CASE_QNAME("scale"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPEED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("speed"),
            AttributeName.SAME_LOWER_CASE_QNAME("speed"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STYLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("style"),
            AttributeName.SAME_LOWER_CASE_QNAME("style"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RULES = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rules"),
            AttributeName.SAME_LOWER_CASE_QNAME("rules"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STEMH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stemh"),
            AttributeName.SAME_LOWER_CASE_QNAME("stemh"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STEMV = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stemv"),
            AttributeName.SAME_LOWER_CASE_QNAME("stemv"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName START = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("start"),
            AttributeName.SAME_LOWER_CASE_QNAME("start"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XMLNS = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/2000/xmlns/"),
            AttributeName.SAME_LOWER_CASE_LOCAL("xmlns"),
            AttributeName.SAME_LOWER_CASE_QNAME("xmlns"), new boolean[] {
                    false, false, false, false }, true);

    public static final AttributeName ACCEPT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accept"),
            AttributeName.SAME_LOWER_CASE_QNAME("accept"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACCENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accent"),
            AttributeName.SAME_LOWER_CASE_QNAME("accent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ASCENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ascent"),
            AttributeName.SAME_LOWER_CASE_QNAME("ascent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACTIVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("active"),
            AttributeName.SAME_LOWER_CASE_QNAME("active"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALTIMG = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("altimg"),
            AttributeName.SAME_LOWER_CASE_QNAME("altimg"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACTION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("action"),
            AttributeName.SAME_LOWER_CASE_QNAME("action"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BORDER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("border"),
            AttributeName.SAME_LOWER_CASE_QNAME("border"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CURSOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cursor"),
            AttributeName.SAME_LOWER_CASE_QNAME("cursor"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COORDS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("coords"),
            AttributeName.SAME_LOWER_CASE_QNAME("coords"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILTER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("filter"),
            AttributeName.SAME_LOWER_CASE_QNAME("filter"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FORMAT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("format"),
            AttributeName.SAME_LOWER_CASE_QNAME("format"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HIDDEN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("hidden"),
            AttributeName.SAME_LOWER_CASE_QNAME("hidden"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("hspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("hspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("height"),
            AttributeName.SAME_LOWER_CASE_QNAME("height"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmove"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmove"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONLOAD = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onload"),
            AttributeName.SAME_LOWER_CASE_QNAME("onload"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAG = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondrag"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondrag"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ORIGIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("origin"),
            AttributeName.SAME_LOWER_CASE_QNAME("origin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONZOOM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onzoom"),
            AttributeName.SAME_LOWER_CASE_QNAME("onzoom"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONHELP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onhelp"),
            AttributeName.SAME_LOWER_CASE_QNAME("onhelp"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSTOP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onstop"),
            AttributeName.SAME_LOWER_CASE_QNAME("onstop"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDROP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondrop"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondrop"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBLUR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onblur"),
            AttributeName.SAME_LOWER_CASE_QNAME("onblur"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OBJECT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("object"),
            AttributeName.SAME_LOWER_CASE_QNAME("object"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OFFSET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("offset"),
            AttributeName.SAME_LOWER_CASE_QNAME("offset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ORIENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("orient"),
            AttributeName.SAME_LOWER_CASE_QNAME("orient"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCOPY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oncopy"),
            AttributeName.SAME_LOWER_CASE_QNAME("oncopy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NOWRAP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("nowrap"),
            AttributeName.SAME_LOWER_CASE_QNAME("nowrap"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NOHREF = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("nohref"),
            AttributeName.SAME_LOWER_CASE_QNAME("nohref"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MACROS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("macros"),
            AttributeName.SAME_LOWER_CASE_QNAME("macros"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName METHOD = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("method"),
            AttributeName.SAME_LOWER_CASE_QNAME("method"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LOWSRC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("lowsrc"),
            AttributeName.SAME_LOWER_CASE_QNAME("lowsrc"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("lspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("lspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LQUOTE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("lquote"),
            AttributeName.SAME_LOWER_CASE_QNAME("lquote"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName USEMAP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("usemap"),
            AttributeName.SAME_LOWER_CASE_QNAME("usemap"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VALUE_ = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("value:"),
            AttributeName.SAME_LOWER_CASE_QNAME("value:"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WIDTHS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("widths"),
            AttributeName.SAME_LOWER_CASE_QNAME("widths"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TARGET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("target"),
            AttributeName.SAME_LOWER_CASE_QNAME("target"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VALUES = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("values"),
            AttributeName.SAME_LOWER_CASE_QNAME("values"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VALIGN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("valign"),
            AttributeName.SAME_LOWER_CASE_QNAME("valign"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("vspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("vspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POSTER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("poster"),
            AttributeName.SAME_LOWER_CASE_QNAME("poster"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POINTS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("points"),
            AttributeName.SAME_LOWER_CASE_QNAME("points"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PROMPT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("prompt"),
            AttributeName.SAME_LOWER_CASE_QNAME("prompt"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCOPED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scoped"),
            AttributeName.SAME_LOWER_CASE_QNAME("scoped"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STRING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("string"),
            AttributeName.SAME_LOWER_CASE_QNAME("string"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCHEME = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scheme"),
            AttributeName.SAME_LOWER_CASE_QNAME("scheme"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RADIUS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("radius"),
            AttributeName.SAME_LOWER_CASE_QNAME("radius"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RESULT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("result"),
            AttributeName.SAME_LOWER_CASE_QNAME("result"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEAT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("repeat"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeat"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("rspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROTATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rotate"),
            AttributeName.SAME_LOWER_CASE_QNAME("rotate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RQUOTE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rquote"),
            AttributeName.SAME_LOWER_CASE_QNAME("rquote"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALTTEXT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alttext"),
            AttributeName.SAME_LOWER_CASE_QNAME("alttext"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARCHIVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("archive"),
            AttributeName.SAME_LOWER_CASE_QNAME("archive"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AZIMUTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("azimuth"),
            AttributeName.SAME_LOWER_CASE_QNAME("azimuth"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLOSURE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("closure"),
            AttributeName.SAME_LOWER_CASE_QNAME("closure"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CHECKED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("checked"),
            AttributeName.SAME_LOWER_CASE_QNAME("checked"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLASSID = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("classid"),
            AttributeName.SAME_LOWER_CASE_QNAME("classid"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CHAROFF = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("charoff"),
            AttributeName.SAME_LOWER_CASE_QNAME("charoff"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BGCOLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("bgcolor"),
            AttributeName.SAME_LOWER_CASE_QNAME("bgcolor"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLSPAN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("colspan"),
            AttributeName.SAME_LOWER_CASE_QNAME("colspan"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CHARSET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("charset"),
            AttributeName.SAME_LOWER_CASE_QNAME("charset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COMPACT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("compact"),
            AttributeName.SAME_LOWER_CASE_QNAME("compact"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("content"),
            AttributeName.SAME_LOWER_CASE_QNAME("content"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ENCTYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("enctype"),
            AttributeName.SAME_LOWER_CASE_QNAME("enctype"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DATASRC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("datasrc"),
            AttributeName.SAME_LOWER_CASE_QNAME("datasrc"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DATAFLD = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("datafld"),
            AttributeName.SAME_LOWER_CASE_QNAME("datafld"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DECLARE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("declare"),
            AttributeName.SAME_LOWER_CASE_QNAME("declare"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DISPLAY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("display"),
            AttributeName.SAME_LOWER_CASE_QNAME("display"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DIVISOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("divisor"),
            AttributeName.SAME_LOWER_CASE_QNAME("divisor"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DEFAULT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("default"),
            AttributeName.SAME_LOWER_CASE_QNAME("default"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DESCENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("descent"),
            AttributeName.SAME_LOWER_CASE_QNAME("descent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KERNING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("kerning"),
            AttributeName.SAME_LOWER_CASE_QNAME("kerning"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HANGING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("hanging"),
            AttributeName.SAME_LOWER_CASE_QNAME("hanging"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HEADERS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("headers"),
            AttributeName.SAME_LOWER_CASE_QNAME("headers"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONPASTE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onpaste"),
            AttributeName.SAME_LOWER_CASE_QNAME("onpaste"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCLICK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onclick"),
            AttributeName.SAME_LOWER_CASE_QNAME("onclick"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OPTIMUM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("optimum"),
            AttributeName.SAME_LOWER_CASE_QNAME("optimum"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEGIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbegin"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbegin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONKEYUP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onkeyup"),
            AttributeName.SAME_LOWER_CASE_QNAME("onkeyup"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFOCUS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onfocus"),
            AttributeName.SAME_LOWER_CASE_QNAME("onfocus"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONERROR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onerror"),
            AttributeName.SAME_LOWER_CASE_QNAME("onerror"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONINPUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oninput"),
            AttributeName.SAME_LOWER_CASE_QNAME("oninput"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONABORT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onabort"),
            AttributeName.SAME_LOWER_CASE_QNAME("onabort"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSTART = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onstart"),
            AttributeName.SAME_LOWER_CASE_QNAME("onstart"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONRESET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onreset"),
            AttributeName.SAME_LOWER_CASE_QNAME("onreset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OPACITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("opacity"),
            AttributeName.SAME_LOWER_CASE_QNAME("opacity"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NOSHADE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("noshade"),
            AttributeName.SAME_LOWER_CASE_QNAME("noshade"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MINSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("minsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("minsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MAXSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("maxsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("maxsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LARGEOP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("largeop"),
            AttributeName.SAME_LOWER_CASE_QNAME("largeop"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNICODE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("unicode"),
            AttributeName.SAME_LOWER_CASE_QNAME("unicode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TARGETX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("targetx",
                    "targetX"), AttributeName.SAME_LOWER_CASE_QNAME("targetX"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TARGETY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("targety",
                    "targetY"), AttributeName.SAME_LOWER_CASE_QNAME("targetY"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VIEWBOX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("viewbox",
                    "viewBox"), AttributeName.SAME_LOWER_CASE_QNAME("viewBox"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERSION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("version"),
            AttributeName.SAME_LOWER_CASE_QNAME("version"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATTERN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("pattern"),
            AttributeName.SAME_LOWER_CASE_QNAME("pattern"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PROFILE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("profile"),
            AttributeName.SAME_LOWER_CASE_QNAME("profile"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName START   = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("start  "),
            AttributeName.SAME_LOWER_CASE_QNAME("start  "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("spacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("spacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RESTART = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("restart"),
            AttributeName.SAME_LOWER_CASE_QNAME("restart"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROWSPAN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rowspan"),
            AttributeName.SAME_LOWER_CASE_QNAME("rowspan"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SANDBOX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("sandbox"),
            AttributeName.SAME_LOWER_CASE_QNAME("sandbox"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SUMMARY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("summary"),
            AttributeName.SAME_LOWER_CASE_QNAME("summary"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STANDBY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("standby"),
            AttributeName.SAME_LOWER_CASE_QNAME("standby"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPLACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("replace"),
            AttributeName.SAME_LOWER_CASE_QNAME("replace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ADDITIVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("additive"),
            AttributeName.SAME_LOWER_CASE_QNAME("additive"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CALCMODE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("calcmode",
                    "calcMode"),
            AttributeName.SAME_LOWER_CASE_QNAME("calcMode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CODETYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("codetype"),
            AttributeName.SAME_LOWER_CASE_QNAME("codetype"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CODEBASE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("codebase"),
            AttributeName.SAME_LOWER_CASE_QNAME("codebase"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BEVELLED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("bevelled"),
            AttributeName.SAME_LOWER_CASE_QNAME("bevelled"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BASELINE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("baseline"),
            AttributeName.SAME_LOWER_CASE_QNAME("baseline"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EXPONENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("exponent"),
            AttributeName.SAME_LOWER_CASE_QNAME("exponent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EDGEMODE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("edgemode",
                    "edgeMode"),
            AttributeName.SAME_LOWER_CASE_QNAME("edgeMode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ENCODING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("encoding"),
            AttributeName.SAME_LOWER_CASE_QNAME("encoding"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GLYPHREF = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("glyphref",
                    "glyphRef"),
            AttributeName.SAME_LOWER_CASE_QNAME("glyphRef"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DATETIME = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("datetime"),
            AttributeName.SAME_LOWER_CASE_QNAME("datetime"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DISABLED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("disabled"),
            AttributeName.SAME_LOWER_CASE_QNAME("disabled"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONTSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fontsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("fontsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KEYTIMES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL("keytimes",
                    "keyTimes"),
            AttributeName.SAME_LOWER_CASE_QNAME("keyTimes"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LOOPEND  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("loopend "),
            AttributeName.SAME_LOWER_CASE_QNAME("loopend "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PANOSE_1 = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("panose-1"),
            AttributeName.SAME_LOWER_CASE_QNAME("panose-1"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HREFLANG = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("hreflang"),
            AttributeName.SAME_LOWER_CASE_QNAME("hreflang"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONRESIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onresize"),
            AttributeName.SAME_LOWER_CASE_QNAME("onresize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onchange"),
            AttributeName.SAME_LOWER_CASE_QNAME("onchange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBOUNCE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbounce"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbounce"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONUNLOAD = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onunload"),
            AttributeName.SAME_LOWER_CASE_QNAME("onunload"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFINISH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onfinish"),
            AttributeName.SAME_LOWER_CASE_QNAME("onfinish"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSCROLL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onscroll"),
            AttributeName.SAME_LOWER_CASE_QNAME("onscroll"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OPERATOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("operator"),
            AttributeName.SAME_LOWER_CASE_QNAME("operator"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OVERFLOW = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("overflow"),
            AttributeName.SAME_LOWER_CASE_QNAME("overflow"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSUBMIT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onsubmit"),
            AttributeName.SAME_LOWER_CASE_QNAME("onsubmit"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONREPEAT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onrepeat"),
            AttributeName.SAME_LOWER_CASE_QNAME("onrepeat"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSELECT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onselect"),
            AttributeName.SAME_LOWER_CASE_QNAME("onselect"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NOTATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("notation"),
            AttributeName.SAME_LOWER_CASE_QNAME("notation"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NORESIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("noresize"),
            AttributeName.SAME_LOWER_CASE_QNAME("noresize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MANIFEST = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("manifest"),
            AttributeName.SAME_LOWER_CASE_QNAME("manifest"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MATHSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mathsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("mathsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MULTIPLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("multiple"),
            AttributeName.SAME_LOWER_CASE_QNAME("multiple"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LONGDESC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("longdesc"),
            AttributeName.SAME_LOWER_CASE_QNAME("longdesc"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LANGUAGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("language"),
            AttributeName.SAME_LOWER_CASE_QNAME("language"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEMPLATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("template"),
            AttributeName.SAME_LOWER_CASE_QNAME("template"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TABINDEX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("tabindex"),
            AttributeName.SAME_LOWER_CASE_QNAME("tabindex"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName READONLY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("readonly"),
            AttributeName.SAME_LOWER_CASE_QNAME("readonly"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SELECTED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("selected"),
            AttributeName.SAME_LOWER_CASE_QNAME("selected"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROWLINES = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rowlines"),
            AttributeName.SAME_LOWER_CASE_QNAME("rowlines"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SEAMLESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("seamless"),
            AttributeName.SAME_LOWER_CASE_QNAME("seamless"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROWALIGN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rowalign"),
            AttributeName.SAME_LOWER_CASE_QNAME("rowalign"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STRETCHY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stretchy"),
            AttributeName.SAME_LOWER_CASE_QNAME("stretchy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REQUIRED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("required"),
            AttributeName.SAME_LOWER_CASE_QNAME("required"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XML_BASE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/XML/1998/namespace"),
            AttributeName.COLONIFIED_LOCAL("xml:base", "base"),
            AttributeName.SAME_LOWER_CASE_QNAME("xml:base"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName XML_LANG = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/XML/1998/namespace"),
            AttributeName.COLONIFIED_LOCAL("xml:lang", "lang"),
            AttributeName.SAME_LOWER_CASE_QNAME("xml:lang"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName X_HEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("x-height"),
            AttributeName.SAME_LOWER_CASE_QNAME("x-height"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTROLS  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("controls "),
            AttributeName.SAME_LOWER_CASE_QNAME("controls "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_OWNS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-owns"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-owns"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AUTOFOCUS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("autofocus"),
            AttributeName.SAME_LOWER_CASE_QNAME("autofocus"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_SORT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-sort"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-sort"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACCESSKEY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accesskey"),
            AttributeName.SAME_LOWER_CASE_QNAME("accesskey"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AMPLITUDE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("amplitude"),
            AttributeName.SAME_LOWER_CASE_QNAME("amplitude"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_LIVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-live"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-live"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLIP_RULE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("clip-rule"),
            AttributeName.SAME_LOWER_CASE_QNAME("clip-rule"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLIP_PATH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("clip-path"),
            AttributeName.SAME_LOWER_CASE_QNAME("clip-path"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EQUALROWS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("equalrows"),
            AttributeName.SAME_LOWER_CASE_QNAME("equalrows"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ELEVATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("elevation"),
            AttributeName.SAME_LOWER_CASE_QNAME("elevation"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DIRECTION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("direction"),
            AttributeName.SAME_LOWER_CASE_QNAME("direction"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DRAGGABLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("draggable"),
            AttributeName.SAME_LOWER_CASE_QNAME("draggable"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILTERRES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "filterres", "filterRes"),
            AttributeName.SAME_LOWER_CASE_QNAME("filterRes"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILL_RULE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fill-rule"),
            AttributeName.SAME_LOWER_CASE_QNAME("fill-rule"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONTSTYLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fontstyle"),
            AttributeName.SAME_LOWER_CASE_QNAME("fontstyle"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_SIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-size"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-size"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KEYPOINTS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "keypoints", "keyPoints"),
            AttributeName.SAME_LOWER_CASE_QNAME("keyPoints"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HIDEFOCUS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("hidefocus"),
            AttributeName.SAME_LOWER_CASE_QNAME("hidefocus"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMESSAGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmessage"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmessage"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName INTERCEPT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("intercept"),
            AttributeName.SAME_LOWER_CASE_QNAME("intercept"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGEND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragend"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragend"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOVEEND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmoveend"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmoveend"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONINVALID = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oninvalid"),
            AttributeName.SAME_LOWER_CASE_QNAME("oninvalid"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONKEYDOWN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onkeydown"),
            AttributeName.SAME_LOWER_CASE_QNAME("onkeydown"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFOCUSIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onfocusin"),
            AttributeName.SAME_LOWER_CASE_QNAME("onfocusin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEUP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmouseup"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmouseup"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName INPUTMODE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("inputmode"),
            AttributeName.SAME_LOWER_CASE_QNAME("inputmode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONROWEXIT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onrowexit"),
            AttributeName.SAME_LOWER_CASE_QNAME("onrowexit"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MATHCOLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mathcolor"),
            AttributeName.SAME_LOWER_CASE_QNAME("mathcolor"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MASKUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "maskunits", "maskUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("maskUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MAXLENGTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("maxlength"),
            AttributeName.SAME_LOWER_CASE_QNAME("maxlength"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LINEBREAK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("linebreak"),
            AttributeName.SAME_LOWER_CASE_QNAME("linebreak"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TRANSFORM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("transform"),
            AttributeName.SAME_LOWER_CASE_QNAME("transform"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName V_HANGING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("v-hanging"),
            AttributeName.SAME_LOWER_CASE_QNAME("v-hanging"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VALUETYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("valuetype"),
            AttributeName.SAME_LOWER_CASE_QNAME("valuetype"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POINTSATZ = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "pointsatz", "pointsAtZ"),
            AttributeName.SAME_LOWER_CASE_QNAME("pointsAtZ"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POINTSATX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "pointsatx", "pointsAtX"),
            AttributeName.SAME_LOWER_CASE_QNAME("pointsAtX"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POINTSATY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "pointsaty", "pointsAtY"),
            AttributeName.SAME_LOWER_CASE_QNAME("pointsAtY"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SYMMETRIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("symmetric"),
            AttributeName.SAME_LOWER_CASE_QNAME("symmetric"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCROLLING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scrolling"),
            AttributeName.SAME_LOWER_CASE_QNAME("scrolling"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEATDUR = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "repeatdur", "repeatDur"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeatDur"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SELECTION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("selection"),
            AttributeName.SAME_LOWER_CASE_QNAME("selection"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SEPARATOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("separator"),
            AttributeName.SAME_LOWER_CASE_QNAME("separator"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AUTOPLAY   = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("autoplay  "),
            AttributeName.SAME_LOWER_CASE_QNAME("autoplay  "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XML_SPACE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/XML/1998/namespace"),
            AttributeName.COLONIFIED_LOCAL("xml:space", "space"),
            AttributeName.SAME_LOWER_CASE_QNAME("xml:space"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName ARIA_GRAB  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-grab "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-grab "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_BUSY  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-busy "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-busy "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AUTOSUBMIT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("autosubmit"),
            AttributeName.SAME_LOWER_CASE_QNAME("autosubmit"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALPHABETIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alphabetic"),
            AttributeName.SAME_LOWER_CASE_QNAME("alphabetic"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACTIONTYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("actiontype"),
            AttributeName.SAME_LOWER_CASE_QNAME("actiontype"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACCUMULATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accumulate"),
            AttributeName.SAME_LOWER_CASE_QNAME("accumulate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_LEVEL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-level"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-level"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLUMNSPAN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("columnspan"),
            AttributeName.SAME_LOWER_CASE_QNAME("columnspan"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CAP_HEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cap-height"),
            AttributeName.SAME_LOWER_CASE_QNAME("cap-height"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BACKGROUND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("background"),
            AttributeName.SAME_LOWER_CASE_QNAME("background"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GLYPH_NAME = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("glyph-name"),
            AttributeName.SAME_LOWER_CASE_QNAME("glyph-name"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GROUPALIGN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("groupalign"),
            AttributeName.SAME_LOWER_CASE_QNAME("groupalign"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONTFAMILY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fontfamily"),
            AttributeName.SAME_LOWER_CASE_QNAME("fontfamily"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONTWEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fontweight"),
            AttributeName.SAME_LOWER_CASE_QNAME("fontweight"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_STYLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-style"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-style"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KEYSPLINES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "keysplines", "keySplines"),
            AttributeName.SAME_LOWER_CASE_QNAME("keySplines"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LOOPSTART  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("loopstart "),
            AttributeName.SAME_LOWER_CASE_QNAME("loopstart "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PLAYCOUNT  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("playcount "),
            AttributeName.SAME_LOWER_CASE_QNAME("playcount "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HTTP_EQUIV = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("http-equiv"),
            AttributeName.SAME_LOWER_CASE_QNAME("http-equiv"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONACTIVATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onactivate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onactivate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OCCURRENCE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("occurrence"),
            AttributeName.SAME_LOWER_CASE_QNAME("occurrence"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName IRRELEVANT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("irrelevant"),
            AttributeName.SAME_LOWER_CASE_QNAME("irrelevant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDBLCLICK = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondblclick"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondblclick"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGDROP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragdrop"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragdrop"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONKEYPRESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onkeypress"),
            AttributeName.SAME_LOWER_CASE_QNAME("onkeypress"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONROWENTER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onrowenter"),
            AttributeName.SAME_LOWER_CASE_QNAME("onrowenter"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGOVER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragover"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragover"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFOCUSOUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onfocusout"),
            AttributeName.SAME_LOWER_CASE_QNAME("onfocusout"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEOUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmouseout"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmouseout"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName NUMOCTAVES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "numoctaves", "numOctaves"),
            AttributeName.SAME_LOWER_CASE_QNAME("numOctaves"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKER_MID = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("marker-mid"),
            AttributeName.SAME_LOWER_CASE_QNAME("marker-mid"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKER_END = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("marker-end"),
            AttributeName.SAME_LOWER_CASE_QNAME("marker-end"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEXTLENGTH = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "textlength", "textLength"),
            AttributeName.SAME_LOWER_CASE_QNAME("textLength"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VISIBILITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("visibility"),
            AttributeName.SAME_LOWER_CASE_QNAME("visibility"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VIEWTARGET = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "viewtarget", "viewTarget"),
            AttributeName.SAME_LOWER_CASE_QNAME("viewTarget"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERT_ADV_Y = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("vert-adv-y"),
            AttributeName.SAME_LOWER_CASE_QNAME("vert-adv-y"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATHLENGTH = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "pathlength", "pathLength"),
            AttributeName.SAME_LOWER_CASE_QNAME("pathLength"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEAT_MAX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("repeat-max"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeat-max"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RADIOGROUP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("radiogroup"),
            AttributeName.SAME_LOWER_CASE_QNAME("radiogroup"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STOP_COLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stop-color"),
            AttributeName.SAME_LOWER_CASE_QNAME("stop-color"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SEPARATORS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("separators"),
            AttributeName.SAME_LOWER_CASE_QNAME("separators"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEAT_MIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("repeat-min"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeat-min"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ROWSPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rowspacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("rowspacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ZOOMANDPAN = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "zoomandpan", "zoomAndPan"),
            AttributeName.SAME_LOWER_CASE_QNAME("zoomAndPan"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XLINK_TYPE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:type", "type"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:type"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName XLINK_ROLE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:role", "role"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:role"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName XLINK_HREF = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:href", "href"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:href"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName XLINK_SHOW = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:show", "show"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:show"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName ACCENTUNDER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accentunder"),
            AttributeName.SAME_LOWER_CASE_QNAME("accentunder"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_SECRET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-secret"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-secret"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_ATOMIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-atomic"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-atomic"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_FLOWTO = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-flowto"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-flowto"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARABIC_FORM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("arabic-form"),
            AttributeName.SAME_LOWER_CASE_QNAME("arabic-form"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CELLPADDING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cellpadding"),
            AttributeName.SAME_LOWER_CASE_QNAME("cellpadding"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CELLSPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("cellspacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("cellspacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLUMNWIDTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("columnwidth"),
            AttributeName.SAME_LOWER_CASE_QNAME("columnwidth"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLUMNALIGN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("columnalign"),
            AttributeName.SAME_LOWER_CASE_QNAME("columnalign"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLUMNLINES = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("columnlines"),
            AttributeName.SAME_LOWER_CASE_QNAME("columnlines"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTEXTMENU = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("contextmenu"),
            AttributeName.SAME_LOWER_CASE_QNAME("contextmenu"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BASEPROFILE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "baseprofile", "baseProfile"),
            AttributeName.SAME_LOWER_CASE_QNAME("baseProfile"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_FAMILY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-family"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-family"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FRAMEBORDER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("frameborder"),
            AttributeName.SAME_LOWER_CASE_QNAME("frameborder"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILTERUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "filterunits", "filterUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("filterUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FLOOD_COLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("flood-color"),
            AttributeName.SAME_LOWER_CASE_QNAME("flood-color"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_WEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-weight"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-weight"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HORIZ_ADV_X = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("horiz-adv-x"),
            AttributeName.SAME_LOWER_CASE_QNAME("horiz-adv-x"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGLEAVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragleave"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragleave"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEMOVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmousemove"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmousemove"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ORIENTATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("orientation"),
            AttributeName.SAME_LOWER_CASE_QNAME("orientation"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEDOWN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmousedown"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmousedown"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEOVER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmouseover"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmouseover"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGENTER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragenter"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragenter"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName IDEOGRAPHIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ideographic"),
            AttributeName.SAME_LOWER_CASE_QNAME("ideographic"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFORECUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforecut"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforecut"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFORMINPUT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onforminput"),
            AttributeName.SAME_LOWER_CASE_QNAME("onforminput"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDRAGSTART = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondragstart"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondragstart"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOVESTART = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmovestart"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmovestart"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKERUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "markerunits", "markerUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("markerUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MATHVARIANT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mathvariant"),
            AttributeName.SAME_LOWER_CASE_QNAME("mathvariant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARGINWIDTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("marginwidth"),
            AttributeName.SAME_LOWER_CASE_QNAME("marginwidth"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKERWIDTH = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "markerwidth", "markerWidth"),
            AttributeName.SAME_LOWER_CASE_QNAME("markerWidth"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEXT_ANCHOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("text-anchor"),
            AttributeName.SAME_LOWER_CASE_QNAME("text-anchor"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TABLEVALUES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "tablevalues", "tableValues"),
            AttributeName.SAME_LOWER_CASE_QNAME("tableValues"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCRIPTLEVEL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scriptlevel"),
            AttributeName.SAME_LOWER_CASE_QNAME("scriptlevel"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEATCOUNT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "repeatcount", "repeatCount"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeatCount"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STITCHTILES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "stitchtiles", "stitchTiles"),
            AttributeName.SAME_LOWER_CASE_QNAME("stitchTiles"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STARTOFFSET = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "startoffset", "startOffset"),
            AttributeName.SAME_LOWER_CASE_QNAME("startOffset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCROLLDELAY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scrolldelay"),
            AttributeName.SAME_LOWER_CASE_QNAME("scrolldelay"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XMLNS_XLINK = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/2000/xmlns/"),
            AttributeName.COLONIFIED_LOCAL("xmlns:xlink", "xlink"),
            AttributeName.SAME_LOWER_CASE_QNAME("xmlns:xlink"), new boolean[] {
                    false, false, false, false }, true);

    public static final AttributeName XLINK_TITLE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:title", "title"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:title"), new boolean[] {
                    false, true, true, false }, false);

    public static final AttributeName ARIA_HIDDEN  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-hidden "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-hidden "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName AUTOCOMPLETE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("autocomplete"),
            AttributeName.SAME_LOWER_CASE_QNAME("autocomplete"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_SETSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-setsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-setsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_CHANNEL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-channel"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-channel"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EQUALCOLUMNS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("equalcolumns"),
            AttributeName.SAME_LOWER_CASE_QNAME("equalcolumns"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DISPLAYSTYLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("displaystyle"),
            AttributeName.SAME_LOWER_CASE_QNAME("displaystyle"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DATAFORMATAS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("dataformatas"),
            AttributeName.SAME_LOWER_CASE_QNAME("dataformatas"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FILL_OPACITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("fill-opacity"),
            AttributeName.SAME_LOWER_CASE_QNAME("fill-opacity"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_VARIANT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-variant"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-variant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_STRETCH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-stretch"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-stretch"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FRAMESPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("framespacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("framespacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KERNELMATRIX = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "kernelmatrix", "kernelMatrix"),
            AttributeName.SAME_LOWER_CASE_QNAME("kernelMatrix"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDEACTIVATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondeactivate"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondeactivate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONROWSDELETE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onrowsdelete"),
            AttributeName.SAME_LOWER_CASE_QNAME("onrowsdelete"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSELEAVE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmouseleave"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmouseleave"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFORMCHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onformchange"),
            AttributeName.SAME_LOWER_CASE_QNAME("onformchange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCELLCHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oncellchange"),
            AttributeName.SAME_LOWER_CASE_QNAME("oncellchange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEWHEEL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmousewheel"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmousewheel"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONMOUSEENTER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onmouseenter"),
            AttributeName.SAME_LOWER_CASE_QNAME("onmouseenter"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONAFTERPRINT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onafterprint"),
            AttributeName.SAME_LOWER_CASE_QNAME("onafterprint"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFORECOPY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforecopy"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforecopy"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARGINHEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("marginheight"),
            AttributeName.SAME_LOWER_CASE_QNAME("marginheight"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKERHEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "markerheight", "markerHeight"),
            AttributeName.SAME_LOWER_CASE_QNAME("markerHeight"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MARKER_START = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("marker-start"),
            AttributeName.SAME_LOWER_CASE_QNAME("marker-start"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MATHEMATICAL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mathematical"),
            AttributeName.SAME_LOWER_CASE_QNAME("mathematical"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LENGTHADJUST = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "lengthadjust", "lengthAdjust"),
            AttributeName.SAME_LOWER_CASE_QNAME("lengthAdjust"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNSELECTABLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("unselectable"),
            AttributeName.SAME_LOWER_CASE_QNAME("unselectable"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNICODE_BIDI = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("unicode-bidi"),
            AttributeName.SAME_LOWER_CASE_QNAME("unicode-bidi"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNITS_PER_EM = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("units-per-em"),
            AttributeName.SAME_LOWER_CASE_QNAME("units-per-em"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WORD_SPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("word-spacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("word-spacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName WRITING_MODE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("writing-mode"),
            AttributeName.SAME_LOWER_CASE_QNAME("writing-mode"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName V_ALPHABETIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("v-alphabetic"),
            AttributeName.SAME_LOWER_CASE_QNAME("v-alphabetic"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATTERNUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "patternunits", "patternUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("patternUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPREADMETHOD = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "spreadmethod", "spreadMethod"),
            AttributeName.SAME_LOWER_CASE_QNAME("spreadMethod"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SURFACESCALE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "surfacescale", "surfaceScale"),
            AttributeName.SAME_LOWER_CASE_QNAME("surfaceScale"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_WIDTH = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-width"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-width"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEAT_START = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("repeat-start"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeat-start"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STDDEVIATION = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "stddeviation", "stdDeviation"),
            AttributeName.SAME_LOWER_CASE_QNAME("stdDeviation"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STOP_OPACITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stop-opacity"),
            AttributeName.SAME_LOWER_CASE_QNAME("stop-opacity"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_CHECKED  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-checked "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-checked "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_PRESSED  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-pressed "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-pressed "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_INVALID  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-invalid "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-invalid "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_CONTROLS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-controls"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-controls"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_HASPOPUP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-haspopup"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-haspopup"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACCENT_HEIGHT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accent-height"),
            AttributeName.SAME_LOWER_CASE_QNAME("accent-height"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_VALUENOW = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-valuenow"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-valuenow"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_RELEVANT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-relevant"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-relevant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_POSINSET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-posinset"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-posinset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_VALUEMAX = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-valuemax"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-valuemax"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_READONLY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-readonly"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-readonly"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_REQUIRED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-required"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-required"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ATTRIBUTETYPE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "attributetype", "attributeType"),
            AttributeName.SAME_LOWER_CASE_QNAME("attributeType"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ATTRIBUTENAME = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "attributename", "attributeName"),
            AttributeName.SAME_LOWER_CASE_QNAME("attributeName"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_DATATYPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-datatype"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-datatype"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_VALUEMIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-valuemin"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-valuemin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BASEFREQUENCY = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "basefrequency", "baseFrequency"),
            AttributeName.SAME_LOWER_CASE_QNAME("baseFrequency"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLUMNSPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("columnspacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("columnspacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLOR_PROFILE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("color-profile"),
            AttributeName.SAME_LOWER_CASE_QNAME("color-profile"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CLIPPATHUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "clippathunits", "clipPathUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("clipPathUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DEFINITIONURL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("definitionurl"),
            AttributeName.SAME_LOWER_CASE_QNAME("definitionurl"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GRADIENTUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "gradientunits", "gradientUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("gradientUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FLOOD_OPACITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("flood-opacity"),
            AttributeName.SAME_LOWER_CASE_QNAME("flood-opacity"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONAFTERUPDATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onafterupdate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onafterupdate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONERRORUPDATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onerrorupdate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onerrorupdate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREPASTE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforepaste"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforepaste"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONLOSECAPTURE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onlosecapture"),
            AttributeName.SAME_LOWER_CASE_QNAME("onlosecapture"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCONTEXTMENU = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oncontextmenu"),
            AttributeName.SAME_LOWER_CASE_QNAME("oncontextmenu"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONSELECTSTART = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onselectstart"),
            AttributeName.SAME_LOWER_CASE_QNAME("onselectstart"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREPRINT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforeprint"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforeprint"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MOVABLELIMITS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("movablelimits"),
            AttributeName.SAME_LOWER_CASE_QNAME("movablelimits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LINETHICKNESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("linethickness"),
            AttributeName.SAME_LOWER_CASE_QNAME("linethickness"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNICODE_RANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("unicode-range"),
            AttributeName.SAME_LOWER_CASE_QNAME("unicode-range"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName THINMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("thinmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("thinmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERT_ORIGIN_X = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("vert-origin-x"),
            AttributeName.SAME_LOWER_CASE_QNAME("vert-origin-x"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERT_ORIGIN_Y = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("vert-origin-y"),
            AttributeName.SAME_LOWER_CASE_QNAME("vert-origin-y"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName V_IDEOGRAPHIC = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("v-ideographic"),
            AttributeName.SAME_LOWER_CASE_QNAME("v-ideographic"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PRESERVEALPHA = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "preservealpha", "preserveAlpha"),
            AttributeName.SAME_LOWER_CASE_QNAME("preserveAlpha"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCRIPTMINSIZE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scriptminsize"),
            AttributeName.SAME_LOWER_CASE_QNAME("scriptminsize"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPECIFICATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("specification"),
            AttributeName.SAME_LOWER_CASE_QNAME("specification"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XLINK_ACTUATE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:actuate", "actuate"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:actuate"),
            new boolean[] { false, true, true, false }, false);

    public static final AttributeName XLINK_ARCROLE = new AttributeName(
            AttributeName.NAMESPACE("http://www.w3.org/1999/xlink"),
            AttributeName.COLONIFIED_LOCAL("xlink:arcrole", "arcrole"),
            AttributeName.SAME_LOWER_CASE_QNAME("xlink:arcrole"),
            new boolean[] { false, true, true, false }, false);

    public static final AttributeName ARIA_EXPANDED  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-expanded "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-expanded "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_DISABLED  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-disabled "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-disabled "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_SELECTED  = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-selected "),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-selected "),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ACCEPT_CHARSET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("accept-charset"),
            AttributeName.SAME_LOWER_CASE_QNAME("accept-charset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALIGNMENTSCOPE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alignmentscope"),
            AttributeName.SAME_LOWER_CASE_QNAME("alignmentscope"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_MULTILINE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-multiline"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-multiline"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName BASELINE_SHIFT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("baseline-shift"),
            AttributeName.SAME_LOWER_CASE_QNAME("baseline-shift"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HORIZ_ORIGIN_X = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("horiz-origin-x"),
            AttributeName.SAME_LOWER_CASE_QNAME("horiz-origin-x"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName HORIZ_ORIGIN_Y = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("horiz-origin-y"),
            AttributeName.SAME_LOWER_CASE_QNAME("horiz-origin-y"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREUPDATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforeupdate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforeupdate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONFILTERCHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onfilterchange"),
            AttributeName.SAME_LOWER_CASE_QNAME("onfilterchange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONROWSINSERTED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onrowsinserted"),
            AttributeName.SAME_LOWER_CASE_QNAME("onrowsinserted"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREUNLOAD = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforeunload"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforeunload"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MATHBACKGROUND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mathbackground"),
            AttributeName.SAME_LOWER_CASE_QNAME("mathbackground"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LETTER_SPACING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("letter-spacing"),
            AttributeName.SAME_LOWER_CASE_QNAME("letter-spacing"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LIGHTING_COLOR = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("lighting-color"),
            AttributeName.SAME_LOWER_CASE_QNAME("lighting-color"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName THICKMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("thickmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("thickmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEXT_RENDERING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("text-rendering"),
            AttributeName.SAME_LOWER_CASE_QNAME("text-rendering"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName V_MATHEMATICAL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("v-mathematical"),
            AttributeName.SAME_LOWER_CASE_QNAME("v-mathematical"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName POINTER_EVENTS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("pointer-events"),
            AttributeName.SAME_LOWER_CASE_QNAME("pointer-events"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PRIMITIVEUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "primitiveunits", "primitiveUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("primitiveUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SYSTEMLANGUAGE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "systemlanguage", "systemLanguage"),
            AttributeName.SAME_LOWER_CASE_QNAME("systemLanguage"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_LINECAP = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-linecap"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-linecap"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SUBSCRIPTSHIFT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("subscriptshift"),
            AttributeName.SAME_LOWER_CASE_QNAME("subscriptshift"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_OPACITY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-opacity"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-opacity"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_DROPEFFECT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-dropeffect"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-dropeffect"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_LABELLEDBY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-labelledby"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-labelledby"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_TEMPLATEID = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-templateid"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-templateid"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLOR_RENDERING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("color-rendering"),
            AttributeName.SAME_LOWER_CASE_QNAME("color-rendering"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTENTEDITABLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("contenteditable"),
            AttributeName.SAME_LOWER_CASE_QNAME("contenteditable"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DIFFUSECONSTANT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "diffuseconstant", "diffuseConstant"),
            AttributeName.SAME_LOWER_CASE_QNAME("diffuseConstant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDATAAVAILABLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondataavailable"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondataavailable"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONCONTROLSELECT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("oncontrolselect"),
            AttributeName.SAME_LOWER_CASE_QNAME("oncontrolselect"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName IMAGE_RENDERING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("image-rendering"),
            AttributeName.SAME_LOWER_CASE_QNAME("image-rendering"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MEDIUMMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("mediummathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("mediummathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName TEXT_DECORATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("text-decoration"),
            AttributeName.SAME_LOWER_CASE_QNAME("text-decoration"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SHAPE_RENDERING = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("shape-rendering"),
            AttributeName.SAME_LOWER_CASE_QNAME("shape-rendering"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_LINEJOIN = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-linejoin"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-linejoin"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REPEAT_TEMPLATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("repeat-template"),
            AttributeName.SAME_LOWER_CASE_QNAME("repeat-template"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_DESCRIBEDBY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-describedby"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-describedby"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTENTSTYLETYPE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "contentstyletype", "contentStyleType"),
            AttributeName.SAME_LOWER_CASE_QNAME("contentStyleType"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName FONT_SIZE_ADJUST = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("font-size-adjust"),
            AttributeName.SAME_LOWER_CASE_QNAME("font-size-adjust"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName KERNELUNITLENGTH = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "kernelunitlength", "kernelUnitLength"),
            AttributeName.SAME_LOWER_CASE_QNAME("kernelUnitLength"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREACTIVATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforeactivate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforeactivate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONPROPERTYCHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onpropertychange"),
            AttributeName.SAME_LOWER_CASE_QNAME("onpropertychange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDATASETCHANGED = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondatasetchanged"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondatasetchanged"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName MASKCONTENTUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "maskcontentunits", "maskContentUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("maskContentUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATTERNTRANSFORM = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "patterntransform", "patternTransform"),
            AttributeName.SAME_LOWER_CASE_QNAME("patternTransform"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REQUIREDFEATURES = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "requiredfeatures", "requiredFeatures"),
            AttributeName.SAME_LOWER_CASE_QNAME("requiredFeatures"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName RENDERING_INTENT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("rendering-intent"),
            AttributeName.SAME_LOWER_CASE_QNAME("rendering-intent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPECULAREXPONENT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "specularexponent", "specularExponent"),
            AttributeName.SAME_LOWER_CASE_QNAME("specularExponent"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SPECULARCONSTANT = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "specularconstant", "specularConstant"),
            AttributeName.SAME_LOWER_CASE_QNAME("specularConstant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SUPERSCRIPTSHIFT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("superscriptshift"),
            AttributeName.SAME_LOWER_CASE_QNAME("superscriptshift"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_DASHARRAY = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-dasharray"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-dasharray"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName XCHANNELSELECTOR = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "xchannelselector", "xChannelSelector"),
            AttributeName.SAME_LOWER_CASE_QNAME("xChannelSelector"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName YCHANNELSELECTOR = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "ychannelselector", "yChannelSelector"),
            AttributeName.SAME_LOWER_CASE_QNAME("yChannelSelector"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_AUTOCOMPLETE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-autocomplete"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-autocomplete"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName CONTENTSCRIPTTYPE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "contentscripttype", "contentScriptType"),
            AttributeName.SAME_LOWER_CASE_QNAME("contentScriptType"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ENABLE_BACKGROUND = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("enable-background"),
            AttributeName.SAME_LOWER_CASE_QNAME("enable-background"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName DOMINANT_BASELINE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("dominant-baseline"),
            AttributeName.SAME_LOWER_CASE_QNAME("dominant-baseline"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GRADIENTTRANSFORM = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "gradienttransform", "gradientTransform"),
            AttributeName.SAME_LOWER_CASE_QNAME("gradientTransform"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFORDEACTIVATE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbefordeactivate"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbefordeactivate"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONDATASETCOMPLETE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("ondatasetcomplete"),
            AttributeName.SAME_LOWER_CASE_QNAME("ondatasetcomplete"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OVERLINE_POSITION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("overline-position"),
            AttributeName.SAME_LOWER_CASE_QNAME("overline-position"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONBEFOREEDITFOCUS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onbeforeeditfocus"),
            AttributeName.SAME_LOWER_CASE_QNAME("onbeforeeditfocus"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName LIMITINGCONEANGLE = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "limitingconeangle", "limitingConeAngle"),
            AttributeName.SAME_LOWER_CASE_QNAME("limitingConeAngle"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERYTHINMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("verythinmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("verythinmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_DASHOFFSET = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-dashoffset"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-dashoffset"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STROKE_MITERLIMIT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("stroke-miterlimit"),
            AttributeName.SAME_LOWER_CASE_QNAME("stroke-miterlimit"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ALIGNMENT_BASELINE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("alignment-baseline"),
            AttributeName.SAME_LOWER_CASE_QNAME("alignment-baseline"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ONREADYSTATECHANGE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("onreadystatechange"),
            AttributeName.SAME_LOWER_CASE_QNAME("onreadystatechange"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName OVERLINE_THICKNESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("overline-thickness"),
            AttributeName.SAME_LOWER_CASE_QNAME("overline-thickness"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNDERLINE_POSITION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("underline-position"),
            AttributeName.SAME_LOWER_CASE_QNAME("underline-position"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERYTHICKMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("verythickmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("verythickmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName REQUIREDEXTENSIONS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "requiredextensions", "requiredExtensions"),
            AttributeName.SAME_LOWER_CASE_QNAME("requiredExtensions"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLOR_INTERPOLATION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("color-interpolation"),
            AttributeName.SAME_LOWER_CASE_QNAME("color-interpolation"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName UNDERLINE_THICKNESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("underline-thickness"),
            AttributeName.SAME_LOWER_CASE_QNAME("underline-thickness"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PRESERVEASPECTRATIO = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "preserveaspectratio", "preserveAspectRatio"),
            AttributeName.SAME_LOWER_CASE_QNAME("preserveAspectRatio"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName PATTERNCONTENTUNITS = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "patterncontentunits", "patternContentUnits"),
            AttributeName.SAME_LOWER_CASE_QNAME("patternContentUnits"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_MULTISELECTABLE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-multiselectable"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-multiselectable"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName SCRIPTSIZEMULTIPLIER = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("scriptsizemultiplier"),
            AttributeName.SAME_LOWER_CASE_QNAME("scriptsizemultiplier"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName ARIA_ACTIVEDESCENDANT = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("aria-activedescendant"),
            AttributeName.SAME_LOWER_CASE_QNAME("aria-activedescendant"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERYVERYTHINMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("veryverythinmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("veryverythinmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName VERYVERYTHICKMATHSPACE = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("veryverythickmathspace"),
            AttributeName.SAME_LOWER_CASE_QNAME("veryverythickmathspace"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STRIKETHROUGH_POSITION = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("strikethrough-position"),
            AttributeName.SAME_LOWER_CASE_QNAME("strikethrough-position"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName STRIKETHROUGH_THICKNESS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("strikethrough-thickness"),
            AttributeName.SAME_LOWER_CASE_QNAME("strikethrough-thickness"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName EXTERNALRESOURCESREQUIRED = new AttributeName(
            AttributeName.ALL_NO_NS, AttributeName.CAMEL_CASE_LOCAL(
                    "externalresourcesrequired", "externalResourcesRequired"),
            AttributeName.SAME_LOWER_CASE_QNAME("externalResourcesRequired"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GLYPH_ORIENTATION_VERTICAL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("glyph-orientation-vertical"),
            AttributeName.SAME_LOWER_CASE_QNAME("glyph-orientation-vertical"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName COLOR_INTERPOLATION_FILTERS = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("color-interpolation-filters"),
            AttributeName.SAME_LOWER_CASE_QNAME("color-interpolation-filters"),
            AttributeName.ALL_NCNAME, false);

    public static final AttributeName GLYPH_ORIENTATION_HORIZONTAL = new AttributeName(
            AttributeName.ALL_NO_NS,
            AttributeName.SAME_LOWER_CASE_LOCAL("glyph-orientation-horizontal"),
            AttributeName.SAME_LOWER_CASE_QNAME("glyph-orientation-horizontal"),
            AttributeName.ALL_NCNAME, false);

    private final static @NoLength AttributeName[] ATTRIBUTE_NAMES = { AttributeName.D,
            AttributeName.K, AttributeName.R, AttributeName.X, AttributeName.Y,
            AttributeName.Z, AttributeName.BY, AttributeName.CX,
            AttributeName.CY, AttributeName.DX, AttributeName.DY,
            AttributeName.G2, AttributeName.G1, AttributeName.FX,
            AttributeName.FY, AttributeName.K4, AttributeName.K2,
            AttributeName.K3, AttributeName.K1, AttributeName.ID,
            AttributeName.IN, AttributeName.U2, AttributeName.U1,
            AttributeName.RT, AttributeName.RX, AttributeName.RY,
            AttributeName.TO, AttributeName.Y2, AttributeName.Y1,
            AttributeName.X1, AttributeName.X2, AttributeName.ALT,
            AttributeName.DIR, AttributeName.DUR, AttributeName.END,
            AttributeName.FOR, AttributeName.IN2, AttributeName.MAX,
            AttributeName.MIN, AttributeName.LOW, AttributeName.REL,
            AttributeName.REV, AttributeName.SRC, AttributeName.AXIS,
            AttributeName.ABBR, AttributeName.BBOX, AttributeName.CITE,
            AttributeName.CODE, AttributeName.BIAS, AttributeName.COLS,
            AttributeName.END , AttributeName.CLIP, AttributeName.CHAR,
            AttributeName.BASE, AttributeName.EDGE, AttributeName.DATA,
            AttributeName.FILL, AttributeName.FROM, AttributeName.FORM,
            AttributeName.FACE, AttributeName.HIGH, AttributeName.HREF,
            AttributeName.OPEN, AttributeName.ICON, AttributeName.NAME,
            AttributeName.MODE, AttributeName.MASK, AttributeName.LINK,
            AttributeName.LANG, AttributeName.LIST, AttributeName.TYPE,
            AttributeName.WHEN, AttributeName.WRAP, AttributeName.TEXT,
            AttributeName.PATH, AttributeName.PING, AttributeName.REFX,
            AttributeName.REFY, AttributeName.SIZE, AttributeName.SEED,
            AttributeName.ROWS, AttributeName.SPAN, AttributeName.STEP,
            AttributeName.ROLE, AttributeName.XREF, AttributeName.ASYNC,
            AttributeName.ALINK, AttributeName.ALIGN, AttributeName.CLOSE,
            AttributeName.COLOR, AttributeName.CLASS, AttributeName.CLEAR,
            AttributeName.BEGIN, AttributeName.DEPTH, AttributeName.DEFER,
            AttributeName.FENCE, AttributeName.FRAME, AttributeName.ISMAP,
            AttributeName.ONEND, AttributeName.INDEX, AttributeName.ORDER,
            AttributeName.OTHER, AttributeName.ONCUT, AttributeName.NARGS,
            AttributeName.MEDIA, AttributeName.LABEL, AttributeName.LOCAL,
            AttributeName.WIDTH, AttributeName.TITLE, AttributeName.VLINK,
            AttributeName.VALUE, AttributeName.SLOPE, AttributeName.SHAPE,
            AttributeName.SCOPE, AttributeName.SCALE, AttributeName.SPEED,
            AttributeName.STYLE, AttributeName.RULES, AttributeName.STEMH,
            AttributeName.STEMV, AttributeName.START, AttributeName.XMLNS,
            AttributeName.ACCEPT, AttributeName.ACCENT, AttributeName.ASCENT,
            AttributeName.ACTIVE, AttributeName.ALTIMG, AttributeName.ACTION,
            AttributeName.BORDER, AttributeName.CURSOR, AttributeName.COORDS,
            AttributeName.FILTER, AttributeName.FORMAT, AttributeName.HIDDEN,
            AttributeName.HSPACE, AttributeName.HEIGHT, AttributeName.ONMOVE,
            AttributeName.ONLOAD, AttributeName.ONDRAG, AttributeName.ORIGIN,
            AttributeName.ONZOOM, AttributeName.ONHELP, AttributeName.ONSTOP,
            AttributeName.ONDROP, AttributeName.ONBLUR, AttributeName.OBJECT,
            AttributeName.OFFSET, AttributeName.ORIENT, AttributeName.ONCOPY,
            AttributeName.NOWRAP, AttributeName.NOHREF, AttributeName.MACROS,
            AttributeName.METHOD, AttributeName.LOWSRC, AttributeName.LSPACE,
            AttributeName.LQUOTE, AttributeName.USEMAP, AttributeName.VALUE_,
            AttributeName.WIDTHS, AttributeName.TARGET, AttributeName.VALUES,
            AttributeName.VALIGN, AttributeName.VSPACE, AttributeName.POSTER,
            AttributeName.POINTS, AttributeName.PROMPT, AttributeName.SCOPED,
            AttributeName.STRING, AttributeName.SCHEME, AttributeName.STROKE,
            AttributeName.RADIUS, AttributeName.RESULT, AttributeName.REPEAT,
            AttributeName.RSPACE, AttributeName.ROTATE, AttributeName.RQUOTE,
            AttributeName.ALTTEXT, AttributeName.ARCHIVE,
            AttributeName.AZIMUTH, AttributeName.CLOSURE,
            AttributeName.CHECKED, AttributeName.CLASSID,
            AttributeName.CHAROFF, AttributeName.BGCOLOR,
            AttributeName.COLSPAN, AttributeName.CHARSET,
            AttributeName.COMPACT, AttributeName.CONTENT,
            AttributeName.ENCTYPE, AttributeName.DATASRC,
            AttributeName.DATAFLD, AttributeName.DECLARE,
            AttributeName.DISPLAY, AttributeName.DIVISOR,
            AttributeName.DEFAULT, AttributeName.DESCENT,
            AttributeName.KERNING, AttributeName.HANGING,
            AttributeName.HEADERS, AttributeName.ONPASTE,
            AttributeName.ONCLICK, AttributeName.OPTIMUM,
            AttributeName.ONBEGIN, AttributeName.ONKEYUP,
            AttributeName.ONFOCUS, AttributeName.ONERROR,
            AttributeName.ONINPUT, AttributeName.ONABORT,
            AttributeName.ONSTART, AttributeName.ONRESET,
            AttributeName.OPACITY, AttributeName.NOSHADE,
            AttributeName.MINSIZE, AttributeName.MAXSIZE,
            AttributeName.LARGEOP, AttributeName.UNICODE,
            AttributeName.TARGETX, AttributeName.TARGETY,
            AttributeName.VIEWBOX, AttributeName.VERSION,
            AttributeName.PATTERN, AttributeName.PROFILE,
            AttributeName.START  , AttributeName.SPACING,
            AttributeName.RESTART, AttributeName.ROWSPAN,
            AttributeName.SANDBOX, AttributeName.SUMMARY,
            AttributeName.STANDBY, AttributeName.REPLACE,
            AttributeName.ADDITIVE, AttributeName.CALCMODE,
            AttributeName.CODETYPE, AttributeName.CODEBASE,
            AttributeName.BEVELLED, AttributeName.BASELINE,
            AttributeName.EXPONENT, AttributeName.EDGEMODE,
            AttributeName.ENCODING, AttributeName.GLYPHREF,
            AttributeName.DATETIME, AttributeName.DISABLED,
            AttributeName.FONTSIZE, AttributeName.KEYTIMES,
            AttributeName.LOOPEND , AttributeName.PANOSE_1,
            AttributeName.HREFLANG, AttributeName.ONRESIZE,
            AttributeName.ONCHANGE, AttributeName.ONBOUNCE,
            AttributeName.ONUNLOAD, AttributeName.ONFINISH,
            AttributeName.ONSCROLL, AttributeName.OPERATOR,
            AttributeName.OVERFLOW, AttributeName.ONSUBMIT,
            AttributeName.ONREPEAT, AttributeName.ONSELECT,
            AttributeName.NOTATION, AttributeName.NORESIZE,
            AttributeName.MANIFEST, AttributeName.MATHSIZE,
            AttributeName.MULTIPLE, AttributeName.LONGDESC,
            AttributeName.LANGUAGE, AttributeName.TEMPLATE,
            AttributeName.TABINDEX, AttributeName.READONLY,
            AttributeName.SELECTED, AttributeName.ROWLINES,
            AttributeName.SEAMLESS, AttributeName.ROWALIGN,
            AttributeName.STRETCHY, AttributeName.REQUIRED,
            AttributeName.XML_BASE, AttributeName.XML_LANG,
            AttributeName.X_HEIGHT, AttributeName.CONTROLS ,
            AttributeName.ARIA_OWNS, AttributeName.AUTOFOCUS,
            AttributeName.ARIA_SORT, AttributeName.ACCESSKEY,
            AttributeName.AMPLITUDE, AttributeName.ARIA_LIVE,
            AttributeName.CLIP_RULE, AttributeName.CLIP_PATH,
            AttributeName.EQUALROWS, AttributeName.ELEVATION,
            AttributeName.DIRECTION, AttributeName.DRAGGABLE,
            AttributeName.FILTERRES, AttributeName.FILL_RULE,
            AttributeName.FONTSTYLE, AttributeName.FONT_SIZE,
            AttributeName.KEYPOINTS, AttributeName.HIDEFOCUS,
            AttributeName.ONMESSAGE, AttributeName.INTERCEPT,
            AttributeName.ONDRAGEND, AttributeName.ONMOVEEND,
            AttributeName.ONINVALID, AttributeName.ONKEYDOWN,
            AttributeName.ONFOCUSIN, AttributeName.ONMOUSEUP,
            AttributeName.INPUTMODE, AttributeName.ONROWEXIT,
            AttributeName.MATHCOLOR, AttributeName.MASKUNITS,
            AttributeName.MAXLENGTH, AttributeName.LINEBREAK,
            AttributeName.TRANSFORM, AttributeName.V_HANGING,
            AttributeName.VALUETYPE, AttributeName.POINTSATZ,
            AttributeName.POINTSATX, AttributeName.POINTSATY,
            AttributeName.SYMMETRIC, AttributeName.SCROLLING,
            AttributeName.REPEATDUR, AttributeName.SELECTION,
            AttributeName.SEPARATOR, AttributeName.AUTOPLAY  ,
            AttributeName.XML_SPACE, AttributeName.ARIA_GRAB ,
            AttributeName.ARIA_BUSY , AttributeName.AUTOSUBMIT,
            AttributeName.ALPHABETIC, AttributeName.ACTIONTYPE,
            AttributeName.ACCUMULATE, AttributeName.ARIA_LEVEL,
            AttributeName.COLUMNSPAN, AttributeName.CAP_HEIGHT,
            AttributeName.BACKGROUND, AttributeName.GLYPH_NAME,
            AttributeName.GROUPALIGN, AttributeName.FONTFAMILY,
            AttributeName.FONTWEIGHT, AttributeName.FONT_STYLE,
            AttributeName.KEYSPLINES, AttributeName.LOOPSTART ,
            AttributeName.PLAYCOUNT , AttributeName.HTTP_EQUIV,
            AttributeName.ONACTIVATE, AttributeName.OCCURRENCE,
            AttributeName.IRRELEVANT, AttributeName.ONDBLCLICK,
            AttributeName.ONDRAGDROP, AttributeName.ONKEYPRESS,
            AttributeName.ONROWENTER, AttributeName.ONDRAGOVER,
            AttributeName.ONFOCUSOUT, AttributeName.ONMOUSEOUT,
            AttributeName.NUMOCTAVES, AttributeName.MARKER_MID,
            AttributeName.MARKER_END, AttributeName.TEXTLENGTH,
            AttributeName.VISIBILITY, AttributeName.VIEWTARGET,
            AttributeName.VERT_ADV_Y, AttributeName.PATHLENGTH,
            AttributeName.REPEAT_MAX, AttributeName.RADIOGROUP,
            AttributeName.STOP_COLOR, AttributeName.SEPARATORS,
            AttributeName.REPEAT_MIN, AttributeName.ROWSPACING,
            AttributeName.ZOOMANDPAN, AttributeName.XLINK_TYPE,
            AttributeName.XLINK_ROLE, AttributeName.XLINK_HREF,
            AttributeName.XLINK_SHOW, AttributeName.ACCENTUNDER,
            AttributeName.ARIA_SECRET, AttributeName.ARIA_ATOMIC,
            AttributeName.ARIA_FLOWTO, AttributeName.ARABIC_FORM,
            AttributeName.CELLPADDING, AttributeName.CELLSPACING,
            AttributeName.COLUMNWIDTH, AttributeName.COLUMNALIGN,
            AttributeName.COLUMNLINES, AttributeName.CONTEXTMENU,
            AttributeName.BASEPROFILE, AttributeName.FONT_FAMILY,
            AttributeName.FRAMEBORDER, AttributeName.FILTERUNITS,
            AttributeName.FLOOD_COLOR, AttributeName.FONT_WEIGHT,
            AttributeName.HORIZ_ADV_X, AttributeName.ONDRAGLEAVE,
            AttributeName.ONMOUSEMOVE, AttributeName.ORIENTATION,
            AttributeName.ONMOUSEDOWN, AttributeName.ONMOUSEOVER,
            AttributeName.ONDRAGENTER, AttributeName.IDEOGRAPHIC,
            AttributeName.ONBEFORECUT, AttributeName.ONFORMINPUT,
            AttributeName.ONDRAGSTART, AttributeName.ONMOVESTART,
            AttributeName.MARKERUNITS, AttributeName.MATHVARIANT,
            AttributeName.MARGINWIDTH, AttributeName.MARKERWIDTH,
            AttributeName.TEXT_ANCHOR, AttributeName.TABLEVALUES,
            AttributeName.SCRIPTLEVEL, AttributeName.REPEATCOUNT,
            AttributeName.STITCHTILES, AttributeName.STARTOFFSET,
            AttributeName.SCROLLDELAY, AttributeName.XMLNS_XLINK,
            AttributeName.XLINK_TITLE, AttributeName.ARIA_HIDDEN ,
            AttributeName.AUTOCOMPLETE, AttributeName.ARIA_SETSIZE,
            AttributeName.ARIA_CHANNEL, AttributeName.EQUALCOLUMNS,
            AttributeName.DISPLAYSTYLE, AttributeName.DATAFORMATAS,
            AttributeName.FILL_OPACITY, AttributeName.FONT_VARIANT,
            AttributeName.FONT_STRETCH, AttributeName.FRAMESPACING,
            AttributeName.KERNELMATRIX, AttributeName.ONDEACTIVATE,
            AttributeName.ONROWSDELETE, AttributeName.ONMOUSELEAVE,
            AttributeName.ONFORMCHANGE, AttributeName.ONCELLCHANGE,
            AttributeName.ONMOUSEWHEEL, AttributeName.ONMOUSEENTER,
            AttributeName.ONAFTERPRINT, AttributeName.ONBEFORECOPY,
            AttributeName.MARGINHEIGHT, AttributeName.MARKERHEIGHT,
            AttributeName.MARKER_START, AttributeName.MATHEMATICAL,
            AttributeName.LENGTHADJUST, AttributeName.UNSELECTABLE,
            AttributeName.UNICODE_BIDI, AttributeName.UNITS_PER_EM,
            AttributeName.WORD_SPACING, AttributeName.WRITING_MODE,
            AttributeName.V_ALPHABETIC, AttributeName.PATTERNUNITS,
            AttributeName.SPREADMETHOD, AttributeName.SURFACESCALE,
            AttributeName.STROKE_WIDTH, AttributeName.REPEAT_START,
            AttributeName.STDDEVIATION, AttributeName.STOP_OPACITY,
            AttributeName.ARIA_CHECKED , AttributeName.ARIA_PRESSED ,
            AttributeName.ARIA_INVALID , AttributeName.ARIA_CONTROLS,
            AttributeName.ARIA_HASPOPUP, AttributeName.ACCENT_HEIGHT,
            AttributeName.ARIA_VALUENOW, AttributeName.ARIA_RELEVANT,
            AttributeName.ARIA_POSINSET, AttributeName.ARIA_VALUEMAX,
            AttributeName.ARIA_READONLY, AttributeName.ARIA_REQUIRED,
            AttributeName.ATTRIBUTETYPE, AttributeName.ATTRIBUTENAME,
            AttributeName.ARIA_DATATYPE, AttributeName.ARIA_VALUEMIN,
            AttributeName.BASEFREQUENCY, AttributeName.COLUMNSPACING,
            AttributeName.COLOR_PROFILE, AttributeName.CLIPPATHUNITS,
            AttributeName.DEFINITIONURL, AttributeName.GRADIENTUNITS,
            AttributeName.FLOOD_OPACITY, AttributeName.ONAFTERUPDATE,
            AttributeName.ONERRORUPDATE, AttributeName.ONBEFOREPASTE,
            AttributeName.ONLOSECAPTURE, AttributeName.ONCONTEXTMENU,
            AttributeName.ONSELECTSTART, AttributeName.ONBEFOREPRINT,
            AttributeName.MOVABLELIMITS, AttributeName.LINETHICKNESS,
            AttributeName.UNICODE_RANGE, AttributeName.THINMATHSPACE,
            AttributeName.VERT_ORIGIN_X, AttributeName.VERT_ORIGIN_Y,
            AttributeName.V_IDEOGRAPHIC, AttributeName.PRESERVEALPHA,
            AttributeName.SCRIPTMINSIZE, AttributeName.SPECIFICATION,
            AttributeName.XLINK_ACTUATE, AttributeName.XLINK_ARCROLE,
            AttributeName.ARIA_EXPANDED , AttributeName.ARIA_DISABLED ,
            AttributeName.ARIA_SELECTED , AttributeName.ACCEPT_CHARSET,
            AttributeName.ALIGNMENTSCOPE, AttributeName.ARIA_MULTILINE,
            AttributeName.BASELINE_SHIFT, AttributeName.HORIZ_ORIGIN_X,
            AttributeName.HORIZ_ORIGIN_Y, AttributeName.ONBEFOREUPDATE,
            AttributeName.ONFILTERCHANGE, AttributeName.ONROWSINSERTED,
            AttributeName.ONBEFOREUNLOAD, AttributeName.MATHBACKGROUND,
            AttributeName.LETTER_SPACING, AttributeName.LIGHTING_COLOR,
            AttributeName.THICKMATHSPACE, AttributeName.TEXT_RENDERING,
            AttributeName.V_MATHEMATICAL, AttributeName.POINTER_EVENTS,
            AttributeName.PRIMITIVEUNITS, AttributeName.SYSTEMLANGUAGE,
            AttributeName.STROKE_LINECAP, AttributeName.SUBSCRIPTSHIFT,
            AttributeName.STROKE_OPACITY, AttributeName.ARIA_DROPEFFECT,
            AttributeName.ARIA_LABELLEDBY, AttributeName.ARIA_TEMPLATEID,
            AttributeName.COLOR_RENDERING, AttributeName.CONTENTEDITABLE,
            AttributeName.DIFFUSECONSTANT, AttributeName.ONDATAAVAILABLE,
            AttributeName.ONCONTROLSELECT, AttributeName.IMAGE_RENDERING,
            AttributeName.MEDIUMMATHSPACE, AttributeName.TEXT_DECORATION,
            AttributeName.SHAPE_RENDERING, AttributeName.STROKE_LINEJOIN,
            AttributeName.REPEAT_TEMPLATE, AttributeName.ARIA_DESCRIBEDBY,
            AttributeName.CONTENTSTYLETYPE, AttributeName.FONT_SIZE_ADJUST,
            AttributeName.KERNELUNITLENGTH, AttributeName.ONBEFOREACTIVATE,
            AttributeName.ONPROPERTYCHANGE, AttributeName.ONDATASETCHANGED,
            AttributeName.MASKCONTENTUNITS, AttributeName.PATTERNTRANSFORM,
            AttributeName.REQUIREDFEATURES, AttributeName.RENDERING_INTENT,
            AttributeName.SPECULAREXPONENT, AttributeName.SPECULARCONSTANT,
            AttributeName.SUPERSCRIPTSHIFT, AttributeName.STROKE_DASHARRAY,
            AttributeName.XCHANNELSELECTOR, AttributeName.YCHANNELSELECTOR,
            AttributeName.ARIA_AUTOCOMPLETE, AttributeName.CONTENTSCRIPTTYPE,
            AttributeName.ENABLE_BACKGROUND, AttributeName.DOMINANT_BASELINE,
            AttributeName.GRADIENTTRANSFORM, AttributeName.ONBEFORDEACTIVATE,
            AttributeName.ONDATASETCOMPLETE, AttributeName.OVERLINE_POSITION,
            AttributeName.ONBEFOREEDITFOCUS, AttributeName.LIMITINGCONEANGLE,
            AttributeName.VERYTHINMATHSPACE, AttributeName.STROKE_DASHOFFSET,
            AttributeName.STROKE_MITERLIMIT, AttributeName.ALIGNMENT_BASELINE,
            AttributeName.ONREADYSTATECHANGE, AttributeName.OVERLINE_THICKNESS,
            AttributeName.UNDERLINE_POSITION, AttributeName.VERYTHICKMATHSPACE,
            AttributeName.REQUIREDEXTENSIONS,
            AttributeName.COLOR_INTERPOLATION,
            AttributeName.UNDERLINE_THICKNESS,
            AttributeName.PRESERVEASPECTRATIO,
            AttributeName.PATTERNCONTENTUNITS,
            AttributeName.ARIA_MULTISELECTABLE,
            AttributeName.SCRIPTSIZEMULTIPLIER,
            AttributeName.ARIA_ACTIVEDESCENDANT,
            AttributeName.VERYVERYTHINMATHSPACE,
            AttributeName.VERYVERYTHICKMATHSPACE,
            AttributeName.STRIKETHROUGH_POSITION,
            AttributeName.STRIKETHROUGH_THICKNESS,
            AttributeName.EXTERNALRESOURCESREQUIRED,
            AttributeName.GLYPH_ORIENTATION_VERTICAL,
            AttributeName.COLOR_INTERPOLATION_FILTERS,
            AttributeName.GLYPH_ORIENTATION_HORIZONTAL, };

    private final static @NoLength int[] ATTRIBUTE_HASHES = { 1153, 1383, 1601, 1793,
            1827, 1857, 68600, 69146, 69177, 70237, 70270, 71572, 71669, 72415,
            72444, 74846, 74904, 74943, 75001, 75276, 75590, 84742, 84839,
            85575, 85963, 85992, 87204, 88074, 88171, 89130, 89163, 3207892,
            3283895, 3284791, 3338752, 3358197, 3369562, 3539124, 3562402,
            3574260, 3670335, 3696933, 3721879, 135280021, 135346322,
            136317019, 136475749, 136548517, 136652214, 136884919, 136896708,
            136902418, 136942992, 137292068, 139120259, 139785574, 142250603,
            142314056, 142331176, 142519584, 144752417, 145106895, 146147200,
            146765926, 148805544, 149655723, 149809441, 150018784, 150445028,
            150923321, 152528754, 152536216, 152647366, 152962785, 155219321,
            155654904, 157317483, 157350248, 157437941, 157447478, 157604838,
            157685404, 157894402, 158315188, 166078431, 169409980, 169700259,
            169856932, 170007032, 170409695, 170466488, 170513710, 170608367,
            173028944, 173896963, 176090625, 176129212, 179390001, 179489057,
            179627464, 179840468, 179849042, 180004216, 181779081, 183027151,
            183645319, 183698797, 185922012, 185997252, 188312483, 188675799,
            190977533, 190992569, 191006194, 191033518, 191038774, 191096249,
            191166163, 191194426, 191522106, 191568039, 200104642, 202506661,
            202537381, 202602917, 203070590, 203120766, 203389054, 203690071,
            203971238, 203986524, 209040857, 209125756, 212055489, 212322418,
            212746849, 213002877, 213055164, 213088023, 213259873, 213273386,
            213435118, 213437318, 213438231, 213493071, 213532268, 213542834,
            213584431, 213659891, 215285828, 215880731, 216112976, 216684637,
            217369699, 217565298, 217576549, 218186795, 219231738, 219743185,
            220082234, 221623802, 221986406, 222283890, 223089542, 223138630,
            223311265, 224547358, 224587256, 224589550, 224655650, 224785518,
            224810917, 224813302, 225429618, 225432950, 225440869, 236107233,
            236709921, 236838947, 237117095, 237143271, 237172455, 237209953,
            237354143, 237372743, 237668065, 237703073, 237714273, 239743521,
            240512803, 240522627, 240560417, 240656513, 241015715, 241062755,
            241065383, 243523041, 245865199, 246261793, 246556195, 246774817,
            246923491, 246928419, 246981667, 247014847, 247058369, 247112833,
            247118177, 247119137, 247128739, 247316903, 249533729, 250235623,
            250269543, 251402351, 252339047, 253260911, 253293679, 254844367,
            255547879, 256077281, 256345377, 257839617, 258124199, 258354465,
            258605063, 258744193, 258845603, 258856961, 258926689, 270174334,
            270709417, 270778994, 270781796, 271478858, 271490090, 272870654,
            273335275, 273369140, 273924313, 274108530, 274116736, 276818662,
            277476156, 278205908, 279156579, 279349675, 280108533, 280128712,
            280132869, 280162403, 280280292, 280413430, 280506130, 280677397,
            280678580, 280686710, 280689066, 282736758, 283110901, 283275116,
            283823226, 283890012, 284479340, 284606461, 286700477, 286798916,
            291557706, 291665349, 291804100, 292138018, 292166446, 292418738,
            292451039, 300298041, 300374839, 300597935, 302075482, 303073389,
            303083839, 303266673, 303354997, 303724281, 303819694, 304242723,
            304382625, 306247792, 307227811, 307468786, 307724489, 309671175,
            310252031, 310358241, 310373094, 311015256, 313357609, 313683893,
            313701861, 313706996, 313707317, 313710350, 314027746, 314038181,
            314091299, 314205627, 314233813, 316741830, 316797986, 317486755,
            317794164, 320076137, 322657125, 322887778, 323506876, 323572412,
            323605180, 325060058, 325320188, 325398738, 325541490, 325671619,
            333866609, 333868843, 335100592, 335107319, 336806130, 337212108,
            337282686, 337285434, 337585223, 338036037, 338298087, 338566051,
            340943551, 341190970, 342995704, 343352124, 343912673, 344585053,
            345331280, 346325327, 346977248, 347218098, 347262163, 347278576,
            347438191, 347655959, 347684788, 347726430, 347727772, 347776035,
            347776629, 349500753, 350880161, 350887073, 353384123, 355496998,
            355906922, 355979793, 356545959, 358637867, 358905016, 359164318,
            359247286, 359350571, 359579447, 365560330, 367399355, 367420285,
            367510727, 368013212, 370234760, 370353345, 370710317, 371122285,
            371194213, 371448425, 371448430, 371545055, 371596922, 371758751,
            371964792, 372151328, 376550136, 376710172, 376795771, 376826271,
            376906556, 380514830, 380774774, 380775037, 381030322, 381136500,
            381281631, 381282269, 381285504, 381330595, 381331422, 381335911,
            381336484, 383907298, 383917408, 384595009, 384595013, 387799894,
            387823201, 392581647, 392584937, 392742684, 392906485, 393003349,
            400644707, 400973830, 402197030, 404469244, 404478897, 404694860,
            406887479, 408294949, 408789955, 410022510, 410467324, 410586448,
            410945965, 411845275, 414327152, 414327932, 414329781, 414346257,
            414346439, 414639928, 414835998, 414894517, 414986533, 417465377,
            417465381, 417492216, 418259232, 419310946, 420103495, 420242342,
            420380455, 420658662, 420717432, 423183880, 424539259, 425929170,
            425972964, 426050649, 426126450, 426142833, 426607922, 435757609,
            435757617, 435757998, 437289840, 437347469, 437412335, 437423943,
            437455540, 437462252, 437597991, 437617485, 437986507, 438015591,
            438034813, 438038966, 438179623, 438347971, 438483573, 438547062,
            438895551, 441592676, 442032555, 443548979, 447881379, 447881655,
            447881895, 447887844, 448416189, 448445746, 448449012, 450942191,
            452816744, 453668677, 454434495, 456610076, 456642844, 456738709,
            457544600, 459451897, 459680944, 468058810, 468083581, 469312038,
            469312046, 469312054, 470964084, 471470955, 471567278, 472267822,
            481177859, 481210627, 481435874, 481455115, 481485378, 481490218,
            485105638, 486005878, 486383494, 487988916, 488103783, 490661867,
            491574090, 491578272, 493041952, 493441205, 493582844, 493716979,
            504577572, 504740359, 505091638, 505592418, 505656212, 509516275,
            514998531, 515571132, 515594682, 518712698, 521362273, 526592419,
            526807354, 527348842, 538294791, 539214049, 544689535, 545535009,
            548544752, 548563346, 548595116, 551679010, 558034099, 560329411,
            560356209, 560671018, 560671152, 560692590, 560845442, 569212097,
            569474241, 572252718, 572768481, 575326764, 576174758, 576190819,
            582099184, 582099438, 582372519, 582558889, 586552164, 591325418,
            594231990, 594243961, 605711268, 615672071, 616086845, 621792370,
            624879850, 627432831, 640040548, 654392808, 658675477, 659420283,
            672891587, 694768102, 705890982, 725543146, 759097578, 761686526,
            795383908, 843809551, 878105336, 908643300, 945213471, };

}
