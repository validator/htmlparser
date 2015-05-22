/*
 * Copyright (c) 2015 Mozilla Foundation
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

package nu.validator.encoding.test;

import nu.validator.encoding.Encoding;

public class Big5Tester extends EncodingTester {
    
    public static void main(String[] args) {
        new Big5Tester().test();
    }

    private void test() {
        // ASCII
        decodeBig5("\u6162", "\u0061\u0062");
        // Edge cases
        decodeBig5("\u8740", "\u43F0");
        decodeBig5("\uFEFE", "\u79D4");
        decodeBig5("\uFEFD", "\uD864\uDD0D");
        decodeBig5("\u8862", "\u00CA\u0304");
        // Edge cases surrounded with ASCII
        decodeBig5("\u6187\u4062", "\u0061\u43F0\u0062");
        decodeBig5("\u61FE\uFE62", "\u0061\u79D4\u0062");
        decodeBig5("\u61FE\uFD62", "\u0061\uD864\uDD0D\u0062");
        decodeBig5("\u6188\u6262", "\u0061\u00CA\u0304\u0062");
        // Bad sequences
        decodeBig5("\uFE39", "\uFFFD\u0039");
    }

    private void decodeBig5(String input, String expectation) {
        decode(input, expectation, Encoding.BIG5);
    }

}
