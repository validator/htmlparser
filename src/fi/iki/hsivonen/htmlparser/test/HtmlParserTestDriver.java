/*
 * Copyright (c) 2005 Henri Sivonen
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

package fi.iki.hsivonen.htmlparser.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import fi.iki.hsivonen.htmlparser.HtmlParser;
import fi.iki.hsivonen.xml.SystemErrErrorHandler;
import gnu.xml.pipeline.NSFilter;
import gnu.xml.pipeline.TextConsumer;

/**
 * @version $Id$
 * @author hsivonen
 */
public class HtmlParserTestDriver {

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < args.length; i++) {
            convert(args[i]);
        }
    }
   /**
     * @param string
     */
    private static void convert(String name) throws Throwable {
        if(name.endsWith(".html")) {
            File in = new File(name);
            File out = new File(name.substring(0, name.length() - 5) + ".xhtml");
            HtmlParser parser = new HtmlParser();
            parser.setContentHandler(new NSFilter(new TextConsumer(new FileOutputStream(out))));
            parser.setErrorHandler(new SystemErrErrorHandler());
            InputSource is = new InputSource();
            is.setByteStream(new FileInputStream(in));
            is.setSystemId(in.toURL().toString());
            try {
                parser.parse(is);
            } catch (SAXParseException e) {
                
            }
        }
     }
}
