/*
 * Copyright (c) 2005, 2006, 2007 Henri Sivonen
 * Copyright (c) 2007-2008 Mozilla Foundation
 * Portions of comments Copyright 2004-2007 Apple Computer, Inc., Mozilla 
 * Foundation, and Opera Software ASA.
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

/*
 * The comments following this one that use the same comment syntax as this 
 * comment are quotes from the WHATWG HTML 5 spec as of 2 June 2007 
 * amended as of June 23 2007.
 * That document came with this statement:
 * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
 * Opera Software ASA. You are granted a license to use, reproduce and 
 * create derivative works of this document."
 */

package nu.validator.htmlparser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.rewindable.RewindableInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * An implementatition of
 * http://www.whatwg.org/specs/web-apps/current-work/multipage/section-tokenisation.html
 * 
 * This class implements the <code>Locator</code> interface. This is not an
 * incidental implementation detail: Users of this class are encouraged to make
 * use of the <code>Locator</code> nature.
 * 
 * By default, the tokenizer may report data that XML 1.0 bans. The tokenizer
 * can be configured to treat these conditions as fatal or to coerce the infoset
 * to something that XML 1.0 allows.
 * 
 * @version $Id$
 * @author hsivonen
 */
public final class Tokenizer implements Locator {

    private enum State {
        DATA, TAG_OPEN, CLOSE_TAG_OPEN_PCDATA, TAG_NAME, BEFORE_ATTRIBUTE_NAME, ATTRIBUTE_NAME, AFTER_ATTRIBUTE_NAME, BEFORE_ATTRIBUTE_VALUE, ATTRIBUTE_VALUE_DOUBLE_QUOTED, ATTRIBUTE_VALUE_SINGLE_QUOTED, ATTRIBUTE_VALUE_UNQUOTED, AFTER_ATTRIBUTE_VALUE_QUOTED, BOGUS_COMMENT, MARKUP_DECLARATION_OPEN, DOCTYPE, BEFORE_DOCTYPE_NAME, DOCTYPE_NAME, AFTER_DOCTYPE_NAME, BEFORE_DOCTYPE_PUBLIC_IDENTIFIER, DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED, DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED, AFTER_DOCTYPE_PUBLIC_IDENTIFIER, BEFORE_DOCTYPE_SYSTEM_IDENTIFIER, DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED, DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED, AFTER_DOCTYPE_SYSTEM_IDENTIFIER, BOGUS_DOCTYPE, COMMENT_START, COMMENT_START_DASH, COMMENT, COMMENT_END_DASH, COMMENT_END, CLOSE_TAG_OPEN_NOT_PCDATA, MARKUP_DECLARATION_HYPHEN, MARKUP_DECLARATION_OCTYPE, DOCTYPE_UBLIC, DOCTYPE_YSTEM, CONSUME_ENTITY, CONSUME_NCR, ENTITY_LOOP, AFTER_ENTITY_LOOP, HEX_NCR_LOOP, DECIMAL_NRC_LOOP, HANDLE_NCR_VALUE
    }

    // String interning

    static int stringToElementMagic(String string) {
        string = string.toLowerCase();
        char[] buf = string.toCharArray();
        return bufToElementMagic(buf, buf.length);
    }

    private static int bufToElementMagic(char[] buf, int len) {
        int magic = len;
        magic <<= 5;
        magic += buf[0] - 0x60;
        int j = len;
        for (int i = 0; i < 4 && j > 0; i++) {
            j--;
            magic <<= 5;
            magic += buf[j] - 0x60;
        }
        return magic;
    }

    private static final int[] elementMagic;

    private static final String[] elements;

