/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2008-2009 Mozilla Foundation
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

import nu.validator.htmlparser.annotation.NoLength;
import nu.validator.htmlparser.impl.Portability;
import nu.validator.htmlparser.impl.TreeBuilder;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class MetaSniffer implements Locator {

    private static final @NoLength char[] CHARSET = "charset".toCharArray();
    
    private static final @NoLength char[] CONTENT = "content".toCharArray();

    private static final int NO = 0;

    private static final int M = 1;
    
    private static final int E = 2;
    
    private static final int T = 3;

    private static final int A = 4;
    
    private static final int DATA = 0;

    private static final int TAG_OPEN = 49;

    private static final int SCAN_UNTIL_GT = 50;

    private static final int TAG_NAME = 58;

    private static final int BEFORE_ATTRIBUTE_NAME = 4;

    private static final int ATTRIBUTE_NAME = 5;

    private static final int AFTER_ATTRIBUTE_NAME = 6;

    private static final int BEFORE_ATTRIBUTE_VALUE = 7;

    private static final int ATTRIBUTE_VALUE_DOUBLE_QUOTED = 8;

    private static final int ATTRIBUTE_VALUE_SINGLE_QUOTED = 9;

    private static final int ATTRIBUTE_VALUE_UNQUOTED = 10;

    private static final int AFTER_ATTRIBUTE_VALUE_QUOTED = 11;

    private static final int MARKUP_DECLARATION_OPEN = 13;
    
    private static final int MARKUP_DECLARATION_HYPHEN = 14;

    private static final int COMMENT_START = 27;

    private static final int COMMENT_START_DASH = 28;

    private static final int COMMENT = 29;

    private static final int COMMENT_END_DASH = 30;

    private static final int COMMENT_END = 31;
    
    private static final int SELF_CLOSING_START_TAG = 32;

    // [NOCPP[
    
    private final ErrorHandler errorHandler;
    
    private final Locator locator;
    
    private int line = 1;
    
    private int col = 0;
    
    private boolean prevWasCR = false;
    
    // ]NOCPP]
    
    private ByteReadable readable;
    
    private Encoding characterEncoding = null;
    
    private int metaState = NO;

    private int contentIndex = -1;
    
    private int charsetIndex = -1;

    private int stateSave = DATA;

    private int strBufLen;

    private char[] strBuf;
    
    /**
     * @param source
     * @param errorHandler
     * @param publicId
     * @param systemId
     */
    public MetaSniffer(ErrorHandler eh, Locator locator) {
        this.errorHandler = eh;
        this.locator = locator;
        this.readable = null;
        this.characterEncoding = null;
        this.metaState = NO;
        this.contentIndex = -1;
        this.charsetIndex = -1;
        this.stateSave = DATA;
        strBufLen = 0;
        strBuf = new char[36];
    }

    /**
     * -1 means end.
     * @return
     * @throws IOException
     */
    private int read() throws IOException {
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

    // WARNING When editing this, makes sure the bytecode length shown by javap
    // stays under 8000 bytes!
    private void stateLoop(int state)
            throws SAXException, IOException {
        int c = -1;
        boolean reconsume = false;
        stateloop: for (;;) {
            switch (state) {
                case DATA:
                    dataloop: for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '<':
                                state = MetaSniffer.TAG_OPEN;
                                break dataloop; // FALL THROUGH continue
                            // stateloop;
                            default:
                                continue;
                        }
                    }
                    // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                case TAG_OPEN:
                    tagopenloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case 'm':
                            case 'M':
                                metaState = M;
                                state = MetaSniffer.TAG_NAME;
                                break tagopenloop;
                                // continue stateloop;                                
                            case '!':
                                state = MetaSniffer.MARKUP_DECLARATION_OPEN;
                                continue stateloop;
                            case '?':
                            case '/':
                                state = MetaSniffer.SCAN_UNTIL_GT;
                                continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                                    metaState = NO;
                                    state = MetaSniffer.TAG_NAME;
                                    break tagopenloop;
                                    // continue stateloop;
                                }
                                state = MetaSniffer.DATA;
                                reconsume = true;
                                continue stateloop;
                        }
                    }
                    // FALL THROUGH DON'T REORDER
                case TAG_NAME:
                    tagnameloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                state = MetaSniffer.BEFORE_ATTRIBUTE_NAME;
                                break tagnameloop;
                            // continue stateloop;
                            case '/':
                                state = MetaSniffer.SELF_CLOSING_START_TAG;
                                continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            case 'e':
                            case 'E':
                                if (metaState == M) {
                                    metaState = E;
                                } else {
                                    metaState = NO;
                                }
                                continue;
                            case 't':
                            case 'T':
                                if (metaState == E) {
                                    metaState = T;
                                } else {
                                    metaState = NO;
                                }
                                continue;
                            case 'a':
                            case 'A':
                                if (metaState == T) {
                                    metaState = A;
                                } else {
                                    metaState = NO;
                                }
                                continue;
                            default:
                                metaState = NO;
                                continue;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case BEFORE_ATTRIBUTE_NAME:
                    beforeattributenameloop: for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        /*
                         * Consume the next input character:
                         */
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                continue;
                            case '/':
                                state = MetaSniffer.SELF_CLOSING_START_TAG;
                                continue stateloop;
                            case '>':
                                state = DATA;
                                continue stateloop;
                            case 'c':
                            case 'C':
                                contentIndex = 0;
                                charsetIndex = 0;
                                state = MetaSniffer.ATTRIBUTE_NAME;
                                break beforeattributenameloop;                                
                            default:
                                contentIndex = -1;
                                charsetIndex = -1;
                                state = MetaSniffer.ATTRIBUTE_NAME;
                                break beforeattributenameloop;
                            // continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case ATTRIBUTE_NAME:
                    attributenameloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                state = MetaSniffer.AFTER_ATTRIBUTE_NAME;
                                continue stateloop;
                            case '/':
                                state = MetaSniffer.SELF_CLOSING_START_TAG;
                                continue stateloop;
                            case '=':
                                strBufLen = 0;
                                state = MetaSniffer.BEFORE_ATTRIBUTE_VALUE;
                                break attributenameloop;
                            // continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                if (metaState == A) {
                                    if (c >= 'A' && c <= 'Z') {
                                        c += 0x20;
                                    }
                                    if (contentIndex == 6) {
                                        contentIndex = -1;
                                    } else if (contentIndex > -1
                                            && contentIndex < 6
                                            && (c == CONTENT[contentIndex + 1])) {
                                        contentIndex++;
                                    }
                                    if (charsetIndex == 6) {
                                        charsetIndex = -1;
                                    } else if (charsetIndex > -1
                                            && charsetIndex < 6
                                            && (c == CHARSET[charsetIndex + 1])) {
                                        charsetIndex++;
                                    }
                                }
                                continue;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case BEFORE_ATTRIBUTE_VALUE:
                    beforeattributevalueloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                continue;
                            case '"':
                                state = MetaSniffer.ATTRIBUTE_VALUE_DOUBLE_QUOTED;
                                break beforeattributevalueloop;
                            // continue stateloop;
                            case '\'':
                                state = MetaSniffer.ATTRIBUTE_VALUE_SINGLE_QUOTED;
                                continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                if (charsetIndex == 6 || contentIndex == 6) {
                                    addToBuffer(c);
                                }
                                state = MetaSniffer.ATTRIBUTE_VALUE_UNQUOTED;
                                continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case ATTRIBUTE_VALUE_DOUBLE_QUOTED:
                    attributevaluedoublequotedloop: for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '"':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                break attributevaluedoublequotedloop;
                            // continue stateloop;
                            default:
                                if (metaState == A && (contentIndex == 6 || charsetIndex == 6)) {
                                    addToBuffer(c);
                                }
                                continue;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case AFTER_ATTRIBUTE_VALUE_QUOTED:
                    afterattributevaluequotedloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                state = MetaSniffer.BEFORE_ATTRIBUTE_NAME;
                                continue stateloop;
                            case '/':
                                state = MetaSniffer.SELF_CLOSING_START_TAG;
                                break afterattributevaluequotedloop;
                            // continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                state = MetaSniffer.BEFORE_ATTRIBUTE_NAME;
                                reconsume = true;
                                continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case SELF_CLOSING_START_TAG:
                    c = read();
                    switch (c) {
                        case -1:
                            break stateloop;
                        case '>':
                            state = MetaSniffer.DATA;
                            continue stateloop;
                        default:
                            state = MetaSniffer.BEFORE_ATTRIBUTE_NAME;
                            reconsume = true;
                            continue stateloop;
                    }
                    // XXX reorder point
                case ATTRIBUTE_VALUE_UNQUOTED:
                    for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':

                            case '\u000C':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.BEFORE_ATTRIBUTE_NAME;
                                continue stateloop;
                            case '>':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                if (metaState == A && (contentIndex == 6 || charsetIndex == 6)) {
                                    addToBuffer(c);
                                }
                                continue;
                        }
                    }
                    // XXX reorder point
                case AFTER_ATTRIBUTE_NAME:
                    for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000C':
                                continue;
                            case '/':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.SELF_CLOSING_START_TAG;
                                continue stateloop;
                            case '=':
                                state = MetaSniffer.BEFORE_ATTRIBUTE_VALUE;
                                continue stateloop;
                            case '>':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            case 'c':
                            case 'C':
                                contentIndex = 0;
                                charsetIndex = 0;
                                state = MetaSniffer.ATTRIBUTE_NAME;
                                continue stateloop;
                            default:
                                contentIndex = -1;
                                charsetIndex = -1;
                                state = MetaSniffer.ATTRIBUTE_NAME;
                                continue stateloop;
                        }
                    }
                    // XXX reorder point
                case MARKUP_DECLARATION_OPEN:
                    markupdeclarationopenloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '-':
                                state = MetaSniffer.MARKUP_DECLARATION_HYPHEN;
                                break markupdeclarationopenloop;
                            // continue stateloop;
                            default:
                                state = MetaSniffer.SCAN_UNTIL_GT;
                                reconsume = true;
                                continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case MARKUP_DECLARATION_HYPHEN:
                    markupdeclarationhyphenloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '-':
                                state = MetaSniffer.COMMENT_START;
                                break markupdeclarationhyphenloop;
                            // continue stateloop;
                            default:
                                state = MetaSniffer.SCAN_UNTIL_GT;
                                reconsume = true;
                                continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case COMMENT_START:
                    commentstartloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '-':
                                state = MetaSniffer.COMMENT_START_DASH;
                                continue stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                state = MetaSniffer.COMMENT;
                                break commentstartloop;
                            // continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case COMMENT:
                    commentloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '-':
                                state = MetaSniffer.COMMENT_END_DASH;
                                break commentloop;
                            // continue stateloop;
                            default:
                                continue;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case COMMENT_END_DASH:
                    commentenddashloop: for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '-':
                                state = MetaSniffer.COMMENT_END;
                                break commentenddashloop;
                            // continue stateloop;
                            default:
                                state = MetaSniffer.COMMENT;
                                continue stateloop;
                        }
                    }
                    // FALLTHRU DON'T REORDER
                case COMMENT_END:
                    for (;;) {
                        c = read();
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            case '-':
                                continue;
                            default:
                                state = MetaSniffer.COMMENT;
                                continue stateloop;
                        }
                    }
                    // XXX reorder point
                case COMMENT_START_DASH:
                    c = read();
                    switch (c) {
                        case -1:
                            break stateloop;
                        case '-':
                            state = MetaSniffer.COMMENT_END;
                            continue stateloop;
                        case '>':
                            state = MetaSniffer.DATA;
                            continue stateloop;
                        default:
                            state = MetaSniffer.COMMENT;
                            continue stateloop;
                    }
                    // XXX reorder point
                case ATTRIBUTE_VALUE_SINGLE_QUOTED:
                    for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '\'':
                                if (tryCharset()) {
                                    break stateloop;
                                }
                                state = MetaSniffer.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                continue stateloop;
                            default:
                                if (metaState == A && (contentIndex == 6 || charsetIndex == 6)) {
                                    addToBuffer(c);
                                }
                                continue;
                        }
                    }
                    // XXX reorder point
                case SCAN_UNTIL_GT:
                    for (;;) {
                        if (reconsume) {
                            reconsume = false;
                        } else {
                            c = read();
                        }
                        switch (c) {
                            case -1:
                                break stateloop;
                            case '>':
                                state = MetaSniffer.DATA;
                                continue stateloop;
                            default:
                                continue;
                        }
                    }
            }
        }
        stateSave  = state;
    }

    private void addToBuffer(int c) {
        if (strBufLen == strBuf.length) {
            char[] newBuf = new char[strBuf.length + (strBuf.length << 1)];
            System.arraycopy(strBuf, 0, newBuf, 0, strBuf.length);
            Portability.releaseArray(strBuf);
            strBuf = newBuf;
        }
        strBuf[strBufLen++] = (char)c;
    }

    private boolean tryCharset() throws SAXException {
        if (metaState != A || !(contentIndex == 6 || charsetIndex == 6)) {
            return false;
        }
        String attVal = Portability.newStringFromBuffer(strBuf, 0, strBufLen);
        String candidateEncoding;
        if (contentIndex == 6) {
            candidateEncoding = TreeBuilder.extractCharsetFromContent(attVal);
            Portability.releaseString(attVal);
        } else {
            candidateEncoding = attVal;
        }
        if (candidateEncoding == null) {
            return false;
        }
        boolean rv = tryCharset(candidateEncoding);
        Portability.releaseString(candidateEncoding);
        contentIndex = -1;
        charsetIndex = -1;
        return rv;
    }
    
    private boolean tryCharset(String encoding) throws SAXException {
        encoding = Encoding.toAsciiLowerCase(encoding);
        try {
            // XXX spec says only UTF-16
            if ("utf-16".equals(encoding) || "utf-16be".equals(encoding) || "utf-16le".equals(encoding) || "utf-32".equals(encoding) || "utf-32be".equals(encoding) || "utf-32le".equals(encoding)) {
                this.characterEncoding = Encoding.UTF8;
                err("The internal character encoding declaration specified \u201C" + encoding + "\u201D which is not a rough superset of ASCII. Using \u201CUTF-8\u201D instead.");
                return true;
            } else {
                Encoding cs = Encoding.forName(encoding);
                String canonName = cs.getCanonName();
                if (!cs.isAsciiSuperset()) {
                    err("The encoding \u201C"
                                + encoding
                                + "\u201D is not an ASCII superset and, therefore, cannot be used in an internal encoding declaration. Continuing the sniffing algorithm.");
                    return false;
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
                return true;
            }
        } catch (UnsupportedCharsetException e) {
            err("Unsupported character encoding name: \u201C" + encoding + "\u201D. Will continue sniffing.");
        }
        return false;
    }

    
}
