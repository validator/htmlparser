/*
 * Copyright (c) 2006 Henri Sivonen
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

package nu.validator.htmlparser.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class Encoding {

    public static final Encoding UTF8;

    public static final Encoding UTF16LE;

    public static final Encoding UTF16BE;

    public static final Encoding WINDOWS1252;

    private static Map<String, Encoding> encodingByLabel =
        new HashMap<String, Encoding>();

    private static void createEncoding(String name, String[] labels) {
        if (!Charset.isSupported(name)) {
            return;
        }
        Charset cs = Charset.forName(name);
        Encoding enc = new Encoding(name.toLowerCase().intern(), cs);
        for (String label : labels) {
            encodingByLabel.put(label, enc);
        }
    }

    static {
        /* See https://encoding.spec.whatwg.org/#names-and-labels */
        createEncoding( //
                "UTF-8", new String[] { //
                        "unicode-1-1-utf-8", //
                        "unicode11utf8", //
                        "unicode20utf8", //
                        "utf-8", //
                        "utf8", //
                        "x-unicode20utf8" });
        createEncoding( //
                "IBM866", new String[] { //
                        "866", //
                        "cp866", //
                        "csibm866", //
                        "ibm866" });
        createEncoding( //
                "ISO-8859-2", new String[] { //
                        "csisolatin2", //
                        "iso-8859-2", //
                        "iso-ir-101", //
                        "iso8859-2", //
                        "iso88592", //
                        "iso_8859-2", //
                        "iso_8859-2:1987", //
                        "l2", //
                        "latin2" });
        createEncoding( //
                "ISO-8859-3", new String[] { //
                        "csisolatin3", //
                        "iso-8859-3", //
                        "iso-ir-109", //
                        "iso8859-3", //
                        "iso88593", //
                        "iso_8859-3", //
                        "iso_8859-3:1988", //
                        "l3", //
                        "latin3" });
        createEncoding( //
                "ISO-8859-4", new String[] { //
                        "csisolatin4", //
                        "iso-8859-4", //
                        "iso-ir-110", //
                        "iso8859-4", //
                        "iso88594", //
                        "iso_8859-4", //
                        "iso_8859-4:1988", //
                        "l4", //
                        "latin4" });
        createEncoding( //
                "ISO-8859-5", new String[] { //
                        "csisolatincyrillic", //
                        "cyrillic", //
                        "iso-8859-5", //
                        "iso-ir-144", //
                        "iso8859-5", //
                        "iso88595", //
                        "iso_8859-5", //
                        "iso_8859-5:1988" });
        createEncoding( //
                "ISO-8859-6", new String[] { //
                        "arabic", //
                        "asmo-708", //
                        "csiso88596e", //
                        "csiso88596i", //
                        "csisolatinarabic", //
                        "ecma-114", //
                        "iso-8859-6", //
                        "iso-8859-6-e", //
                        "iso-8859-6-i", //
                        "iso-ir-127", //
                        "iso8859-6", //
                        "iso88596", //
                        "iso_8859-6", //
                        "iso_8859-6:1987" });
        createEncoding( //
                "ISO-8859-7", new String[] { //
                        "csisolatingreek", //
                        "ecma-118", //
                        "elot_928", //
                        "greek", //
                        "greek8", //
                        "iso-8859-7", //
                        "iso-ir-126", //
                        "iso8859-7", //
                        "iso88597", //
                        "iso_8859-7", //
                        "iso_8859-7:1987", //
                        "sun_eu_greek" });
        createEncoding( //
                "ISO-8859-8", new String[] { //
                        "csiso88598e", //
                        "csisolatinhebrew", //
                        "hebrew", //
                        "iso-8859-8", //
                        "iso-8859-8-e", //
                        "iso-ir-138", //
                        "iso8859-8", //
                        "iso88598", //
                        "iso_8859-8", //
                        "iso_8859-8:1988", //
                        "visual" });
        createEncoding( //
                // Unsupported in Java
                "ISO-8859-8-I", new String[] { //
                        "csiso88598i", //
                        "iso-8859-8-i", //
                        "logical" });
        createEncoding( //
                // Unsupported in Java
                "ISO-8859-10", new String[] { //
                        "csisolatin6", //
                        "iso-8859-10", //
                        "iso-ir-157", //
                        "iso8859-10", //
                        "iso885910", //
                        "l6", //
                        "latin6" });
        createEncoding( //
                "ISO-8859-13", new String[] { //
                        "iso-8859-13", //
                        "iso8859-13", //
                        "iso885913" });
        createEncoding( //
                // Unsupported in Java
                "ISO-8859-14", new String[] { //
                        "iso-8859-14", //
                        "iso8859-14", //
                        "iso885914" });
        createEncoding( //
                "ISO-8859-15", new String[] { //
                        "csisolatin9", //
                        "iso-8859-15", //
                        "iso8859-15", //
                        "iso885915", //
                        "iso_8859-15", //
                        "l9" });
        createEncoding( //
                "ISO-8859-16", new String[] { //
                        "iso-8859-16" });
        createEncoding( //
                "KOI8-R", new String[] { //
                        "cskoi8r", //
                        "koi", //
                        "koi8", //
                        "koi8-r", //
                        "koi8_r" });
        createEncoding( //
                "KOI8-U", new String[] { //
                        "koi8-ru", //
                        "koi8-u" });
        createEncoding( //
                // Unsupported in Java
                "macintosh", new String[] { //
                        "csmacintosh", //
                        "mac", //
                        "macintosh", //
                        "x-mac-roman" });
        createEncoding( //
                "windows-874", new String[] { //
                        "dos-874", //
                        "iso-8859-11", //
                        "iso8859-11", //
                        "iso885911", //
                        "tis-620", //
                        "windows-874" });
        createEncoding( //
                "windows-1250", new String[] { //
                        "cp1250", //
                        "windows-1250", //
                        "x-cp1250" });
        createEncoding( //
                "windows-1251", new String[] { //
                        "cp1251", //
                        "windows-1251", //
                        "x-cp1251" });
        createEncoding( //
                "windows-1252", new String[] { //
                        "ansi_x3.4-1968", //
                        "ascii", //
                        "cp1252", //
                        "cp819", //
                        "csisolatin1", //
                        "ibm819", //
                        "iso-8859-1", //
                        "iso-ir-100", //
                        "iso8859-1", //
                        "iso88591", //
                        "iso_8859-1", //
                        "iso_8859-1:1987", //
                        "l1", //
                        "latin1", //
                        "us-ascii", //
                        "windows-1252", //
                        "x-cp1252" });
        createEncoding( //
                "windows-1253", new String[] { //
                        "cp1253", //
                        "windows-1253", //
                        "x-cp1253" });
        createEncoding( //
                "windows-1254", new String[] { //
                        "cp1254", //
                        "csisolatin5", //
                        "iso-8859-9", //
                        "iso-ir-148", //
                        "iso8859-9", //
                        "iso88599", //
                        "iso_8859-9", //
                        "iso_8859-9:1989", //
                        "l5", //
                        "latin5", //
                        "windows-1254", //
                        "x-cp1254" });
        createEncoding( //
                "windows-1255", new String[] { //
                        "cp1255", //
                        "windows-1255", //
                        "x-cp1255" });
        createEncoding( //
                "windows-1256", new String[] { //
                        "cp1256", //
                        "windows-1256", //
                        "x-cp1256" });
        createEncoding( //
                "windows-1257", new String[] { //
                        "cp1257", //
                        "windows-1257", //
                        "x-cp1257" });
        createEncoding( //
                "windows-1258", new String[] { //
                        "cp1258", //
                        "windows-1258", //
                        "x-cp1258" });
        createEncoding( //
                // Unsupported in Java
                "x-mac-cyrillic", new String[] { //
                        "x-mac-cyrillic", //
                        "x-mac-ukrainian" });
        createEncoding( //
                "GBK", new String[] { //
                        "chinese", //
                        "csgb2312", //
                        "csiso58gb231280", //
                        "gb2312", //
                        "gb_2312", //
                        "gb_2312-80", //
                        "gbk", //
                        "iso-ir-58", //
                        "x-gbk" });
        createEncoding( //
                "gb18030", new String[] { //
                        "gb18030" });
        createEncoding( //
                "Big5", new String[] { //
                        "big5", //
                        "big5-hkscs", //
                        "cn-big5", //
                        "csbig5", //
                        "x-x-big5" });
        createEncoding( //
                "EUC-JP", new String[] { //
                        "cseucpkdfmtjapanese", //
                        "euc-jp", //
                        "x-euc-jp" });
        createEncoding( //
                "ISO-2022-JP", new String[] { //
                        "csiso2022jp", //
                        "iso-2022-jp" });
        createEncoding( //
                "Shift_JIS", new String[] { //
                        "csshiftjis", //
                        "ms932", //
                        "ms_kanji", //
                        "shift-jis", //
                        "shift_jis", //
                        "sjis", //
                        "windows-31j", //
                        "x-sjis" });
        createEncoding( //
                "EUC-KR", new String[] { //
                        "cseuckr", //
                        "csksc56011987", //
                        "euc-kr", //
                        "iso-ir-149", //
                        "korean", //
                        "ks_c_5601-1987", //
                        "ks_c_5601-1989", //
                        "ksc5601", //
                        "ksc_5601", //
                        "windows-949" });
        createEncoding( //
                // Special case
                "replacement", new String[] { //
                        "csiso2022kr", //
                        "hz-gb-2312", //
                        "iso-2022-cn", //
                        "iso-2022-cn-ext", //
                        "iso-2022-kr", //
                        "replacement" });
        createEncoding( //
                "UTF-16BE", new String[] { //
                        "unicodefffe", //
                        "utf-16be" });
        createEncoding( //
                "UTF-16LE", new String[] { //
                        "csunicode", //
                        "iso-10646-ucs-2", //
                        "ucs-2", //
                        "unicode", //
                        "unicodefeff", //
                        "utf-16", //
                        "utf-16le" });
        createEncoding( //
                // Special case
                "x-user-defined", new String[] { //
                        "x-user-defined" });
    }

    private final String canonName;

    private final Charset charset;

    static {
        UTF8 = forName("utf-8");
        UTF16BE = forName("utf-16be");
        UTF16LE = forName("utf-16le");
        WINDOWS1252 = forName("windows-1252");
    }

    public static Encoding forName(String name) {
        Encoding rv = encodingByLabel.get(toNameKey(name));
        if (rv == null) {
            throw new UnsupportedCharsetException(name);
        } else {
            return rv;
        }
    }

    public static String toNameKey(String str) {
        if (str == null) {
            return null;
        }
        int j = 0;
        char[] buf = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
            }
            if (!(c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r')) {
                buf[j] = c;
                j++;
            }
        }
        return new String(buf, 0, j);
    }

    /**
     * @param canonName
     * @param charset
     */
    private Encoding(final String canonName, final Charset charset) {
        this.canonName = canonName;
        this.charset = charset;
    }

    /**
     * Returns the canonName.
     * 
     * @return the canonName
     */
    public String getCanonName() {
        return canonName;
    }

    /**
     * @return
     * @see java.nio.charset.Charset#canEncode()
     */
    public boolean canEncode() {
        return charset.canEncode();
    }

    /**
     * @return
     * @see java.nio.charset.Charset#newDecoder()
     */
    public CharsetDecoder newDecoder() {
        return charset.newDecoder();
    }

    /**
     * @return
     * @see java.nio.charset.Charset#newEncoder()
     */
    public CharsetEncoder newEncoder() {
        return charset.newEncoder();
    }

    protected static String msgLegacyEncoding(String name) {
        return "Legacy encoding \u201C" + name + "\u201D used. Documents must"
                + " use UTF-8.";
    }

    protected static String msgIgnoredCharset(String ignored, String name) {
        return "Internal encoding declaration specified \u201C" + ignored
                + "\u201D. Continuing as if the encoding had been \u201C"
                + name + "\u201D.";
    }
    protected static String msgNotCanonicalName(String label, String name) {
        return "The encoding \u201C" + label + "\u201D is not the canonical"
                + " name of the character encoding in use. The canonical name"
                + " is \u201C" + name + "\u201D. (Charmod C024)";
    }

    protected static String msgBadInternalCharset(String internalCharset) {
        return "Internal encoding declaration named an unsupported character"
            + " encoding \u201C" + internalCharset + "\u201D.";
    }

    protected static String msgBadEncoding(String name) {
        return "Unsupported character encoding name: \u201C" + name + "\u201D.";
    }

    public static void main(String[] args) {
        for (Map.Entry<String, Encoding> entry : encodingByLabel.entrySet()) {
            String name = entry.getKey();
            Encoding enc = entry.getValue();
            System.out.printf("%21s: canon %13s\n", name, enc.getCanonName());
        }
    }

}
