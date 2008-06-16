/*
 * Copyright (c) 2007 Henri Sivonen
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

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;

import nu.validator.htmlparser.impl.TreeBuilder;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class MetaSniffer implements Locator {

    private class StopSniffingException extends Exception {

    }

    private enum MetaState {
        NO, M, E, T, A
    }

    private final ByteReadable source;
    
    private final ErrorHandler errorHandler;
    
    private Encoding characterEncoding = null;
    
    private StringBuilder attributeName = new StringBuilder();

    private StringBuilder attributeValue = new StringBuilder();

    private MetaState metaState = MetaState.NO;

    private int unread = -1;

    private int line = 1;
    
    private int col = 0;
    
    private boolean prevWasCR = false;

    private final Locator locator;
    
    /**
     * @param source
     * @param errorHandler
     * @param publicId
     * @param systemId
     */
    public MetaSniffer(ByteReadable source, ErrorHandler eh, Locator locator) {
        this.source = source;
        this.errorHandler = eh;
        this.locator = locator;
    }

    // Making this method return an int instead of a char was
    // probably a mistake :-(
    private int read() throws IOException, StopSniffingException {
        if (unread == -1) {
            int b = source.readByte();
            switch (b) {
                case -1: // end
                    throw new StopSniffingException();
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
            return b;
        } else {
            int b = unread;
            unread = -1;
            return b;
        }
    }

    private void unread(int b) {
        this.unread = b;
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
    public Encoding sniff() throws SAXException, IOException {
        try {
            for (;;) {
                if (read() == '<') {
                    markup();
                }
            }
        } catch (StopSniffingException e) {
            return characterEncoding;
        }
    }

    /**
     * <
     * 
     * @throws SAXException
     * @throws StopSniffingException 
     * @throws IOException 
     */
    private void markup() throws SAXException, StopSniffingException, IOException {
        int b = read();
        if (b == '!') {
            markupDecl();
        } else if (b == '/') {
            endTag();
        } else if (b == '?') {
            consumeUntilAndIncludingGt();
        } else if (b == 'm' || b == 'M') {
            metaState = MetaState.M;
            tag();
        } else if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z')) {
            metaState = MetaState.NO;
            tag();
        }
    }

    /**
     * < , x
     * 
     * @throws SAXException
     * @throws StopSniffingException 
     * @throws IOException 
     */
    private void tag() throws SAXException, StopSniffingException, IOException {
        int b;
        loop: for (;;) {
            b = read();
            switch (b) {
                case '\t':
                case '\n':
                
                case 0x0C: // FF
                case '\r':
                case ' ':
                case '>':
                    break loop;
                case 'E':
                case 'e':
                    if (metaState == MetaState.M) {
                        metaState = MetaState.E;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                case 'T':
                case 't':
                    if (metaState == MetaState.E) {
                        metaState = MetaState.T;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                case 'A':
                case 'a':
                    if (metaState == MetaState.T) {
                        metaState = MetaState.A;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                case '/':
                    if (metaState == MetaState.A) {
                        break loop;
                    } else {
                        metaState = MetaState.NO;
                        continue loop;                        
                    }
                default:
                    metaState = MetaState.NO;
                    continue loop;
            }
        }
        unread(b);
        if (b != '>') {
            while (attribute())
                ;
        }
    }

    /**
     * The "get an attribute" subalgorithm.
     * 
     * @return <code>false</code> when to stop
     * @throws SAXException
     * @throws StopSniffingException 
     * @throws IOException 
     */
    private boolean attribute() throws SAXException, StopSniffingException, IOException {
        int b;
        loop: for (;;) {
            b = read();
            switch (b) {
                case '\t':
                case '\n':
                
                case 0x0C: // FF
                case '\r':
                case ' ':
                case '/':
                    continue loop;
                default:
                    break loop;
            }
        }
        if (b == '>') {
            return false;
        }
        attributeName.setLength(0);
        attributeValue.setLength(0);
        unread(b); // this is a bit ugly
        name: for (;;) {
            b = read();
            switch (b) {
                case '=': // =
                    // not actually advancing here yet
                    break name;
                case '\t':
                case '\n':
                
                case 0x0C: // FF
                case '\r':
                case ' ':
                    spaces: for (;;) {
                        b = read();
                        switch (b) {
                            case '\t':
                            case '\n':
                            
                            case 0x0C: // FF
                            case '\r':
                            case ' ':
                                continue spaces;
                            default:
                                break name;
                        }
                    }
                case '/':
                    return true;
                case '>':
                    return false;
                default:
                    if (metaState == MetaState.A) {
                        // could use a highly-efficient state machine
                        // here instead of a buffer...
                        if (b >= 'A' && b <= 'Z') {
                            attributeName.append((char) (b + 0x20));
                        } else {
                            attributeName.append((char) b);
                        }
                    }
                    continue name;
            }
        }
        if (b != '=') {
            // "If the byte at position is not 0x3D (ASCII '='), stop looking
            // for
            // an attribute. Move position back to the previous byte."
            unread(b);
            return true;
        }
        value: for (;;) {
            b = read();
            switch (b) {
                case '\t':
                case '\n':
                
                case 0x0C: // FF
                case '\r':
                case ' ':
                    continue value;
                default:
                    break value;
            }
        }
        switch (b) {
            case '\"':
                quotedAttribute(0x22);
                return true;
            case '\'':
                quotedAttribute(0x27);
                return true;
            case '>':
                return false;
            default:
                unread(b);
                return unquotedAttribute();
        }
    }

    private boolean unquotedAttribute() throws SAXException, StopSniffingException, IOException {
        int b;
        for (;;) {
            b = read();
            switch (b) {
                case '\t':
                case '\n':
                
                case 0x0C: // FF
                case '\r':
                case ' ':
                    checkAttribute();
                    return true;
                case '>':
                    checkAttribute();
                    return false;
                case '<':
                    checkAttribute();
                    unread(b);
                    return false;
                default:
                    // omitting uppercasing
                    if (metaState == MetaState.A) {
                        attributeValue.append((char) b);
                    }
                    break;
            }
        }
    }

    private void checkAttribute() throws SAXException, StopSniffingException {
        if (metaState == MetaState.A) {
            String name = attributeName.toString();
            if ("charset".equals(name)) {
                // XXX revisit trim() to trim only space characters
                tryCharset(attributeValue.toString().trim());
            } else if ("content".equals(name)) {
                String charset = TreeBuilder.extractCharsetFromContent(attributeValue);
                if (charset != null) {
                    tryCharset(charset);
                }
            }
        }
    }

    private void tryCharset(String encoding) throws SAXException, StopSniffingException {
        encoding = Encoding.toAsciiLowerCase(encoding);
        try {
            // XXX spec says only UTF-16
            if ("utf-16".equals(encoding) || "utf-16be".equals(encoding) || "utf-16le".equals(encoding) || "utf-32".equals(encoding) || "utf-32be".equals(encoding) || "utf-32le".equals(encoding)) {
                this.characterEncoding = Encoding.UTF8;
                err("The internal character encoding declaration specified \u201C" + encoding + "\u201D which is not a rough superset of ASCII. Using \u201CUTF-8\u201D instead.");
                throw new StopSniffingException();
            } else {
                Encoding cs = Encoding.forName(encoding);
                String canonName = cs.getCanonName();
                if (!cs.isAsciiSuperset()) {
                    err("The encoding \u201C"
                                + encoding
                                + "\u201D is not an ASCII superset and, therefore, cannot be used in an internal encoding declaration. Continuing the sniffing algorithm.");
                    return;
                }
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
                } else if (!cs.getCanonName().equals(encoding)) {
                    err("The encoding \u201C" + encoding
                            + "\u201D is not the preferred name of the character encoding in use. The preferred name is \u201C"
                            + canonName + "\u201D. (Charmod C024)");
                }
                if (cs.isShouldNot()) {
                    warn("Authors should not use the character encoding \u201C"
                            + encoding
                            + "\u201D. It is recommended to use \u201CUTF-8\u201D.");                
                } else if (cs.isObscure()) {
                    warn("The character encoding \u201C" + encoding + "\u201D is not widely supported. Better interoperability may be achieved by using \u201CUTF-8\u201D.");
                }
                Encoding actual = cs.getActualHtmlEncoding();
                if (actual == null) {
                    this.characterEncoding = cs;
                } else {
                    warn("Using \u201C" + actual.getCanonName() + "\u201D instead of the declared encoding \u201C" + encoding + "\u201D.");
                    this.characterEncoding = actual;
                }
                throw new StopSniffingException();
            }
        } catch (UnsupportedCharsetException e) {
            err("Unsupported character encoding name: \u201C" + encoding + "\u201D. Will continue sniffing.");
        }
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
    
    private void quotedAttribute(int delim) throws SAXException, StopSniffingException, IOException {
        int b;
        for (;;) {
            b = read();
            if (b == delim) {
                checkAttribute();
                return;
            } else {
                if (metaState == MetaState.A) {
                    attributeValue.append((char) b);
                }
            }
        }
    }

    private void consumeUntilAndIncludingGt() throws IOException, StopSniffingException {
        for (;;) {
            if (read() == 0x3E) { // >
                return;
            }
        }
    }

    /**
     * Seen < , /
     * 
     * @throws SAXException
     * @throws StopSniffingException 
     * @throws IOException 
     */
    private void endTag() throws SAXException, StopSniffingException, IOException {
        int b = read();
        if ((b >= 0x41 && b <= 0x5A) || (b >= 0x61 && b <= 0x7A)) { // ASCII
            // letter
            metaState = MetaState.NO;
            tag();
        } else {
            consumeUntilAndIncludingGt();
        }
    }

    /**
     * Seen < , !
     * @throws IOException 
     * @throws StopSniffingException 
     */
    private void markupDecl() throws IOException, StopSniffingException {
        if (read() == 0x2D) { // -
            comment();
        } else {
            consumeUntilAndIncludingGt();
        }
    }

    /**
     * Seen < , ! , -
     * @throws IOException 
     * @throws StopSniffingException 
     */
    private void comment() throws IOException, StopSniffingException {
        if (read() == '-') {
            int hyphensSeen = 2;
            for (;;) {
                int b = read();
                if (b == '-') {
                    hyphensSeen++;
                } else if (b == '>') {
                    if (hyphensSeen >= 2) {
                        return;
                    } else {
                        hyphensSeen = 0;
                    }
                } else {
                    hyphensSeen = 0;
                }
            }
        } else {
            consumeUntilAndIncludingGt();
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

}
