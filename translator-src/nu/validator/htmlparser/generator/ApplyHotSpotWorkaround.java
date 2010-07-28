/*
 * Copyright (c) 2010 Mozilla Foundation
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

package nu.validator.htmlparser.generator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Applies a workaround that splits the <code>stateLoop</code> method in the
 * tokenizer into two methods. This way, each method stays under 8000 bytes in
 * size. By default, HotSpot doesn't compile methods that are over 8000 bytes in
 * size, which is a performance problem.
 * 
 * This program should have been written in Perl, but to avoid introducing new
 * dependencies, it's written in Java. No attempt at efficiency has been made.
 * 
 * Warning! This modifies Tokenizer.java in place!
 * 
 * @version $Id$
 * @author hsivonen
 */
public class ApplyHotSpotWorkaround {

    private static final String BEGIN_WORKAROUND = "// BEGIN HOTSPOT WORKAROUND";

    private static final String END_WORKAROUND = "// END HOTSPOT WORKAROUND";

    public static void main(String[] args) throws Throwable {
        String tokenizer = readFileIntoString(args[0]);
        String workaround = readFileIntoString(args[1]);

        int beginIndex = tokenizer.indexOf(BEGIN_WORKAROUND);
        int endIndex = tokenizer.indexOf(END_WORKAROUND);
        String tokenizerHead = tokenizer.substring(0, beginIndex);
        String tokenizerMiddle = tokenizer.substring(beginIndex, endIndex);
        String tokenizerTail = tokenizer.substring(endIndex);

        beginIndex = workaround.indexOf(BEGIN_WORKAROUND);
        endIndex = workaround.indexOf(END_WORKAROUND);
        String workaroundHead = workaround.substring(0, beginIndex);
        String workaroundMiddle = workaround.substring(beginIndex, endIndex);
        String workaroundTail = workaround.substring(endIndex);

        String newTokenizer = tokenizerHead + workaroundMiddle + tokenizerTail;
        String newWorkaround = workaroundHead + tokenizerMiddle
                + workaroundTail;

        newTokenizer.replace("// HOTSPOT WORKAROUND INSERTION POINT",
                newWorkaround);

        Writer out = new OutputStreamWriter(new FileOutputStream(args[0]),
                "utf-8");
        out.write(newTokenizer);
        out.flush();
        out.close();
    }

    private static String readFileIntoString(String name) throws IOException {
        Reader in = new InputStreamReader(new FileInputStream(name), "UTF-8");
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            builder.append((char) c);
        }
        in.close();
        return builder.toString();
    }

}
