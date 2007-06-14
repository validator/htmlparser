/*
 * Copyright (c) 2007 Henri Sivonen
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

package nu.validator.htmlparser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fi.iki.hsivonen.io.EncodingInfo;

public class MetaSniffer implements Locator {

    private class StopSniffingException extends Exception {

    }

    private static final Pattern CONTENT = Pattern.compile("^[^;]*;[\\x09\\x0A\\x0B\\x0C\\x0D\\x20]*[cC][hH][aA][rR][sS][eE][tT][\\x09\\x0A\\x0B\\x0C\\x0D\\x20]*=[\\x09\\x0A\\x0B\\x0C\\x0D\\x20]*(?:(?:([^'\"\\x09\\x0A\\x0B\\x0C\\x0D\\x20][^\\x09\\x0A\\x0B\\x0C\\x0D\\x20]*)(?:[\\x09\\x0A\\x0B\\x0C\\x0D\\x20].*)?)|(?:\"([^\"]*)\".*)|(?:'([^']*)'.*))$", Pattern.DOTALL);
    
    private enum MetaState {
        NO, M, E, T, A
    }

    private final ByteReadable source;
    
    private final ErrorHandler errorHandler;
    
    private CharsetDecoder charsetDecoder = null;
    
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
                case 0x0A: // LF
                    if (!prevWasCR) {
                        line++;
                        col = 0;
                    }
                    prevWasCR = false;
                    break;
                case 0x0D: // CR
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
    public CharsetDecoder sniff() throws SAXException, IOException {
        try {
            for (;;) {
                if (read() == 0x3C) { // <
                    markup();
                }
            }
        } catch (StopSniffingException e) {
            return charsetDecoder;
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
        if (b == 0x21) { // !
            markupDecl();
        } else if (b == 0x2F) { // /
            endTag();
        } else if (b == 0x3F) { // ?
            consumeUntilAndIncludingGt();
        } else if (b == 0x4D || b == 0x6D) { // m or M
            metaState = MetaState.M;
            tag();
        } else if ((b >= 0x41 && b <= 0x5A) || (b >= 0x61 && b <= 0x7A)) { // ASCII
                                                                            // letter
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
                case 0x09: // tab
                case 0x0A: // LF
                case 0x0B: // VT
                case 0x0C: // FF
                case 0x0D: // CR
                case 0x20: // space
                case 0x3E: // >
                case 0x3C: // <
                    break loop;
                case 0x45: // E
                case 0x65: // e
                    if (metaState == MetaState.M) {
                        metaState = MetaState.E;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                case 0x54: // T
                case 0x74: // t
                    if (metaState == MetaState.E) {
                        metaState = MetaState.T;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                case 0x41: // A
                case 0x61: // a
                    if (metaState == MetaState.T) {
                        metaState = MetaState.A;
                    } else {
                        metaState = MetaState.NO;
                    }
                    continue loop;
                default:
                    metaState = MetaState.NO;
                    continue loop;
            }
        }
        unread(b);
        if (b != 0x3C) {
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
                case 0x09: // tab
                case 0x0A: // LF
                case 0x0B: // VT
                case 0x0C: // FF
                case 0x0D: // CR
                case 0x20: // space
                case 0x2F: // /
                    continue loop;
                default:
                    break loop;
            }
        }
        if (b == 0x3C) { // <
            unread(b);
            return false;
        }
        if (b == 0x3E) { // >
            return false;
        }
        attributeName.setLength(0);
        attributeValue.setLength(0);
        unread(b); // this is a bit ugly
        name: for (;;) {
            b = read();
            switch (b) {
                case 0x3D: // =
                    // not actually advancing here yet
                    break name;
                case 0x09: // tab
                case 0x0A: // LF
                case 0x0B: // VT
                case 0x0C: // FF
                case 0x0D: // CR
                case 0x20: // space
                    spaces: for (;;) {
                        b = read();
                        switch (b) {
                            case 0x09: // tab
                            case 0x0A: // LF
                            case 0x0B: // VT
                            case 0x0C: // FF
                            case 0x0D: // CR
                            case 0x20: // space
                                continue spaces;
                            default:
                                break name;
                        }
                    }
                case 0x2f: // /
                    return true;
                case 0x3C: // <
                    unread(b);
                    return false;
                case 0x3E: // >
                    return false;
                default:
                    if (metaState == MetaState.A) {
                        // could use a highly-efficient state machine
                        // here instead of a buffer...
                        if (b >= 0x41 && b <= 0x5A) {
                            attributeName.append((char) (b + 0x20));
                        } else {
                            attributeName.append((char) b);
                        }
                    }
                    continue name;
            }
        }
        if (b != 0x3D) {
            // "If the byte at position is not 0x3D (ASCII '='), stop looking
            // for
            // an attribute. Move position back to the previous byte."
            unread(b);
            return true;
        }
        value: for (;;) {
            b = read();
            switch (b) {
                case 0x09: // tab
                case 0x0A: // LF
                case 0x0B: // VT
                case 0x0C: // FF
                case 0x0D: // CR
                case 0x20: // space
                    continue value;
                default:
                    break value;
            }
        }
        switch (b) {
            case 0x22: // "
                quotedAttribute(0x22);
                return true;
            case 0x27: // '
                quotedAttribute(0x27);
                return true;
            case 0x3C: // <
                unread(b);
                return false;
            case 0x3E: // >
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
                case 0x09: // tab
                case 0x0A: // LF
                case 0x0B: // VT
                case 0x0C: // FF
                case 0x0D: // CR
                case 0x20: // space
                    checkAttribute();
                    return true;
                case 0x3E: // >
                    checkAttribute();
                    return false;
                case 0x3C: // <
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
                tryCharset(attributeValue.toString());
            } else if ("content".equals(name)) {
                Matcher m = CONTENT.matcher(attributeValue);
                if (m.matches()) {
                    String value = null;
                    for (int i = 1; i < 4; i++) {
                        value = m.group(i);
                        if (value != null) {
                            tryCharset(value);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void tryCharset(String encoding) throws SAXException, StopSniffingException {
        encoding = encoding.toUpperCase();
        try {
            // XXX deviating from the spec as per mjs on IRC.
            if ("UTF-16".equals(encoding) || "UTF-16BE".equals(encoding) || "UTF-16LE".equals(encoding) || "UTF-32".equals(encoding) || "UTF-32BE".equals(encoding) || "UTF-32LE".equals(encoding)) {
                this.charsetDecoder = Charset.forName("UTF-8").newDecoder();
                err("The internal character encoding declaration specified \u201C" + encoding + "\u201D which is not a rough superset of ASCII. Using \u201CUTF-8\u201D instead.");
                throw new StopSniffingException();
            } else {
                Charset cs = Charset.forName(encoding);
                String canonName = cs.name();
                if (!EncodingInfo.isAsciiSuperset(canonName)) {
                    err("The encoding \u201C"
                                + encoding
                                + "\u201D is not an ASCII superset and, therefore, cannot be used in an internal encoding declaration. Continuing the sniffing algorithm.");
                    return;
                }
                if (canonName.startsWith("X-") || canonName.startsWith("x-")
                        || canonName.startsWith("Mac")) {
                    if (encoding.startsWith("X-")) {
                        err("The encoding \u201C" + encoding
                                + "\u201D is not an IANA-registered encoding. (Charmod C022)");
                    } else {
                        err("The encoding \u201C" + encoding
                                + "\u201D is not an IANA-registered encoding and did\u2019t start with \u201CX-\u201D. (Charmod C023)");
                    }
                } else if (!canonName.equalsIgnoreCase(encoding)) {
                    err("The encoding \u201C" + encoding
                            + "\u201D is not the preferred name of the character encoding in use. The preferred name is \u201C"
                            + canonName + "\u201D. (Charmod C024)");
                }
                if (EncodingInfo.isObscure(canonName)) {
                    warn("The character encoding \u201C" + encoding + "\u201D is not widely supported. Better interoperability may be achieved by using \u201CUTF-8\u201D.");
                }
                this.charsetDecoder = cs.newDecoder();
                throw new StopSniffingException();
            }
        } catch (IllegalCharsetNameException e) {
            err("Illegal character encoding name: \u201C" + encoding + "\u201D. Will continue sniffing.");
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
        if (read() == 0x2D) { // -
            int hyphensSeen = 0;
            for (;;) {
                int b = read();
                if (b == 0x2D) { // -
                    hyphensSeen++;
                } else if (b == 0x3E) { // >
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
