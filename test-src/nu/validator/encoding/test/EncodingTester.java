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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import nu.validator.encoding.Encoding;

public class EncodingTester {

    protected byte[] stringToBytes(String str) {
        byte[] bytes = new byte[str.length() * 2];
        for (int i = 0; i < str.length(); i++) {
            int pair = (int) str.charAt(i);
            bytes[i * 2] = (byte) (pair >> 8);
            bytes[i * 2 + 1] = (byte) (pair & 0xFF);
        }
        return bytes;
    }

    protected void decode(String input, String expectation, Encoding encoding) {
        byte[] bytes = stringToBytes(input);
        ByteBuffer byteBuf = ByteBuffer.wrap(bytes);
        CharBuffer charBuf = encoding.decode(byteBuf);

        if (charBuf.remaining() != expectation.length()) {
            err("When decoding from a single long buffer, the output length was wrong. Expected: "
                    + expectation.length() + ", got: " + charBuf.remaining(),
                    bytes, expectation);
            return;
        }

        for (int i = 0; i < expectation.length(); i++) {
            char expect = expectation.charAt(i);
            char actual = charBuf.get();
            if (actual != expect) {
                err("When decoding from a single long buffer, failed at position "
                        + i
                        + ", expected: "
                        + charToHex(expect)
                        + ", got: "
                        + charToHex(actual), bytes, expectation);
                return;
            }
        }

        byteBuf = ByteBuffer.allocate(1);
        charBuf = CharBuffer.allocate(expectation.length() + 2);
        CharsetDecoder decoder = encoding.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        for (int i = 0; i < bytes.length; i++) {
            byteBuf.position(0);
            byteBuf.put(bytes[i]);
            byteBuf.position(0);
            CoderResult result = decoder.decode(byteBuf, charBuf,
                    (i + 1) == bytes.length);
            if (result.isMalformed()) {
                err("Decoder reported a malformed sequence when asked to replace at index: "
                        + i, bytes, expectation);
                return;
            } else if (result.isUnmappable()) {
                err("Decoder claimed unmappable sequence, which none of these decoders should do.",
                        bytes, expectation);
                return;
            } else if (result.isOverflow()) {
                err("Decoder claimed overflow when the output buffer is know to be large enough.",
                        bytes, expectation);
            } else if (!result.isUnderflow()) {
                err("Bogus coder result, expected underflow.", bytes,
                        expectation);
            }
        }
        CoderResult result = decoder.flush(charBuf);

        charBuf.limit(charBuf.position());
        charBuf.position(0);

        for (int i = 0; i < expectation.length(); i++) {
            char expect = expectation.charAt(i);
            char actual = charBuf.get();
            if (actual != expect) {
                err("When decoding one byte at a time in REPORT mode, failed at position "
                        + i
                        + ", expected: "
                        + charToHex(expect)
                        + ", got: "
                        + charToHex(actual), bytes, expectation);
                return;
            }
        }
        // TODO: 2 bytes at a time starting at 0 and 2 bytes at a time starting
        // at 1
    }

    private String charToHex(char c) {
        String hex = Integer.toHexString(c);
        switch (hex.length()) {
            case 1:
                return "000" + hex;
            case 2:
                return "00" + hex;
            case 3:
                return "0" + hex;
            default:
                return hex;
        }
    }

    private String byteToHex(byte b) {
        String hex = Integer.toHexString(((int) b & 0xFF));
        switch (hex.length()) {
            case 1:
                return "0" + hex;
            default:
                return hex;
        }
    }

    private void err(String msg, byte[] bytes, String expectation) {
        System.err.println(msg);
        System.err.print("Input:");
        for (int i = 0; i < bytes.length; i++) {
            System.err.print(' ');
            System.err.print(byteToHex(bytes[i]));
        }
        System.err.println();
        System.err.print("Expect:");
        for (int i = 0; i < expectation.length(); i++) {
            System.err.print(' ');
            System.err.print(charToHex(expectation.charAt(i)));
        }
        System.err.println();
    }
}
