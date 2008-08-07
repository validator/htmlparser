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

import nu.validator.htmlparser.annotation.Local;
import nu.validator.htmlparser.annotation.NsUri;
import nu.validator.htmlparser.annotation.QName;

public final class Portability {

    // Allocating methods
    
    public static String newAsciiLowerCaseStringFromString(String str) {
        if (str == null) {
            return null;
        }
        char[] buf = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
            }
            buf[i] = c;
        }
        return new String(buf);
    }

    public static @Local String newLocalNameFromBuffer(char[] buf, int offset, int length) {
        return new String(buf, offset, length).intern();
    }

    public static String newStringFromBuffer(char[] buf, int length) {
        return new String(buf, 0, length);
    }
    
    public static char[] newCharArrayFromLocal(@Local String local) {
        return local.toCharArray();
    }
    
    // Deallocation methods
    
    public static void releaseString(String str) {
        // No-op in Java
    }
    
    public static void releaseLocal(@Local String local) {
        // No-op in Java
    }
    
    public static void releaseCharArray(char[] buf) {
        // No-op in Java
    }    
    
    // Comparison methods
    
    public static boolean stringEqualsBuffer(@Local String local, char[] buf, int offset, int length) {
        if (local.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (local.charAt(i) != buf[offset + i]) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean stringEqualsString(String one, String other) {
        return one.equals(other);
    }

    public static boolean stringEqualsIgnoreAsciiCaseString(String one,
            String other) {
        if (other == null && one == null) {
            return true;
        }
        if (other == null || one == null) {
            return false;
        }
        if (one.length() != other.length()) {
            return false;
        }
        for (int i = 0; i < other.length(); i++) {
            char c0 = one.charAt(i);
            if (c0 >= 'A' && c0 <= 'Z') {
                c0 += 0x20;
            }
            char c1 = other.charAt(i);
            if (c1 >= 'A' && c1 <= 'Z') {
                c1 += 0x20;
            }
            if (c0 != c1) {
                return false;
            }
        }
        return true;
    }
}
