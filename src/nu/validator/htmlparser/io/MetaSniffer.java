/*
 * Copyright (c) 2009 Mozilla Foundation
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

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;

import nu.validator.htmlparser.common.ByteReadable;
import nu.validator.htmlparser.impl.MetaScanner;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.ext.Locator2;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class MetaSniffer extends MetaScanner implements Locator, Locator2 {
    
    private Encoding characterEncoding = null;

    private final ErrorHandler errorHandler;
    
    private final Locator locator;
    
    private int line = 1;
    
    private int col = 0;
    
    private boolean prevWasCR = false;

    public MetaSniffer(ErrorHandler eh, Locator locator) {
        this.errorHandler = eh;
        this.locator = locator;
        this.characterEncoding = null;
    }
    
    /**
     * -1 means end.
     * @return
     * @throws IOException
     */
    protected int read() throws IOException {
        int b = readable.readByte();
        // [NOCPP[
        switch (b) {
            case '\n':
                if (!prevWasCR) {
                    line++;
                    col = 0;
                }
                prevWasCR = false;
                break;
            case '\r':
                line++;
                col = 0;
                prevWasCR = true;
                break;
            default:
                col++;
                prevWasCR = false;
                break;
        }
        // ]NOCPP]
        return b;
    }

    /**
     * Main loop.
     * 
     * @return
     * 
     * @throws SAXException
     * @throws IOException
     * @throws
     */
    public Encoding sniff(ByteReadable readable) throws SAXException, IOException {
        this.readable = readable;
        stateLoop(stateSave);
        return characterEncoding;
    }
    

    /**
     * @param string
     * @throws SAXException
     */
    private void err(String message) throws SAXException {
        if (errorHandler != null) {
          SAXParseException spe = new SAXParseException(message, this);
          errorHandler.error(spe);
        }
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void warn(String message) throws SAXException {
        if (errorHandler != null) {
          SAXParseException spe = new SAXParseException(message, this);
          errorHandler.warning(spe);
        }
    }
    
    public int getColumnNumber() {
        return col;
    }

    public int getLineNumber() {
        return line;
    }

    public String getPublicId() {
        if (locator != null) {
            return locator.getPublicId();
        }
        return null;
    }

    public String getSystemId() {
        if (locator != null) {
            return locator.getSystemId();
        }
        return null;
    }

    public String getXMLVersion() {
        if (locator != null) {
            return ((Locator2)locator).getXMLVersion();
        }
        return null;
    }

    public String getEncoding() {
        if (locator != null) {
            return ((Locator2)locator).getEncoding();
        }
        return null;
    }
    
    protected boolean tryCharset(String encoding) throws SAXException {
        encoding = encoding.toLowerCase();
        try {
            if ("utf-16be".equals(encoding) || "utf-16le".equals(encoding)) {
                this.characterEncoding = Encoding.UTF8;
                err(Encoding.msgIgnoredCharset(encoding, "utf-8"));
                return true;
            } else if ("x-user-defined".equals(encoding)) {
                this.characterEncoding = Encoding.WINDOWS1252;
                err(Encoding.msgIgnoredCharset("x-user-defined", "windows-1252"));
                return true;
            } else {
                Encoding cs = Encoding.forName(encoding);
                String canonName = cs.getCanonName();
                if (!cs.getCanonName().equals(encoding)) {
                    err(Encoding.msgNotCanonicalName(encoding, canonName));
                    this.characterEncoding = cs;
                }
                return true;
            }
        } catch (UnsupportedCharsetException e) {
            err(Encoding.msgBadInternalCharset(encoding)
                    + " Will continue sniffing.");
        }
        return false;
    }
}    
