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

package nu.validator.encoding;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;

public class Big5Decoder extends Decoder {

    private int big5Lead = 0;
    
    private char pendingTrail = '\u0000';
    
    protected Big5Decoder(Charset cs) {
        super(cs, 0.5f, 1.0f);
    }

    @Override protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        if (pendingTrail != '\u0000') {
            if (!out.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            out.put(pendingTrail);
            pendingTrail = '\u0000';
        }
        for (;;) {
            if (!in.hasRemaining()) {
                return CoderResult.UNDERFLOW;
            }
            if (!out.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            int b = ((int) in.get() & 0xFF);
            if (big5Lead == 0) {
                if (b <= 0x7F) {
                    out.put((char)b);
                } else if (b >= 0x81 && b <= 0xFE) {
                    if (this.report && !in.hasRemaining()) {
                        // The Java API is badly designed. Need to do this
                        // crazy thing and hope the caller knows about the
                        // undocumented aspects of the API!
                        in.position(in.position() - 1);
                        return CoderResult.UNDERFLOW;
                    }
                    big5Lead = b;
                } else {
                    if (this.report) {
                        in.position(in.position() - 1);                        
                        return CoderResult.malformedForLength(1);
                    }
                    out.put('\uFFFD');                    
                }
            } else {
                int lead = big5Lead;
                big5Lead = 0;
                int offset = 0x62;
                if (b < 0x7F) {
                    offset = 0x40;
                }
                if ((b >= 0x40 && b <= 0x7E) || (b >= 0xA1 && b <= 0xFE)) {
                    int pointer = (lead - 0x81) * 157 + (b - offset);
                    char outLead = Big5Data.lead(pointer);
                    if (this.report && outLead == '\uFFFD') {
                        in.position(in.position() - 1);                        
                        return CoderResult.malformedForLength(1);
                    }
                    out.put(outLead);
                    char outTrail = Big5Data.trail(pointer);
                    if (outTrail != '\u0000') {
                        if (out.hasRemaining()) {
                            out.put(outTrail);
                        } else {
                            pendingTrail = outTrail;
                            return CoderResult.OVERFLOW;
                        }
                    }
                } else {
                    // pointer is null
                    if (b <= 0x7F) {
                        // prepend byte to stream
                        // Always legal, since we've always just read a byte
                        // if we come here.
                        in.position(in.position() - 1);
                    }
                    if (this.report) {
                        // if position() == 0, the caller is not using the
                        // undocumented part of the API right and the line
                        // below will throw!
                        in.position(in.position() - 1);
                        return CoderResult.malformedForLength(1);
                    } else {
                        out.put('\uFFFD');
                    }
                }
            }
        }
    }

    @Override protected CoderResult implFlush(CharBuffer out) {
        if (pendingTrail != '\u0000') {
            if (!out.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            out.put(pendingTrail);
            pendingTrail = '\u0000';
        }
        if (big5Lead != 0) {
            assert !this.report: "How come big5Lead got to be non-zero when decodeLoop() returned in the reporting mode?";
            if (!out.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            out.put('\uFFFD');
            big5Lead = 0;            
        }
        return CoderResult.UNDERFLOW;
    }

    @Override protected void implReset() {
        big5Lead = 0;
        pendingTrail = '\u0000';
    }

}
