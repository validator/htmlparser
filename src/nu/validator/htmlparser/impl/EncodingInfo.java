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

package nu.validator.htmlparser.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class EncodingInfo {

    private static String[] SHOULD_NOT = { "jis_x0212-1990",
        "utf-32",
        "utf-32be",
        "utf-32le",
        "x-jis0208" };
    
    private static String[] BANNED = { "bocu-1", "cesu-8", "compound_text",
            "scsu", "utf-7", "x-imap-mailbox-name", "x-jisautodetect",
            "x-utf-16be-bom", "x-utf-16le-bom", "x-utf-32be-bom",
            "x-utf-32le-bom", "x-utf16_oppositeendian",
            "x-utf16_platformendian", "x-utf32_oppositeendian",
            "x-utf32_platformendian" };
    
    private static String[] NOT_OBSCURE = {"big5",
        "big5-hkscs",
        "euc-jp",
        "euc-kr",
        "gb18030",
        "gbk",
        "iso-2022-jp",
        "iso-2022-kr",
        "iso-8859-1",
        "iso-8859-13",
        "iso-8859-15",
        "iso-8859-2",
        "iso-8859-3",
        "iso-8859-4",
        "iso-8859-5",
        "iso-8859-6",
        "iso-8859-7",
        "iso-8859-8",
        "iso-8859-9",
        "koi8-r",
        "shift_jis",
        "tis-620",
        "us-ascii",
        "utf-16",
        "utf-16be",
        "utf-16le",
        "utf-8",
        "windows-1250",
        "windows-1251",
        "windows-1252",
        "windows-1253",
        "windows-1254",
        "windows-1255",
        "windows-1256",
        "windows-1257",
        "windows-1258"};
    
    private static String[] asciiSuperset;

    private static String[] notAsciiSuperset;   

    static {
        byte[] testBuf = new byte[0x63];
        for (int i = 0; i < 0x60; i++) {
            testBuf[i] = (byte) (i + 0x20);
        }
        testBuf[0x60] = (byte) '\n';
        testBuf[0x61] = (byte) '\r';
        testBuf[0x62] = (byte) '\t';

        SortedSet<String> asciiSupersetSet = new TreeSet<String>();
        SortedSet<String> notAsciiSupersetSet = new TreeSet<String>();
        
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        for (Map.Entry<String, Charset> entry : charsets.entrySet()) {
            Charset cs = entry.getValue();
            String name = cs.name();
            if (!isBanned(name)) {
                if (asciiMapsToBasicLatin(testBuf, cs)) {
                    asciiSupersetSet.add(name.intern());
                } else {
                    notAsciiSupersetSet.add(name.intern());
                }
            }
        }
        
        asciiSuperset = asciiSupersetSet.toArray(new String[0]);
        notAsciiSuperset = notAsciiSupersetSet.toArray(new String[0]);
    }

    public static boolean isAsciiSuperset(String preferredIanaName) {
        return (Arrays.binarySearch(asciiSuperset, preferredIanaName) > -1);
    }

    public static boolean isNotAsciiSuperset(String preferredIanaName) {
        return (Arrays.binarySearch(notAsciiSuperset, preferredIanaName) > -1);
    }

    public static boolean isObscure(String preferredIanaName) {
        // XXX Turkish i
        return !(Arrays.binarySearch(NOT_OBSCURE, preferredIanaName.toLowerCase()) > -1);
    }

    public static boolean isBanned(String preferredIanaName) {
        // XXX Turkish i
        return (Arrays.binarySearch(BANNED, preferredIanaName.toLowerCase()) > -1);
    }

    public static boolean isShouldNot(String preferredIanaName) {
        // XXX Turkish i
        return (Arrays.binarySearch(SHOULD_NOT, preferredIanaName.toLowerCase()) > -1);
    }
    
    /**
     * @param testBuf
     * @param cs
     */
    private static boolean asciiMapsToBasicLatin(byte[] testBuf, Charset cs) {
        CharsetDecoder dec = cs.newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPORT);
        dec.onUnmappableCharacter(CodingErrorAction.REPORT);
        Reader r = new InputStreamReader(new ByteArrayInputStream(testBuf), dec);
        try {
            for (int i = 0; i < 0x60; i++) {
                if ((i + 0x20) != r.read()) {
                    return false;
                }
            }
            if ('\n' != r.read()) {
                return false;
            }
            if ('\r' != r.read()) {
                return false;
            }
            if ('\t' != r.read()) {
                return false;
            }        
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println("ASCII maps to Basic Latin:");
        for (int i = 0; i < asciiSuperset.length; i++) {
            System.out.println(asciiSuperset[i]);            
        }
        System.out.println();
        System.out.println("ASCII does not map to Basic Latin:");
        for (int i = 0; i < notAsciiSuperset.length; i++) {
            System.out.println(notAsciiSuperset[i]);            
        }
    }

    public static boolean isLikelyEbcdic(String canonName) {
        if (isNotAsciiSuperset(canonName)) {
            // XXX Turkish i
            canonName = canonName.toLowerCase();
            return (canonName.startsWith("cp") || canonName.startsWith("ibm"));
        } else {
            return false;
        }
    }
}
