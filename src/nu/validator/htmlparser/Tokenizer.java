/*
 * Copyright (c) 2005, 2006, 2007 Henri Sivonen
 * Portions of comments Copyright 2004-2007 Apple Computer, Inc., Mozilla 
 * Foundation, and Opera Software ASA.
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

/*
 * The comments following this one that use the same comment syntax as this 
 * comment are quotes from the WHATWG HTML 5 spec as of 2 June 2007 
 * amended as of June 23 2007.
 * That document came with this statement:
 * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
 * Opera Software ASA. You are granted a license to use, reproduce and 
 * create derivative works of this document."
 */

package nu.validator.htmlparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

import org.whattf.checker.NormalizationChecker;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fi.iki.hsivonen.io.EncodingInfo;
import fi.iki.hsivonen.xml.EmptyAttributes;

/**
 * An implementatition of
 * http://www.whatwg.org/specs/web-apps/current-work/multipage/section-tokenisation.html
 * 
 * This class implements the <code>Locator</code> interface. This is not an
 * incidental implementation detail: Users of this class are encouraged to make
 * use of the <code>Locator</code> nature.
 * 
 * By default, the tokenizer may report data that XML 1.0 bans. The tokenizer
 * can be configured to treat these conditions as fatal or to coerce the infoset
 * to something that XML 1.0 allows.
 * 
 * @version $Id$
 * @author hsivonen
 */
public final class Tokenizer implements Locator {