    static {
        SortedSet<SortStruct> set = new TreeSet<SortStruct>();
        String[] names = NameData.ELEMENT_DATA;
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            SortStruct struct = new SortStruct(name);
            if (set.contains(struct)) {
                throw new RuntimeException(
                        "String magic number function doesn't fit the data!");
            }
            set.add(struct);
        }
        elementMagic = new int[set.size()];
        elements = new String[set.size()];
        int i = 0;
        for (SortStruct sortStruct : set) {
            elementMagic[i] = sortStruct.getMagic();
            elements[i] = sortStruct.getString();
            i++;
        }
    }

    // end string interning

    private static final Pattern NCNAME_PATTERN = Pattern.compile("(?:[\\u0041-\\u005A]|[\\u0061-\\u007A]|[\\u00C0-\\u00D6]|[\\u00D8-\\u00F6]|[\\u00F8-\\u00FF]|[\\u0100-\\u0131]|[\\u0134-\\u013E]|[\\u0141-\\u0148]|[\\u014A-\\u017E]|[\\u0180-\\u01C3]|[\\u01CD-\\u01F0]|[\\u01F4-\\u01F5]|[\\u01FA-\\u0217]|[\\u0250-\\u02A8]|[\\u02BB-\\u02C1]|\\u0386|[\\u0388-\\u038A]|\\u038C|[\\u038E-\\u03A1]|[\\u03A3-\\u03CE]|[\\u03D0-\\u03D6]|\\u03DA|\\u03DC|\\u03DE|\\u03E0|[\\u03E2-\\u03F3]|[\\u0401-\\u040C]|[\\u040E-\\u044F]|[\\u0451-\\u045C]|[\\u045E-\\u0481]|[\\u0490-\\u04C4]|[\\u04C7-\\u04C8]|[\\u04CB-\\u04CC]|[\\u04D0-\\u04EB]|[\\u04EE-\\u04F5]|[\\u04F8-\\u04F9]|[\\u0531-\\u0556]|\\u0559|[\\u0561-\\u0586]|[\\u05D0-\\u05EA]|[\\u05F0-\\u05F2]|[\\u0621-\\u063A]|[\\u0641-\\u064A]|[\\u0671-\\u06B7]|[\\u06BA-\\u06BE]|[\\u06C0-\\u06CE]|[\\u06D0-\\u06D3]|\\u06D5|[\\u06E5-\\u06E6]|[\\u0905-\\u0939]|\\u093D|[\\u0958-\\u0961]|[\\u0985-\\u098C]|[\\u098F-\\u0990]|[\\u0993-\\u09A8]|[\\u09AA-\\u09B0]|\\u09B2|[\\u09B6-\\u09B9]|[\\u09DC-\\u09DD]|[\\u09DF-\\u09E1]|[\\u09F0-\\u09F1]|[\\u0A05-\\u0A0A]|[\\u0A0F-\\u0A10]|[\\u0A13-\\u0A28]|[\\u0A2A-\\u0A30]|[\\u0A32-\\u0A33]|[\\u0A35-\\u0A36]|[\\u0A38-\\u0A39]|[\\u0A59-\\u0A5C]|\\u0A5E|[\\u0A72-\\u0A74]|[\\u0A85-\\u0A8B]|\\u0A8D|[\\u0A8F-\\u0A91]|[\\u0A93-\\u0AA8]|[\\u0AAA-\\u0AB0]|[\\u0AB2-\\u0AB3]|[\\u0AB5-\\u0AB9]|\\u0ABD|\\u0AE0|[\\u0B05-\\u0B0C]|[\\u0B0F-\\u0B10]|[\\u0B13-\\u0B28]|[\\u0B2A-\\u0B30]|[\\u0B32-\\u0B33]|[\\u0B36-\\u0B39]|\\u0B3D|[\\u0B5C-\\u0B5D]|[\\u0B5F-\\u0B61]|[\\u0B85-\\u0B8A]|[\\u0B8E-\\u0B90]|[\\u0B92-\\u0B95]|[\\u0B99-\\u0B9A]|\\u0B9C|[\\u0B9E-\\u0B9F]|[\\u0BA3-\\u0BA4]|[\\u0BA8-\\u0BAA]|[\\u0BAE-\\u0BB5]|[\\u0BB7-\\u0BB9]|[\\u0C05-\\u0C0C]|[\\u0C0E-\\u0C10]|[\\u0C12-\\u0C28]|[\\u0C2A-\\u0C33]|[\\u0C35-\\u0C39]|[\\u0C60-\\u0C61]|[\\u0C85-\\u0C8C]|[\\u0C8E-\\u0C90]|[\\u0C92-\\u0CA8]|[\\u0CAA-\\u0CB3]|[\\u0CB5-\\u0CB9]|\\u0CDE|[\\u0CE0-\\u0CE1]|[\\u0D05-\\u0D0C]|[\\u0D0E-\\u0D10]|[\\u0D12-\\u0D28]|[\\u0D2A-\\u0D39]|[\\u0D60-\\u0D61]|[\\u0E01-\\u0E2E]|\\u0E30|[\\u0E32-\\u0E33]|[\\u0E40-\\u0E45]|[\\u0E81-\\u0E82]|\\u0E84|[\\u0E87-\\u0E88]|\\u0E8A|\\u0E8D|[\\u0E94-\\u0E97]|[\\u0E99-\\u0E9F]|[\\u0EA1-\\u0EA3]|\\u0EA5|\\u0EA7|[\\u0EAA-\\u0EAB]|[\\u0EAD-\\u0EAE]|\\u0EB0|[\\u0EB2-\\u0EB3]|\\u0EBD|[\\u0EC0-\\u0EC4]|[\\u0F40-\\u0F47]|[\\u0F49-\\u0F69]|[\\u10A0-\\u10C5]|[\\u10D0-\\u10F6]|\\u1100|[\\u1102-\\u1103]|[\\u1105-\\u1107]|\\u1109|[\\u110B-\\u110C]|[\\u110E-\\u1112]|\\u113C|\\u113E|\\u1140|\\u114C|\\u114E|\\u1150|[\\u1154-\\u1155]|\\u1159|[\\u115F-\\u1161]|\\u1163|\\u1165|\\u1167|\\u1169|[\\u116D-\\u116E]|[\\u1172-\\u1173]|\\u1175|\\u119E|\\u11A8|\\u11AB|[\\u11AE-\\u11AF]|[\\u11B7-\\u11B8]|\\u11BA|[\\u11BC-\\u11C2]|\\u11EB|\\u11F0|\\u11F9|[\\u1E00-\\u1E9B]|[\\u1EA0-\\u1EF9]|[\\u1F00-\\u1F15]|[\\u1F18-\\u1F1D]|[\\u1F20-\\u1F45]|[\\u1F48-\\u1F4D]|[\\u1F50-\\u1F57]|\\u1F59|\\u1F5B|\\u1F5D|[\\u1F5F-\\u1F7D]|[\\u1F80-\\u1FB4]|[\\u1FB6-\\u1FBC]|\\u1FBE|[\\u1FC2-\\u1FC4]|[\\u1FC6-\\u1FCC]|[\\u1FD0-\\u1FD3]|[\\u1FD6-\\u1FDB]|[\\u1FE0-\\u1FEC]|[\\u1FF2-\\u1FF4]|[\\u1FF6-\\u1FFC]|\\u2126|[\\u212A-\\u212B]|\\u212E|[\\u2180-\\u2182]|[\\u3041-\\u3094]|[\\u30A1-\\u30FA]|[\\u3105-\\u312C]|[\\uAC00-\\uD7A3]|[\\u4E00-\\u9FA5]|\\u3007|[\\u3021-\\u3029]|_)(?:[\\u0030-\\u0039]|[\\u0660-\\u0669]|[\\u06F0-\\u06F9]|[\\u0966-\\u096F]|[\\u09E6-\\u09EF]|[\\u0A66-\\u0A6F]|[\\u0AE6-\\u0AEF]|[\\u0B66-\\u0B6F]|[\\u0BE7-\\u0BEF]|[\\u0C66-\\u0C6F]|[\\u0CE6-\\u0CEF]|[\\u0D66-\\u0D6F]|[\\u0E50-\\u0E59]|[\\u0ED0-\\u0ED9]|[\\u0F20-\\u0F29]|[\\u0041-\\u005A]|[\\u0061-\\u007A]|[\\u00C0-\\u00D6]|[\\u00D8-\\u00F6]|[\\u00F8-\\u00FF]|[\\u0100-\\u0131]|[\\u0134-\\u013E]|[\\u0141-\\u0148]|[\\u014A-\\u017E]|[\\u0180-\\u01C3]|[\\u01CD-\\u01F0]|[\\u01F4-\\u01F5]|[\\u01FA-\\u0217]|[\\u0250-\\u02A8]|[\\u02BB-\\u02C1]|\\u0386|[\\u0388-\\u038A]|\\u038C|[\\u038E-\\u03A1]|[\\u03A3-\\u03CE]|[\\u03D0-\\u03D6]|\\u03DA|\\u03DC|\\u03DE|\\u03E0|[\\u03E2-\\u03F3]|[\\u0401-\\u040C]|[\\u040E-\\u044F]|[\\u0451-\\u045C]|[\\u045E-\\u0481]|[\\u0490-\\u04C4]|[\\u04C7-\\u04C8]|[\\u04CB-\\u04CC]|[\\u04D0-\\u04EB]|[\\u04EE-\\u04F5]|[\\u04F8-\\u04F9]|[\\u0531-\\u0556]|\\u0559|[\\u0561-\\u0586]|[\\u05D0-\\u05EA]|[\\u05F0-\\u05F2]|[\\u0621-\\u063A]|[\\u0641-\\u064A]|[\\u0671-\\u06B7]|[\\u06BA-\\u06BE]|[\\u06C0-\\u06CE]|[\\u06D0-\\u06D3]|\\u06D5|[\\u06E5-\\u06E6]|[\\u0905-\\u0939]|\\u093D|[\\u0958-\\u0961]|[\\u0985-\\u098C]|[\\u098F-\\u0990]|[\\u0993-\\u09A8]|[\\u09AA-\\u09B0]|\\u09B2|[\\u09B6-\\u09B9]|[\\u09DC-\\u09DD]|[\\u09DF-\\u09E1]|[\\u09F0-\\u09F1]|[\\u0A05-\\u0A0A]|[\\u0A0F-\\u0A10]|[\\u0A13-\\u0A28]|[\\u0A2A-\\u0A30]|[\\u0A32-\\u0A33]|[\\u0A35-\\u0A36]|[\\u0A38-\\u0A39]|[\\u0A59-\\u0A5C]|\\u0A5E|[\\u0A72-\\u0A74]|[\\u0A85-\\u0A8B]|\\u0A8D|[\\u0A8F-\\u0A91]|[\\u0A93-\\u0AA8]|[\\u0AAA-\\u0AB0]|[\\u0AB2-\\u0AB3]|[\\u0AB5-\\u0AB9]|\\u0ABD|\\u0AE0|[\\u0B05-\\u0B0C]|[\\u0B0F-\\u0B10]|[\\u0B13-\\u0B28]|[\\u0B2A-\\u0B30]|[\\u0B32-\\u0B33]|[\\u0B36-\\u0B39]|\\u0B3D|[\\u0B5C-\\u0B5D]|[\\u0B5F-\\u0B61]|[\\u0B85-\\u0B8A]|[\\u0B8E-\\u0B90]|[\\u0B92-\\u0B95]|[\\u0B99-\\u0B9A]|\\u0B9C|[\\u0B9E-\\u0B9F]|[\\u0BA3-\\u0BA4]|[\\u0BA8-\\u0BAA]|[\\u0BAE-\\u0BB5]|[\\u0BB7-\\u0BB9]|[\\u0C05-\\u0C0C]|[\\u0C0E-\\u0C10]|[\\u0C12-\\u0C28]|[\\u0C2A-\\u0C33]|[\\u0C35-\\u0C39]|[\\u0C60-\\u0C61]|[\\u0C85-\\u0C8C]|[\\u0C8E-\\u0C90]|[\\u0C92-\\u0CA8]|[\\u0CAA-\\u0CB3]|[\\u0CB5-\\u0CB9]|\\u0CDE|[\\u0CE0-\\u0CE1]|[\\u0D05-\\u0D0C]|[\\u0D0E-\\u0D10]|[\\u0D12-\\u0D28]|[\\u0D2A-\\u0D39]|[\\u0D60-\\u0D61]|[\\u0E01-\\u0E2E]|\\u0E30|[\\u0E32-\\u0E33]|[\\u0E40-\\u0E45]|[\\u0E81-\\u0E82]|\\u0E84|[\\u0E87-\\u0E88]|\\u0E8A|\\u0E8D|[\\u0E94-\\u0E97]|[\\u0E99-\\u0E9F]|[\\u0EA1-\\u0EA3]|\\u0EA5|\\u0EA7|[\\u0EAA-\\u0EAB]|[\\u0EAD-\\u0EAE]|\\u0EB0|[\\u0EB2-\\u0EB3]|\\u0EBD|[\\u0EC0-\\u0EC4]|[\\u0F40-\\u0F47]|[\\u0F49-\\u0F69]|[\\u10A0-\\u10C5]|[\\u10D0-\\u10F6]|\\u1100|[\\u1102-\\u1103]|[\\u1105-\\u1107]|\\u1109|[\\u110B-\\u110C]|[\\u110E-\\u1112]|\\u113C|\\u113E|\\u1140|\\u114C|\\u114E|\\u1150|[\\u1154-\\u1155]|\\u1159|[\\u115F-\\u1161]|\\u1163|\\u1165|\\u1167|\\u1169|[\\u116D-\\u116E]|[\\u1172-\\u1173]|\\u1175|\\u119E|\\u11A8|\\u11AB|[\\u11AE-\\u11AF]|[\\u11B7-\\u11B8]|\\u11BA|[\\u11BC-\\u11C2]|\\u11EB|\\u11F0|\\u11F9|[\\u1E00-\\u1E9B]|[\\u1EA0-\\u1EF9]|[\\u1F00-\\u1F15]|[\\u1F18-\\u1F1D]|[\\u1F20-\\u1F45]|[\\u1F48-\\u1F4D]|[\\u1F50-\\u1F57]|\\u1F59|\\u1F5B|\\u1F5D|[\\u1F5F-\\u1F7D]|[\\u1F80-\\u1FB4]|[\\u1FB6-\\u1FBC]|\\u1FBE|[\\u1FC2-\\u1FC4]|[\\u1FC6-\\u1FCC]|[\\u1FD0-\\u1FD3]|[\\u1FD6-\\u1FDB]|[\\u1FE0-\\u1FEC]|[\\u1FF2-\\u1FF4]|[\\u1FF6-\\u1FFC]|\\u2126|[\\u212A-\\u212B]|\\u212E|[\\u2180-\\u2182]|[\\u3041-\\u3094]|[\\u30A1-\\u30FA]|[\\u3105-\\u312C]|[\\uAC00-\\uD7A3]|[\\u4E00-\\u9FA5]|\\u3007|[\\u3021-\\u3029]|_|\\.|-|[\\u0300-\\u0345]|[\\u0360-\\u0361]|[\\u0483-\\u0486]|[\\u0591-\\u05A1]|[\\u05A3-\\u05B9]|[\\u05BB-\\u05BD]|\\u05BF|[\\u05C1-\\u05C2]|\\u05C4|[\\u064B-\\u0652]|\\u0670|[\\u06D6-\\u06DC]|[\\u06DD-\\u06DF]|[\\u06E0-\\u06E4]|[\\u06E7-\\u06E8]|[\\u06EA-\\u06ED]|[\\u0901-\\u0903]|\\u093C|[\\u093E-\\u094C]|\\u094D|[\\u0951-\\u0954]|[\\u0962-\\u0963]|[\\u0981-\\u0983]|\\u09BC|\\u09BE|\\u09BF|[\\u09C0-\\u09C4]|[\\u09C7-\\u09C8]|[\\u09CB-\\u09CD]|\\u09D7|[\\u09E2-\\u09E3]|\\u0A02|\\u0A3C|\\u0A3E|\\u0A3F|[\\u0A40-\\u0A42]|[\\u0A47-\\u0A48]|[\\u0A4B-\\u0A4D]|[\\u0A70-\\u0A71]|[\\u0A81-\\u0A83]|\\u0ABC|[\\u0ABE-\\u0AC5]|[\\u0AC7-\\u0AC9]|[\\u0ACB-\\u0ACD]|[\\u0B01-\\u0B03]|\\u0B3C|[\\u0B3E-\\u0B43]|[\\u0B47-\\u0B48]|[\\u0B4B-\\u0B4D]|[\\u0B56-\\u0B57]|[\\u0B82-\\u0B83]|[\\u0BBE-\\u0BC2]|[\\u0BC6-\\u0BC8]|[\\u0BCA-\\u0BCD]|\\u0BD7|[\\u0C01-\\u0C03]|[\\u0C3E-\\u0C44]|[\\u0C46-\\u0C48]|[\\u0C4A-\\u0C4D]|[\\u0C55-\\u0C56]|[\\u0C82-\\u0C83]|[\\u0CBE-\\u0CC4]|[\\u0CC6-\\u0CC8]|[\\u0CCA-\\u0CCD]|[\\u0CD5-\\u0CD6]|[\\u0D02-\\u0D03]|[\\u0D3E-\\u0D43]|[\\u0D46-\\u0D48]|[\\u0D4A-\\u0D4D]|\\u0D57|\\u0E31|[\\u0E34-\\u0E3A]|[\\u0E47-\\u0E4E]|\\u0EB1|[\\u0EB4-\\u0EB9]|[\\u0EBB-\\u0EBC]|[\\u0EC8-\\u0ECD]|[\\u0F18-\\u0F19]|\\u0F35|\\u0F37|\\u0F39|\\u0F3E|\\u0F3F|[\\u0F71-\\u0F84]|[\\u0F86-\\u0F8B]|[\\u0F90-\\u0F95]|\\u0F97|[\\u0F99-\\u0FAD]|[\\u0FB1-\\u0FB7]|\\u0FB9|[\\u20D0-\\u20DC]|\\u20E1|[\\u302A-\\u302F]|\\u3099|\\u309A|\\u00B7|\\u02D0|\\u02D1|\\u0387|\\u0640|\\u0E46|\\u0EC6|\\u3005|[\\u3031-\\u3035]|[\\u309D-\\u309E]|[\\u30FC-\\u30FE])*");

    /**
     * Magic value for UTF-16 operations.
     */
    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);

    /**
     * Magic value for UTF-16 operations.
     */
    private static final int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;

    /**
     * UTF-16 code unit array containing less than and greater than for emitting
     * those characters on certain parse errors.
     */
    private static final char[] LT_GT = { '<', '>' };

    /**
     * UTF-16 code unit array containing less than and solidus for emitting
     * those characters on certain parse errors.
     */
    private static final char[] LT_SOLIDUS = { '<', '/' };

    /**
     * Array version of U+FFFD.
     */
    private static final char[] REPLACEMENT_CHARACTER = { '\uFFFD' };

    /**
     * Array version of space.
     */
    private static final char[] SPACE = { ' ' };

    /**
     * Array version of line feed.
     */
    private static final char[] LF = { '\n' };

    /**
     * Buffer growth parameter.
     */
    private static final int BUFFER_GROW_BY = 1024;

    /**
     * Lexically sorted void element names
     */
    private static final String[] VOID_ELEMENTS = { "area", "base", "br",
            "col", "embed", "hr", "img", "input", "link", "meta", "param" };

    /**
     * "octype" as <code>char[]</code>
     */
    private static final char[] OCTYPE = "octype".toCharArray();

    /**
     * "ublic" as <code>char[]</code>
     */
    private static final char[] UBLIC = "ublic".toCharArray();

    /**
     * "ystem" as <code>char[]</code>
     */
    private static final char[] YSTEM = "ystem".toCharArray();

    /**
     * The token handler.
     */
    private final TokenHandler tokenHandler;

    /**
     * The error handler.
     */
    private ErrorHandler errorHandler;

    /**
     * The input UTF-16 code unit stream. If a byte stream was given, this
     * object is an instance of <code>HtmlInputStreamReader</code>.
     */
    private Reader reader;

    /**
     * The reference to the rewindable byte stream. <code>null</code> if p
     * rohibited or no longer needed.
     */
    private RewindableInputStream rewindableInputStream;

    /**
     * The main input buffer that the tokenizer reads from. Filled from
     * <code>reader</code>.
     */
    private char[] buf = new char[2048];

    /**
     * The index of the last <code>char</code> read from <code>buf</code>.
     */
    private int pos;

    /**
     * The index of the first <code>char</code> in <code>buf</code> that is
     * part of a coalesced run of character tokens or <code>-1</code> if there
     * is not a current run being coalesced.
     */
    private int cstart;

    /**
     * The number of <code>char</code>s in <code>buf</code> that have
     * meaning. (The rest of the array is garbage and should not be examined.)
     */
    private int bufLen;

    /**
     * The previous <code>char</code> read from the buffer with infoset
     * alteration applied except for CR. Used for CRLF normalization and
     * surrogate pair checking.
     */
    private char prev;

    /**
     * Lookbehind buffer for magic RCDATA/CDATA escaping.
     */
    private final char[] prevFour = new char[4];

    /**
     * Points to the last <code>char</code> written to <code>prevFour</code>.
     */
    private int prevFourPtr = 0;

    /**
     * Single code unit buffer for reconsuming an input character. If
     * <code>-1</code> the next <code>read()</code> returns from the real
     * buffer, otherwise from here.
     */
    private int unreadBuffer = -1;

    /**
     * The current line number in the current resource being parsed. (First line
     * is 1.) Passed on as locator data.
     */
    private int line;

    private int linePrev;

    /**
     * The current column number in the current resource being tokenized. (First
     * column is 1, counted by UTF-16 code units.) Passed on as locator data.
     */
    private int col;

    private int colPrev;

    private boolean nextCharOnNewLine;

    /**
     * The SAX public id for the resource being tokenized. (Only passed to back
     * as part of locator data.)
     */
    private String publicId;

    /**
     * The SAX system id for the resource being tokenized. (Only passed to back
     * as part of locator data.)
     */
    private String systemId;

    /**
     * Buffer for short identifiers.
     */
    private char[] strBuf = new char[64];

    /**
     * Number of significant <code>char</code>s in <code>strBuf</code>.
     */
    private int strBufLen = 0;

    /**
     * Buffer for long strings.
     */
    private char[] longStrBuf = new char[1024];

    /**
     * Number of significant <code>char</code>s in <code>longStrBuf</code>.
     */
    private int longStrBufLen = 0;

    /**
     * If not U+0000, a pending code unit to be appended to
     * <code>longStrBuf</code>.
     */
    private char longStrBufPending = '\u0000';

    /**
     * The attribute holder.
     */
    private AttributesImpl attributes;

    /**
     * Buffer for expanding NCRs falling into the Basic Multilingual Plane.
     */
    private final char[] bmpChar = new char[1];

    /**
     * Buffer for expanding astral NCRs.
     */
    private final char[] astralChar = new char[2];

    /**
     * Keeps track of PUA warnings.
     */
    private boolean alreadyWarnedAboutPrivateUseCharacters;

    /**
     * http://www.whatwg.org/specs/web-apps/current-work/#content2
     */
    private ContentModelFlag contentModelFlag = ContentModelFlag.PCDATA;

    /**
     * http://www.whatwg.org/specs/web-apps/current-work/#escape
     */
    private boolean escapeFlag = false;

    /**
     * The element whose end tag closes the current CDATA or RCDATA element.
     */
    private String contentModelElement = "";

    /**
     * <code>true</code> if tokenizing an end tag
     */
    private boolean endTag;

    /**
     * The current tag token name.
     */
    private String tagName = null;

    /**
     * The current attribute name.
     */
    private String attributeName = null;

    /**
     * Whether comment tokens are emitted.
     */
    private boolean wantsComments = false;

    /**
     * If <code>false</code>, <code>addAttribute*()</code> are no-ops.
     */
    private boolean shouldAddAttributes;

    /**
     * <code>true</code> when in text content or in attribute value.
     */
    private boolean inContent;

    /**
     * <code>true</code> when HTML4-specific additional errors are requested.
     */
    private boolean html4;

    /**
     * Used together with <code>nonAsciiProhibited</code>.
     */
    private boolean alreadyComplainedAboutNonAscii;

    private boolean canSwitchDecoder;

    /**
     * Whether the stream is past the first 512 bytes.
     */
    private boolean metaBoundaryPassed;

    /**
     * The name of the current doctype token.
     */
    private String doctypeName;

    /**
     * The public id of the current doctype token.
     */
    private String publicIdentifier;

    /**
     * The system id of the current doctype token.
     */
    private String systemIdentifier;

    /**
     * Used for NFC checking if non-<code>null</code>, source code capture,
     * etc.
     */
    private CharacterHandler[] characterHandlers = new CharacterHandler[0];

    /**
     * The policy for vertical tab and form feed.
     */
    private XmlViolationPolicy contentSpacePolicy = XmlViolationPolicy.ALLOW;

    /**
     * The policy for non-space non-XML characters.
     */
    private XmlViolationPolicy contentNonXmlCharPolicy = XmlViolationPolicy.ALLOW;

    /**
     * The policy for comments.
     */
    private XmlViolationPolicy commentPolicy = XmlViolationPolicy.ALLOW;

    private XmlViolationPolicy xmlnsPolicy = XmlViolationPolicy.ALLOW;

    private XmlViolationPolicy namePolicy = XmlViolationPolicy.ALLOW;

    private boolean swallowBom;

    private boolean html4ModeCompatibleWithXhtml1Schemata;

    private boolean mappingLangToXmlLang;

    private XmlViolationPolicy bogusXmlnsPolicy;

    private Encoding characterEncoding;

    private Confidence confidence;

    private boolean allowRewinding = true;

    private Heuristics heuristics = Heuristics.NONE;

    // start public API

    /**
     * The constuctor.
     * 
     * @param tokenHandler
     *            the handler for receiving tokens
     */
    public Tokenizer(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    /**
     * Returns the allowRewinding.
     * 
     * @return the allowRewinding
     */
    public boolean isAllowRewinding() {
        return allowRewinding;
    }

    /**
     * Sets the allowRewinding.
     * 
     * @param allowRewinding
     *            the allowRewinding to set
     */
    public void setAllowRewinding(boolean allowRewinding) {
        this.allowRewinding = allowRewinding;
    }

    /**
     * Turns NFC checking on or off.
     * 
     * @param enable
     *            <code>true</code> if checking on
     */
    public void setCheckingNormalization(boolean enable) {
        if (enable) {
            if (isCheckingNormalization()) {
                return;
            } else {
                NormalizationChecker normalizationChecker = new NormalizationChecker(
                        this);
                normalizationChecker.setErrorHandler(errorHandler);

            }
        } else {
            if (isCheckingNormalization()) {
                CharacterHandler[] newHandlers = new CharacterHandler[characterHandlers.length - 1];
                boolean skipped = false;
                int j = 0;
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    if (!(!skipped && (ch instanceof NormalizationChecker))) {
                        newHandlers[j] = ch;
                        j++;
                    }
                }
                characterHandlers = newHandlers;
            } else {
                return;
            }
        }
    }

    public void addCharacterHandler(CharacterHandler characterHandler) {
        if (characterHandler == null) {
            throw new IllegalArgumentException("Null argument.");
        }
        CharacterHandler[] newHandlers = new CharacterHandler[characterHandlers.length + 1];
        System.arraycopy(characterHandlers, 0, newHandlers, 0,
                characterHandlers.length);
        newHandlers[characterHandlers.length] = characterHandler;
        characterHandlers = newHandlers;
    }

    /**
     * Query if checking normalization.
     * 
     * @return <code>true</code> if checking on
     */
    public boolean isCheckingNormalization() {
        for (int i = 0; i < characterHandlers.length; i++) {
            CharacterHandler ch = characterHandlers[i];
            if (ch instanceof NormalizationChecker) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the error handler.
     * 
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
        for (int i = 0; i < characterHandlers.length; i++) {
            CharacterHandler ch = characterHandlers[i];
            if (ch instanceof NormalizationChecker) {
                NormalizationChecker nc = (NormalizationChecker) ch;
                nc.setErrorHandler(eh);
            }
        }
    }

    /**
     * Returns the commentPolicy.
     * 
     * @return the commentPolicy
     */
    public XmlViolationPolicy getCommentPolicy() {
        return commentPolicy;
    }

    /**
     * Sets the commentPolicy.
     * 
     * @param commentPolicy
     *            the commentPolicy to set
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        this.commentPolicy = commentPolicy;
    }

    /**
     * Returns the contentNonXmlCharPolicy.
     * 
     * @return the contentNonXmlCharPolicy
     */
    public XmlViolationPolicy getContentNonXmlCharPolicy() {
        return contentNonXmlCharPolicy;
    }

    /**
     * Sets the contentNonXmlCharPolicy.
     * 
     * @param contentNonXmlCharPolicy
     *            the contentNonXmlCharPolicy to set
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        this.contentNonXmlCharPolicy = contentNonXmlCharPolicy;
    }

    /**
     * Returns the contentSpacePolicy.
     * 
     * @return the contentSpacePolicy
     */
    public XmlViolationPolicy getContentSpacePolicy() {
        return contentSpacePolicy;
    }

    /**
     * Sets the contentSpacePolicy.
     * 
     * @param contentSpacePolicy
     *            the contentSpacePolicy to set
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        this.contentSpacePolicy = contentSpacePolicy;
    }

    /**
     * Sets the xmlnsPolicy.
     * 
     * @param xmlnsPolicy
     *            the xmlnsPolicy to set
     */
    public void setXmlnsPolicy(XmlViolationPolicy xmlnsPolicy) {
        if (xmlnsPolicy == XmlViolationPolicy.FATAL) {
            throw new IllegalArgumentException("Can't use FATAL here.");
        }
        this.xmlnsPolicy = xmlnsPolicy;
    }

    public void setNamePolicy(XmlViolationPolicy namePolicy) {
        this.namePolicy = namePolicy;
    }

    /**
     * Sets the bogusXmlnsPolicy.
     * 
     * @param bogusXmlnsPolicy
     *            the bogusXmlnsPolicy to set
     */
    public void setBogusXmlnsPolicy(XmlViolationPolicy bogusXmlnsPolicy) {
        this.bogusXmlnsPolicy = bogusXmlnsPolicy;
    }

    /**
     * Sets the html4ModeCompatibleWithXhtml1Schemata.
     * 
     * @param html4ModeCompatibleWithXhtml1Schemata
     *            the html4ModeCompatibleWithXhtml1Schemata to set
     */
    public void setHtml4ModeCompatibleWithXhtml1Schemata(
            boolean html4ModeCompatibleWithXhtml1Schemata) {
        this.html4ModeCompatibleWithXhtml1Schemata = html4ModeCompatibleWithXhtml1Schemata;
    }

    /**
     * Runs the tokenization. This is the main entry point.
     * 
     * @param is
     *            the input source
     * @throws SAXException
     *             on fatal error (if configured to treat XML violations as
     *             fatal) or if the token handler threw
     * @throws IOException
     *             if the stream threw
     */
    public void tokenize(InputSource is) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource was null.");
        }
        confidence = Confidence.TENTATIVE;
        alreadyComplainedAboutNonAscii = false;
        canSwitchDecoder = true;
        swallowBom = true;
        rewindableInputStream = null;
        this.systemId = is.getSystemId();
        this.publicId = is.getPublicId();
        this.reader = is.getCharacterStream();
        this.characterEncoding = encodingFromExternalDeclaration(is.getEncoding());
        if (this.reader == null) {
            InputStream inputStream = is.getByteStream();
            if (inputStream == null) {
                throw new SAXException("Both streams in InputSource were null.");
            }
            if (this.characterEncoding == null) {
                if (allowRewinding) {
                    inputStream = rewindableInputStream = new RewindableInputStream(
                            inputStream);
                }
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this, heuristics);
            } else {
                becomeConfident();
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this, this.characterEncoding);
            }
        } else {
            becomeConfident();
        }
        Throwable t = null;
        try {
            for (;;) {
                try {
                    contentModelFlag = ContentModelFlag.PCDATA;
                    escapeFlag = false;
                    inContent = true;
                    pos = -1;
                    cstart = -1;
                    line = linePrev = 0;
                    col = colPrev = 1;
                    nextCharOnNewLine = true;
                    prev = '\u0000';
                    bufLen = 0;
                    html4 = false;
                    alreadyWarnedAboutPrivateUseCharacters = false;
                    metaBoundaryPassed = false;
                    tokenHandler.start(this);
                    for (int i = 0; i < characterHandlers.length; i++) {
                        CharacterHandler ch = characterHandlers[i];
                        ch.start();
                    }
                    wantsComments = tokenHandler.wantsComments();
                    if (swallowBom) {
                        // Swallow the BOM
                        char c = read();
                        if (c == '\uFEFF') {
                            line = linePrev = 0;
                            col = colPrev = 1;
                            nextCharOnNewLine = true;
                        } else {
                            unread(c);
                        }
                    }
                    runStates();
                    if (confidence == Confidence.TENTATIVE
                            && !alreadyComplainedAboutNonAscii) {
                        warnWithoutLocation("The character encoding of the document was not declared.");
                    }
                    break;
                } catch (ReparseException e) {
                    if (rewindableInputStream == null) {
                        fatal("Changing encoding at this point would need non-streamable behavior.");
                    } else {
                        rewindableInputStream.rewind();
                        becomeConfident();
                        this.reader = new HtmlInputStreamReader(
                                rewindableInputStream, errorHandler, this,
                                this, this.characterEncoding);
                    }
                    continue;
                }
            }
        } catch (Throwable tr) {
            t = tr;
        } finally {
            try {
                systemIdentifier = null;
                publicIdentifier = null;
                doctypeName = null;
                tagName = null;
                attributeName = null;
                characterEncoding = null;
                tokenHandler.eof();
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    ch.end();
                }
                reader.close();
                reader = null;
                rewindableInputStream = null;
            } catch (Throwable tr) {
                if (t == null) {
                    t = tr;
                } // else drop the later throwable
            }
            if (t != null) {
                if (t instanceof IOException) {
                    throw (IOException) t;
                } else if (t instanceof SAXException) {
                    throw (SAXException) t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    // impossible
                    throw new RuntimeException(t);
                }
            }
        }
    }

    // For the token handler to call
    /**
     * Sets the content model flag and the associated element name.
     * 
     * @param contentModelFlag
     *            the flag
     * @param contentModelElement
     *            the element causing the flag to be set
     */
    public void setContentModelFlag(ContentModelFlag contentModelFlag,
            String contentModelElement) {
        this.contentModelFlag = contentModelFlag;
        this.contentModelElement = contentModelElement;
    }

    // start Locator impl

    /**
     * @see org.xml.sax.Locator#getPublicId()
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * @see org.xml.sax.Locator#getSystemId()
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @see org.xml.sax.Locator#getLineNumber()
     */
    public int getLineNumber() {
        if (line > 0) {
            return line;
        } else {
            return -1;
        }
    }

    /**
     * @see org.xml.sax.Locator#getColumnNumber()
     */
    public int getColumnNumber() {
        if (col > 0) {
            return col;
        } else {
            return -1;
        }
    }

    // end Locator impl

    // end public API

    void notifyAboutMetaBoundary() {
        metaBoundaryPassed = true;
    }

    void turnOnAdditionalHtml4Errors() {
        html4 = true;
    }

    void dontSwallowBom() {
        swallowBom = false;
    }

    AttributesImpl newAttributes() {
        if (mappingLangToXmlLang) {
            return new XmlLangAttributesImpl();
        } else {
            return new AttributesImpl();
        }
    }

    /**
     * Clears the smaller buffer.
     */
    private void clearStrBuf() {
        strBufLen = 0;
    }

    /**
     * Appends to the smaller buffer.
     * 
     * @param c
     *            the UTF-16 code unit to append
     */
    private void appendStrBuf(char c) {
        if (strBufLen == strBuf.length) {
            char[] newBuf = new char[strBuf.length + BUFFER_GROW_BY];
            System.arraycopy(strBuf, 0, newBuf, 0, strBuf.length);
            strBuf = newBuf;
        }
        strBuf[strBufLen++] = c;
    }

    /**
     * The smaller buffer as a string.
     * 
     * @return the smaller buffer as a string
     */
    private String strBufToString() {
        return new String(strBuf, 0, strBufLen);
    }

    /**
     * Emits the smaller buffer as character tokens.
     * 
     * @throws SAXException
     *             if the token handler threw
     */
    private void emitStrBuf() throws SAXException {
        if (strBufLen > 0) {
            tokenHandler.characters(strBuf, 0, strBufLen);
        }
    }

    private boolean isNcname(String str) {
        Matcher m = NCNAME_PATTERN.matcher(str);
        return m.matches();
    }

    /**
     * Clears the larger buffer.
     */
    private void clearLongStrBuf() {
        longStrBufLen = 0;
        longStrBufPending = '\u0000';
    }

    /**
     * Appends to the larger buffer.
     * 
     * @param c
     *            the UTF-16 code unit to append
     */
    private void appendLongStrBuf(char c) {
        if (longStrBufLen == longStrBuf.length) {
            char[] newBuf = new char[longStrBuf.length + BUFFER_GROW_BY];
            System.arraycopy(longStrBuf, 0, newBuf, 0, longStrBuf.length);
            longStrBuf = newBuf;
        }
        longStrBuf[longStrBufLen++] = c;
    }

    /**
     * Appends to the larger buffer when it is used to buffer a comment. Checks
     * for two consecutive hyphens.
     * 
     * @param c
     *            the UTF-16 code unit to append
     * @throws SAXException
     */
    private void appendToComment(char c) throws SAXException {
        if (longStrBufPending == '-' && c == '-') {
            if (commentPolicy == XmlViolationPolicy.FATAL) {
                fatal("This document is not mappable to XML 1.0 without data loss due to \u201C--\u201D in a comment.");
            } else {
                warn("This document is not mappable to XML 1.0 without data loss due to \u201C--\u201D in a comment.");
                if (wantsComments) {
                    if (commentPolicy == XmlViolationPolicy.ALLOW) {
                        appendLongStrBuf('-');
                    } else {
                        appendLongStrBuf('-');
                        appendLongStrBuf(' ');
                    }
                }
                longStrBufPending = '-';
            }
        } else {
            if (longStrBufPending != '\u0000') {
                if (wantsComments) {
                    appendLongStrBuf(longStrBufPending);
                }
                longStrBufPending = '\u0000';
            }
            if (c == '-') {
                longStrBufPending = '-';
            } else {
                if (wantsComments) {
                    appendLongStrBuf(c);
                }
            }
        }
    }

    /**
     * Appends to the larger buffer.
     * 
     * @param arr
     *            the UTF-16 code units to append
     */
    private void appendLongStrBuf(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            appendLongStrBuf(arr[i]);
        }
    }

    /**
     * Append the contents of the smaller buffer to the larger one.
     */
    private void appendStrBufToLongStrBuf() {
        for (int i = 0; i < strBufLen; i++) {
            appendLongStrBuf(strBuf[i]);
        }
    }

    /**
     * The larger buffer as a string.
     * 
     * @return the larger buffer as a string
     */
    private String longStrBufToString() {
        if (longStrBufPending != '\u0000') {
            appendLongStrBuf(longStrBufPending);
        }
        return new String(longStrBuf, 0, longStrBufLen);
    }

    /**
     * Emits the current comment token.
     * 
     * @throws SAXException
     */
    private void emitComment() throws SAXException {
        if (wantsComments) {
            if (longStrBufPending != '\u0000') {
                appendLongStrBuf(longStrBufPending);
            }
        }
        tokenHandler.comment(longStrBuf, longStrBufLen);
    }

    /**
     * Unreads a code unit so that it is returned the next time
     * <code>read()</code> is called.
     * 
     * @param c
     *            the code unit to unread
     */
    private void unread(char c) {
        unreadBuffer = c;
    }

    /**
     * Reads the next UTF-16 code unit.
     * 
     * @return the next code unit
     * @throws SAXException
     * @throws IOException
     */
    private char read() throws SAXException, IOException {
        for (;;) { // the loop is here for the CRLF case
            if (unreadBuffer != -1) {
                char c = (char) unreadBuffer;
                unreadBuffer = -1;
                return c;
            }
            assert (bufLen > -1);
            pos++;
            assert pos <= bufLen;
            linePrev = line;
            colPrev = col;
            if (nextCharOnNewLine) {
                line++;
                col = 1;
                nextCharOnNewLine = false;
            } else {
                col++;
            }
            if (pos == bufLen) {
                boolean charDataContinuation = false;
                if (cstart > -1) {
                    flushChars();
                    charDataContinuation = true;
                }
                bufLen = reader.read(buf);
                assert bufLen != 0;
                assert bufLen <= buf.length;
                if (bufLen == -1) {
                    return '\u0000';
                } else {
                    for (int i = 0; i < characterHandlers.length; i++) {
                        CharacterHandler ch = characterHandlers[i];
                        ch.characters(buf, 0, bufLen);
                    }
                }
                if (charDataContinuation) {
                    cstart = 0;
                }
                pos = 0;
            }
            char c = buf[pos];
            if (confidence == Confidence.TENTATIVE
                    && !alreadyComplainedAboutNonAscii && c > '\u007F') {
                err("The character encoding of the document was not explicit (assumed \u201C"
                        + characterEncoding.getCanonName()
                        + "\u201D) but the document contains non-ASCII.");
                alreadyComplainedAboutNonAscii = true;
            }
            if (canSwitchDecoder
                    && !((c >= 0x09 && c <= 0x0D) || (c >= 0x20 && c <= 0x22)
                            || (c >= 0x26 && c <= 0x27)
                            || (c >= 0x2C && c <= 0x3F)
                            || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A))) {
                canSwitchDecoder = false;
            }
            switch (c) {
                case '\n':
                    /*
                     * U+000D CARRIAGE RETURN (CR) characters, and U+000A LINE
                     * FEED (LF) characters, are treated specially. Any CR
                     * characters that are followed by LF characters must be
                     * removed, and any CR characters not followed by LF
                     * characters must be converted to LF characters.
                     */
                    if (prev == '\r') {
                        // swallow the LF
                        if (cstart != -1) {
                            flushChars();
                            cstart = pos + 1;
                        }
                        col = colPrev;
                        line = linePrev;
                        nextCharOnNewLine = true;
                        prev = c;
                        continue;
                    } else {
                        nextCharOnNewLine = true;
                    }
                    break;
                case '\r':
                    c = buf[pos] = '\n';
                    nextCharOnNewLine = true;
                    prev = '\r';
                    if (contentModelFlag != ContentModelFlag.PCDATA) {
                        prevFourPtr++;
                        prevFourPtr %= 4;
                        prevFour[prevFourPtr] = c;
                    }
                    return c;
                case '\u0000':
                    /*
                     * All U+0000 NULL characters in the input must be replaced
                     * by U+FFFD REPLACEMENT CHARACTERs. Any occurrences of such
                     * characters is a parse error.
                     */
                    err("Found U+0000 in the character stream.");
                    c = buf[pos] = '\uFFFD';
                    break;
                case '\u000B':
                case '\u000C':
                    if (inContent) {
                        if (contentNonXmlCharPolicy == XmlViolationPolicy.FATAL) {
                            fatal("This document is not mappable to XML 1.0 without data loss due to "
                                    + toUPlusString(c)
                                    + " which is not a legal XML 1.0 character.");
                        } else {
                            if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                c = buf[pos] = ' ';
                            }
                            warn("This document is not mappable to XML 1.0 without data loss due to "
                                    + toUPlusString(c)
                                    + " which is not a legal XML 1.0 character.");
                        }
                    }
                    break;
                default:
                    if ((c & 0xFC00) == 0xDC00) {
                        // Got a low surrogate. See if prev was high surrogate
                        if ((prev & 0xFC00) == 0xD800) {
                            int intVal = (prev << 10) + c + SURROGATE_OFFSET;
                            if (isNonCharacter(intVal)) {
                                err("Astral non-character.");
                            }
                            if (isAstralPrivateUse(intVal)) {
                                warnAboutPrivateUseChar();
                            }
                        } else {
                            // XXX figure out what to do about lone high
                            // surrogates
                            err("Found low surrogate without high surrogate.");
                            c = buf[pos] = '\uFFFD';
                        }
                    } else if ((c < ' ' || isNonCharacter(c)) && (c != '\t')) {
                        if (inContent) {
                            if (contentNonXmlCharPolicy == XmlViolationPolicy.FATAL) {
                                fatal("Forbidden code point "
                                        + toUPlusString(c) + ".");
                            } else {
                                if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                    c = buf[pos] = '\uFFFD';
                                }
                                err("Forbidden code point " + toUPlusString(c)
                                        + ".");
                            }
                        } else {
                            err("Forbidden code point " + toUPlusString(c)
                                    + ".");
                        }
                    } else if ((c >= '\u007F') && (c <= '\u009F')
                            || (c >= '\uFDD0') && (c <= '\uFDDF')) {
                        err("Forbidden code point " + toUPlusString(c) + ".");
                    } else if (isPrivateUse(c)) {
                        warnAboutPrivateUseChar();
                    }
            }
            prev = c;
            if (contentModelFlag != ContentModelFlag.PCDATA) {
                prevFourPtr++;
                prevFourPtr %= 4;
                prevFour[prevFourPtr] = c;
            }
            return c;
        }
    }

    private String toUPlusString(char c) {
        String hexString = Integer.toHexString(c);
        switch (hexString.length()) {
            case 1:
                return "U+000" + hexString;
            case 2:
                return "U+00" + hexString;
            case 3:
                return "U+0" + hexString;
            case 4:
                return "U+" + hexString;
            default:
                throw new RuntimeException("Unreachable.");
        }
    }

    /**
     * Emits a warning about private use characters if the warning has not been
     * emitted yet.
     * 
     * @throws SAXException
     */
    private void warnAboutPrivateUseChar() throws SAXException {
        if (!alreadyWarnedAboutPrivateUseCharacters) {
            warn("Document uses the Unicode Private Use Area(s), which should not be used in publicly exchanged documents. (Charmod C073)");
            alreadyWarnedAboutPrivateUseCharacters = true;
        }
    }

    /**
     * Tells if the argument is a BMP PUA character.
     * 
     * @param c
     *            the UTF-16 code unit to check
     * @return <code>true</code> if PUA character
     */
    private boolean isPrivateUse(char c) {
        return c >= '\uE000' && c <= '\uF8FF';
    }

    /**
     * Tells if the argument is an astral PUA character.
     * 
     * @param c
     *            the code point to check
     * @return <code>true</code> if astral private use
     */
    private boolean isAstralPrivateUse(int c) {
        return (c >= 0xF0000 && c <= 0xFFFFD)
                || (c >= 0x100000 && c <= 0x10FFFD);
    }

    /**
     * Tells if the argument is a non-character (works for BMP and astral).
     * 
     * @param c
     *            the code point to check
     * @return <code>true</code> if non-character
     */
    private boolean isNonCharacter(int c) {
        return (c & 0xFFFE) == 0xFFFE;
    }

    /**
     * Flushes coalesced character tokens.
     * 
     * @throws SAXException
     */
    private void flushChars() throws SAXException, IOException {
        if (cstart != -1) {
            if (pos > cstart) {
                int currLine = line;
                int currCol = col;
                line = linePrev;
                col = colPrev;
                try {
                    tokenHandler.characters(buf, cstart, pos - cstart);
                } finally {
                    line = currLine;
                    col = currCol;
                }
            }
        }
        cstart = -1;
    }

    /**
     * Reports an condition that would make the infoset incompatible with XML
     * 1.0 as fatal.
     * 
     * @param message
     *            the message
     * @throws SAXException
     * @throws SAXParseException
     */
    private void fatal(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        if (errorHandler != null) {
            errorHandler.fatalError(spe);
        }
        throw spe;
    }

    /**
     * Reports a Parse Error.
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    private void err(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, this);
        errorHandler.error(spe);
    }

    private void errTreeBuilder(String message) throws SAXException {
        ErrorHandler eh = null;
        if (tokenHandler instanceof TreeBuilder<?>) {
            TreeBuilder<?> treeBuilder = (TreeBuilder<?>) tokenHandler;
            eh = treeBuilder.getErrorHandler();
        }
        if (eh == null) {
            eh = errorHandler;
        }
        if (eh == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, this);
        eh.error(spe);
    }

    /**
     * Reports a warning
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    private void warn(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, this);
        errorHandler.warning(spe);
    }

    /**
     * Reports a warning without line/col
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    private void warnWithoutLocation(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, null,
                getSystemId(), -1, -1);
        errorHandler.warning(spe);
    }

    /**
     * Initializes a decoder from external decl.
     */
    private Encoding encodingFromExternalDeclaration(String encoding)
            throws SAXException {
        if (encoding == null) {
            return null;
        }
        encoding = Encoding.toAsciiLowerCase(encoding);
        try {
            Encoding cs = Encoding.forName(encoding);
            if ("utf-16".equals(cs.getCanonName())
                    || "utf-32".equals(cs.getCanonName())) {
                swallowBom = false;
            }
            return whineAboutEncodingAndReturnActual(encoding, cs);
        } catch (UnsupportedCharsetException e) {
            err("Unsupported character encoding name: \u201C" + encoding
                    + "\u201D. Will sniff.");
            swallowBom = true;
        }
        return null; // keep the compiler happy
    }

    /**
     * @param encoding
     * @param cs
     * @return
     * @throws SAXException
     */
    private Encoding whineAboutEncodingAndReturnActual(String encoding,
            Encoding cs) throws SAXException {
        String canonName = cs.getCanonName();
        if (!cs.isRegistered()) {
            if (encoding.startsWith("x-")) {
                err("The encoding \u201C"
                        + encoding
                        + "\u201D is not an IANA-registered encoding. (Charmod C022)");
            } else {
                err("The encoding \u201C"
                        + encoding
                        + "\u201D is not an IANA-registered encoding and did not use the \u201Cx-\u201D prefix. (Charmod C023)");
            }
        } else if (!canonName.equals(encoding)) {
            err("The encoding \u201C"
                    + encoding
                    + "\u201D is not the preferred name of the character encoding in use. The preferred name is \u201C"
                    + canonName + "\u201D. (Charmod C024)");
        }
        if (cs.isShouldNot()) {
            warn("Authors should not use the character encoding \u201C"
                    + encoding
                    + "\u201D. It is recommended to use \u201CUTF-8\u201D.");
        } else if (cs.isLikelyEbcdic()) {
            warn("Authors should not use EBCDIC-based encodings. It is recommended to use \u201CUTF-8\u201D.");
        } else if (cs.isObscure()) {
            warn("The character encoding \u201C"
                    + encoding
                    + "\u201D is not widely supported. Better interoperability may be achieved by using \u201CUTF-8\u201D.");
        }
        Encoding actual = cs.getActualHtmlEncoding();
        if (actual == null) {
            return cs;
        } else {
            warn("Using \u201C" + actual.getCanonName()
                    + "\u201D instead of the declared encoding \u201C"
                    + encoding + "\u201D.");
            return actual;
        }
    }

    private boolean currentIsVoid() {
        return Arrays.binarySearch(VOID_ELEMENTS, tagName) > -1;
    }

    private boolean lastHyphHyph() {
        return prevFour[(prevFourPtr - 1 + 4) % 4] == '-'
                && prevFour[(prevFourPtr - 2 + 4) % 4] == '-';
    }

    private boolean lastLtExclHyph() {
        return prevFour[(prevFourPtr - 1 + 4) % 4] == '-'
                && prevFour[(prevFourPtr - 2 + 4) % 4] == '!'
                && prevFour[(prevFourPtr - 3 + 4) % 4] == '<';
    }

    /**
     * 
     */
    private void resetAttributes() {
        attributes = null; // XXX figure out reuse
    }

    private void parseErrorUnlessPermittedSlash() throws SAXException,
            IOException {
        /*
         * A permitted slash is a U+002F SOLIDUS character that is immediately
         * followed by a U+003E GREATER-THAN SIGN, if, and only if, the current
         * token being processed is a start tag token whose tag name is one of
         * the following: base, link, meta, hr, br, img, embed, param, area,
         * col, input
         */
        if (endTag) {
            err("Stray \u201C/\u201D in an end tag.");
            return;
        }
        char c = read();
        int saveLine = line;
        int saveCol = col;
        line = linePrev;
        col = colPrev;
        if (c == '>') {
            if (!currentIsVoid() && !html4) {
                if (html4) {
                    err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is not permitted in HTML4.");
                } else {
                    err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is only permitted on void elements.");
                }
            } else if (html4) {
                err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is not permitted in HTML4. (HTML4-only error)");
            }
        } else {
            err("Stray \u201C/\u201D in tag.");
        }
        line = saveLine;
        col = saveCol;
        unread(c);
    }

    private String strBufToElementNameString() {
        int magic = bufToElementMagic(strBuf, strBufLen);
        int index = Arrays.binarySearch(elementMagic, magic);
        if (index < 0) {
            return strBufToString(); // intern?
        } else {
            String rv = elements[index];
            if (rv.length() != strBufLen) {
                return strBufToString(); // intern?
            }
            for (int i = 0; i < strBufLen; i++) {
                if (rv.charAt(i) != strBuf[i]) {
                    return strBufToString(); // intern?
                }
            }
            return rv;
        }
    }

    private void emitCurrentTagToken() throws SAXException {
        if (namePolicy != XmlViolationPolicy.ALLOW) {
            if (!isNcname(tagName)) {
                if (namePolicy == XmlViolationPolicy.FATAL) {
                    fatal((endTag ? "End" : "Start") + " tag \u201C" + tagName
                            + "\u201D has a non-NCName name.");
                } else {
                    warn((endTag ? "End" : "Start") + " tag \u201C" + tagName
                            + "\u201D has a non-NCName name. Ignoring token.");
                    return;
                }
            }
        }
        Attributes attrs = (attributes == null ? EmptyAttributes.EMPTY_ATTRIBUTES
                : attributes);
        if (endTag) {
            /*
             * When an end tag token is emitted, the content model flag must be
             * switched to the PCDATA state.
             */
            escapeFlag = false;
            contentModelFlag = ContentModelFlag.PCDATA;
            if (attrs.getLength() != 0) {
                /*
                 * When an end tag token is emitted with attributes, that is a
                 * parse error.
                 */
                err("End tag had attributes.");
            }
            tokenHandler.endTag(tagName, attrs);
        } else {
            tokenHandler.startTag(tagName, attrs);
        }
    }

    private void attributeNameComplete() throws SAXException {
        attributeName = strBufToString().intern();
        if (attributes == null) {
            attributes = newAttributes();
        }
        /*
         * When the user agent leaves the attribute name state (and before
         * emitting the tag token, if appropriate), the complete attribute's
         * name must be compared to the other attributes on the same token; if
         * there is already an attribute on the token with the exact same name,
         * then this is a parse error and the new attribute must be dropped,
         * along with the value that gets associated with it (if any).
         */
        if (attributes.getIndex(attributeName) == -1) {
            if (namePolicy == XmlViolationPolicy.ALLOW) {
                shouldAddAttributes = true;
            } else {
                if (isNcname(attributeName)) {
                    shouldAddAttributes = true;
                } else {
                    if (namePolicy == XmlViolationPolicy.FATAL) {
                        fatal("Attribute name \u201C" + attributeName
                                + "\u201D is not an NCName.");
                    } else {
                        shouldAddAttributes = false;
                        warn("Attribute name \u201C"
                                + attributeName
                                + "\u201D is not an NCName. Ignoring the attribute.");
                    }
                }
            }
        } else {
            shouldAddAttributes = false;
            err("Duplicate attribute \u201C" + attributeName + "\u201D.");
        }
    }

    private void addAttributeWithoutValue() throws SAXException {
        if (metaBoundaryPassed && "charset".equals(attributeName)
                && "meta".equals(tagName)) {
            err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
        }
        if (shouldAddAttributes) {
            if (html4) {
                if (AttributeInfo.isBoolean(attributeName)) {
                    if (html4ModeCompatibleWithXhtml1Schemata) {
                        attributes.addAttribute(attributeName, attributeName);
                    } else {
                        attributes.addAttribute(attributeName, "");
                    }
                } else {
                    err("Attribute value omitted for a non-boolean attribute. (HTML4-only error.)");
                    attributes.addAttribute(attributeName, "");
                }
            } else {
                if ("src".equals(attributeName) || "href".equals(attributeName)) {
                    warn("Attribute \u201C"
                            + attributeName
                            + "\u201D without an explicit value seen. The attribute may be dropped by IE7.");
                }
                attributes.addAttribute(attributeName, "");
            }
        }
    }

    private void addAttributeWithValue() throws SAXException {
        if (metaBoundaryPassed && "meta" == tagName
                && "charset".equals(attributeName)) {
            err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
        }
        if (shouldAddAttributes) {
            String value = longStrBufToString();
            if (!endTag) {
                if ("xmlns".equals(attributeName)) {
                    if ("html" == tagName
                            && "http://www.w3.org/1999/xhtml".equals(value)) {
                        if (xmlnsPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                            return;
                        }
                    } else {
                        if (bogusXmlnsPolicy == XmlViolationPolicy.FATAL) {
                            fatal("Forbidden attribute \u201C"
                                    + attributeName
                                    + "\u201D is not mappable to namespace-aware XML 1.0.");
                        } else {
                            warn("Forbidden attribute \u201C"
                                    + attributeName
                                    + "\u201D is not mappable to namespace-aware XML 1.0.");
                            if (bogusXmlnsPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                return;
                            }
                        }
                    }
                } else if (attributeName.startsWith("xmlns:")) {
                    if (bogusXmlnsPolicy == XmlViolationPolicy.FATAL) {
                        fatal("Forbidden attribute \u201C"
                                + attributeName
                                + "\u201D is not mappable to namespace-aware XML 1.0.");
                    } else {
                        warn("Forbidden attribute \u201C"
                                + attributeName
                                + "\u201D is not mappable to namespace-aware XML 1.0.");
                        if (bogusXmlnsPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                            return;
                        }
                    }
                } else if (html4 && html4ModeCompatibleWithXhtml1Schemata
                        && AttributeInfo.isCaseFolded(attributeName)) {
                    value = toAsciiLowerCase(value);
                }
            }
            attributes.addAttribute(attributeName, value);
        }
    }

    private String toAsciiLowerCase(String str) {
        if (str == null) {
            return null;
        }
        char[] b = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
            }
            b[i] = c;
        }
        return new String(b);
    }

    private void runStates() throws SAXException, IOException {
        State returnState = State.DATA;
        State state = State.DATA;
        int index = 0;
        boolean forceQuirks = false;
        char additional = '\u0000';
        char c = '\u0000';
        int entCol = -1;
        int lo = 0;
        int hi = (Entities.NAMES.length - 1);
        int candidate = -1;
        int strBufMark = 0;
        int prevValue = -1;
        int value = 0;
        boolean seenDigits = false;
        consume: for (;;) {
            c = read();
            reconsume: for (;;) {
                switch (state) {
                    case DATA:
                        inContent = true;
                        if (c == '&'
                                && (contentModelFlag == ContentModelFlag.PCDATA || (contentModelFlag == ContentModelFlag.RCDATA)
                                        && !escapeFlag)) {
                            /*
                             * U+0026 AMPERSAND (&) When the content model flag
                             * is set to one of the PCDATA or RCDATA states and
                             * the escape flag is false: switch to the entity
                             * data state. Otherwise: treat it as per the
                             * "anything else" entry below.
                             */
                            flushChars();
                            additional = '\u0000';
                            returnState = state;
                            state = State.CONSUME_ENTITY;
                            continue consume;
                        } else if (c == '<'
                                && ((contentModelFlag == ContentModelFlag.PCDATA) || (escapeFlag == false && (contentModelFlag == ContentModelFlag.CDATA || contentModelFlag == ContentModelFlag.RCDATA)))) {
                            /*
                             * U+003C LESS-THAN SIGN (<) When the content model
                             * flag is set to the PCDATA state: switch to the
                             * tag open state. When the content model flag is
                             * set to either the RCDATA state or the CDATA state
                             * and the escape flag is false: switch to the tag
                             * open state. Otherwise: treat it as per the
                             * "anything else" entry below.
                             */
                            flushChars();
                            resetAttributes();
                            inContent = false;
                            state = State.TAG_OPEN;
                            continue consume;
                        } else if (c == '\u0000') {
                            /*
                             * EOF Emit an end-of-file token.
                             */
                            flushChars();
                            return; // eof() called in parent finally block
                        } else {
                            if (c == '-'
                                    && (escapeFlag == false)
                                    && (contentModelFlag == ContentModelFlag.RCDATA || contentModelFlag == ContentModelFlag.CDATA)
                                    && lastLtExclHyph()) {
                                /*
                                 * U+002D HYPHEN-MINUS (-) If the content model
                                 * flag is set to either the RCDATA state or the
                                 * CDATA state, and the escape flag is false,
                                 * and there are at least three characters
                                 * before this one in the input stream, and the
                                 * last four characters in the input stream,
                                 * including this one, are U+003C LESS-THAN
                                 * SIGN, U+0021 EXCLAMATION MARK, U+002D
                                 * HYPHEN-MINUS, and U+002D HYPHEN-MINUS ("<!--"),
                                 * then set the escape flag to true.
                                 * 
                                 * In any case, emit the input character as a
                                 * character token. Stay in the data state.
                                 */
                                escapeFlag = true;
                            } else if (c == '>' && escapeFlag && lastHyphHyph()) {
                                /*
                                 * U+003E GREATER-THAN SIGN (>) If the content
                                 * model flag is set to either the RCDATA state
                                 * or the CDATA state, and the escape flag is
                                 * true, and the last three characters in the
                                 * input stream including this one are U+002D
                                 * HYPHEN-MINUS, U+002D HYPHEN-MINUS, U+003E
                                 * GREATER-THAN SIGN ("-->"), set the escape
                                 * flag to false.
                                 * 
                                 * In any case, emit the input character as a
                                 * character token. Stay in the data state.
                                 */
                                escapeFlag = false;
                            }
                            /*
                             * Anything else Emit the input character as a
                             * character token.
                             */
                            if (cstart == -1) {
                                // start coalescing character tokens
                                cstart = pos;
                            }
                            /*
                             * Stay in the data state.
                             */
                            continue consume;
                        }
                    case TAG_OPEN:
                        /*
                         * The behaviour of this state depends on the content
                         * model flag.
                         */
                        // this can't happen in PLAINTEXT, so using not PCDATA
                        // as the condition
                        if (contentModelFlag != ContentModelFlag.PCDATA) {
                            /*
                             * If the content model flag is set to the RCDATA or
                             * CDATA states Consume the next input character.
                             */
                            if (c == '/' && contentModelElement != null) {
                                /*
                                 * If it is a U+002F SOLIDUS (/) character,
                                 * switch to the close tag open state.
                                 */
                                index = 0;
                                clearStrBuf();
                                state = State.CLOSE_TAG_OPEN_NOT_PCDATA;
                                continue consume;
                            } else {
                                /*
                                 * Otherwise, emit a U+003C LESS-THAN SIGN
                                 * character token
                                 */
                                tokenHandler.characters(LT_GT, 0, 1);
                                /*
                                 * and reconsume the current input character in
                                 * the data state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            }
                        } else {
                            /*
                             * If the content model flag is set to the PCDATA
                             * state Consume the next input character:
                             */
                            if (c == '!') {
                                /*
                                 * U+0021 EXCLAMATION MARK (!) Switch to the
                                 * markup declaration open state.
                                 */
                                clearLongStrBuf();
                                state = State.MARKUP_DECLARATION_OPEN;
                                continue consume;
                            } else if (c == '/') {
                                /*
                                 * U+002F SOLIDUS (/) Switch to the close tag
                                 * open state.
                                 */
                                state = State.CLOSE_TAG_OPEN_PCDATA;
                                continue consume;
                            } else if (c >= 'A' && c <= 'Z') {
                                /*
                                 * U+0041 LATIN CAPITAL LETTER A through to
                                 * U+005A LATIN CAPITAL LETTER Z Create a new
                                 * start tag token,
                                 */
                                endTag = false;
                                /*
                                 * set its tag name to the lowercase version of
                                 * the input character (add 0x0020 to the
                                 * character's code point),
                                 */
                                clearStrBuf();
                                appendStrBuf((char) (c + 0x20));
                                /* then switch to the tag name state. */
                                state = State.TAG_NAME;
                                /*
                                 * (Don't emit the token yet; further details
                                 * will be filled in before it is emitted.)
                                 */
                                continue consume;
                            } else if (c >= 'a' && c <= 'z') {
                                /*
                                 * U+0061 LATIN SMALL LETTER A through to U+007A
                                 * LATIN SMALL LETTER Z Create a new start tag
                                 * token,
                                 */
                                endTag = false;
                                /*
                                 * set its tag name to the input character,
                                 */
                                clearStrBuf();
                                appendStrBuf(c);
                                /* then switch to the tag name state. */
                                state = State.TAG_NAME;
                                /*
                                 * (Don't emit the token yet; further details
                                 * will be filled in before it is emitted.)
                                 */
                                continue consume;
                            } else if (c == '>') {
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("Bad character \u201C>\u201D in the tag open state.");
                                /*
                                 * Emit a U+003C LESS-THAN SIGN character token
                                 * and a U+003E GREATER-THAN SIGN character
                                 * token.
                                 */
                                tokenHandler.characters(LT_GT, 0, 2);
                                /* Switch to the data state. */
                                state = State.DATA;
                                continue consume;
                            } else if (c == '?') {
                                /*
                                 * U+003F QUESTION MARK (?) Parse error.
                                 */
                                err("Bad character \u201C?\u201D in the tag open state.");
                                /*
                                 * Switch to the bogus comment state.
                                 */
                                clearLongStrBuf();
                                appendLongStrBuf(c);
                                state = State.BOGUS_COMMENT;
                                continue consume;
                            } else {
                                /*
                                 * Anything else Parse error.
                                 */
                                err("Bad character \u201C" + c
                                        + "\u201D in the tag open state.");
                                /*
                                 * Emit a U+003C LESS-THAN SIGN character token
                                 */
                                tokenHandler.characters(LT_GT, 0, 1);
                                /*
                                 * and reconsume the current input character in
                                 * the data state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            }
                        }
                    case CLOSE_TAG_OPEN_NOT_PCDATA:
                        // ASSERT! when entering this state, set index to 0 and
                        // call clearStrBuf()
                        assert (contentModelElement != null);
                        /*
                         * If the content model flag is set to the RCDATA or
                         * CDATA states but no start tag token has ever been
                         * emitted by this instance of the tokeniser (fragment
                         * case), or, if the content model flag is set to the
                         * RCDATA or CDATA states and the next few characters do
                         * not match the tag name of the last start tag token
                         * emitted (case insensitively), or if they do but they
                         * are not immediately followed by one of the following
                         * characters: + U+0009 CHARACTER TABULATION + U+000A
                         * LINE FEED (LF) + U+000B LINE TABULATION + U+000C FORM
                         * FEED (FF) + U+0020 SPACE + U+003E GREATER-THAN SIGN
                         * (>) + U+002F SOLIDUS (/) + EOF
                         * 
                         * ...then emit a U+003C LESS-THAN SIGN character token,
                         * a U+002F SOLIDUS character token, and switch to the
                         * data state to process the next input character.
                         */
                        // Let's implement the above without lookahead. strBuf
                        // holds
                        // characters that need to be emitted if looking for an
                        // end tag
                        // fails.
                        // Duplicating the relevant part of tag name state here
                        // as well.
                        if (index < contentModelElement.length()) {
                            char e = contentModelElement.charAt(index);
                            char folded = c;
                            if (c >= 'A' && c <= 'Z') {
                                folded += 0x20;
                            }
                            if (folded != e) {
                                if (index > 0
                                        || (folded >= 'a' && folded <= 'z')) {
                                    if (html4) {
                                        if (!"iframe".equals(contentModelElement)) {
                                            err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                    : "RCDATA")
                                                    + " element \u201C"
                                                    + contentModelElement
                                                    + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                                        }
                                    } else {
                                        warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                : "RCDATA")
                                                + " element \u201C"
                                                + contentModelElement
                                                + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                                    }
                                }
                                tokenHandler.characters(LT_SOLIDUS, 0, 2);
                                emitStrBuf();
                                state = State.DATA;
                                continue reconsume;
                            }
                            appendStrBuf(c);
                            index++;
                            continue consume;
                        } else {
                            endTag = true;
                            tagName = contentModelElement;
                            switch (c) {
                                case ' ':
                                case '\t':
                                case '\n':
                                case '\u000B':
                                case '\u000C':
                                    /*
                                     * U+0009 CHARACTER TABULATION U+000A LINE
                                     * FEED (LF) U+000B LINE TABULATION U+000C
                                     * FORM FEED (FF) U+0020 SPACE Switch to the
                                     * before attribute name state.
                                     */
                                    state = State.BEFORE_ATTRIBUTE_NAME;
                                    continue consume;
                                case '>':
                                    /*
                                     * U+003E GREATER-THAN SIGN (>) Emit the
                                     * current tag token.
                                     */
                                    emitCurrentTagToken();
                                    /*
                                     * Switch to the data state.
                                     */
                                    state = State.DATA;
                                    continue consume;
                                case '\u0000':
                                    /*
                                     * EOF Parse error.
                                     */
                                    err("Expected \u201C>\u201D but saw end of file instead.");
                                    /*
                                     * Emit the current tag token.
                                     */
                                    emitCurrentTagToken();
                                    /*
                                     * Reconsume the character in the data
                                     * state.
                                     */
                                    state = State.DATA;
                                    continue reconsume;
                                case '/':
                                    /*
                                     * U+002F SOLIDUS (/) Parse error unless
                                     * this is a permitted slash.
                                     */
                                    // never permitted here
                                    err("Stray \u201C/\u201D in end tag.");
                                    /*
                                     * Switch to the before attribute name
                                     * state.
                                     */
                                    state = State.BEFORE_ATTRIBUTE_NAME;
                                    continue consume;
                                default:
                                    if (html4) {
                                        err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                : "RCDATA")
                                                + " element \u201C"
                                                + contentModelElement
                                                + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                                    } else {
                                        warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                : "RCDATA")
                                                + " element \u201C"
                                                + contentModelElement
                                                + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                                    }
                                    tokenHandler.characters(LT_SOLIDUS, 0, 2);
                                    emitStrBuf();
                                    cstart = pos; // don't drop the character
                                    state = State.DATA;
                                    continue consume;
                            }
                        }
                    case CLOSE_TAG_OPEN_PCDATA:
                        /*
                         * Otherwise, if the content model flag is set to the
                         * PCDATA state, or if the next few characters do match
                         * that tag name, consume the next input character:
                         */
                        if (c >= 'A' && c <= 'Z') {
                            /*
                             * U+0041 LATIN CAPITAL LETTER A through to U+005A
                             * LATIN CAPITAL LETTER Z Create a new end tag
                             * token,
                             */
                            endTag = true;
                            clearStrBuf();
                            /*
                             * set its tag name to the lowercase version of the
                             * input character (add 0x0020 to the character's
                             * code point),
                             */
                            appendStrBuf((char) (c + 0x20));
                            /*
                             * then switch to the tag name state. (Don't emit
                             * the token yet; further details will be filled in
                             * before it is emitted.)
                             */
                            state = State.TAG_NAME;
                            continue consume;
                        } else if (c >= 'a' && c <= 'z') {
                            /*
                             * U+0061 LATIN SMALL LETTER A through to U+007A
                             * LATIN SMALL LETTER Z Create a new end tag token,
                             */
                            endTag = true;
                            clearStrBuf();
                            /*
                             * set its tag name to the input character,
                             */
                            appendStrBuf(c);
                            /*
                             * then switch to the tag name state. (Don't emit
                             * the token yet; further details will be filled in
                             * before it is emitted.)
                             */
                            state = State.TAG_NAME;
                            continue consume;
                        } else if (c == '>') {
                            /* U+003E GREATER-THAN SIGN (>) Parse error. */
                            err("Saw \u201C</>\u201D.");
                            /*
                             * Switch to the data state.
                             */
                            state = State.DATA;
                            continue consume;
                        } else if (c == '\u0000') {
                            /* EOF Parse error. */
                            err("Saw \u201C</\u201D immediately before end of file.");
                            /*
                             * Emit a U+003C LESS-THAN SIGN character token and
                             * a U+002F SOLIDUS character token.
                             */
                            tokenHandler.characters(LT_SOLIDUS, 0, 2);
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            state = State.DATA;
                            continue reconsume;
                        } else {
                            /* Anything else Parse error. */
                            err("Garbage after \u201C</\u201D.");
                            /*
                             * Switch to the bogus comment state.
                             */
                            clearLongStrBuf();
                            appendToComment(c);
                            state = State.BOGUS_COMMENT;
                            continue consume;
                        }
                    case TAG_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the before
                                 * attribute name state.
                                 */
                                tagName = strBufToElementNameString();
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                tagName = strBufToElementNameString();
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file seen when looking for tag name");
                                /*
                                 * Emit the current tag token.
                                 */
                                tagName = strBufToElementNameString();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            case '/':
                                /*
                                 * U+002F SOLIDUS (/) Parse error unless this is
                                 * a permitted slash.
                                 */
                                tagName = strBufToElementNameString();
                                parseErrorUnlessPermittedSlash();
                                /*
                                 * Switch to the before attribute name state.
                                 */
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            default:
                                if (c >= 'A' && c <= 'Z') {
                                    /*
                                     * U+0041 LATIN CAPITAL LETTER A through to
                                     * U+005A LATIN CAPITAL LETTER Z Append the
                                     * lowercase version of the current input
                                     * character (add 0x0020 to the character's
                                     * code point) to the current tag token's
                                     * tag name.
                                     */
                                    appendStrBuf((char) (c + 0x20));
                                } else {
                                    /*
                                     * Anything else Append the current input
                                     * character to the current tag token's tag
                                     * name.
                                     */
                                    appendStrBuf(c);
                                }
                                /*
                                 * Stay in the tag name state.
                                 */
                                continue consume;
                        }
                    case BEFORE_ATTRIBUTE_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the before
                                 * attribute name state.
                                 */
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '/':
                                /*
                                 * U+002F SOLIDUS (/) Parse error unless this is
                                 * a permitted slash.
                                 */
                                parseErrorUnlessPermittedSlash();
                                /*
                                 * Stay in the before attribute name state.
                                 */
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                                /*
                                 * Emit the current tag token.
                                 */
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            case '\"':
                            case '\'':
                            case '=':
                                /*
                                 * U+0022 QUOTATION MARK (") U+0027 APOSTROPHE
                                 * (') U+003D EQUALS SIGN (=) Parse error.
                                 */
                                err("Saw \u201C"
                                        + c
                                        + "\u201D when expecting an attribute name.");
                                /*
                                 * Treat it as per the "anything else" entry
                                 * below.
                                 */
                            default:
                                /*
                                 * Anything else Start a new attribute in the
                                 * current tag token.
                                 */
                                clearStrBuf();

                                if (c >= 'A' && c <= 'Z') {
                                    /*
                                     * U+0041 LATIN CAPITAL LETTER A through to
                                     * U+005A LATIN CAPITAL LETTER Z Set that
                                     * attribute's name to the lowercase version
                                     * of the current input character (add
                                     * 0x0020 to the character's code point)
                                     */
                                    appendStrBuf((char) (c + 0x20));
                                } else {
                                    /*
                                     * Set that attribute's name to the current
                                     * input character,
                                     */
                                    appendStrBuf(c);
                                }
                                /*
                                 * and its value to the empty string.
                                 */
                                // Will do later.
                                /*
                                 * Switch to the attribute name state.
                                 */
                                state = State.ATTRIBUTE_NAME;
                                continue consume;
                        }
                    case ATTRIBUTE_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the after
                                 * attribute name state.
                                 */
                                attributeNameComplete();
                                state = State.AFTER_ATTRIBUTE_NAME;
                                continue consume;
                            case '=':
                                /*
                                 * U+003D EQUALS SIGN (=) Switch to the before
                                 * attribute value state.
                                 */
                                attributeNameComplete();
                                clearLongStrBuf();
                                state = State.BEFORE_ATTRIBUTE_VALUE;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                attributeNameComplete();
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '/':
                                /*
                                 * U+002F SOLIDUS (/) Parse error unless this is
                                 * a permitted slash.
                                 */
                                parseErrorUnlessPermittedSlash();
                                /* Switch to the before attribute name state. */
                                attributeNameComplete();
                                addAttributeWithoutValue();
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file occurred in an attribute name.");
                                /*
                                 * Emit the current tag token.
                                 */
                                attributeNameComplete();
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            case '\"':
                            case '\'':
                                /*
                                 * U+0022 QUOTATION MARK (") U+0027 APOSTROPHE
                                 * (') Parse error.
                                 */
                                err("Quote \u201C" + c
                                        + "\u201D in attribute name.");
                                /*
                                 * Treat it as per the "anything else" entry
                                 * below.
                                 */
                            default:
                                if (c >= 'A' && c <= 'Z') {
                                    /*
                                     * U+0041 LATIN CAPITAL LETTER A through to
                                     * U+005A LATIN CAPITAL LETTER Z Append the
                                     * lowercase version of the current input
                                     * character (add 0x0020 to the character's
                                     * code point) to the current attribute's
                                     * name.
                                     */
                                    appendStrBuf((char) (c + 0x20));
                                } else {
                                    /*
                                     * Anything else Append the current input
                                     * character to the current attribute's
                                     * name.
                                     */
                                    appendStrBuf(c);
                                }
                                /*
                                 * Stay in the attribute name state.
                                 */
                                continue consume;
                        }
                    case AFTER_ATTRIBUTE_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the after attribute
                                 * name state.
                                 */
                                continue consume;
                            case '=':
                                /*
                                 * U+003D EQUALS SIGN (=) Switch to the before
                                 * attribute value state.
                                 */
                                clearLongStrBuf();
                                state = State.BEFORE_ATTRIBUTE_VALUE;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '/':
                                /*
                                 * U+002F SOLIDUS (/) Parse error unless this is
                                 * a permitted slash.
                                 */
                                addAttributeWithoutValue();
                                parseErrorUnlessPermittedSlash();
                                /* Switch to the before attribute name state. */
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                                /*
                                 * Emit the current tag token.
                                 */
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the character in the data state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                addAttributeWithoutValue();
                                /*
                                 * Anything else Start a new attribute in the
                                 * current tag token.
                                 */
                                clearStrBuf();

                                if (c >= 'A' && c <= 'Z') {
                                    /*
                                     * U+0041 LATIN CAPITAL LETTER A through to
                                     * U+005A LATIN CAPITAL LETTER Z Set that
                                     * attribute's name to the lowercase version
                                     * of the current input character (add
                                     * 0x0020 to the character's code point)
                                     */
                                    appendStrBuf((char) (c + 0x20));
                                } else {
                                    /*
                                     * Set that attribute's name to the current
                                     * input character,
                                     */
                                    appendStrBuf(c);
                                }
                                /*
                                 * and its value to the empty string.
                                 */
                                // Will do later.
                                /*
                                 * Switch to the attribute name state.
                                 */
                                state = State.ATTRIBUTE_NAME;
                                continue consume;
                        }
                    case BEFORE_ATTRIBUTE_VALUE:
                        // ASSERT! call clearLongStrBuf() before transitioning
                        // to this state!
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the before
                                 * attribute value state.
                                 */
                                continue consume;
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Switch to the
                                 * attribute value (double-quoted) state.
                                 */
                                state = State.ATTRIBUTE_VALUE_DOUBLE_QUOTED;
                                continue consume;
                            case '&':
                                /*
                                 * U+0026 AMPERSAND (&) Switch to the attribute
                                 * value (unquoted) state and reconsume this
                                 * input character.
                                 */
                                state = State.ATTRIBUTE_VALUE_UNQUOTED;
                                continue reconsume;
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Switch to the attribute
                                 * value (single-quoted) state.
                                 */
                                state = State.ATTRIBUTE_VALUE_SINGLE_QUOTED;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                                /*
                                 * Emit the current tag token.
                                 */
                                addAttributeWithoutValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the character in the data state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            case '=':
                                /*
                                 * U+003D EQUALS SIGN (=) Parse error.
                                 */
                                err("\u201C=\u201D in an unquoted attribute value.");
                                /*
                                 * Treat it as per the "anything else" entry
                                 * below.
                                 */
                            default:
                                if (html4
                                        && !((c >= 'a' && c <= 'z')
                                                || (c >= 'A' && c <= 'Z')
                                                || (c >= '0' && c <= '9')
                                                || c == '.' || c == '-'
                                                || c == '_' || c == ':')) {
                                    err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                                }
                                /*
                                 * Anything else Append the current input
                                 * character to the current attribute's value.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Switch to the attribute value (unquoted)
                                 * state.
                                 */
                                state = State.ATTRIBUTE_VALUE_UNQUOTED;
                                continue consume;
                        }
                    case ATTRIBUTE_VALUE_DOUBLE_QUOTED:
                        inContent = true;
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Switch to the after
                                 * attribute value (quoted) state.
                                 */
                                addAttributeWithValue();
                                inContent = false;
                                state = State.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                continue consume;
                            case '&':
                                /*
                                 * U+0026 AMPERSAND (&) Switch to the entity in
                                 * attribute value state, with the additional
                                 * allowed character being U+0022 QUOTATION MARK
                                 * (").
                                 */
                                additional = '\"';
                                returnState = state;
                                state = State.CONSUME_ENTITY;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file reached when inside a quoted attribute value.");
                                /* Emit the current tag token. */
                                addAttributeWithValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the character in the data state.
                                 */
                                inContent = false;
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current attribute's value.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the attribute value (double-quoted)
                                 * state.
                                 */
                                continue consume;
                        }
                    case ATTRIBUTE_VALUE_SINGLE_QUOTED:
                        inContent = true;
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Switch to the after
                                 * attribute value (quoted) state.
                                 */
                                addAttributeWithValue();
                                inContent = false;
                                state = State.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                continue consume;
                            case '&':
                                /*
                                 * U+0026 AMPERSAND (&) Switch to the entity in
                                 * attribute value state, with the + additional
                                 * allowed character being U+0027 APOSTROPHE
                                 * (').
                                 */
                                additional = '\'';
                                returnState = state;
                                state = State.CONSUME_ENTITY;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file reached when inside a quoted attribute value.");
                                /* Emit the current tag token. */
                                addAttributeWithValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the character in the data state.
                                 */
                                inContent = false;
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current attribute's value.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the attribute value (double-quoted)
                                 * state.
                                 */
                                continue consume;
                        }
                    case ATTRIBUTE_VALUE_UNQUOTED:
                        inContent = true;
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the before
                                 * attribute name state.
                                 */
                                addAttributeWithValue();
                                inContent = false;
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            case '&':
                                /*
                                 * U+0026 AMPERSAND (&) Switch to the entity in
                                 * attribute value state, with no + additional
                                 * allowed character.
                                 */
                                additional = '\u0000';
                                returnState = state;
                                state = State.CONSUME_ENTITY;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                addAttributeWithValue();
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                inContent = false;
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                                /*
                                 * Emit the current tag token.
                                 */
                                addAttributeWithValue();
                                emitCurrentTagToken();
                                /*
                                 * Reconsume the character in the data state.
                                 */
                                inContent = false;
                                state = State.DATA;
                                continue reconsume;
                            case '<':
                            case '\"':
                            case '\'':
                            case '=':
                                if (c == '<') {
                                    warn("\u201C<\u201D in an unquoted attribute value. This does not end the tag.");
                                } else {
                                    /*
                                     * U+0022 QUOTATION MARK (") U+0027
                                     * APOSTROPHE (') U+003D EQUALS SIGN (=)
                                     * Parse error.
                                     */
                                    err("\u201C"
                                            + c
                                            + "\u201D in an unquoted attribute value.");
                                    /*
                                     * Treat it as per the "anything else" entry
                                     * below.
                                     */
                                }
                                // fall through
                            default:
                                if (html4
                                        && !((c >= 'a' && c <= 'z')
                                                || (c >= 'A' && c <= 'Z')
                                                || (c >= '0' && c <= '9')
                                                || c == '.' || c == '-'
                                                || c == '_' || c == ':')) {
                                    err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                                }
                                /*
                                 * Anything else Append the current input
                                 * character to the current attribute's value.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the attribute value (unquoted) state.
                                 */
                                continue consume;
                        }
                    case AFTER_ATTRIBUTE_VALUE_QUOTED:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the before
                                 * attribute name state.
                                 */
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * tag token.
                                 */
                                emitCurrentTagToken();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '/':
                                /*
                                 * U+002F SOLIDUS (/) Parse error unless this is
                                 * a permitted slash.
                                 */
                                parseErrorUnlessPermittedSlash();
                                /*
                                 * Switch to the before attribute name state.
                                 */
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue consume;
                            default:
                                /*
                                 * Anything else Parse error.
                                 */
                                err("No space between attributes.");
                                /*
                                 * Reconsume the character in the before
                                 * attribute name state.
                                 */
                                state = State.BEFORE_ATTRIBUTE_NAME;
                                continue reconsume;
                        }
                    case BOGUS_COMMENT:
                        /*
                         * (This can only happen if the content model flag is
                         * set to the PCDATA state.)
                         * 
                         * Consume every character up to the first U+003E
                         * GREATER-THAN SIGN character (>) or the end of the
                         * file (EOF), whichever comes first. Emit a comment
                         * token whose data is the concatenation of all the
                         * characters starting from and including the character
                         * that caused the state machine to switch into the
                         * bogus comment state, up to and including the last
                         * consumed character before the U+003E character, if
                         * any, or up to the end of the file otherwise. (If the
                         * comment was started by the end of the file (EOF), the
                         * token is empty.)
                         * 
                         * Switch to the data state.
                         * 
                         * If the end of the file was reached, reconsume the EOF
                         * character.
                         */
                        switch (c) {
                            case '>':
                                emitComment();
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                emitComment();
                                state = State.DATA;
                                continue reconsume;
                            default:
                                appendToComment(c);
                                continue consume;
                        }
                    case MARKUP_DECLARATION_OPEN:
                        // ASSERT! call clearLongStrBuf() before coming here!

                        /*
                         * (This can only happen if the content model flag is
                         * set to the PCDATA state.)
                         * 
                         * If the next two characters are both U+002D
                         * HYPHEN-MINUS (-) characters, consume those two
                         * characters, create a comment token whose data is the
                         * empty string, and switch to the comment start state.
                         * 
                         * Otherwise if the next seven characters are a
                         * case-insensitive match for the word "DOCTYPE", then
                         * consume those characters and switch to the DOCTYPE
                         * state.
                         * 
                         * Otherwise, is is a parse error. Switch to the bogus
                         * comment state. The next character that is consumed,
                         * if any, is the first character that will be in the
                         * comment.
                         */
                        switch (c) {
                            case '-':
                                state = State.MARKUP_DECLARATION_HYPHEN;
                                continue consume;
                            case 'd':
                            case 'D':
                                appendToComment(c);
                                index = 0;
                                state = State.MARKUP_DECLARATION_OCTYPE;
                                continue consume;
                            default:
                                err("Bogus comment.");
                                state = State.BOGUS_COMMENT;
                                continue reconsume;
                        }
                    case MARKUP_DECLARATION_HYPHEN:
                        if (c == '-') {
                            state = State.COMMENT_START;
                            continue consume;
                        } else {
                            err("Bogus comment.");
                            appendToComment('-');
                            state = State.BOGUS_COMMENT;
                            continue reconsume;
                        }
                    case MARKUP_DECLARATION_OCTYPE:
                        if (index < OCTYPE.length) {
                            char folded = c;
                            if (c >= 'A' && c <= 'Z') {
                                folded += 0x20;
                            }
                            if (folded == OCTYPE[index]) {
                                appendToComment(c);
                            } else {
                                err("Bogus comment.");
                                state = State.BOGUS_COMMENT;
                                continue reconsume;
                            }
                            index++;
                            continue consume;
                        } else {
                            state = State.DOCTYPE;
                            continue reconsume;
                        }
                    case COMMENT_START:
                        /*
                         * Comment start state
                         * 
                         * 
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '-':
                                /*
                                 * U+002D HYPHEN-MINUS (-) Switch to the comment
                                 * start dash state.
                                 */
                                state = State.COMMENT_START_DASH;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("Premature end of comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file inside comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the input character to
                                 * the comment token's data.
                                 */
                                appendToComment(c);
                                /*
                                 * Switch to the comment state.
                                 */
                                state = State.COMMENT;
                                continue consume;
                        }
                    case COMMENT_START_DASH:
                        /*
                         * Comment start dash state
                         * 
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '-':
                                /*
                                 * U+002D HYPHEN-MINUS (-) Switch to the comment
                                 * end state
                                 */
                                state = State.COMMENT_END;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("Premature end of comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file inside comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append a U+002D HYPHEN-MINUS
                                 * (-) character and the input character to the
                                 * comment token's data.
                                 */
                                appendToComment('-');
                                appendToComment(c);
                                /*
                                 * Switch to the comment state.
                                 */
                                state = State.COMMENT;
                                continue consume;
                        }
                    case COMMENT:
                        /*
                         * Comment state Consume the next input character:
                         */
                        switch (c) {
                            case '-':
                                /*
                                 * U+002D HYPHEN-MINUS (-) Switch to the comment
                                 * end dash state
                                 */
                                state = State.COMMENT_END_DASH;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file inside comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the input character to
                                 * the comment token's data.
                                 */
                                appendToComment(c);
                                /*
                                 * Stay in the comment state.
                                 */
                                continue consume;
                        }
                    case COMMENT_END_DASH:
                        /*
                         * Comment end dash state Consume the next input
                         * character:
                         */
                        switch (c) {
                            case '-':
                                /*
                                 * U+002D HYPHEN-MINUS (-) Switch to the comment
                                 * end state
                                 */
                                state = State.COMMENT_END;
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file inside comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append a U+002D HYPHEN-MINUS
                                 * (-) character and the input character to the
                                 * comment token's data.
                                 */
                                appendToComment('-');
                                appendToComment(c);
                                /*
                                 * Switch to the comment state.
                                 */
                                state = State.COMMENT;
                                continue consume;
                        }
                    case COMMENT_END:
                        /*
                         * Comment end dash state Consume the next input
                         * character:
                         */
                        switch (c) {
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the comment
                                 * token.
                                 */
                                emitComment();
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '-':
                                /* U+002D HYPHEN-MINUS (-) Parse error. */
                                err("Consecutive hyphens did not terminate a comment.");
                                /*
                                 * Append a U+002D HYPHEN-MINUS (-) character to
                                 * the comment token's data.
                                 */
                                appendToComment('-');
                                /*
                                 * Stay in the comment end state.
                                 */
                                continue consume;
                            case '\u0000':
                                /*
                                 * EOF Parse error.
                                 */
                                err("End of file inside comment.");
                                /* Emit the comment token. */
                                emitComment();
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Parse error.
                                 */
                                err("Consecutive hyphens did not terminate a comment.");
                                /*
                                 * Append two U+002D HYPHEN-MINUS (-) characters
                                 * and the input character to the comment
                                 * token's data.
                                 */
                                appendToComment('-');
                                appendToComment('-');
                                appendToComment(c);
                                /*
                                 * Switch to the comment state.
                                 */
                                state = State.COMMENT;
                                continue consume;
                        }
                    case DOCTYPE:
                        systemIdentifier = null;
                        publicIdentifier = null;
                        doctypeName = null;
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the before
                                 * DOCTYPE name state.
                                 */
                                state = State.BEFORE_DOCTYPE_NAME;
                                continue consume;
                            default:
                                /*
                                 * Anything else Parse error.
                                 */
                                err("Missing space before doctype name.");
                                /*
                                 * Reconsume the current character in the before
                                 * DOCTYPE name state.
                                 */
                                state = State.BEFORE_DOCTYPE_NAME;
                                continue reconsume;
                        }
                    case BEFORE_DOCTYPE_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the before DOCTYPE
                                 * name state.
                                 */
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("Nameless doctype.");
                                /*
                                 * Create a new DOCTYPE token. Set its
                                 * force-quirks flag to on. Emit the token.
                                 */
                                tokenHandler.doctype("", null, null, true);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Create a new DOCTYPE token. Set its
                                 * force-quirks flag to on. Emit the token.
                                 */
                                tokenHandler.doctype("", null, null, true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /* Anything else Create a new DOCTYPE token. */
                                clearStrBuf();
                                /*
                                 * Set the token's name name to the current
                                 * input character.
                                 */
                                appendStrBuf(c);
                                /*
                                 * Switch to the DOCTYPE name state.
                                 */
                                state = State.DOCTYPE_NAME;
                                continue consume;
                        }
                    case DOCTYPE_NAME:
                        /*
                         * First, consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Switch to the after DOCTYPE
                                 * name state.
                                 */
                                doctypeName = strBufToString();
                                state = State.AFTER_DOCTYPE_NAME;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * DOCTYPE token.
                                 */
                                tokenHandler.doctype(strBufToString(), null,
                                        null, false);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(strBufToString(), null,
                                        null, true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current DOCTYPE token's
                                 * name.
                                 */
                                appendStrBuf(c);
                                /*
                                 * Stay in the DOCTYPE name state.
                                 */
                                continue consume;
                        }
                    case AFTER_DOCTYPE_NAME:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the after DOCTYPE
                                 * name state.
                                 */
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        false);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            case 'p':
                            case 'P':
                                index = 0;
                                state = State.DOCTYPE_UBLIC;
                                continue consume;
                            case 's':
                            case 'S':
                                index = 0;
                                state = State.DOCTYPE_YSTEM;
                                continue consume;
                            default:
                                /*
                                 * Otherwise, this is the parse error.
                                 */
                                err("Bogus doctype.");

                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on.
                                 */
                                forceQuirks = true;
                                /*
                                 * Switch to the bogus DOCTYPE state.
                                 */
                                state = State.BOGUS_DOCTYPE;
                                continue consume;
                        }
                    case DOCTYPE_UBLIC:
                        /*
                         * If the next six characters are a case-insensitive
                         * match for the word "PUBLIC", then consume those
                         * characters and switch to the before DOCTYPE public
                         * identifier state.
                         */
                        if (index < UBLIC.length) {
                            char folded = c;
                            if (c >= 'A' && c <= 'Z') {
                                folded += 0x20;
                            }
                            if (folded != UBLIC[index]) {
                                err("Bogus doctype.");
                                forceQuirks = true;
                                state = State.BOGUS_DOCTYPE;
                                continue reconsume;
                            }
                            index++;
                            continue consume;
                        } else {
                            state = State.BEFORE_DOCTYPE_PUBLIC_IDENTIFIER;
                            continue reconsume;
                        }
                    case DOCTYPE_YSTEM:
                        /*
                         * Otherwise, if the next six characters are a
                         * case-insensitive match for the word "SYSTEM", then
                         * consume those characters and switch to the before
                         * DOCTYPE system identifier state.
                         */
                        if (index < YSTEM.length) {
                            char folded = c;
                            if (c >= 'A' && c <= 'Z') {
                                folded += 0x20;
                            }
                            if (folded != YSTEM[index]) {
                                err("Bogus doctype.");
                                forceQuirks = true;
                                state = State.BOGUS_DOCTYPE;
                                continue reconsume;
                            }
                            index++;
                            continue consume;
                        } else {
                            state = State.BEFORE_DOCTYPE_SYSTEM_IDENTIFIER;
                            continue reconsume;
                        }
                    case BEFORE_DOCTYPE_PUBLIC_IDENTIFIER:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the before DOCTYPE
                                 * public identifier state.
                                 */
                                continue consume;
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                 * token's public identifier to the empty
                                 * string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE public identifier
                                 * (double-quoted) state.
                                 */
                                state = State.DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED;
                                continue consume;
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                 * public identifier to the empty string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE public identifier
                                 * (single-quoted) state.
                                 */
                                state = State.DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED;
                                continue consume;
                            case '>':
                                /* U+003E GREATER-THAN SIGN (>) Parse error. */
                                err("Expected a public identifier but the doctype ended.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        true);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside a doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Parse error.
                                 */
                                err("Bogus doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on.
                                 */
                                forceQuirks = true;
                                /*
                                 * Switch to the bogus DOCTYPE state.
                                 */
                                state = State.BOGUS_DOCTYPE;
                                continue consume;
                        }
                    case DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Switch to the after
                                 * DOCTYPE public identifier state.
                                 */
                                publicIdentifier = longStrBufToString();
                                state = State.AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("\u201C>\u201D in public identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        longStrBufToString(), null, true);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside public identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        longStrBufToString(), null, true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current DOCTYPE token's
                                 * public identifier.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the DOCTYPE public identifier
                                 * (double-quoted) state.
                                 */
                                continue consume;
                        }
                    case DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Switch to the after
                                 * DOCTYPE public identifier state.
                                 */
                                publicIdentifier = longStrBufToString();
                                state = State.AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("\u201C>\u201D in public identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        longStrBufToString(), null, true);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside public identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        longStrBufToString(), null, true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current DOCTYPE token's
                                 * public identifier.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the DOCTYPE public identifier
                                 * (single-quoted) state.
                                 */
                                continue consume;
                        }
                    case AFTER_DOCTYPE_PUBLIC_IDENTIFIER:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the after DOCTYPE
                                 * public identifier state.
                                 */
                                continue consume;
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                 * token's system identifier to the empty
                                 * string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE system identifier
                                 * (double-quoted) state.
                                 */
                                state = State.DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
                                continue consume;
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                 * system identifier to the empty string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE system identifier
                                 * (single-quoted) state.
                                 */
                                state = State.DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, null, false);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, null, true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;

                            default:
                                /* Anything else Parse error. */
                                err("Bogus doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on.
                                 */
                                forceQuirks = true;
                                /*
                                 * Switch to the bogus DOCTYPE state.
                                 */
                                state = State.BOGUS_DOCTYPE;
                                continue consume;
                        }
                    case BEFORE_DOCTYPE_SYSTEM_IDENTIFIER:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the before DOCTYPE
                                 * system identifier state.
                                 */
                                continue consume;
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                 * token's system identifier to the empty
                                 * string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE system identifier
                                 * (double-quoted) state.
                                 */
                                state = State.DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
                                continue consume;
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                 * system identifier to the empty string,
                                 */
                                clearLongStrBuf();
                                /*
                                 * then switch to the DOCTYPE system identifier
                                 * (single-quoted) state.
                                 */
                                state = State.DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
                                continue consume;
                            case '>':
                                /* U+003E GREATER-THAN SIGN (>) Parse error. */
                                err("Expected a system identifier but the doctype ended.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        true);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside a doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName, null, null,
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /* Anything else Parse error. */
                                err("Bogus doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on.
                                 */
                                forceQuirks = true;
                                /*
                                 * Switch to the bogus DOCTYPE state.
                                 */
                                state = State.BOGUS_DOCTYPE;
                                continue consume;
                        }
                    case DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '"':
                                /*
                                 * U+0022 QUOTATION MARK (") Switch to the after
                                 * DOCTYPE system identifier state.
                                 */
                                systemIdentifier = longStrBufToString();
                                state = State.AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("\u201C>\u201D in system identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, longStrBufToString(),
                                        true);

                                /*
                                 * Switch to the data state.
                                 * 
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside system identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, longStrBufToString(),
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current DOCTYPE token's
                                 * system identifier.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the DOCTYPE system identifier
                                 * (double-quoted) state.
                                 */
                                continue consume;
                        }
                    case DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '\'':
                                /*
                                 * U+0027 APOSTROPHE (') Switch to the after
                                 * DOCTYPE system identifier state.
                                 */
                                systemIdentifier = longStrBufToString();
                                state = State.AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Parse error.
                                 */
                                err("\u201C>\u201D in system identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, longStrBufToString(),
                                        true);
                                /*
                                 * Switch to the data state.
                                 * 
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside system identifier.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, longStrBufToString(),
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Append the current input
                                 * character to the current DOCTYPE token's
                                 * system identifier.
                                 */
                                appendLongStrBuf(c);
                                /*
                                 * Stay in the DOCTYPE system identifier
                                 * (double-quoted) state.
                                 */
                                continue consume;
                        }
                    case AFTER_DOCTYPE_SYSTEM_IDENTIFIER:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE Stay in the after DOCTYPE
                                 * system identifier state.
                                 */
                                continue consume;
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit the current
                                 * DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, systemIdentifier,
                                        false);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Set the DOCTYPE token's force-quirks flag to
                                 * on. Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, systemIdentifier,
                                        true);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /* Anything else Parse error. */
                                err("Bogus doctype.");
                                /*
                                 * Switch to the bogus DOCTYPE state. (This does
                                 * not set the DOCTYPE token's force-quirks flag
                                 * to on.)
                                 */
                                forceQuirks = false;
                                state = State.BOGUS_DOCTYPE;
                                continue consume;
                        }
                    case BOGUS_DOCTYPE:
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case '>':
                                /*
                                 * U+003E GREATER-THAN SIGN (>) Emit that
                                 * DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, systemIdentifier,
                                        forceQuirks);
                                /*
                                 * Switch to the data state.
                                 */
                                state = State.DATA;
                                continue consume;
                            case '\u0000':
                                /* EOF Parse error. */
                                err("End of file inside doctype.");
                                /*
                                 * Emit that DOCTYPE token.
                                 */
                                tokenHandler.doctype(doctypeName,
                                        publicIdentifier, systemIdentifier,
                                        forceQuirks);
                                /*
                                 * Reconsume the EOF character in the data
                                 * state.
                                 */
                                state = State.DATA;
                                continue reconsume;
                            default:
                                /*
                                 * Anything else Stay in the bogus DOCTYPE
                                 * state.
                                 */
                                continue consume;
                        }
                    case CONSUME_ENTITY:
                        /*
                         * Unlike the definition is the spec, this state does
                         * not return a value and never requires the caller to
                         * backtrack. This state takes care of emitting
                         * characters or appending to the current attribute
                         * value. It also takes care of that in the case when
                         * consuming the entity fails.
                         */
                        clearStrBuf();
                        appendStrBuf('&');
                        /*
                         * This section defines how to consume an entity. This
                         * definition is used when parsing entities in text and
                         * in attributes.
                         * 
                         * The behaviour depends on the identity of the next
                         * character (the one immediately after the U+0026
                         * AMPERSAND character):
                         */
                        switch (c) {
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                            case '<':
                            case '&':
                            case '\u0000':
                                /*
                                 * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                 * (LF) U+000B LINE TABULATION U+000C FORM FEED
                                 * (FF) U+0020 SPACE U+003C LESS-THAN SIGN
                                 * U+0026 AMPERSAND EOF The additional allowed
                                 * character, if there is one Not an entity. No
                                 * characters are consumed, and nothing is
                                 * returned. (This is not an error, either.)
                                 */
                                // additional handled under default
                                if (returnState != State.DATA) {
                                    appendStrBufToLongStrBuf();
                                } else {
                                    emitStrBuf();
                                }
                                state = returnState;
                                continue reconsume;
                            case '#':
                                /*
                                 * U+0023 NUMBER SIGN (#) Consume the U+0023
                                 * NUMBER SIGN.
                                 */
                                appendStrBuf('#');
                                state = State.CONSUME_NCR;
                                continue consume;
                            default:
                                if (c == additional) {
                                    if (returnState != State.DATA) {
                                        appendStrBufToLongStrBuf();
                                    } else {
                                        emitStrBuf();
                                    }
                                    state = returnState;
                                    continue reconsume;
                                }
                                entCol = -1;
                                lo = 0;
                                hi = (Entities.NAMES.length - 1);
                                candidate = -1;
                                strBufMark = 0;
                                state = State.ENTITY_LOOP;
                                continue reconsume;
                        }
                    case ENTITY_LOOP:
                        entCol++;
                        /*
                         * Anything else Consume the maximum number
                         * of characters possible, with the consumed
                         * characters case-sensitively matching one
                         * of the identifiers in the first column of
                         * the entities table.
                         */
                        hiloop: for (;;) {
                            if (hi == -1) {
                                break hiloop;
                            }
                            if (entCol == Entities.NAMES[hi].length()) {
                                break hiloop;
                            }
                            if (entCol > Entities.NAMES[hi].length()) {
                                state = State.AFTER_ENTITY_LOOP;
                                continue reconsume;
                            } else if (c < Entities.NAMES[hi].charAt(entCol)) {
                                hi--;
                            } else {
                                break hiloop;
                            }
                        }

                        loloop: for (;;) {
                            if (hi < lo) {
                                state = State.AFTER_ENTITY_LOOP;
                                continue reconsume;
                            }
                            if (entCol == Entities.NAMES[lo].length()) {
                                candidate = lo;
                                strBufMark = strBufLen;
                                lo++;
                            } else if (entCol > Entities.NAMES[lo].length()) {
                                state = State.AFTER_ENTITY_LOOP;
                                continue reconsume;
                            } else if (c > Entities.NAMES[lo].charAt(entCol)) {
                                lo++;
                            } else {
                                break loloop;
                            }
                        }
                        if (hi < lo) {
                            state = State.AFTER_ENTITY_LOOP;
                            continue reconsume;
                        }
                        appendStrBuf(c);
                        continue consume;
                    case AFTER_ENTITY_LOOP:
                        // ASSERT c hold the *next* character. we always come here with reconsume
                        // TODO warn about apos (IE) and TRADE (Opera)
                        if (candidate == -1) {
                            /*
                             * If no match can be made, then this is a
                             * parse error.
                             */
                            err("Text after \u201C&\u201D did not match an entity name.");
                            /*
                             * No characters are consumed, and nothing
                             * is returned.
                             */
                            if (returnState != State.DATA) {
                                appendStrBufToLongStrBuf();
                            } else {
                                emitStrBuf();
                            }
                            state = returnState;
                            continue reconsume;
                        } else {
                            if (!Entities.NAMES[candidate].endsWith(";")) {
                                /*
                                 * If the last character matched is not
                                 * a U+003B SEMICOLON (;), there is a
                                 * parse error.
                                 */
                                err("Entity reference was not terminated by a semicolon.");
                                if (returnState != State.DATA) {
                                    /*
                                     * If the entity is being consumed
                                     * as part of an attribute, and the
                                     * last character matched is not a
                                     * U+003B SEMICOLON (;),
                                     */
                                    char ch;
                                    if (strBufMark == strBufLen) {
                                        ch = c;
                                    } else {
                                        ch = strBuf[strBufMark];
                                    }
                                    if ((ch >= '0' && ch <= '9')
                                            || (ch >= 'A' && ch <= 'Z')
                                            || (ch >= 'a' && ch <= 'z')) {
                                        /*
                                         * and the next character is in
                                         * the range U+0030 DIGIT ZERO
                                         * to U+0039 DIGIT NINE, U+0041
                                         * LATIN CAPITAL LETTER A to
                                         * U+005A LATIN CAPITAL LETTER
                                         * Z, or U+0061 LATIN SMALL
                                         * LETTER A to U+007A LATIN
                                         * SMALL LETTER Z, then, for
                                         * historical reasons, all the
                                         * characters that were matched
                                         * after the U+0026 AMPERSAND
                                         * (&) must be unconsumed, and
                                         * nothing is returned.
                                         */
                                        appendStrBufToLongStrBuf();
                                        state = returnState;
                                        continue reconsume;
                                    }
                                }
                            }

                            /*
                             * Otherwise, return a character token for
                             * the character corresponding to the entity
                             * name (as given by the second column of
                             * the entities table).
                             */
                            char[] val = Entities.VALUES[candidate];
                            emitOrAppend(val, returnState != State.DATA);
                            // this is so complicated!
                            if (strBufMark < strBufLen) {
                                if (returnState != State.DATA) {
                                    for (int i = strBufMark; i < strBufLen; i++) {
                                        appendLongStrBuf(strBuf[i]);
                                    }
                                } else {
                                    tokenHandler.characters(strBuf, strBufMark,
                                            strBufLen - strBufMark);
                                }
                            }
                            state = returnState;
                            continue reconsume;
                            /*
                             * If the markup contains I'm &notit; I tell
                             * you, the entity is parsed as "not", as
                             * in, I'm ¬it; I tell you. But if the
                             * markup was I'm &notin; I tell you, the
                             * entity would be parsed as "notin;",
                             * resulting in I'm ∉ I tell you.
                             */
                        }
                    case CONSUME_NCR:
                        prevValue = -1;
                        value = 0;
                        seenDigits = false;
                        /*
                         * The behaviour further depends on the character after the U+0023
                         * NUMBER SIGN:
                         */
                        switch (c) {
                            case 'x':
                            case 'X':

                                /*
                                 * U+0078 LATIN SMALL LETTER X U+0058 LATIN CAPITAL LETTER X Consume
                                 * the X.
                                 * 
                                 * Follow the steps below, but using the range of characters U+0030
                                 * DIGIT ZERO through to U+0039 DIGIT NINE, U+0061 LATIN SMALL
                                 * LETTER A through to U+0066 LATIN SMALL LETTER F, and U+0041 LATIN
                                 * CAPITAL LETTER A, through to U+0046 LATIN CAPITAL LETTER F (in
                                 * other words, 0-9, A-F, a-f).
                                 * 
                                 * When it comes to interpreting the number, interpret it as a
                                 * hexadecimal number.
                                 */
                                appendStrBuf(c);
                                state = State.HEX_NCR_LOOP;
                                continue consume;
                            default:
                                /*
                                 * Anything else Follow the steps below, but using the range of
                                 * characters U+0030 DIGIT ZERO through to U+0039 DIGIT NINE (i.e.
                                 * just 0-9).
                                 * 
                                 * When it comes to interpreting the number, interpret it as a
                                 * decimal number.
                                 */
                                state = State.DECIMAL_NRC_LOOP;
                                continue reconsume;
                        }
                    case HEX_NCR_LOOP:
                        // Deal with overflow gracefully
                        if (value < prevValue) {
                            value = 0x110000; // Value above Unicode range but within int
                            // range
                        }
                        prevValue = value;
                        /*
                         * Consume as many characters as match the range of characters given
                         * above.
                         */
                        if (c >= '0' && c <= '9') {
                            seenDigits = true;
                            value *= 16;
                            value += c - '0';
                            continue consume;
                        } else if (c >= 'A' && c <= 'F') {
                            seenDigits = true;
                            value *= 16;
                            value += c - 'A' + 10;
                            continue consume;
                        } else if (c >= 'a' && c <= 'f') {
                            seenDigits = true;
                            value *= 16;
                            value += c - 'a' + 10;
                            continue consume;
                        } else if (c == ';') {
                            if (seenDigits) {
                                state = State.HANDLE_NCR_VALUE;
                                continue consume;
                            } else {
                                err("No digits after \u201C" + strBufToString()
                                        + "\u201D.");
                                appendStrBuf(';');
                                if (returnState != State.DATA) {
                                    appendStrBufToLongStrBuf();
                                } else {
                                    emitStrBuf();
                                }
                                state = returnState;
                                continue consume;
                            }
                        } else {
                            /*
                             * If no characters match the range, then don't consume any
                             * characters (and unconsume the U+0023 NUMBER SIGN character
                             * and, if appropriate, the X character). This is a parse error;
                             * nothing is returned.
                             * 
                             * Otherwise, if the next character is a U+003B SEMICOLON,
                             * consume that too. If it isn't, there is a parse error.
                             */
                            if (seenDigits) {
                                err("Character reference was not terminated by a semicolon.");
                                state = State.HANDLE_NCR_VALUE;
                                continue reconsume;
                            } else {
                                err("No digits after \u201C" + strBufToString()
                                        + "\u201D.");
                                if (returnState != State.DATA) {
                                    appendStrBufToLongStrBuf();
                                } else {
                                    emitStrBuf();
                                }
                                state = returnState;
                                continue reconsume;
                            }
                        }
                    case DECIMAL_NRC_LOOP:
                        // Deal with overflow gracefully
                        if (value < prevValue) {
                            value = 0x110000; // Value above Unicode range but within int
                            // range
                        }
                        prevValue = value;
                        /*
                         * Consume as many characters as match the range of characters given
                         * above.
                         */
                        if (c >= '0' && c <= '9') {
                            seenDigits = true;
                            value *= 10;
                            value += c - '0';
                            continue consume;
                        } else if (c == ';') {
                            if (seenDigits) {
                                state = State.HANDLE_NCR_VALUE;
                                continue consume;
                            } else {
                                err("No digits after \u201C" + strBufToString()
                                        + "\u201D.");
                                appendStrBuf(';');
                                if (returnState != State.DATA) {
                                    appendStrBufToLongStrBuf();
                                } else {
                                    emitStrBuf();
                                }
                                state = returnState;
                                continue consume;
                            }
                        } else {
                            /*
                             * If no characters match the range, then don't consume any
                             * characters (and unconsume the U+0023 NUMBER SIGN character
                             * and, if appropriate, the X character). This is a parse error;
                             * nothing is returned.
                             * 
                             * Otherwise, if the next character is a U+003B SEMICOLON,
                             * consume that too. If it isn't, there is a parse error.
                             */
                            if (seenDigits) {
                                err("Character reference was not terminated by a semicolon.");
                                state = State.HANDLE_NCR_VALUE;
                                continue reconsume;
                            } else {
                                err("No digits after \u201C" + strBufToString()
                                        + "\u201D.");
                                if (returnState != State.DATA) {
                                    appendStrBufToLongStrBuf();
                                } else {
                                    emitStrBuf();
                                }
                                state = returnState;
                                continue reconsume;
                            }
                        }
                    case HANDLE_NCR_VALUE:
                        // WARNING c is now the *next* char! leave with reconsume!

                        /*
                         * If one or more characters match the range, then take them all and
                         * interpret the string of characters as a number (either hexadecimal or
                         * decimal as appropriate).
                         */
                        if (value >= 0x80 && value <= 0x9f) {
                            /*
                             * If that number is one of the numbers in the first column of the
                             * following table, then this is a parse error.
                             */
                            err("A numeric character reference expanded to the C1 controls range.");
                            /*
                             * Find the row with that number in the first column, and return a
                             * character token for the Unicode character given in the second
                             * column of that row.
                             */
                            char[] val = Entities.WINDOWS_1252[value - 0x80];
                            emitOrAppend(val, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else if (value == 0x0D) {
                            err("A numeric character reference expanded to carriage return.");
                            emitOrAppend(LF, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else if (value == 0) {
                            /*
                             * Otherwise, if the number is zero, if the number is higher than
                             * 0x10FFFF, or if it's one of the surrogate characters (characters
                             * in the range 0xD800 to 0xDFFF), then this is a parse error;
                             * return a character token for the U+FFFD REPLACEMENT CHARACTER
                             * character instead.
                             */
                            err("Character reference expands to U+0000.");
                            emitOrAppend(REPLACEMENT_CHARACTER, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else if ((contentSpacePolicy != XmlViolationPolicy.ALLOW)
                                && (value == 0xB || value == 0xC)) {
                            if (contentSpacePolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                emitOrAppend(SPACE, returnState != State.DATA);
                            } else if (contentSpacePolicy == XmlViolationPolicy.FATAL) {
                                fatal("A character reference expanded to a space character that is not legal XML 1.0 white space.");
                            }
                            state = returnState;
                            continue reconsume;
                        } else if ((value & 0xF800) == 0xD800) {
                            err("Character reference expands to a surrogate.");
                            emitOrAppend(REPLACEMENT_CHARACTER, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else if (value <= 0xFFFF) {
                            /*
                             * Otherwise, return a character token for the Unicode character
                             * whose code point is that number.
                             */
                            char ch = (char) value;
                            if (ch < '\t' || (ch > '\r' && ch < ' ') || isNonCharacter(ch)) {
                                if (contentNonXmlCharPolicy != XmlViolationPolicy.FATAL) {
                                    if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                        ch = '\uFFFD';
                                    }
                                    warn("Character reference expanded to a character that is not a legal XML 1.0 character.");
                                } else {
                                    fatal("Character reference expanded to a character that is not a legal XML 1.0 character.");
                                }
                            }
                            if (isPrivateUse(ch)) {
                                warnAboutPrivateUseChar();
                            }
                            bmpChar[0] = ch;
                            emitOrAppend(bmpChar, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else if (value <= 0x10FFFF) {
                            if (isNonCharacter(value)) {
                                warn("Character reference expands to an astral non-character.");
                            }
                            if (isAstralPrivateUse(value)) {
                                warnAboutPrivateUseChar();
                            }
                            astralChar[0] = (char) (LEAD_OFFSET + (value >> 10));
                            astralChar[1] = (char) (0xDC00 + (value & 0x3FF));
                            emitOrAppend(astralChar, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        } else {
                            err("Character reference outside the permissible Unicode range.");
                            emitOrAppend(REPLACEMENT_CHARACTER, returnState != State.DATA);
                            state = returnState;
                            continue reconsume;
                        }
                }
            }
        }
    }

    /**
     * @param val
     * @throws SAXException
     * @throws IOException
     */
    private void emitOrAppend(char[] val, boolean inAttribute)
            throws SAXException, IOException {
        if (inAttribute) {
            appendLongStrBuf(val);
        } else {
            tokenHandler.characters(val, 0, val.length);
        }
    }

    /**
     * Returns the mappingLangToXmlLang.
     * 
     * @return the mappingLangToXmlLang
     */
    public boolean isMappingLangToXmlLang() {
        return mappingLangToXmlLang;
    }

    /**
     * Sets the mappingLangToXmlLang.
     * 
     * @param mappingLangToXmlLang
     *            the mappingLangToXmlLang to set
     */
    public void setMappingLangToXmlLang(boolean mappingLangToXmlLang) {
        this.mappingLangToXmlLang = mappingLangToXmlLang;
    }

    void setEncoding(Encoding encoding, Confidence confidence) {
        this.characterEncoding = encoding;
        if (confidence == Confidence.CERTAIN) {
            becomeConfident();
        }
    }

    void internalEncodingDeclaration(String internalCharset)
            throws SAXException {
        try {
            internalCharset = Encoding.toAsciiLowerCase(internalCharset);
            Encoding cs;
            if ("utf-16".equals(internalCharset)
                    || "utf-16be".equals(internalCharset)
                    || "utf-16le".equals(internalCharset)
                    || "utf-32".equals(internalCharset)
                    || "utf-32be".equals(internalCharset)
                    || "utf-32le".equals(internalCharset)) {
                cs = Encoding.UTF8;
                errTreeBuilder("Internal encoding declaration specified \u201C"
                        + internalCharset
                        + "\u201D which is not an ASCII superset. Continuing as if the encoding had been \u201Cutf-8\u201D.");
            } else {
                cs = Encoding.forName(internalCharset);
            }
            Encoding actual = cs.getActualHtmlEncoding();
            if (actual == null) {
                actual = cs;
            }
            if (!actual.isAsciiSuperset()) {
                errTreeBuilder("Internal encoding declaration specified \u201C"
                        + internalCharset
                        + "\u201D which is not an ASCII superset. Not changing the encoding.");
                return;
            }

            if (characterEncoding != null && characterEncoding == actual) {
                becomeConfident();
                return;
            }
            if (confidence == Confidence.CERTAIN) {
                errTreeBuilder("Internal encoding declaration \u201C"
                        + internalCharset
                        + "\u201D disagrees with the actual encoding of the document (\u201C"
                        + characterEncoding.getCanonName() + "\u201D).");
            } else {
                Encoding newEnc = whineAboutEncodingAndReturnActual(
                        internalCharset, cs);
                if (characterEncoding == Encoding.WINDOWS1252
                        && canSwitchDecoder && restOfBufferCanSwitchDecoder()) {
                    errTreeBuilder("Changing character encoding in mid-parse to \u201C"
                            + internalCharset + "\u201D.");
                    canSwitchDecoder = false;
                    characterEncoding = newEnc;
                    ((HtmlInputStreamReader) reader).switchEncoding(newEnc);
                    becomeConfident();
                } else {
                    errTreeBuilder("Changing character encoding \u201C"
                            + internalCharset + "\u201D and reparsing.");
                    canSwitchDecoder = false;
                    characterEncoding = newEnc;
                    throw new ReparseException();
                }
            }
        } catch (UnsupportedCharsetException e) {
            errTreeBuilder("Internal encoding declaration named an unsupported chararacter encoding \u201C"
                    + internalCharset + "\u201D.");
        }
    }

    private boolean restOfBufferCanSwitchDecoder() {
        for (int i = pos; i < bufLen; i++) {
            char c = buf[i];
            if (!((c >= 0x09 && c <= 0x0D) || (c >= 0x20 && c <= 0x22)
                    || (c >= 0x26 && c <= 0x27) || (c >= 0x2C && c <= 0x3F)
                    || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     */
    private void becomeConfident() {
        if (rewindableInputStream != null) {
            rewindableInputStream.willNotRewind();
        }
        confidence = Confidence.CERTAIN;
    }

    private class ReparseException extends SAXException {

    }

    /**
     * Sets the encoding sniffing heuristics.
     * 
     * @param heuristics
     *            the heuristics to set
     */
    public void setHeuristics(Heuristics heuristics) {
        this.heuristics = heuristics;
    }
}