    /**
     * Magic value for UTF-16 operations.
     */
    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);

    /**
     * Magic value for UTF-16 operations.
     */
    private static final int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;

    /**
     * UTF-16 code unit array containing less than and greater than for emitting
     * those characters on certain parse errors.
     */
    private static final char[] LT_GT = { '<', '>' };

    /**
     * UTF-16 code unit array containing less than and solidus for emitting
     * those characters on certain parse errors.
     */
    private static final char[] LT_SOLIDUS = { '<', '/' };

    /**
     * Array version of U+FFFD.
     */
    private static final char[] REPLACEMENT_CHARACTER = { '\uFFFD' };

    /**
     * Array version of space.
     */
    private static final char[] SPACE = { ' ' };

    /**
     * Array version of line feed.
     */
    private static final char[] LF = { '\n' };

    /**
     * Buffer growth parameter.
     */
    private static final int BUFFER_GROW_BY = 1024;

    /**
     * Lexically sorted void element names
     */
    private static final String[] VOID_ELEMENTS = { "area", "base", "br",
            "col", "embed", "hr", "img", "input", "link", "meta", "param" };

    /**
     * "octype" as <code>char[]</code>
     */
    private static final char[] OCTYPE = "octype".toCharArray();

    /**
     * "ublic" as <code>char[]</code>
     */
    private static final char[] UBLIC = "ublic".toCharArray();

    /**
     * "ystem" as <code>char[]</code>
     */
    private static final char[] YSTEM = "ystem".toCharArray();

    /**
     * The token handler.
     */
    private final TokenHandler tokenHandler;

    /**
     * The error handler.
     */
    private ErrorHandler errorHandler;

    /**
     * The input UTF-16 code unit stream. If a byte stream was given, this
     * object is an instance of <code>HtmlInputStreamReader</code>.
     */
    private Reader reader;

    /**
     * The main input buffer that the tokenizer reads from. Filled from
     * <code>reader</code>.
     */
    private char[] buf = new char[2048];

    /**
     * The index of the last <code>char</code> read from <code>buf</code>.
     */
    private int pos;

    /**
     * The index of the first <code>char</code> in <code>buf</code> that is
     * part of a coalesced run of character tokens or <code>-1</code> if there
     * is not a current run being coalesced.
     */
    private int cstart;

    /**
     * The number of <code>char</code>s in <code>buf</code> that have
     * meaning. (The rest of the array is garbage and should not be examined.)
     */
    private int bufLen;

    /**
     * The previous <code>char</code> read from the buffer with infoset
     * alteration applied except for CR. Used for CRLF normalization and
     * surrogate pair checking.
     */
    private char prev;

    /**
     * Lookbehind buffer for magic RCDATA/CDATA escaping.
     */
    private final char[] prevFour = new char[4];

    /**
     * Points to the last <code>char</code> written to <code>prevFour</code>.
     */
    private int prevFourPtr = 0;

    /**
     * Single code unit buffer for reconsuming an input character. If
     * <code>-1</code> the next <code>read()</code> returns from the real
     * buffer, otherwise from here.
     */
    private int unreadBuffer = -1;

    /**
     * The current line number in the current resource being parsed. (First line
     * is 1.) Passed on as locator data.
     */
    private int line;

    /**
     * The current column number in the current resource being tokenized. (First
     * column is 1, counted by UTF-16 code units.) Passed on as locator data.
     */
    private int col;

    /**
     * The SAX public id for the resource being tokenized. (Only passed to back
     * as part of locator data.)
     */
    private String publicId;

    /**
     * The SAX system id for the resource being tokenized. (Only passed to back
     * as part of locator data.)
     */
    private String systemId;

    /**
     * Buffer for short identifiers.
     */
    private char[] strBuf = new char[64];

    /**
     * Number of significant <code>char</code>s in <code>strBuf</code>.
     */
    private int strBufLen = 0;

    /**
     * Buffer for long strings.
     */
    private char[] longStrBuf = new char[1024];

    /**
     * Number of significant <code>char</code>s in <code>longStrBuf</code>.
     */
    private int longStrBufLen = 0;

    /**
     * If not U+0000, a pending code unit to be appended to
     * <code>longStrBuf</code>.
     */
    private char longStrBufPending = '\u0000';

    /**
     * The attribute holder.
     */
    private AttributesImpl attributes;

    /**
     * Buffer for expanding NCRs falling into the Basic Multilingual Plane.
     */
    private final char[] bmpChar = new char[1];

    /**
     * Buffer for expanding astral NCRs.
     */
    private final char[] astralChar = new char[2];

    /**
     * Keeps track of PUA warnings.
     */
    private boolean alreadyWarnedAboutPrivateUseCharacters;

    /**
     * http://www.whatwg.org/specs/web-apps/current-work/#content2
     */
    private ContentModelFlag contentModelFlag = ContentModelFlag.PCDATA;

    /**
     * http://www.whatwg.org/specs/web-apps/current-work/#escape
     */
    private boolean escapeFlag = false;

    /**
     * The element whose end tag closes the current CDATA or RCDATA element.
     */
    private String contentModelElement = "";

    /**
     * <code>true</code> if tokenizing an end tag
     */
    private boolean endTag;

    /**
     * The current tag token name.
     */
    private String tagName = null;

    /**
     * The current attribute name.
     */
    private String attributeName = null;

    /**
     * Whether comment tokens are emitted.
     */
    private boolean wantsComments = false;

    /**
     * If <code>false</code>, <code>addAttribute*()</code> are no-ops.
     */
    private boolean shouldAddAttributes;

    /**
     * <code>true</code> when in text content or in attribute value.
     */
    private boolean inContent;

    /**
     * <code>true</code> when HTML4-specific additional errors are requested.
     */
    private boolean html4;

    /**
     * Whether non-ASCII causes an error.
     */
    private boolean nonAsciiProhibited;

    /**
     * Used together with <code>nonAsciiProhibited</code>.
     */
    private boolean alreadyComplainedAboutNonAscii;

    /**
     * Whether the stream is past the first 512 bytes.
     */
    private boolean metaBoundaryPassed;

    /**
     * The name of the current doctype token.
     */
    private String doctypeName;

    /**
     * The public id of the current doctype token.
     */
    private String publicIdentifier;

    /**
     * The system id of the current doctype token.
     */
    private String systemIdentifier;

    /**
     * Used for NFC checking if non-<code>null</code>.
     */
    private NormalizationChecker normalizationChecker = null;

    /**
     * The policy for vertical tab and form feed.
     */
    private XmlViolationPolicy contentSpacePolicy = XmlViolationPolicy.ALLOW;

    /**
     * The policy for non-space non-XML characters.
     */
    private XmlViolationPolicy contentNonXmlCharPolicy = XmlViolationPolicy.ALLOW;

    /**
     * The policy for comments.
     */
    private XmlViolationPolicy commentPolicy = XmlViolationPolicy.ALLOW;

    private boolean swallowBom;

    // start public API

    /**
     * The constuctor.
     * 
     * @param tokenHandler
     *            the handler for receiving tokens
     */
    public Tokenizer(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    /**
     * Turns NFC checking on or off.
     * 
     * @param enable
     *            <code>true</code> if checking on
     */
    public void setCheckingNormalization(boolean enable) {
        if (enable) {
            normalizationChecker = new NormalizationChecker(true);
            normalizationChecker.setDocumentLocator(this);
            normalizationChecker.setErrorHandler(errorHandler);
        } else {
            normalizationChecker = null;
        }
    }

    /**
     * Query if checking normalization.
     * 
     * @return <code>true</code> if checking on
     */
    public boolean isCheckingNormalization() {
        return normalizationChecker != null;
    }

    /**
     * Sets the error handler.
     * 
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
        if (this.normalizationChecker != null) {
            this.normalizationChecker.setErrorHandler(eh);
        }
    }

    /**
     * Returns the commentPolicy.
     * 
     * @return the commentPolicy
     */
    public XmlViolationPolicy getCommentPolicy() {
        return commentPolicy;
    }

    /**
     * Sets the commentPolicy.
     * 
     * @param commentPolicy
     *            the commentPolicy to set
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        this.commentPolicy = commentPolicy;
    }

    /**
     * Returns the contentNonXmlCharPolicy.
     * 
     * @return the contentNonXmlCharPolicy
     */
    public XmlViolationPolicy getContentNonXmlCharPolicy() {
        return contentNonXmlCharPolicy;
    }

    /**
     * Sets the contentNonXmlCharPolicy.
     * 
     * @param contentNonXmlCharPolicy
     *            the contentNonXmlCharPolicy to set
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        this.contentNonXmlCharPolicy = contentNonXmlCharPolicy;
    }

    /**
     * Returns the contentSpacePolicy.
     * 
     * @return the contentSpacePolicy
     */
    public XmlViolationPolicy getContentSpacePolicy() {
        return contentSpacePolicy;
    }

    /**
     * Sets the contentSpacePolicy.
     * 
     * @param contentSpacePolicy
     *            the contentSpacePolicy to set
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        this.contentSpacePolicy = contentSpacePolicy;
    }

    /**
     * Runs the tokenization. This is the main entry point.
     * 
     * @param is
     *            the input source
     * @throws SAXException
     *             on fatal error (if configured to treat XML violations as
     *             fatal) or if the token handler threw
     * @throws IOException
     *             if the stream threw
     */
    public void tokenize(InputSource is) throws SAXException, IOException {
        swallowBom = true;
        this.systemId = is.getSystemId();
        this.publicId = is.getPublicId();
        this.reader = is.getCharacterStream();
        CharsetDecoder decoder = decoderFromExternalDeclaration(is.getEncoding());
        if (this.reader == null) {
            InputStream inputStream = is.getByteStream();
            if (inputStream == null) {
                throw new SAXException("Both streams in InputSource were null.");
            }
            if (decoder == null) {
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this);
            } else {
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this, decoder);
            }
        }
        contentModelFlag = ContentModelFlag.PCDATA;
        escapeFlag = false;
        inContent = true;
        pos = -1;
        cstart = -1;
        line = 1;
        col = 0;
        prev = '\u0000';
        bufLen = 0;
        nonAsciiProhibited = false;
        alreadyComplainedAboutNonAscii = false;
        html4 = false;
        alreadyWarnedAboutPrivateUseCharacters = false;
        metaBoundaryPassed = false;
        tokenHandler.start(this);
        wantsComments = tokenHandler.wantsComments();
        try {
            if (swallowBom) {
                // Swallow the BOM
                char c = read();
                if (c == '\uFEFF') {
                    col = 0;
                } else {
                    unread(c);
                }
            }
            dataState();
        } finally {
            systemIdentifier = null;
            publicIdentifier = null;
            doctypeName = null;
            tagName = null;
            attributeName = null;
            tokenHandler.eof();
            reader.close();
        }
    }

    // For the token handler to call
    /**
     * Sets the content model flag and the associated element name.
     * 
     * @param contentModelFlag
     *            the flag
     * @param contentModelElement
     *            the element causing the flag to be set
     */
    public void setContentModelFlag(ContentModelFlag contentModelFlag,
            String contentModelElement) {
        this.contentModelFlag = contentModelFlag;
        this.contentModelElement = contentModelElement;
    }

    // start Locator impl

    /**
     * @see org.xml.sax.Locator#getPublicId()
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * @see org.xml.sax.Locator#getSystemId()
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @see org.xml.sax.Locator#getLineNumber()
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * @see org.xml.sax.Locator#getColumnNumber()
     */
    public int getColumnNumber() {
        return col;
    }

    // end Locator impl

    // end public API

    void notifyAboutMetaBoundary() {
        metaBoundaryPassed = true;
    }

    void turnOnAdditionalHtml4Errors() {
        html4 = true;
    }

    void dontSwallowBom() {
        swallowBom = false;
    }

    void noEncodingDeclared() {
        nonAsciiProhibited = true;
    }

    AttributesImpl newAttributes() {
        return new AttributesImpl();
    }
    
    /**
     * Clears the smaller buffer.
     */
    private void clearStrBuf() {
        strBufLen = 0;
    }

    /**
     * Appends to the smaller buffer.
     * 
     * @param c
     *            the UTF-16 code unit to append
     */
    private void appendStrBuf(char c) {
        if (strBufLen == strBuf.length) {
            char[] newBuf = new char[strBuf.length + BUFFER_GROW_BY];
            System.arraycopy(strBuf, 0, newBuf, 0, strBuf.length);
        } else {
            strBuf[strBufLen++] = c;
        }
    }

    /**
     * The smaller buffer as a string.
     * 
     * @return the smaller buffer as a string
     */
    private String strBufToString() {
        return new String(strBuf, 0, strBufLen);
    }

    /**
     * Emits the smaller buffer as character tokens.
     * 
     * @throws SAXException
     *             if the token handler threw
     */
    private void emitStrBuf() throws SAXException {
        if (strBufLen > 0) {
            tokenHandler.characters(strBuf, 0, strBufLen);
        }
    }

    /**
     * Clears the larger buffer.
     */
    private void clearLongStrBuf() {
        longStrBufLen = 0;
        longStrBufPending = '\u0000';
    }

    /**
     * Appends to the larger buffer.
     * 
     * @param c
     *            the UTF-16 code unit to append
     */
    private void appendLongStrBuf(char c) {
        if (longStrBufLen == longStrBuf.length) {
            char[] newBuf = new char[longStrBuf.length + BUFFER_GROW_BY];
            System.arraycopy(longStrBuf, 0, newBuf, 0, longStrBuf.length);
        } else {
            longStrBuf[longStrBufLen++] = c;
        }
    }

    /**
     * Appends to the larger buffer when it is used to buffer a comment. Checks
     * for two consecutive hyphens.
     * 
     * @param c
     *            the UTF-16 code unit to append
     * @throws SAXException
     */
    private void appendToComment(char c) throws SAXException {
        if (longStrBufPending == '-' && c == '-') {
            if (commentPolicy == XmlViolationPolicy.FATAL) {
                fatal("This document is not mappable to XML 1.0 without data loss to \u201C--\u201D in a comment.");
            } else {
                warn("This document is not mappable to XML 1.0 without data loss to \u201C--\u201D in a comment.");
                if (wantsComments) {
                    if (commentPolicy == XmlViolationPolicy.ALLOW) {
                        appendLongStrBuf('-');
                    } else {
                        appendLongStrBuf('-');
                        appendLongStrBuf(' ');
                    }
                }
                longStrBufPending = '-';
            }
        } else {
            if (longStrBufPending != '\u0000') {
                if (wantsComments) {
                    appendLongStrBuf(longStrBufPending);
                }
                longStrBufPending = '\u0000';
            }
            if (c == '-') {
                longStrBufPending = '-';
            } else {
                if (wantsComments) {
                    appendLongStrBuf(c);
                }
            }
        }
    }

    /**
     * Appends to the larger buffer.
     * 
     * @param arr
     *            the UTF-16 code units to append
     */
    private void appendLongStrBuf(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            appendLongStrBuf(arr[i]);
        }
    }

    /**
     * Append the contents of the smaller buffer to the larger one.
     */
    private void appendStrBufToLongStrBuf() {
        for (int i = 0; i < strBufLen; i++) {
            appendLongStrBuf(strBuf[i]);
        }
    }

    /**
     * The larger buffer as a string.
     * 
     * @return the larger buffer as a string
     */
    private String longStrBufToString() {
        if (longStrBufPending != '\u0000') {
            appendLongStrBuf(longStrBufPending);
        }
        return new String(longStrBuf, 0, longStrBufLen);
    }

    /**
     * Emits the current comment token.
     * 
     * @throws SAXException
     */
    private void emitComment() throws SAXException {
        if (wantsComments) {
            if (longStrBufPending != '\u0000') {
                appendLongStrBuf(longStrBufPending);
            }
        }
        tokenHandler.comment(longStrBuf, longStrBufLen);
    }

    /**
     * Unreads a code unit so that it is returned the next time
     * <code>read()</code> is called.
     * 
     * @param c
     *            the code unit to unread
     */
    private void unread(char c) {
        unreadBuffer = c;
    }

    /**
     * Reads the next UTF-16 code unit.
     * 
     * @return the next code unit
     * @throws SAXException
     * @throws IOException
     */
    private char read() throws SAXException, IOException {
        assert (bufLen > -1);
        for (;;) { // the loop is here for the CRLF case
            if (unreadBuffer != -1) {
                char c = (char) unreadBuffer;
                unreadBuffer = -1;
                return c;
            }
            pos++;
            assert pos <= bufLen;
            col++;
            if (pos == bufLen) {
                boolean charDataContinuation = false;
                if (cstart > -1) {
                    flushChars();
                    charDataContinuation = true;
                }
                bufLen = reader.read(buf);
                assert bufLen <= buf.length;
                if (bufLen == -1) {
                    return '\u0000';
                } else if (normalizationChecker != null) {
                    normalizationChecker.characters(buf, 0, bufLen);
                }
                if (charDataContinuation) {
                    cstart = 0;
                }
                pos = 0;
            }
            char c = buf[pos];
            if (c > '\u007F' && nonAsciiProhibited
                    && !alreadyComplainedAboutNonAscii) {
                err("The character encoding of the document was not explicit but the document contains non-ASCII.");
            }
            switch (c) {
                case '\n':
                    /*
                     * U+000D CARRIAGE RETURN (CR) characters, and U+000A LINE
                     * FEED (LF) characters, are treated specially. Any CR
                     * characters that are followed by LF characters must be
                     * removed, and any CR characters not followed by LF
                     * characters must be converted to LF characters.
                     */
                    if (prev == '\r') {
                        // swallow the LF
                        col = 0;
                        if (cstart != -1) {
                            flushChars();
                            cstart = pos + 1;
                        }
                        prev = c;
                        continue;
                    } else {
                        line++;
                        col = 0;
                    }
                    break;
                case '\r':
                    c = buf[pos] = '\n';
                    line++;
                    col = 0;
                    break;
                case '\u0000':
                    /*
                     * All U+0000 NULL characters in the input must be replaced
                     * by U+FFFD REPLACEMENT CHARACTERs. Any occurrences of such
                     * characters is a parse error.
                     */
                    err("Found U+0000 in the character stream.");
                    c = buf[pos] = '\uFFFD';
                    break;
                case '\u000B':
                case '\u000C':
                    if (inContent) {
                        if (contentNonXmlCharPolicy == XmlViolationPolicy.FATAL) {
                            fatal("This document is not mappable to XML 1.0 without data loss due to a character that is not a legal XML 1.0 character.");
                        } else {
                            if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                c = buf[pos] = ' ';
                            }
                            warn("This document is not mappable to XML 1.0 without data loss due to a character that is not a legal XML 1.0 character.");
                        }
                    }
                    break;
                default:
                    if ((c & 0xFC00) == 0xDC00) {
                        // Got a low surrogate. See if prev was high surrogate
                        if ((prev & 0xFC00) == 0xD800) {
                            int intVal = (prev << 10) + c + SURROGATE_OFFSET;
                            if (isNonCharacter(intVal)) {
                                warn("Astral non-character.");
                            }
                            if (isAstralPrivateUse(intVal)) {
                                warnAboutPrivateUseChar();
                            }
                        } else {
                            // XXX figure out what to do about lone high
                            // surrogates
                            err("Found low surrogate without high surrogate.");
                            c = buf[pos] = '\uFFFD';
                        }
                    } else if (inContent && (c < ' ' || isNonCharacter(c))) {
                        if (contentNonXmlCharPolicy == XmlViolationPolicy.FATAL) {
                            fatal("This document is not mappable to XML 1.0 without data loss due to a character that is not a legal XML 1.0 character.");
                        } else {
                            if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                                c = buf[pos] = '\uFFFD';
                            }
                            warn("This document is not mappable to XML 1.0 without data loss due to a character that is not a legal XML 1.0 character.");
                        }
                    } else if (isPrivateUse(c)) {
                        warnAboutPrivateUseChar();
                    }
            }
            prev = c;
            if (contentModelFlag != ContentModelFlag.PCDATA) {
                prevFourPtr++;
                prevFourPtr %= 4;
                prevFour[prevFourPtr] = c;
            }
            return c;
        }
    }

    /**
     * Emits a warning about private use characters if the warning has not been
     * emitted yet.
     * 
     * @throws SAXException
     */
    private void warnAboutPrivateUseChar() throws SAXException {
        if (!alreadyWarnedAboutPrivateUseCharacters) {
            warn("Document uses the Unicode Private Use Area(s), which should not be used in publicly exchanged documents. (Charmod C073)");
            alreadyWarnedAboutPrivateUseCharacters = true;
        }
    }

    /**
     * Tells if the argument is a BMP PUA character.
     * 
     * @param c
     *            the UTF-16 code unit to check
     * @return <code>true</code> if PUA character
     */
    private boolean isPrivateUse(char c) {
        return c >= '\uE000' && c <= '\uF8FF';
    }

    /**
     * Tells if the argument is an astral PUA character.
     * 
     * @param c
     *            the code point to check
     * @return <code>true</code> if astral private use
     */
    private boolean isAstralPrivateUse(int c) {
        return (c >= 0xF0000 && c <= 0xFFFFD)
                || (c >= 0x100000 && c <= 0x10FFFD);
    }

    /**
     * Tells if the argument is a non-character (works for BMP and astral).
     * 
     * @param c
     *            the code point to check
     * @return <code>true</code> if non-character
     */
    private boolean isNonCharacter(int c) {
        return (c & 0xFFFE) == 0xFFFE;
    }

    /**
     * Flushes coalesced character tokens.
     * 
     * @throws SAXException
     */
    private void flushChars() throws SAXException, IOException {
        if (cstart != -1) {
            if (pos > cstart) {
                tokenHandler.characters(buf, cstart, pos - cstart);
            }
        }
        cstart = -1;
    }

    /**
     * Reports an condition that would make the infoset incompatible with XML
     * 1.0 as fatal.
     * 
     * @param message
     *            the message
     * @throws SAXException
     * @throws SAXParseException
     */
    private void fatal(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        errorHandler.fatalError(spe);
        throw spe;
    }

    /**
     * Reports a Parse Error.
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    private void err(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        errorHandler.error(spe);
    }

    /**
     * Reports a warning
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    private void warn(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        errorHandler.warning(spe);
    }

    /**
     * Initializes a decoder from external decl.
     */
    private CharsetDecoder decoderFromExternalDeclaration(String encoding)
            throws SAXException {
        if (encoding == null) {
            return null;
        }
        encoding = encoding.toUpperCase();
        if ("ISO-8859-1".equals(encoding)) {
            encoding = "Windows-1252";
        }
        if ("UTF-16".equals(encoding) || "UTF-32".equals(encoding)) {
            swallowBom = false;
        }
        try {
            Charset cs = Charset.forName(encoding);
            String canonName = cs.name();
            if (canonName.startsWith("X-") || canonName.startsWith("x-")
                    || canonName.startsWith("Mac")) {
                if (encoding.startsWith("X-")) {
                    err("The encoding \u201C"
                            + encoding
                            + "\u201D is not an IANA-registered encoding. (Charmod C022)");
                } else {
                    err("The encoding \u201C"
                            + encoding
                            + "\u201D is not an IANA-registered encoding and did\u2019t start with \u201CX-\u201D. (Charmod C023)");
                }
            } else if (!canonName.equalsIgnoreCase(encoding)) {
                err("The encoding \u201C"
                        + encoding
                        + "\u201D is not the preferred name of the character encoding in use. The preferred name is \u201C"
                        + canonName + "\u201D. (Charmod C024)");
            }
            if (EncodingInfo.isObscure(canonName)) {
                warn("The character encoding \u201C"
                        + encoding
                        + "\u201D is not widely supported. Better interoperability may be achieved by using \u201CUTF-8\u201D.");
            }
            return cs.newDecoder();
        } catch (IllegalCharsetNameException e) {
            err("Illegal character encoding name: \u201C" + encoding
                    + "\u201D. Will sniff.");
        } catch (UnsupportedCharsetException e) {
            err("Unsupported character encoding name: \u201C" + encoding
                    + "\u201D. Will sniff.");
            swallowBom = true;
        }
        return null; // keep the compiler happy
    }

    private boolean currentIsVoid() {
        return Arrays.binarySearch(VOID_ELEMENTS, tagName) > -1;
    }

    /**
     * Data state
     * 
     * @throws IOException
     * @throws SAXException
     * 
     */
    private void dataState() throws SAXException, IOException {
        char c = '\u0000';
        for (;;) {
            c = read();
            if (c == '&'
                    && (contentModelFlag == ContentModelFlag.PCDATA || contentModelFlag == ContentModelFlag.RCDATA)) {
                /*
                 * U+0026 AMPERSAND (&) When the content model flag is set to
                 * one of the PCDATA or RCDATA states: switch to the entity data
                 * state. Otherwise: treat it as per the "anything else" entry
                 * below.
                 */
                flushChars();
                entityDataState();
                continue;
            } else if (c == '<'
                    && ((contentModelFlag == ContentModelFlag.PCDATA) || (escapeFlag == false && (contentModelFlag == ContentModelFlag.CDATA || contentModelFlag == ContentModelFlag.RCDATA)))) {
                /*
                 * U+003C LESS-THAN SIGN (<) When the content model flag is set
                 * to the PCDATA state: switch to the tag open state. When the
                 * content model flag is set to either the RCDATA state or the
                 * CDATA state and the escape flag is false: switch to the tag
                 * open state. Otherwise: treat it as per the "anything else"
                 * entry below.
                 */
                flushChars();
                resetAttributes();
                inContent = false;
                tagOpenState();
                inContent = true;
                continue;
            } else if (c == '\u0000') {
                /*
                 * EOF Emit an end-of-file token.
                 */
                flushChars();
                return; // eof() called in parent finally block
            } else {
                if (c == '-'
                        && (escapeFlag == false)
                        && (contentModelFlag == ContentModelFlag.RCDATA || contentModelFlag == ContentModelFlag.CDATA)
                        && lastLtExclHyph()) {
                    /*
                     * U+002D HYPHEN-MINUS (-) If the content model flag is set
                     * to either the RCDATA state or the CDATA state, and the
                     * escape flag is false, and there are at least three
                     * characters before this one in the input stream, and the
                     * last four characters in the input stream, including this
                     * one, are U+003C LESS-THAN SIGN, U+0021 EXCLAMATION MARK,
                     * U+002D HYPHEN-MINUS, and U+002D HYPHEN-MINUS ("<!--"),
                     * then set the escape flag to true.
                     * 
                     * In any case, emit the input character as a character
                     * token. Stay in the data state.
                     */
                    escapeFlag = true;
                } else if (c == '>' && escapeFlag && lastHyphHyph()) {
                    /*
                     * U+003E GREATER-THAN SIGN (>) If the content model flag is
                     * set to either the RCDATA state or the CDATA state, and
                     * the escape flag is true, and the last three characters in
                     * the input stream including this one are U+002D
                     * HYPHEN-MINUS, U+002D HYPHEN-MINUS, U+003E GREATER-THAN
                     * SIGN ("-->"), set the escape flag to false.
                     * 
                     * In any case, emit the input character as a character
                     * token. Stay in the data state.
                     */
                    escapeFlag = false;
                }
                /*
                 * Anything else Emit the input character as a character token.
                 */
                if (cstart == -1) {
                    // start coalescing character tokens
                    cstart = pos;
                }
                /*
                 * Stay in the data state.
                 */
                continue;
            }
        }
    }

    private boolean lastHyphHyph() {
        return prevFour[(prevFourPtr - 1 + 4) % 4] == '-'
                && prevFour[(prevFourPtr - 2 + 4) % 4] == '-';
    }

    private boolean lastLtExclHyph() {
        return prevFour[(prevFourPtr - 1 + 4) % 4] == '-'
                && prevFour[(prevFourPtr - 2 + 4) % 4] == '!'
                && prevFour[(prevFourPtr - 3 + 4) % 4] == '<';
    }

    /**
     * 
     * Entity data state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void entityDataState() throws SAXException, IOException {
        /*
         * (This cannot happen if the content model flag is set to the CDATA
         * state.)
         * 
         * Attempt to consume an entity.
         */
        consumeEntity(false);
        /*
         * If nothing is returned, emit a U+0026 AMPERSAND character token.
         * 
         * Otherwise, emit the character token that was returned.
         */
        // Handled by consumeEntity()
        /*
         * Finally, switch to the data state.
         */
        return;
    }

    /**
     * Tag open state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void tagOpenState() throws SAXException, IOException {
        /*
         * The behaviour of this state depends on the content model flag.
         */
        // this can't happen in PLAINTEXT, so using not PCDATA as the condition
        if (contentModelFlag != ContentModelFlag.PCDATA) {
            /*
             * If the content model flag is set to the RCDATA or CDATA states
             * Consume the next input character.
             */
            char c = read();
            if (c == '/') {
                /*
                 * If it is a U+002F SOLIDUS (/) character, switch to the close
                 * tag open state.
                 */
                closeTagOpenState();
                return;
            } else {
                /*
                 * Otherwise, emit a U+003C LESS-THAN SIGN character token
                 */
                tokenHandler.characters(LT_GT, 0, 1);
                /*
                 * and reconsume the current input character in the data state.
                 */
                unread(c);
                return;
            }
        } else {
            /*
             * If the content model flag is set to the PCDATA state Consume the
             * next input character:
             */
            char c = read();
            if (c == '!') {
                /*
                 * U+0021 EXCLAMATION MARK (!) Switch to the markup declaration
                 * open state.
                 */
                markupDeclarationOpenState();
                return;
            } else if (c == '/') {
                /* U+002F SOLIDUS (/) Switch to the close tag open state. */
                closeTagOpenState();
                return;
            } else if (c >= 'A' && c <= 'Z') {
                /*
                 * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN CAPITAL
                 * LETTER Z Create a new start tag token,
                 */
                endTag = false;
                /*
                 * set its tag name to the lowercase version of the input
                 * character (add 0x0020 to the character's code point),
                 */
                clearStrBuf();
                appendStrBuf((char) (c + 0x20));
                /* then switch to the tag name state. */
                tagNameState();
                /*
                 * (Don't emit the token yet; further details will be filled in
                 * before it is emitted.)
                 */
                return;
            } else if (c >= 'a' && c <= 'z') {
                /*
                 * U+0061 LATIN SMALL LETTER A through to U+007A LATIN SMALL
                 * LETTER Z Create a new start tag token,
                 */
                endTag = false;
                /*
                 * set its tag name to the input character,
                 */
                clearStrBuf();
                appendStrBuf(c);
                /* then switch to the tag name state. */
                tagNameState();
                /*
                 * (Don't emit the token yet; further details will be filled in
                 * before it is emitted.)
                 */
                return;
            } else if (c == '>') {
                /*
                 * U+003E GREATER-THAN SIGN (>) Parse error.
                 */
                err("Bad character \u201C>\u201D in the tag open state.");
                /*
                 * Emit a U+003C LESS-THAN SIGN character token and a U+003E
                 * GREATER-THAN SIGN character token.
                 */
                tokenHandler.characters(LT_GT, 0, 2);
                /* Switch to the data state. */
                return;
            } else if (c == '?') {
                /*
                 * U+003F QUESTION MARK (?) Parse error.
                 */
                err("Bad character \u201C?\u201D in the tag open state.");
                /*
                 * Switch to the bogus comment state.
                 */
                clearLongStrBuf();
                appendLongStrBuf(c);
                bogusCommentState();
                return;
            } else {
                /*
                 * Anything else Parse error.
                 */
                err("Bad character \u201C" + c
                        + "\u201D in the tag open state.");
                /*
                 * Emit a U+003C LESS-THAN SIGN character token
                 */
                tokenHandler.characters(LT_GT, 0, 1);
                /*
                 * and reconsume the current input character in the data state.
                 */
                unread(c);
                return;
            }
        }
    }

    /**
     * Close tag open state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void closeTagOpenState() throws SAXException, IOException {
        // this can't happen in PLAINTEXT, so using not PCDATA as the condition
        if (contentModelFlag != ContentModelFlag.PCDATA
                && contentModelElement != null) {
            /*
             * If the content model flag is set to the RCDATA or CDATA states
             * but no start tag token has ever been emitted by this instance of
             * the tokeniser (fragment case), or, if the content model flag is
             * set to the RCDATA or CDATA states and the next few characters do
             * not match the tag name of the last start tag token emitted (case
             * insensitively), or if they do but they are not immediately
             * followed by one of the following characters: + U+0009 CHARACTER
             * TABULATION + U+000A LINE FEED (LF) + U+000B LINE TABULATION +
             * U+000C FORM FEED (FF) + U+0020 SPACE + U+003E GREATER-THAN SIGN
             * (>) + U+002F SOLIDUS (/) + EOF
             * 
             * ...then emit a U+003C LESS-THAN SIGN character token, a U+002F
             * SOLIDUS character token, and switch to the data state to process
             * the next input character.
             */
            // Let's implement the above without lookahead. strBuf holds
            // characters that need to be emitted if looking for an end tag
            // fails.
            // Duplicating the relevant part of tag name state here as well.
            clearStrBuf();
            for (int i = 0; i < contentModelElement.length(); i++) {
                char e = contentModelElement.charAt(i);
                char c = read();
                char folded = c;
                if (c >= 'A' && c <= 'Z') {
                    folded += 0x20;
                }
                if (folded != e) {
                    if (html4) {
                        err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                : "RCDATA")
                                + " element \u201C"
                                + contentModelElement
                                + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                    } else {
                        warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                : "RCDATA")
                                + " element \u201C"
                                + contentModelElement
                                + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                    }
                    tokenHandler.characters(LT_SOLIDUS, 0, 2);
                    emitStrBuf();
                    unread(c);
                    return;
                }
                appendStrBuf(c);
            }
            endTag = true;
            tagName = contentModelElement;
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch
                     * to the before attribute name state.
                     */
                    beforeAttributeNameState();
                    return;
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Emit the current tag token. */
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /*
                     * EOF Parse error.
                     */
                    err("Expected \u201C>\u201D but saw end of file instead.");
                    /*
                     * Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /* Reconsume the character in the data state. */
                    unread(c);
                    return;
                case '/':
                    /*
                     * U+002F SOLIDUS (/) Parse error unless this is a permitted
                     * slash.
                     */
                    // never permitted here
                    err("Stray \u201C/\u201D in end tag.");
                    /* Switch to the before attribute name state. */
                    beforeAttributeNameState();
                    return;
                default:
                    if (html4) {
                        err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                : "RCDATA")
                                + " element \u201C"
                                + contentModelElement
                                + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                    } else {
                        warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                : "RCDATA")
                                + " element \u201C"
                                + contentModelElement
                                + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                    }
                    tokenHandler.characters(LT_SOLIDUS, 0, 2);
                    emitStrBuf();
                    return;
            }
        } else {
            /*
             * Otherwise, if the content model flag is set to the PCDATA state,
             * or if the next few characters do match that tag name, consume the
             * next input character:
             */
            char c = read();
            if (c >= 'A' && c <= 'Z') {
                /*
                 * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN CAPITAL
                 * LETTER Z Create a new end tag token,
                 */
                endTag = true;
                clearStrBuf();
                /*
                 * set its tag name to the lowercase version of the input
                 * character (add 0x0020 to the character's code point),
                 */
                appendStrBuf((char) (c + 0x20));
                /*
                 * then switch to the tag name state. (Don't emit the token yet;
                 * further details will be filled in before it is emitted.)
                 */
                tagNameState();
                return;
            } else if (c >= 'a' && c <= 'z') {
                /*
                 * U+0061 LATIN SMALL LETTER A through to U+007A LATIN SMALL
                 * LETTER Z Create a new end tag token,
                 */
                endTag = true;
                clearStrBuf();
                /*
                 * set its tag name to the input character,
                 */
                appendStrBuf(c);
                /*
                 * then switch to the tag name state. (Don't emit the token yet;
                 * further details will be filled in before it is emitted.)
                 */
                tagNameState();
                return;
            } else if (c == '>') {
                /* U+003E GREATER-THAN SIGN (>) Parse error. */
                err("Saw \u201C</>\u201D.");
                /*
                 * Switch to the data state.
                 */
                return;
            } else if (c == '\u0000') {
                /* EOF Parse error. */
                err("Saw \u201C</\u201D immediately before end of file.");
                /*
                 * Emit a U+003C LESS-THAN SIGN character token and a U+002F
                 * SOLIDUS character token.
                 */
                tokenHandler.characters(LT_SOLIDUS, 0, 2);
                /*
                 * Reconsume the EOF character in the data state.
                 */
                unread(c);
                return;
            } else {
                /* Anything else Parse error. */
                err("Garbage after \u201C</\u201D.");
                /*
                 * Switch to the bogus comment state.
                 */
                clearLongStrBuf();
                appendToComment(c);
                bogusCommentState();
                return;
            }
        }
    }

    /**
     * Tag name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void tagNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch
                     * to the before attribute name state.
                     */
                    tagName = strBufToElementNameString();
                    beforeAttributeNameState();
                    return;
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Emit the current tag token. */
                    tagName = strBufToElementNameString();
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /*
                     * EOF Parse error.
                     */
                    err("End of file seen when looking for tag name");
                    /*
                     * Emit the current tag token.
                     */
                    tagName = strBufToElementNameString();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                case '/':
                    /*
                     * U+002F SOLIDUS (/) Parse error unless this is a permitted
                     * slash.
                     */
                    tagName = strBufToElementNameString();
                    parseErrorUnlessPermittedSlash();
                    /*
                     * Switch to the before attribute name state.
                     */
                    beforeAttributeNameState();
                    return;
                default:
                    if (c >= 'A' && c <= 'Z') {
                        /*
                         * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN
                         * CAPITAL LETTER Z Append the lowercase version of the
                         * current input character (add 0x0020 to the
                         * character's code point) to the current tag token's
                         * tag name.
                         */
                        appendStrBuf((char) (c + 0x20));
                    } else {
                        /*
                         * Anything else Append the current input character to
                         * the current tag token's tag name.
                         */
                        appendStrBuf(c);
                    }
                    /*
                     * Stay in the tag name state.
                     */
                    continue;
            }
        }
    }

    private String strBufToElementNameString() {
        // TODO Generate a better interning function
        return strBufToString().intern();
    }

    /**
     * This method implements a wrapper loop for the attribute-related states to
     * avoid recursion to an arbitrary depth.
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void beforeAttributeNameState() throws SAXException, IOException {
        while (beforeAttributeNameStateImpl()) {
            // Spin.
        }
    }

    /**
     * 
     */
    private void resetAttributes() {
        attributes = null; // XXX figure out reuse
    }

    /**
     * Before attribute name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean beforeAttributeNameStateImpl() throws SAXException,
            IOException {
        /*
         * Consume the next input character:
         */
        for (;;) {
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the before attribute name state.
                     */
                    continue;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return false;
                case '/':
                    /*
                     * U+002F SOLIDUS (/) Parse error unless this is a permitted
                     * slash.
                     */
                    parseErrorUnlessPermittedSlash();
                    /*
                     * Stay in the before attribute name state.
                     */
                    continue;
                case '\u0000':
                    /* EOF Parse error. */
                    err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return false;
                default:
                    /*
                     * Anything else Start a new attribute in the current tag
                     * token.
                     */
                    clearStrBuf();

                    if (c >= 'A' && c <= 'Z') {
                        /*
                         * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN
                         * CAPITAL LETTER Z Set that attribute's name to the
                         * lowercase version of the current input character (add
                         * 0x0020 to the character's code point)
                         */
                        appendStrBuf((char) (c + 0x20));
                    } else {
                        /*
                         * Set that attribute's name to the current input
                         * character,
                         */
                        appendStrBuf(c);
                    }
                    /*
                     * and its value to the empty string.
                     */
                    // Will do later.
                    /*
                     * Switch to the attribute name state.
                     */
                    return attributeNameState();
            }
        }
    }

    private void parseErrorUnlessPermittedSlash() throws SAXException,
            IOException {
        /*
         * A permitted slash is a U+002F SOLIDUS character that is immediately
         * followed by a U+003E GREATER-THAN SIGN, if, and only if, the current
         * token being processed is a start tag token whose tag name is one of
         * the following: base, link, meta, hr, br, img, embed, param, area,
         * col, input
         */
        char c = read();
        if (c == '>') {
            if (!currentIsVoid() && !html4) {
                if (html4) {
                    err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is not permitted in HTML4.");
                } else {
                    err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is only permitted on void elements.");
                }
            } else if (html4) {
                err("Stray \u201C/\u201D in tag. The \u201C/>\u201D syntax is not permitted in HTML4. (HTML4-only error)");
            }
        } else {
            err("Stray \u201C/\u201D in tag.");
        }
        unread(c);
    }

    private void emitCurrentTagToken() throws SAXException {
        Attributes attrs = (attributes == null ? EmptyAttributes.EMPTY_ATTRIBUTES
                : attributes);
        if (endTag) {
            /*
             * When an end tag token is emitted, the content model flag must be
             * switched to the PCDATA state.
             */
            escapeFlag = false;
            contentModelFlag = ContentModelFlag.PCDATA;
            if (attrs.getLength() != 0) {
                /*
                 * When an end tag token is emitted with attributes, that is a
                 * parse error.
                 */
                err("End tag had attributes.");
            }
            tokenHandler.endTag(tagName, attrs);
        } else {
            tokenHandler.startTag(tagName, attrs);
        }
    }

    /**
     * Attribute name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean attributeNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch
                     * to the after attribute name state.
                     */
                    attributeNameComplete();
                    return afterAttributeNameState();
                case '=':
                    /*
                     * U+003D EQUALS SIGN (=) Switch to the before attribute
                     * value state.
                     */
                    attributeNameComplete();
                    return beforeAttributeValueState();
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Emit the current tag token. */
                    attributeNameComplete();
                    addAttributeWithoutValue();
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return false;
                case '/':
                    /*
                     * U+002F SOLIDUS (/) Parse error unless this is a permitted
                     * slash.
                     */
                    parseErrorUnlessPermittedSlash();
                    /* Switch to the before attribute name state. */
                    attributeNameComplete();
                    return true;
                case '\u0000':
                    /*
                     * EOF Parse error.
                     */
                    err("End of file occurred in an attribute name.");
                    /*
                     * Emit the current tag token.
                     */
                    attributeNameComplete();
                    addAttributeWithoutValue();
                    emitCurrentTagToken();
                    /* Reconsume the EOF character in the data state. */
                    unread(c);
                    return false;
                default:
                    if (c >= 'A' && c <= 'Z') {
                        /*
                         * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN
                         * CAPITAL LETTER Z Append the lowercase version of the
                         * current input character (add 0x0020 to the
                         * character's code point) to the current attribute's
                         * name.
                         */
                        appendStrBuf((char) (c + 0x20));
                    } else {
                        /*
                         * Anything else Append the current input character to
                         * the current attribute's name.
                         */
                        appendStrBuf(c);
                    }
            }
            /*
             * Stay in the attribute name state.
             */
            continue;
        }
    }

    private void attributeNameComplete() throws SAXException {
        attributeName = strBufToString();
        if (attributes == null) {
            attributes = new AttributesImpl();
        }
        /*
         * When the user agent leaves the attribute name state (and before
         * emitting the tag token, if appropriate), the complete attribute's
         * name must be compared to the other attributes on the same token; if
         * there is already an attribute on the token with the exact same name,
         * then this is a parse error and the new attribute must be dropped,
         * along with the value that gets associated with it (if any).
         */
        if (attributes.getIndex(attributeName) == -1) {
            shouldAddAttributes = true;
        } else {
            shouldAddAttributes = false;
            err("Duplicate attribute \u201C" + attributeName + "\u201D.");
        }
    }

    private void addAttributeWithoutValue() throws SAXException {
        if (metaBoundaryPassed && "charset".equals(attributeName)
                && "meta".equals(tagName)) {
            err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
        }
        if (shouldAddAttributes) {
            attributes.addAttribute(attributeName);
        }
    }

    private void addAttributeWithValue() throws SAXException {
        if (metaBoundaryPassed && "charset".equals(attributeName)
                && "meta".equals(tagName)) {
            err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
        }
        if (shouldAddAttributes) {
            attributes.addAttribute(attributeName, longStrBufToString());
        }
    }

    /**
     * After attribute name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean afterAttributeNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the after attribute name state.
                     */
                    continue;
                case '=':
                    /*
                     * U+003D EQUALS SIGN (=) Switch to the before attribute
                     * value state.
                     */
                    return beforeAttributeValueState();
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return false;
                case '/':
                    /*
                     * U+002F SOLIDUS (/) Parse error unless this is a permitted
                     * slash.
                     */
                    parseErrorUnlessPermittedSlash();
                    /* Switch to the before attribute name state. */
                    return true;
                case '<':
                    /* U+003C LESS-THAN SIGN (<) Parse error. */
                    err("Saw \u201C<\u201C without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    return false;
                case '\u0000':
                    /* EOF Parse error. */
                    err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    return false;
                default:
                    /*
                     * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN
                     * CAPITAL LETTER Z Start a new attribute in the current tag
                     * token. Set that attribute's name to the lowercase version
                     * of the current input character (add 0x0020 to the
                     * character's code point), and its value to the empty
                     * string. Switch to the attribute name state.
                     * 
                     * Anything else Start a new attribute in the current tag
                     * token. Set that attribute's name to the current input
                     * character, and its value to the empty string. Switch to
                     * the attribute name state.
                     */
                    // let's do this by respinning through the attribute loop
                    unread(c);
                    return true;
            }
        }
    }

    /**
     * Before attribute value state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean beforeAttributeValueState() throws SAXException,
            IOException {
        clearLongStrBuf();
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the before attribute value state.
                     */
                    continue;
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Switch to the attribute value
                     * (double-quoted) state.
                     */
                    return attributeValueDoubleQuotedState();
                case '&':
                    /*
                     * U+0026 AMPERSAND (&) Switch to the attribute value
                     * (unquoted) state and reconsume this input character.
                     */
                    unread(c);
                    return attributeValueUnquotedState();
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Switch to the attribute value
                     * (single-quoted) state.
                     */
                    return attributeValueSingleQuotedState();
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Emit the current tag token. */
                    addAttributeWithoutValue();
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    return false;
                case '<':
                    /* U+003C LESS-THAN SIGN (<) Parse error. */
                    err("Saw \u201C<\u201C without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    addAttributeWithoutValue();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    return false;
                case '\u0000':
                    /* EOF Parse error. */
                    err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    addAttributeWithoutValue();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    return false;
                default:
                    if (html4
                            && !((c >= 'a' && c <= 'z')
                                    || (c >= 'A' && c <= 'Z')
                                    || (c >= '0' && c <= '9') || c == '.'
                                    || c == '-' || c == '_' || c == ':')) {
                        err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                    }
                    /*
                     * Anything else Append the current input character to the
                     * current attribute's value.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Switch to the attribute value (unquoted) state.
                     */
                    return attributeValueUnquotedState();
            }
        }
    }

    /**
     * Attribute value (double-quoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean attributeValueDoubleQuotedState() throws SAXException,
            IOException {
        inContent = true;
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Switch to the before attribute
                     * name state.
                     */
                    addAttributeWithValue();
                    inContent = false;
                    return true;
                case '&':
                    /*
                     * U+0026 AMPERSAND (&) Switch to the entity in attribute
                     * value state.
                     */
                    entityInAttributeValueState();
                    continue;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file reached when inside a quoted attribute value.");
                    /* Emit the current tag token. */
                    addAttributeWithValue();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    inContent = false;
                    return false;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current attribute's value.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the attribute value (double-quoted) state.
                     */
                    continue;
            }
        }
    }

    /**
     * Attribute value (single-quoted) state
     * 
     * @throws SAXException
     * @throws IOException
     */
    private boolean attributeValueSingleQuotedState() throws SAXException,
            IOException {
        inContent = true;
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Switch to the before attribute name
                     * state.
                     */
                    addAttributeWithValue();
                    inContent = false;
                    return true;
                case '&':
                    /*
                     * U+0026 AMPERSAND (&) Switch to the entity in attribute
                     * value state.
                     */
                    entityInAttributeValueState();
                    continue;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file reached when inside a quoted attribute value.");
                    /* Emit the current tag token. */
                    addAttributeWithValue();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    inContent = false;
                    return false;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current attribute's value.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the attribute value (double-quoted) state.
                     */
                    continue;
            }
        }
    }

    /**
     * Attribute value (unquoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private boolean attributeValueUnquotedState() throws SAXException,
            IOException {
        inContent = true;
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch
                     * to the before attribute name state.
                     */
                    addAttributeWithValue();
                    inContent = false;
                    return true;
                case '&':
                    /*
                     * U+0026 AMPERSAND (&) Switch to the entity in attribute
                     * value state.
                     */
                    entityInAttributeValueState();
                    continue;
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Emit the current tag token. */
                    addAttributeWithValue();
                    emitCurrentTagToken();
                    /*
                     * Switch to the data state.
                     */
                    inContent = false;
                    return false;
                case '\u0000':
                    /* EOF Parse error. */
                    err("Saw end of file without the previous tag ending with \u201C>\u201C.");
                    /*
                     * Emit the current tag token.
                     */
                    addAttributeWithValue();
                    emitCurrentTagToken();
                    /*
                     * Reconsume the character in the data state.
                     */
                    unread(c);
                    inContent = false;
                    return false;
                case '<':
                    warn("\u201C<\u201D in an unquoted attribute value. This does not end the tag.");
                    // fall through
                default:
                    if (html4
                            && !((c >= 'a' && c <= 'z')
                                    || (c >= 'A' && c <= 'Z')
                                    || (c >= '0' && c <= '9') || c == '.'
                                    || c == '-' || c == '_' || c == ':')) {
                        err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                    }
                    /*
                     * Anything else Append the current input character to the
                     * current attribute's value.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the attribute value (unquoted) state.
                     */
                    continue;
            }
        }
    }

    /**
     * Entity in attribute value state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void entityInAttributeValueState() throws SAXException, IOException {
        /*
         * Attempt to consume an entity.
         */
        consumeEntity(true);
        /*
         * If nothing is returned, append a U+0026 AMPERSAND character to the
         * current attribute's value.
         * 
         * Otherwise, append the returned character token to the current
         * attribute's value.
         */
        // handled in consumeEntity();
        /*
         * Finally, switch back to the attribute value state that you were in
         * when were switched into this state.
         */
        return;
    }

    /**
     * Bogus comment state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void bogusCommentState() throws SAXException, IOException {
        /*
         * (This can only happen if the content model flag is set to the PCDATA
         * state.)
         * 
         * Consume every character up to the first U+003E GREATER-THAN SIGN
         * character (>) or the end of the file (EOF), whichever comes first.
         * Emit a comment token whose data is the concatenation of all the
         * characters starting from and including the character that caused the
         * state machine to switch into the bogus comment state, up to and
         * including the last consumed character before the U+003E character, if
         * any, or up to the end of the file otherwise. (If the comment was
         * started by the end of the file (EOF), the token is empty.)
         * 
         * Switch to the data state.
         * 
         * If the end of the file was reached, reconsume the EOF character.
         */
        for (;;) {
            char c = read();
            switch (c) {
                case '>':
                    emitComment();
                    return;
                case '\u0000':
                    emitComment();
                    unread(c);
                    return;
                default:
                    appendToComment(c);
            }
        }
    }

    /**
     * Markup declaration open state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void markupDeclarationOpenState() throws SAXException, IOException {
        /*
         * (This can only happen if the content model flag is set to the PCDATA
         * state.)
         */
        clearLongStrBuf();
        /*
         * If the next two characters are both U+002D HYPHEN-MINUS (-)
         * characters, consume those two characters, create a comment token
         * whose data is the empty string, and switch to the comment start
         * state.
         * 
         * Otherwise if the next seven characters are a case-insensitive match
         * for the word "DOCTYPE", then consume those characters and switch to
         * the DOCTYPE state.
         * 
         * Otherwise, is is a parse error. Switch to the bogus comment state.
         * The next character that is consumed, if any, is the first character
         * that will be in the comment.
         */
        char c = read();
        switch (c) {
            case '-':
                c = read();
                if (c == '-') {
                    commentStates();
                    return;
                } else {
                    err("Bogus comment.");
                    appendToComment('-');
                    unread(c);
                    bogusCommentState();
                    return;
                }
            case 'd':
            case 'D':
                appendToComment(c);
                for (int i = 0; i < OCTYPE.length; i++) {
                    c = read();
                    char folded = c;
                    if (c >= 'A' && c <= 'Z') {
                        folded += 0x20;
                    }
                    if (folded == OCTYPE[i]) {
                        appendToComment(c);
                    } else {
                        err("Bogus comment.");
                        unread(c);
                        bogusCommentState();
                        return;
                    }
                }
                doctypeState();
                return;
            default:
                err("Bogus comment.");
                unread(c);
                bogusCommentState();
                return;
        }
    }

    private enum CommentState {
        COMMENT_START_STATE, COMMENT_START_DASH_STATE, COMMENT_STATE, COMMENT_END_DASH_STATE, COMMENT_END_STATE
    }

    /**
     * Comment start state, Comment start dash state, Comment state, Comment end
     * dash state and Comment end state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void commentStates() throws SAXException, IOException {
        CommentState state = CommentState.COMMENT_START_STATE;
        for (;;) {
            char c = read();
            switch (state) {
                case COMMENT_START_STATE:
                    /*
                     * Comment start state
                     * 
                     * 
                     * Consume the next input character:
                     */
                    switch (c) {
                        case '-':
                            /*
                             * U+002D HYPHEN-MINUS (-) Switch to the comment
                             * start dash state.
                             */
                            state = CommentState.COMMENT_START_DASH_STATE;
                            continue;
                        case '>':
                            /*
                             * U+003E GREATER-THAN SIGN (>) Parse error.
                             */
                            err("Premature end of comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Switch to the data state.
                             */
                            return;
                        case '\u0000':
                            /*
                             * EOF Parse error.
                             */
                            err("End of file inside comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            unread(c);
                            return;
                        default:
                            /*
                             * Anything else Append the input character to the
                             * comment token's data.
                             */
                            appendToComment(c);
                            /*
                             * Switch to the comment state.
                             */
                            state = CommentState.COMMENT_STATE;
                            continue;
                    }
                case COMMENT_START_DASH_STATE:
                    /*
                     * Comment start dash state
                     * 
                     * Consume the next input character:
                     */
                    switch (c) {
                        case '-':
                            /*
                             * U+002D HYPHEN-MINUS (-) Switch to the comment end
                             * state
                             */
                            state = CommentState.COMMENT_END_STATE;
                            continue;
                        case '>':
                            /*
                             * U+003E GREATER-THAN SIGN (>) Parse error.
                             */
                            err("Premature end of comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Switch to the data state.
                             */
                            return;
                        case '\u0000':
                            /*
                             * EOF Parse error.
                             */
                            err("End of file inside comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            unread(c);
                            return;
                        default:
                            /*
                             * Anything else Append a U+002D HYPHEN-MINUS (-)
                             * character and the input character to the comment
                             * token's data.
                             */
                            appendToComment('-');
                            appendToComment(c);
                            /*
                             * Switch to the comment state.
                             */
                            state = CommentState.COMMENT_STATE;
                            continue;
                    }
                case COMMENT_STATE:
                    /*
                     * Comment state Consume the next input character:
                     */
                    switch (c) {
                        case '-':
                            /*
                             * U+002D HYPHEN-MINUS (-) Switch to the comment end
                             * dash state
                             */
                            state = CommentState.COMMENT_END_DASH_STATE;
                            continue;
                        case '\u0000':
                            /*
                             * EOF Parse error.
                             */
                            err("End of file inside comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            unread(c);
                            return;
                        default:
                            /*
                             * Anything else Append the input character to the
                             * comment token's data.
                             */
                            appendToComment(c);
                            /*
                             * Stay in the comment state.
                             */
                            continue;
                    }
                case COMMENT_END_DASH_STATE:
                    /*
                     * Comment end dash state Consume the next input character:
                     */
                    switch (c) {
                        case '-':
                            /*
                             * U+002D HYPHEN-MINUS (-) Switch to the comment end
                             * state
                             */
                            state = CommentState.COMMENT_END_STATE;
                            continue;
                        case '\u0000':
                            /*
                             * EOF Parse error.
                             */
                            err("End of file inside comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            unread(c);
                            return;
                        default:
                            /*
                             * Anything else Append a U+002D HYPHEN-MINUS (-)
                             * character and the input character to the comment
                             * token's data.
                             */
                            appendToComment('-');
                            appendToComment(c);
                            /*
                             * Switch to the comment state.
                             */
                            state = CommentState.COMMENT_STATE;
                            continue;
                    }
                case COMMENT_END_STATE:
                    /*
                     * Comment end dash state Consume the next input character:
                     */
                    switch (c) {
                        case '>':
                            /*
                             * U+003E GREATER-THAN SIGN (>) Emit the comment
                             * token.
                             */
                            emitComment();
                            /*
                             * Switch to the data state.
                             */
                            return;
                        case '-':
                            /* U+002D HYPHEN-MINUS (-) Parse error. */
                            err("Consecutive hyphens did not terminate a comment.");
                            /*
                             * Append a U+002D HYPHEN-MINUS (-) character to the
                             * comment token's data.
                             */
                            appendToComment('-');
                            /*
                             * Stay in the comment end state.
                             */
                            continue;
                        case '\u0000':
                            /*
                             * EOF Parse error.
                             */
                            err("End of file inside comment.");
                            /* Emit the comment token. */
                            emitComment();
                            /*
                             * Reconsume the EOF character in the data state.
                             */
                            unread(c);
                            return;
                        default:
                            /*
                             * Anything else Parse error.
                             */
                            err("Consecutive hyphens did not terminate a comment.");
                            /*
                             * Append two U+002D HYPHEN-MINUS (-) characters and
                             * the input character to the comment token's data.
                             */
                            appendToComment('-');
                            appendToComment('-');
                            appendToComment(c);
                            /*
                             * Switch to the comment state.
                             */
                            state = CommentState.COMMENT_STATE;
                            continue;
                    }
            }
        }
    }

    /**
     * DOCTYPE state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypeState() throws SAXException, IOException {
        systemIdentifier = null;
        publicIdentifier = null;
        doctypeName = null;
        /*
         * Consume the next input character:
         */
        char c = read();
        switch (c) {
            case ' ':
            case '\t':
            case '\n':
            case '\u000B':
            case '\u000C':
                /*
                 * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B LINE
                 * TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch to the
                 * before DOCTYPE name state.
                 */
                beforeDoctypeNameState();
                return;
            default:
                /*
                 * Anything else Parse error.
                 */
                err("Missing space before doctype name.");
                /*
                 * Reconsume the current character in the before DOCTYPE name
                 * state.
                 */
                unread(c);
                beforeDoctypeNameState();
                return;
        }
    }

    /**
     * Before DOCTYPE name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void beforeDoctypeNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the before DOCTYPE name state.
                     */
                    continue;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Parse error.
                     */
                    err("Nameless doctype.");
                    /*
                     * Create a new DOCTYPE token. Set its correctness flag to
                     * incorrect. Emit the token.
                     */
                    tokenHandler.doctype("", null, null, false);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Create a new DOCTYPE token. Set its correctness flag to
                     * incorrect. Emit the token.
                     */
                    tokenHandler.doctype("", null, null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /* Anything else Create a new DOCTYPE token. */
                    clearStrBuf();
                    /*
                     * Set the token's name name to the current input character.
                     */
                    appendStrBuf(c);
                    /*
                     * Switch to the DOCTYPE name state.
                     */
                    doctypeNameState();
                    return;
            }
        }
    }

    /**
     * DOCTYPE name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypeNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * First, consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Switch
                     * to the after DOCTYPE name state.
                     */
                    doctypeName = strBufToString();
                    afterDoctypeNameState();
                    return;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current DOCTYPE
                     * token.
                     */
                    tokenHandler.doctype(strBufToString(), null, null, true);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(strBufToString(), null, null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current DOCTYPE token's name.
                     */
                    appendStrBuf(c);
                    /*
                     * Stay in the DOCTYPE name state.
                     */
                    continue;
            }
        }
    }

    /**
     * After DOCTYPE name state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void afterDoctypeNameState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the after DOCTYPE name state.
                     */
                    continue;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current DOCTYPE
                     * token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, true);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                case 'p':
                case 'P':
                    /*
                     * If the next six characters are a case-insensitive match
                     * for the word "PUBLIC", then consume those characters and
                     * switch to the before DOCTYPE public identifier state.
                     */
                    for (int i = 0; i < UBLIC.length; i++) {
                        c = read();
                        char folded = c;
                        if (c >= 'A' && c <= 'Z') {
                            folded += 0x20;
                        }
                        if (folded != UBLIC[i]) {
                            err("Bogus doctype.");
                            unread(c);
                            bogusDoctypeState();
                            return;
                        }
                    }
                    beforeDoctypePublicIdentifierState();
                    return;
                case 's':
                case 'S':
                    /*
                     * Otherwise, if the next six characters are a
                     * case-insensitive match for the word "SYSTEM", then
                     * consume those characters and switch to the before DOCTYPE
                     * system identifier state.
                     */
                    for (int i = 0; i < YSTEM.length; i++) {
                        c = read();
                        char folded = c;
                        if (c >= 'A' && c <= 'Z') {
                            folded += 0x20;
                        }
                        if (folded != YSTEM[i]) {
                            err("Bogus doctype.");
                            unread(c);
                            bogusDoctypeState();
                            return;
                        }
                    }
                    beforeDoctypeSystemIdentifierState();
                    return;
                default:
                    /*
                     * Otherwise, this is the parse error. Switch to the bogus
                     * DOCTYPE state.
                     */
                    bogusDoctypeState();
                    return;
            }
        }
    }

    /**
     * Before DOCTYPE public identifier state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void beforeDoctypePublicIdentifierState() throws SAXException,
            IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the before DOCTYPE public identifier state.
                     */
                    continue;
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Set the DOCTYPE token's public
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE public identifier
                     * (double-quoted) state.
                     */
                    doctypePublicIdentifierDoubleQuotedState();
                    return;
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Set the DOCTYPE token's public
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE public identifier
                     * (single-quoted) state.
                     */
                    doctypePublicIdentifierSingleQuotedState();
                    return;
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Parse error. */
                    err("Expected a public identifier but the doctype ended.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, false);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside a doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /* Anything else Parse error. */
                    err("Bogus doctype.");
                    /*
                     * Switch to the bogus DOCTYPE state.
                     */
                    bogusDoctypeState();
                    return;
            }
        }
    }

    /**
     * DOCTYPE public identifier (double-quoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypePublicIdentifierDoubleQuotedState()
            throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Switch to the after DOCTYPE
                     * public identifier state.
                     */
                    publicIdentifier = longStrBufToString();
                    afterDoctypePublicIdentifierState();
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside public identifier.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, longStrBufToString(),
                            null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current DOCTYPE token's public identifier.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the DOCTYPE public identifier (double-quoted)
                     * state.
                     */
                    continue;
            }
        }
    }

    /**
     * DOCTYPE public identifier (single-quoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypePublicIdentifierSingleQuotedState()
            throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Switch to the after DOCTYPE public
                     * identifier state.
                     */
                    publicIdentifier = longStrBufToString();
                    afterDoctypePublicIdentifierState();
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside public identifier.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, longStrBufToString(),
                            null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current DOCTYPE token's public identifier.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the DOCTYPE public identifier (single-quoted)
                     * state.
                     */
                    continue;
            }
        }
    }

    /**
     * After DOCTYPE public identifier state
     * 
     * @throws IOException
     * @throws SAXException
     * 
     */
    private void afterDoctypePublicIdentifierState() throws SAXException,
            IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the after DOCTYPE public identifier state.
                     */
                    continue;
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Set the DOCTYPE token's system
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE system identifier
                     * (double-quoted) state.
                     */
                    doctypeSystemIdentifierDoubleQuotedState();
                    return;
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Set the DOCTYPE token's system
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE system identifier
                     * (single-quoted) state.
                     */
                    doctypeSystemIdentifierSingleQuotedState();
                    return;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current DOCTYPE
                     * token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier, null,
                            true);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier, null,
                            false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /* Anything else Parse error. */
                    err("Bogus doctype.");
                    /*
                     * Switch to the bogus DOCTYPE state.
                     */
                    bogusDoctypeState();
                    return;
            }
        }
    }

    /**
     * Before DOCTYPE system identifier state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void beforeDoctypeSystemIdentifierState() throws SAXException,
            IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the before DOCTYPE system identifier state.
                     */
                    continue;
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Set the DOCTYPE token's system
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE system identifier
                     * (double-quoted) state.
                     */
                    doctypeSystemIdentifierDoubleQuotedState();
                    return;
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Set the DOCTYPE token's system
                     * identifier to the empty string,
                     */
                    clearLongStrBuf();
                    /*
                     * then switch to the DOCTYPE system identifier
                     * (single-quoted) state.
                     */
                    doctypeSystemIdentifierSingleQuotedState();
                    return;
                case '>':
                    /* U+003E GREATER-THAN SIGN (>) Parse error. */
                    err("Expected a system identifier but the doctype ended.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, false);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside a doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, null, null, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /* Anything else Parse error. */
                    err("Bogus doctype.");
                    /*
                     * Switch to the bogus DOCTYPE state.
                     */
                    bogusDoctypeState();
                    return;
            }
        }
    }

    /**
     * DOCTYPE system identifier (double-quoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypeSystemIdentifierDoubleQuotedState()
            throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '"':
                    /*
                     * U+0022 QUOTATION MARK (") Switch to the after DOCTYPE
                     * system identifier state.
                     */
                    systemIdentifier = longStrBufToString();
                    afterDoctypeSystemIdentifierState();
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside system identifier.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            longStrBufToString(), false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current DOCTYPE token's system identifier.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the DOCTYPE system identifier (double-quoted)
                     * state.
                     */
                    continue;
            }
        }
    }

    /**
     * DOCTYPE system identifier (single-quoted) state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void doctypeSystemIdentifierSingleQuotedState()
            throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '\'':
                    /*
                     * U+0027 APOSTROPHE (') Switch to the after DOCTYPE system
                     * identifier state.
                     */
                    systemIdentifier = longStrBufToString();
                    afterDoctypeSystemIdentifierState();
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside system identifier.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            longStrBufToString(), false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Append the current input character to the
                     * current DOCTYPE token's system identifier.
                     */
                    appendLongStrBuf(c);
                    /*
                     * Stay in the DOCTYPE system identifier (double-quoted)
                     * state.
                     */
                    continue;
            }
        }
    }

    /**
     * After DOCTYPE system identifier state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void afterDoctypeSystemIdentifierState() throws SAXException,
            IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B
                     * LINE TABULATION U+000C FORM FEED (FF) U+0020 SPACE Stay
                     * in the after DOCTYPE system identifier state.
                     */
                    continue;
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Emit the current DOCTYPE
                     * token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            systemIdentifier, true);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            systemIdentifier, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /* Anything else Parse error. */
                    err("Bogus doctype.");
                    /*
                     * Switch to the bogus DOCTYPE state.
                     */
                    bogusDoctypeState();
                    return;
            }
        }
    }

    /**
     * Bogus DOCTYPE state
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void bogusDoctypeState() throws SAXException, IOException {
        for (;;) {
            /*
             * Consume the next input character:
             */
            char c = read();
            switch (c) {
                case '>':
                    /*
                     * U+003E GREATER-THAN SIGN (>) Set the DOCTYPE token's
                     * correctness flag to incorrect. Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            systemIdentifier, false);
                    /*
                     * Switch to the data state.
                     */
                    return;
                case '\u0000':
                    /* EOF Parse error. */
                    err("End of file inside doctype.");
                    /*
                     * Set the DOCTYPE token's correctness flag to incorrect.
                     * Emit that DOCTYPE token.
                     */
                    tokenHandler.doctype(doctypeName, publicIdentifier,
                            systemIdentifier, false);
                    /*
                     * Reconsume the EOF character in the data state.
                     */
                    unread(c);
                    return;
                default:
                    /*
                     * Anything else Stay in the bogus DOCTYPE state.
                     */
                    continue;
            }
        }
    }

    /**
     * Consume entity
     * 
     * Unlike the definition is the spec, this method does not return a value
     * and never requires the caller to backtrack. This method takes care of
     * emitting characters or appending to the current attribute value. It also
     * takes care of that in the case when consuming the entity fails.
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void consumeEntity(boolean inAttribute) throws SAXException,
            IOException {
        clearStrBuf();
        appendStrBuf('&');
        /*
         * This section defines how to consume an entity. This definition is
         * used when parsing entities in text and in attributes.
         * 
         * The behaviour depends on the identity of the next character (the one
         * immediately after the U+0026 AMPERSAND character):
         */
        char c = read();
        switch (c) {
            case ' ':
            case '\t':
            case '\n':
            case '\u000B':
            case '\u000C':
            case '<':
            case '&':
            case '\u0000':
                /*
                 * U+0009 CHARACTER TABULATION U+000A LINE FEED (LF) U+000B LINE
                 * TABULATION U+000C FORM FEED (FF) U+0020 SPACE U+003C
                 * LESS-THAN SIGN U+0026 AMPERSAND EOF Not an entity. No
                 * characters are consumed, and nothing is returned. (This is
                 * not an error, either.)
                 */
                if (inAttribute) {
                    appendStrBufToLongStrBuf();
                } else {
                    emitStrBuf();
                }
                unread(c);
                return;
            case '#':
                /*
                 * U+0023 NUMBER SIGN (#) Consume the U+0023 NUMBER SIGN.
                 */
                appendStrBuf('#');
                consumeNCR(inAttribute);
                return;
            default:
                unread(c);
                int entCol = -1;
                int lo = 0;
                int hi = (Entities.NAMES.length - 1);
                int candidate = -1;
                int strBufMark = 0;
                outer: for (;;) {
                    entCol++;
                    c = read();
                    /*
                     * Anything else Consume the maximum number of characters
                     * possible, with the consumed characters case-sensitively
                     * matching one of the identifiers in the first column of
                     * the entities table.
                     */
                    hiloop: for (;;) {
                        if (hi == -1) {
                            break;
                        }
                        if (entCol == Entities.NAMES[hi].length()) {
                            break hiloop;
                        }
                        if (entCol > Entities.NAMES[hi].length()) {
                            break outer;
                        } else if (c < Entities.NAMES[hi].charAt(entCol)) {
                            hi--;
                        } else {
                            break hiloop;
                        }
                    }

                    loloop: for (;;) {
                        if (hi < lo) {
                            break outer;
                        }
                        if (entCol == Entities.NAMES[lo].length()) {
                            candidate = lo;
                            strBufMark = strBufLen;
                            lo++;
                        } else if (entCol > Entities.NAMES[lo].length()) {
                            break outer;
                        } else if (c > Entities.NAMES[lo].charAt(entCol)) {
                            lo++;
                        } else {
                            break loloop;
                        }
                    }
                    if (hi < lo) {
                        break outer;
                    }
                    appendStrBuf(c);
                }
                unread(c);
                // TODO warn about apos (IE) and TRADE (Opera)
                if (candidate == -1) {
                    /* If no match can be made, then this is a parse error. */
                    err("Text after \u201C&\u201D did not match an entity name.");
                    /*
                     * No characters are consumed, and nothing is returned.
                     */
                    if (inAttribute) {
                        appendStrBufToLongStrBuf();
                    } else {
                        emitStrBuf();
                    }
                    return;
                } else {
                    if (!Entities.NAMES[candidate].endsWith(";")) {
                        /*
                         * If the last character matched is not a U+003B
                         * SEMICOLON (;), there is a parse error.
                         */
                        err("Entity reference was not terminated by a semicolon.");
                        if (inAttribute) {
                            /*
                             * If the entity is being consumed as part of an
                             * attribute, and the last character matched is not
                             * a U+003B SEMICOLON (;),
                             */
                            c = read();
                            if ((c >= '0' && c <= '9')
                                    || (c >= 'A' && c <= 'Z')
                                    || (c >= 'a' && c <= 'z')) {
                                /*
                                 * and the next character is in the range U+0030
                                 * DIGIT ZERO to U+0039 DIGIT NINE, U+0041 LATIN
                                 * CAPITAL LETTER A to U+005A LATIN CAPITAL
                                 * LETTER Z, or U+0061 LATIN SMALL LETTER A to
                                 * U+007A LATIN SMALL LETTER Z, then, for
                                 * historical reasons, all the characters that
                                 * were matched after the U+0026 AMPERSAND (&)
                                 * must be unconsumed, and nothing is returned.
                                 */
                                appendStrBufToLongStrBuf();
                                unread(c);
                                return;
                            }
                            unread(c);
                        }
                    }

                    /*
                     * Otherwise, return a character token for the character
                     * corresponding to the entity name (as given by the second
                     * column of the entities table).
                     */
                    char[] val = Entities.VALUES[candidate];
                    emitOrAppend(val, inAttribute);
                    // this is so complicated!
                    if (strBufMark < strBufLen) {
                        if (inAttribute) {
                            for (int i = strBufMark; i < strBufLen; i++) {
                                appendLongStrBuf(strBuf[i]);
                            }
                        } else {
                            tokenHandler.characters(strBuf, strBufMark,
                                    strBufLen - strBufMark);
                        }
                    }
                    return;
                    /*
                     * If the markup contains I'm &notit; I tell you, the entity
                     * is parsed as "not", as in, I'm ¬it; I tell you. But if
                     * the markup was I'm &notin; I tell you, the entity would
                     * be parsed as "notin;", resulting in I'm ∉ I tell you.
                     */
                }

        }
    }

    private void consumeNCR(boolean inAttribute) throws SAXException,
            IOException {
        int value = 0;
        boolean seenDigits = false;
        boolean hex = false;
        /*
         * The behaviour further depends on the character after the U+0023
         * NUMBER SIGN:
         */
        char c = read();
        if (c == 'x' || c == 'X') {
            /*
             * U+0078 LATIN SMALL LETTER X U+0058 LATIN CAPITAL LETTER X Consume
             * the X.
             * 
             * Follow the steps below, but using the range of characters U+0030
             * DIGIT ZERO through to U+0039 DIGIT NINE, U+0061 LATIN SMALL
             * LETTER A through to U+0066 LATIN SMALL LETTER F, and U+0041 LATIN
             * CAPITAL LETTER A, through to U+0046 LATIN CAPITAL LETTER F (in
             * other words, 0-9, A-F, a-f).
             * 
             * When it comes to interpreting the number, interpret it as a
             * hexadecimal number.
             */
            appendStrBuf(c);
            hex = true;
        } else {
            unread(c);
            /*
             * Anything else Follow the steps below, but using the range of
             * characters U+0030 DIGIT ZERO through to U+0039 DIGIT NINE (i.e.
             * just 0-9).
             * 
             * When it comes to interpreting the number, interpret it as a
             * decimal number.
             */
        }
        for (;;) {
            // Deal with overflow gracefully
            if (value < 0) {
                value = 0x110000; // Value above Unicode range but within int
                // range
            }
            /*
             * Consume as many characters as match the range of characters given
             * above.
             */
            c = read();
            if (c >= '0' && c <= '9') {
                seenDigits = true;
                if (hex) {
                    value *= 16;
                } else {
                    value *= 10;
                }
                value += c - '0';
            } else if (hex && c >= 'A' && c <= 'F') {
                seenDigits = true;
                value *= 16;
                value += c - 'A' + 10;
            } else if (hex && c >= 'a' && c <= 'f') {
                seenDigits = true;
                value *= 16;
                value += c - 'a' + 10;
            } else if (c == ';') {
                if (seenDigits) {
                    handleNCRValue(value, inAttribute);
                    return;
                } else {
                    err("No digits after \u201C" + strBufToString() + "\u201D.");
                    appendStrBuf(';');
                    if (inAttribute) {
                        appendStrBufToLongStrBuf();
                    } else {
                        emitStrBuf();
                    }
                    return;
                }
            } else {
                /*
                 * If no characters match the range, then don't consume any
                 * characters (and unconsume the U+0023 NUMBER SIGN character
                 * and, if appropriate, the X character). This is a parse error;
                 * nothing is returned.
                 * 
                 * Otherwise, if the next character is a U+003B SEMICOLON,
                 * consume that too. If it isn't, there is a parse error.
                 */
                unread(c);
                if (seenDigits) {
                    err("Character reference was not terminated by a semicolon.");
                    handleNCRValue(value, inAttribute);
                    return;
                } else {
                    err("No digits after \u201C" + strBufToString() + "\u201D.");
                    if (inAttribute) {
                        appendStrBufToLongStrBuf();
                    } else {
                        emitStrBuf();
                    }
                    return;
                }
            }
        }
    }

    private void handleNCRValue(int value, boolean inAttribute)
            throws SAXException, IOException {
        /*
         * If one or more characters match the range, then take them all and
         * interpret the string of characters as a number (either hexadecimal or
         * decimal as appropriate).
         */
        if (value >= 0x80 && value <= 0x9f) {
            /*
             * If that number is one of the numbers in the first column of the
             * following table, then this is a parse error.
             */
            err("A numeric character reference expanded to the C1 controls range.");
            /*
             * Find the row with that number in the first column, and return a
             * character token for the Unicode character given in the second
             * column of that row.
             */
            char[] val = Entities.WINDOWS_1252[value - 0x80];
            emitOrAppend(val, inAttribute);
            return;
        } else if (value == 0x0D) {
            err("A numeric character reference expanded to carriage return.");
            emitOrAppend(LF, inAttribute);
            return;
        } else if (value == 0) {
            /*
             * Otherwise, if the number is zero, if the number is higher than
             * 0x10FFFF, or if it's one of the surrogate characters (characters
             * in the range 0xD800 to 0xDFFF), then this is a parse error;
             * return a character token for the U+FFFD REPLACEMENT CHARACTER
             * character instead.
             */
            err("Character reference expands to U+0000.");
            emitOrAppend(REPLACEMENT_CHARACTER, inAttribute);
            return;
        } else if ((contentSpacePolicy != XmlViolationPolicy.ALLOW)
                && (value == 0xB || value == 0xC)) {
            if (contentSpacePolicy == XmlViolationPolicy.ALTER_INFOSET) {
                emitOrAppend(SPACE, inAttribute);
            } else if (contentSpacePolicy == XmlViolationPolicy.FATAL) {
                fatal("A character reference expanded to a space character that is not legal XML 1.0 white space.");
            }
        } else if ((value & 0xF800) == 0xD800) {
            err("Character reference expands to a surrogate.");
            emitOrAppend(REPLACEMENT_CHARACTER, inAttribute);
            return;
        } else if (value <= 0xFFFF) {
            /*
             * Otherwise, return a character token for the Unicode character
             * whose code point is that number.
             */
            char c = (char) value;
            if (c < '\t' || (c > '\r' || c < ' ') || isNonCharacter(c)) {
                if (contentNonXmlCharPolicy != XmlViolationPolicy.FATAL) {
                    if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                        c = '\uFFFD';
                    }
                    warn("Character reference expanded to a character that is not a legal XML 1.0 character.");
                } else {
                    fatal("Character reference expanded to a character that is not a legal XML 1.0 character.");
                }
            }
            if (isPrivateUse(c)) {
                warnAboutPrivateUseChar();
            }
            bmpChar[0] = c;
            emitOrAppend(bmpChar, inAttribute);
            return;
        } else if (value <= 0x10FFFF) {
            if (isNonCharacter(value)) {
                warn("Character reference expands to an astral non-character.");
            }
            if (isAstralPrivateUse(value)) {
                warnAboutPrivateUseChar();
            }
            astralChar[0] = (char) (LEAD_OFFSET + (value >> 10));
            astralChar[1] = (char) (0xDC00 + (value & 0x3FF));
            emitOrAppend(astralChar, inAttribute);
            return;
        } else {
            err("Character reference outside the permissible Unicode range.");
            emitOrAppend(REPLACEMENT_CHARACTER, inAttribute);
            return;
        }
    }

    /**
     * @param val
     * @throws SAXException
     * @throws IOException
     */
    private void emitOrAppend(char[] val, boolean inAttribute)
            throws SAXException, IOException {
        if (inAttribute) {
            appendLongStrBuf(val);
        } else {
            tokenHandler.characters(val, 0, val.length);
        }
    }
}
