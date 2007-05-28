/*
 * Copyright (c) 2005, 2006 Henri Sivonen
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

package fi.iki.hsivonen.htmlparser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.whattf.checker.NormalizationChecker;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fi.iki.hsivonen.io.EncodingInfo;
import fi.iki.hsivonen.io.NonBufferingAsciiInputStreamReader;
import fi.iki.hsivonen.xml.AttributesImpl;
import fi.iki.hsivonen.xml.ContentHandlerFilter;
import fi.iki.hsivonen.xml.EmptyAttributes;
import fi.iki.hsivonen.xml.SilentDraconianErrorHandler;
import fi.iki.hsivonen.xml.XhtmlSaxEmitter;

/**
 * WARNING: This parser is incomplete. It does not perform tag inference, yet. It does not yet perform 
 * case folding for attribute value like method="POST".
 * 
 * @version $Id$
 * @author hsivonen
 */
public final class HtmlParser implements XMLReader, Locator {
        
    private static final int CASE_MASK = (1 << 5);

    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);

    private static final int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;

    private static final char[] LT = { '<' };

    private static final char[] APOS = { '\'' };

    private static final char[] OCTYPE = "octype".toCharArray();

    private static final char[] TML = "tml".toCharArray();

    private static final char[] UBLIC = "ublic".toCharArray();

    private static final int PCDATA = 0;

    private static final int SCRIPT = 1;

    private static final int STYLE = 2;

    private String publicId;

    private String systemId;

    private boolean nonWhiteSpaceAllowed;

    private int cdataState;

    private ErrorHandler eh;

    private ContentHandler ch;
    
    private DoctypeHandler doctypeHandler;

    private XhtmlSaxEmitter emitter;

    private Reader reader;

    private int pos;

    private int cstart;

    private char[] buf = new char[2048];

    private int bufLen;

    private int line;

    private int col;

    private boolean doctypeSeen;
    
    private int doctypeMode;
    
    private boolean html5;

    private char prev;

    private boolean wasLt;

    private char[] strBuf = new char[64];

    private int strBufLen = 0;

    private char[] attrBuf = new char[1024];

    private int attrBufLen = 0;

    private AttributesImpl attrs = new AttributesImpl();

    private char[] bmpChar = { '\u0000' };

    private char[] astralChar = { '\u0000', '\u0000' };

    private DTDHandler dtdHandler;

    private EmptyElementFilter eef;

    private TagInferenceFilter tif;

    private CharacterEncodingDeclarationFilter cedf;

    private ContentHandlerFilter pipelineLast;

    private EntityResolver entityResolver = null;

    private String encoding = null;

    private InputStream stream;

    private boolean foldedAttributeValue;

    private boolean alreadyWarnedAboutPrivateUseCharacters;
    
    private NormalizationChecker normalizationChecker = null;

    public HtmlParser() {
        eef = new EmptyElementFilter();
        tif = new TagInferenceFilter(this);
        cedf = new CharacterEncodingDeclarationFilter(this);
        ch = eef;
        emitter = new XhtmlSaxEmitter(ch);
        eef.setContentHandler(tif);
        tif.setContentHandler(cedf);
        pipelineLast = cedf;
        setErrorHandler(new SilentDraconianErrorHandler());
        setContentHandler(new DefaultHandler());
    }

    private void clearStrBuf() {
        strBufLen = 0;
    }

    private void appendStrBufAsciiLowerCase(char c) throws SAXException,
            IOException {
        if (c >= 'A' && c <= 'Z') {
            appendStrBuf((char) (c | CASE_MASK));
        } else {
            appendStrBuf(c);
        }
    }

    private void appendStrBuf(char c) throws SAXException, IOException {
        if (strBufLen == strBuf.length) {
            fatal("Identifier too long.");
        } else {
            strBuf[strBufLen] = c;
            strBufLen++;
        }
    }

    private String strBufToString() {
        return new String(strBuf, 0, strBufLen);
    }

    private void clearAttrBuf() {
        attrBufLen = 0;
    }

    private void appendAttrBuf(char c) throws SAXException, IOException {
        if (attrBufLen == attrBuf.length) {
            fatal("Attribute value or other quoted string too long.");
        } else {
            attrBuf[attrBufLen] = c;
            attrBufLen++;
        }
    }

    private void appendAttrBufAsciiLowerCase(char c) throws SAXException,
            IOException {
        if (c >= 'A' && c <= 'Z') {
            appendAttrBuf((char) (c | CASE_MASK));
        } else if (c <= '\u007F') {
            appendAttrBuf(c);
        } else {
            fatal("Non-ASCII character in an attribute value that is subject to case folding.");
        }
    }

    /**
     * @param cs
     * @throws SAXException
     */
    private void appendAttrBuf(char[] cs) throws SAXException, IOException {
        for (int i = 0; i < cs.length; i++) {
            appendAttrBuf(cs[i]);
        }
    }

    /**
     * @param cs
     * @throws SAXException
     */
    private void appendAttrBufAsciiLowerCase(char[] cs) throws SAXException,
            IOException {
        for (int i = 0; i < cs.length; i++) {
            appendAttrBufAsciiLowerCase(cs[i]);
        }
    }

    private String attrBufToString() {
        return new String(attrBuf, 0, attrBufLen);
    }

    private void parse() throws SAXException, IOException {
        pos = -1;
        cstart = -1;
        line = 1;
        col = 0;
        doctypeSeen = false;
        prev = '\u0000';
        cdataState = PCDATA;
        nonWhiteSpaceAllowed = false;
        wasLt = false;
        bufLen = 0;
        html5 = false;
        char c;
        for (;;) {
            if (cdataState == PCDATA) {
                c = nextMayEnd();
                if (c == '\u0000') {
                    flushChars();
                    if (!doctypeSeen) {
                        err("The document did not have a doctype.");
                    }
                    return;
                } else if (c == '<') {
                    flushChars();
                    consumeMarkup();
                } else if (c == '&') {
                    flushChars();
                    emitter.characters(consumeCharRef());
                } else if (isWhiteSpace(c)) {
                    if (nonWhiteSpaceAllowed) {
                        if (cstart == -1) {
                            cstart = pos;
                        }
                    }
                } else {
                    doctypeNotOk();
                    if (nonWhiteSpaceAllowed) {
                        if (cstart == -1) {
                            cstart = pos;
                        }
                    } else {
                        fatal("Character data not allowed at this point.");
                    }
                }
            } else {
                c = next();
                if (c == '<') {
                    wasLt = true;
                    flushChars();
                } else if (c == '/') {
                    if (wasLt) {
                        consumeEndTag();
                    } else if (cstart == -1) {
                        cstart = pos;
                    }
                    wasLt = false;
                } else {
                    if (wasLt) {
                        emitter.characters(LT);
                    }
                    if (cstart == -1) {
                        cstart = pos;
                    }
                    wasLt = false;
                }
            }
        }
    }

    /**
     * @throws SAXException
     *  
     */
    private void doctypeNotOk() throws SAXException, IOException {
        if (!doctypeSeen) {
            // there was no doctype
            err("There was no doctype.");
            doctypeSeen = true;
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isWhiteSpace(char c) {
        return (c == ' ') || (c == '\t') || (c == '\n');
    }

    /**
     * @throws SAXException
     *  
     */
    private char[] consumeCharRef() throws SAXException, IOException {
        char c = next();
        if (c == '#') {
            return consumeNCR();
        } else if (isNameStart(c)) {
            return consumeEntityRef(c);
        } else {
            // XXX should we err or continue here
            fatal("& not followed by # or name start.");
        }
        throw new RuntimeException("Unreachable");
    }

    /**
     * @param c
     * @throws SAXException
     */
    private char[] consumeEntityRef(char c) throws SAXException, IOException {
        clearStrBuf();
        appendStrBuf(c);
        for (;;) {
            c = next();
            if (isNameChar(c)) {
                appendStrBuf(c);
            } else if (c == ';') {
                String name = strBufToString();
                char[] rv = html5 ? Entities.resolve5(name) : Entities.resolve(name);
                if (rv == null) {
                    if ("apos".equals(name)) {
                        if (html5) {
                            warn("&apos; is not supported by IE6.");
                        } else {
                            err("Even though there is a predefined entity called \u201Capos\u201D is XML, there is no such thing in HTML 4.01. Continuing parsing pretending that such an entity exists.");
                        }
                        return APOS;
                    } else {
                        fatal("Unknown entity \u201C" + name + "\u201D.");
                    }
                }
                return rv;
            } else {
                // XXX should we allow implicit close as in HTML4?
                fatal("Found a non-name character in entity reference / unterminated entity reference.");
            }
        }
    }

    /**
     * @throws SAXException
     *  
     */
    private char[] consumeNCR() throws SAXException, IOException {
        clearStrBuf();
        int intVal = 0;
        char c = next();
        if (c == 'x' || c == 'X') {
            for (int i = 0;; i++) {
                if (i == 6) {
                    fatal("Hexadecimal character reference too long.");
                }
                c = next();
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                        || (c >= 'A' && c <= 'F')) {
                    appendStrBuf(c);
                } else if (c == ';') {
                    if (i == 0) {
                        fatal("No digits in hexadecimal character reference.");
                    }
                    intVal = Integer.parseInt(strBufToString(), 16);
                    break;
                } else {
                    fatal("Bad character in hexadecimal character reference.");
                }
            }
        } else if (c >= '0' && c <= '9') {
            appendStrBuf(c);
            for (int i = 0;; i++) {
                if (i == 6) {
                    fatal("Decimal character reference too long.");
                }
                c = next();
                if (c >= '0' && c <= '9') {
                    appendStrBuf(c);
                } else if (c == ';') {
                    intVal = Integer.parseInt(strBufToString());
                    break;
                } else {
                    fatal("Bad character in decimal character reference.");
                }
            }
        } else {
            fatal("Bad character in numeric character reference.");
        }
        if ((intVal & 0xF800) == 0xD800) {
            fatal("Character reference expands to a surrogate.");
        } else if (intVal <= 0xFFFF) {
            c = (char) intVal;
            if (isForbidden(c)) {
                fatal("Character reference expands to a forbidden character.");
            }
            if (isPrivateUse(c)) {
                warnAboutPrivateUseChar();
            }
            bmpChar[0] = c;
            return bmpChar;
        } else if (intVal <= 0x10FFFF) {
            // XXX astral non-characters are not banned
            if (isNonCharacter(intVal)) {
                warn("Character reference expands to an astral non-character.");
            }
            if (isAstralPrivateUse(intVal)) {
                warnAboutPrivateUseChar();
            }
            astralChar[0] = (char) (LEAD_OFFSET + (intVal >> 10));
            astralChar[1] = (char) (0xDC00 + (intVal & 0x3FF));
            return astralChar;
        } else {
            fatal("Character reference outside the permissible Unicode range.");
        }
        throw new RuntimeException("Unreachable");
    }

    /**
     * @throws SAXException
     *  
     */
    private void consumeMarkup() throws SAXException, IOException {
        char c = next();
        if (c == '!') {
            consumeMarkupDecl();
        } else if (c == '?') {
            consumePI();
        } else if (c == '/') {
            consumeEndTag();
        } else if (isNameStart(c)) {
            consumeStartTag(c);
        } else {
            fatal("Found illegal character after <.");
        }
    }

    /**
     * @return
     * @throws SAXException
     */
    private char next() throws SAXException, IOException {
        char c = nextMayEnd();
        if (c == '\u0000') {
            fatal("Unexpected end of file.");
        }
        return c;
    }

    /**
     * @param c
     * @throws SAXException
     */
    private void consumeStartTag(char c) throws SAXException, IOException {
        doctypeNotOk();
        clearStrBuf();
        appendStrBufAsciiLowerCase(c);
        for (;;) {
            c = next();
            if (c == '>') {
                String gi = strBufToString();
                maybeBeginCdata(gi);
                emitter.startElement(gi,
                        EmptyAttributes.EMPTY_ATTRIBUTES);
                return;
            } else if (c == '/') {
                c = next();
                if (c == '>') {
                    String gi = strBufToString();
                    maybeErrOnEmptyElementSyntax(gi);
                    maybeBeginCdata(gi);
                    emitter.startElement(gi,
                            EmptyAttributes.EMPTY_ATTRIBUTES);
                    return;
                } else {
                    fatal("Stray slash in start tag.");
                }                
            } else if (isNameChar(c)) {
                appendStrBufAsciiLowerCase(c);
            } else if (isWhiteSpace(c)) {
                attrs.clear();
                String gi = strBufToString();
                maybeBeginCdata(gi);
                c = nextAfterZeroOrMoreWhiteSpace();
                for (;;) {
                    if (isNameStart(c)) {
                        c = consumeAttribute(c, gi);
                    } else if (c == '/') {
                        c = next();
                        if (c == '>') {
                            maybeErrOnEmptyElementSyntax(gi);
                            emitter.startElement(gi, attrs);
                            return;
                        } else {
                            fatal("Stray slash in start tag.");
                        }
                    } else if (c == '>') {
                        emitter.startElement(gi, attrs);
                        return;
                    } else {
                        fatal("Garbage in start tag.");
                    }
                }
            } else {
                fatal("Illegal character in element name.");
            }
        }
    }

    /**
     * @param gi 
     * @throws SAXException
     */
    private void maybeErrOnEmptyElementSyntax(String gi) throws SAXException {
        if (!(html5 && EmptyElementFilter.isEmpty(gi))) {
            err("XML-style empty element syntax (\u201C<" + gi + "/>\u201D) is not legal in HTML. Skipping the slash.");
        }
    }

    /**
     * @param gi
     */
    private void maybeBeginCdata(String gi) {
        if ("style".equals(gi)) {
            cdataState = STYLE;
            wasLt = false;
        } else if ("script".equals(gi)) {
            cdataState = SCRIPT;
            wasLt = false;
        }
    }

    /**
     * @param c
     * @param gi 
     * @return
     * @throws SAXException
     */
    private char consumeAttribute(char c, String gi) throws SAXException, IOException {
        clearStrBuf();
        appendStrBufAsciiLowerCase(c);
        for (;;) {
            c = next();
            if (isNameChar(c)) {
                appendStrBufAsciiLowerCase(c);
            } else {
                break;
            }
        }
        String name = strBufToString();
        if (isWhiteSpace(c)) {
            c = nextAfterZeroOrMoreWhiteSpace();
        }
        if (c == '=') {
            foldedAttributeValue = AttributeInfo.isCaseFolded(name);
            c = nextAfterZeroOrMoreWhiteSpace();
            clearAttrBuf();
            if (c == '\"') {
                consumeQuotedAttributeValue('\"');
                c = next();
            } else if (c == '\'') {
                consumeQuotedAttributeValue('\'');
                c = next();
            } else if (isUnquotedAttributeChar(c)) {
                // XXX should the real definition of Name Start and Name Char be
                // used here?
                if (foldedAttributeValue) {
                    appendAttrBufAsciiLowerCase(c);
                } else {
                    appendAttrBuf(c);
                }
                for (;;) {
                    c = next();
                    if (isUnquotedAttributeChar(c)) {
                        if (foldedAttributeValue) {
                            appendAttrBufAsciiLowerCase(c);
                        } else {
                            appendAttrBuf(c);
                        }
                    } else {
                        break;
                    }
                }
            } else {
                fatal("Garbage in place of attribute value. Possibly quotes missing.");
            }
            if (isWhiteSpace(c)) {
                c = nextAfterZeroOrMoreWhiteSpace();
            }
            String value = attrBufToString();
            if (name.startsWith("xml")) {
                if (!(html5 && "html".equals(gi) && "xmlns".equals(name) && "http://www.w3.org/1999/xhtml".equals(value))) {
                    err("Attribute \u201C" + name +" \201D not allowed here; ignored.");
                }
            } else if ("lang".equals(name)) {
                fatalIfAttributeExists("xml:lang");
                attrs.addAttribute("http://www.w3.org/XML/1998/namespace",
                        "lang", "xml:lang", "CDATA", value);
            } else {
                fatalIfAttributeExists(name);
                attrs.addAttribute(name, value);
            }
            return c;
        } else {
            if (!AttributeInfo.isBoolean(name)) {
                fatal("Cannot minimize non-boolean attributes.");
            }
            fatalIfAttributeExists(name);
            attrs.addAttribute(name, name);
            return c;
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isUnquotedAttributeChar(char c) {
        return (c == '.' || c == '-' || c == '_' || (c >= '0' && c <= ':')
                || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    /**
     * @param c
     * @return
     * @throws SAXException
     * @throws IOException
     */
    private void consumeQuotedAttributeValue(char delim) throws SAXException,
            IOException {
        char c;
        for (;;) {
            c = next();
            if (c == delim) {
                return;
            } else if (c == '&') {
                if (foldedAttributeValue) {
                    appendAttrBufAsciiLowerCase(consumeCharRef());
                } else {
                    appendAttrBuf(consumeCharRef());
                }
            } else if (isWhiteSpace(c)) {
                appendAttrBuf(' ');
            } else {
                if (foldedAttributeValue) {
                    appendAttrBufAsciiLowerCase(c);
                } else {
                    appendAttrBuf(c);
                }
            }
            // XXX is a warning called for when there is < or >?
        }
    }

    /**
     * @param name
     * @throws SAXException
     */
    private void fatalIfAttributeExists(String name) throws SAXException,
            IOException {
        if (attrs.getIndex(name) != -1) {
            fatal("Duplicate attribute.");
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isNameStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * @param c
     * @return
     */
    private boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || (c == '-');
    }

    /**
     * @throws SAXException
     *  
     */
    private void consumeEndTag() throws SAXException, IOException {
        doctypeNotOk();
        clearStrBuf();
        char c = next();
        if (isNameStart(c)) {
            appendStrBufAsciiLowerCase(c);
        } else {
            fatal("Element name in end tag did not start with a legal name character.");
        }
        for (;;) {
            c = next();
            if (isNameChar(c)) {
                appendStrBufAsciiLowerCase(c);
            } else if (c == '>') {
                String gi = strBufToString();
                cdataStateEnd(gi);
                emitter.endElement(gi);
                return;
            } else if (isWhiteSpace(c)) {
                if (nextAfterZeroOrMoreWhiteSpace() == '>') {
                    String gi = strBufToString();
                    cdataStateEnd(gi);
                    emitter.endElement(gi);
                    return;
                } else {
                    fatal("Garbage in end tag.");
                }
            } else {
                fatal("Element name in end tag contained an illegal character.");
            }
        }
    }

    /**
     * @param gi
     * @throws SAXException
     */
    private void cdataStateEnd(String gi) throws SAXException, IOException {
        if (cdataState == STYLE) {
            if ("style".equals(gi)) {
                cdataState = PCDATA;
            } else {
                fatal("\u201C</\u201D seen inside the style element, but the string did not constitute the start of the end tag of the element.");
            }
        } else if (cdataState == SCRIPT) {
            if ("script".equals(gi)) {
                cdataState = PCDATA;
            } else {
                fatal("\u201C</\u201D seen inside the script element, but the string did not constitute the start of the end tag of the element.");
            }
        }
    }

    /**
     * @return
     * @throws SAXException
     */
    private char nextAfterZeroOrMoreWhiteSpace() throws SAXException,
            IOException {
        for (;;) {
            char c = next();
            if (!isWhiteSpace(c)) {
                return c;
            }
        }
    }

    /**
     * @throws SAXException
     *  
     */
    private void consumePI() throws SAXException, IOException {
        // XXX should PIs be allowed?
        fatal("Processing instructions are not allowed.");
    }

    /**
     * @throws SAXException
     *  
     */
    private void consumeMarkupDecl() throws SAXException, IOException {
        char c = next();
        if (c == '-') {
            for (;;) {
                c = consumeComment();
                if (c == '>') {
                    return;
                } else if (c != '-') {
                    fatal("Garbage after comment.");
                }
            }
        } else if (c == 'd' || c == 'D') {
            consumeDoctype();
        } else if (c == '>') {
            return;
        } else if (c == '[') {
            fatal("Marked sections not allowed.");
        } else {
            fatal("Bad character in markup declaration.");
        }
    }

    /**
     * @throws SAXException
     *  
     */
    private void consumeDoctype() throws SAXException, IOException {
        if (doctypeSeen) {
            fatal("Doctype not allowed at this point.");
        }
        doctypeSeen = true;
        if (!consumeCaseInsensitiveAsciiLetterString(OCTYPE)) {
            fatal("Expected string \u201CDOCTYPE\u201D.");
        }
        char c = next();
        if (!isWhiteSpace(c)) {
            fatal("Expected white space after \u201CDOCTYPE\u201D.");
        }
        c = nextAfterZeroOrMoreWhiteSpace();
        if (!(c == 'h' || c == 'H')) {
            fatal("Expected string \u201Chtml\u201D.");
        }
        if (!consumeCaseInsensitiveAsciiLetterString(TML)) {
            fatal("Expected string \u201Chtml\u201D.");
        }
        c = next();
        if (c == '>') {
            sawHtml5Doctype();
            return;
        } else if (!isWhiteSpace(c)) {
            fatal("Garbage in doctype");
        }
        c = nextAfterZeroOrMoreWhiteSpace();
        if (c == '>') {
            sawHtml5Doctype();
            return;
        } else if (c == 's' || c == 'S') {
            fatal("Doctype with possibly a SYSTEM id only.");
        } else if (c == '[') {
            fatal("Doctype with internal subset.");
        } else if (!(c == 'p' || c == 'P')) {
            fatal("Expected string \u201CPUBLIC\u201D.");
        }
        if (!consumeCaseInsensitiveAsciiLetterString(UBLIC)) {
            fatal("Expected string \u201CPUBLIC\u201D.");
        }
        c = next();
        if (!isWhiteSpace(c)) {
            fatal("Expected white space after \u201CPUBLIC\u201D.");
        }
        c = nextAfterZeroOrMoreWhiteSpace();
        String publicId = null;
        String systemId = null;
        if (c == '\"') {
            publicId = unescapedStringUntil('\"');
        } else if (c == '\'') {
            publicId = unescapedStringUntil('\'');
        } else {
            fatal("Garbage in doctype. Expected a quoted string.");
        }
        c = next();
        if (c == '>') {
            checkPublicAndSystemIds(publicId, systemId);
            return;
        } else if (!isWhiteSpace(c)) {
            fatal("Expected white space or \u201C>\u201D after the public id.");
        }
        c = nextAfterZeroOrMoreWhiteSpace();
        if (c == '>') {
            checkPublicAndSystemIds(publicId, systemId);
            return;
        } else if (c == '\"') {
            systemId = unescapedStringUntil('\"');
        } else if (c == '\'') {
            systemId = unescapedStringUntil('\'');
        } else {
            fatal("Garbage in doctype. Expected a quoted string or \u201C>\u201D.");
        }
        checkPublicAndSystemIds(publicId, systemId);
        c = next();
        if (c == '>') {
            return;
        } else if (!isWhiteSpace(c)) {
            fatal("Expected white space or \u201C>\u201D after the system id.");
        }
        c = nextAfterZeroOrMoreWhiteSpace();
        if (c == '>') {
            return;
        } else {
            fatal("Garbage in doctype. Expected \u201C>\u201D.");
        }
    }

    /**
     * @throws SAXException 
     * 
     */
    private void sawHtml5Doctype() throws SAXException {
        html5 = true;
        switch (doctypeMode) {
            case DoctypeHandler.ANY_DOCTYPE:
                if (doctypeHandler != null) {
                    doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML5);
                }
                break;
            case DoctypeHandler.DOCTYPE_HTML5:
                return;
            case DoctypeHandler.DOCTYPE_HTML401_STRICT:
                err("Expected an HTML 4.01 Strict document but saw the HTML5 doctype.");
                break;
            case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
                err("Expected an HTML 4.01 Transitional document but saw the HTML5 doctype.");
                break;
            default:
                throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
        }
    }

    /**
     * @param publicId
     * @param systemId
     * @throws SAXException
     */
    private void checkPublicAndSystemIds(String publicId, String systemId)
            throws SAXException, IOException {
        if ("-//W3C//DTD HTML 4.01//EN".equals(publicId)) {
            switch (doctypeMode) {
                case DoctypeHandler.ANY_DOCTYPE:
                    if (doctypeHandler != null) {
                        doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML401_STRICT);
                    }
                    break;
                case DoctypeHandler.DOCTYPE_HTML401_STRICT:
                    return;
                case DoctypeHandler.DOCTYPE_HTML5:
                    err("Expected an HTML5 document but saw an HTML 4.01 Strict doctype.");
                    break;
                case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
                    err("Expected HTML 4.01 Transitional document but saw an HTML 4.01 Strict doctype.");
                    break;
                default:
                    throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
            }
            if (systemId == null) {
                // XXX err, because HTML 4.01 says "must"?
                warn("The Strict doctype lacks the system id (URI). This kind of Strict doctype is considered quirky by Mac IE 5. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\u201D.");
            } else if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemId)) {
                // XXX err, because HTML 4.01 says "must"?
                warn("The Strict doctype has a non-canonical system id (URI). The form required by the HTML 4.01 specification is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\u201D.");
            }
        } else if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicId)) {
            switch (doctypeMode) {
                case DoctypeHandler.ANY_DOCTYPE:
                    if (doctypeHandler != null) {
                        doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL);
                    }
                    break;
                case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
                    return;
                case DoctypeHandler.DOCTYPE_HTML401_STRICT:
                    err("Expected an HTML 4.01 Strict document but saw an HTML 4.01 Transitional doctype.");
                    break;
                case DoctypeHandler.DOCTYPE_HTML5:
                    err("Expected an HTML5 document but saw an HTML 4.01 Transitional doctype.");
                    break;
                default:
                    throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
            }
            if (systemId == null) {
                err("The Transitional doctype lacks the system id (URI). This kind of Transitional doctype is considered quirky by browsers. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\u201D.");
            } else if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemId)) {
                // XXX err, because HTML 4.01 says "must"?
                warn("The Transitional doctype has a non-canonical system id (URI). This kind of Transitional doctype may be considered quirky by some legacy browsers. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\u201D.");
            }
        } else if (publicId.startsWith("-//W3C//DTD XHTML ")) {
            fatal("XHTML public id seen. XHTML documents are not conforming HTML5 or HTML 4.01 documents.");
        } else {
            err("Legacy doctype or inappropriate doctype. This parser is designed for HTML5 and also supports the HTML5-like subset of HTML 4.01.");
        }
    }

    /**
     * @param c
     * @return
     * @throws SAXException
     */
    private String unescapedStringUntil(char delim) throws SAXException,
            IOException {
        clearAttrBuf();
        for (;;) {
            char c = next();
            if (c == delim) {
                return attrBufToString();
            } else {
                appendAttrBuf(c);
            }
        }
    }

    private boolean consumeCaseInsensitiveAsciiLetterString(char[] str)
            throws SAXException, IOException {
        for (int i = 0; i < str.length; i++) {
            if (!((next() | CASE_MASK) == str[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * @throws SAXException
     *  
     */
    private char consumeComment() throws SAXException, IOException {
        doctypeNotOk();
        char c = next();
        if (c != '-') {
            fatal("Malformed comment.");
        }
        boolean prevWasHyphen = false;
        for (;;) {
            c = next();
            if (c == '-') {
                if (prevWasHyphen) {
                    return nextAfterZeroOrMoreWhiteSpace();
                } else {
                    prevWasHyphen = true;
                }
            } else {
                prevWasHyphen = false;
            }
        }
    }

    private char nextMayEnd() throws SAXException, IOException {
        pos++;
        col++;
        if (pos == bufLen) {
            boolean charDataContinuation = false;
            if (cstart > -1) {
                flushChars();
                charDataContinuation = false;
            }
            try {
                bufLen = reader.read(buf);
            } catch (CharacterCodingException cce) {
                fatal("Input data does not conform to the input encoding.");
            }
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
                fatal("Unmatched low surrogate.");
            }
            prev = c;
        } else {
            // see if there was a lone high surrogate
            if ((prev & 0xFC00) == 0xD800) {
                fatal("Unmatched high surrogate.");
            }
            if (isForbidden(c)) {
                fatal("Forbidden character.");
            } else if (c == '\r') {
                prev = '\r';
                c = buf[pos] = '\n';
                line++;
                col = 0;
            } else if (c == '\n') {
                if (prev != '\r') {
                    prev = c;
                    line++;
                    col = 0;
                } else {
                    prev = c;
                    // swallow the LF
                    col = 0;
                    int tmpCstart = cstart;
                    flushChars();
                    if (tmpCstart != -1) {
                        cstart = pos + 1;
                    }
                    return nextMayEnd();
                }
            } else if (isPrivateUse(c)) {
                warnAboutPrivateUseChar();
            }
        }
        return c;
    }

    private void warnAboutPrivateUseChar() throws SAXException {
        if (!alreadyWarnedAboutPrivateUseCharacters) {
            warn("Document uses the Unicode Private Use Area(s), which should not be used in publicly exchanged documents. (Charmod C073)");
            alreadyWarnedAboutPrivateUseCharacters = true;
        }
    }

    private boolean isPrivateUse(char c) {
        return c >= '\uE000' && c <= '\uF8FF';
    }

    private boolean isAstralPrivateUse(int c) {
        return (c >= 0xF0000 && c <= 0xFFFFD) || (c >= 0x100000 && c <= 0x10FFFD);
    }
    
    /**
     * @param intVal
     * @return
     */
    private boolean isNonCharacter(int c) {
        return (c & 0xFFFE) == 0xFFFE;
    }

    /**
     * @param c
     * @return
     */
    private boolean isForbidden(char c) {
        return !(c == '\t' || c == '\n' || c == '\r'
                || (c >= '\u0020' && c < '\u007F')
                || (c >= '\u00A0' && c < '\uFDD0') || (c > '\uFDDF' && c <= '\uFFFD'));
    }

    /**
     * @throws SAXException
     *  
     */
    private void flushChars() throws SAXException, IOException {
        if (nonWhiteSpaceAllowed) {
            if (cstart > -1) {
                if (pos > cstart) {
                    ch.characters(buf, cstart, pos - cstart);
                }
            }
            cstart = -1;
        }
    }

    /**
     * @throws SAXException
     * @throws SAXParseException
     */
    private void fatal(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        eh.fatalError(spe);
        throw spe;
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void err(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        eh.error(spe);
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void warn(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, this);
        eh.warning(spe);
    }

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

    /**
     * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
     */
    public boolean getFeature(String key) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if ("http://xml.org/sax/features/namespaces".equals(key)) {
            return true;
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(key)) {
            return false;
        } else if ("http://hsivonen.iki.fi/checkers/nfc/".equals(key)) {
            return normalizationChecker != null;
        } else {
            throw new SAXNotRecognizedException(key);
        }
    }

    /**
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public void setFeature(String key, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/features/namespaces".equals(key)) {
            if (!value) {
                throw new SAXNotSupportedException(
                        "Cannot turn off namespace support.");
            }
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(key)) {
            if (value) {
                throw new SAXNotSupportedException("Cannot turn on prefixing.");
            }
        } else if ("http://hsivonen.iki.fi/checkers/nfc/".equals(key)) {
            if (value) {
                if (normalizationChecker == null) {
                    normalizationChecker = new NormalizationChecker(true);
                    normalizationChecker.setDocumentLocator(this);
                    normalizationChecker.setErrorHandler(getErrorHandler());
                }
            } else {
                normalizationChecker = null;
            }
        } else {
            throw new SAXNotRecognizedException(key);
        }
    }

    /**
     * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
     */
    public Object getProperty(String key) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        throw new SAXNotRecognizedException(key);
    }

    /**
     * @see org.xml.sax.XMLReader#setProperty(java.lang.String,
     *      java.lang.Object)
     */
    public void setProperty(String key, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(key);
    }

    /**
     * @see org.xml.sax.XMLReader#setEntityResolver(org.xml.sax.EntityResolver)
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * @see org.xml.sax.XMLReader#getEntityResolver()
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * @see org.xml.sax.XMLReader#setDTDHandler(org.xml.sax.DTDHandler)
     */
    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    /**
     * @see org.xml.sax.XMLReader#getDTDHandler()
     */
    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    /**
     * @see org.xml.sax.XMLReader#setContentHandler(org.xml.sax.ContentHandler)
     */
    public void setContentHandler(ContentHandler ch) {
        pipelineLast.setContentHandler(ch);
    }

    /**
     * @see org.xml.sax.XMLReader#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return pipelineLast.getContentHandler();
    }

    /**
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler eh) {
        this.eh = eh;
        eef.setErrorHandler(eh);
        tif.setErrorHandler(eh);
        cedf.setErrorHandler(eh);
    }

    /**
     * @see org.xml.sax.XMLReader#getErrorHandler()
     */
    public ErrorHandler getErrorHandler() {
        return eh;
    }

    /**
     * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
     */
    public void parse(InputSource is) throws IOException, SAXException {
        alreadyWarnedAboutPrivateUseCharacters = false;
        reader = null;
        stream = null;
        systemId = null;
        publicId = null;
        encoding = null;
        reader = is.getCharacterStream();
        systemId = is.getSystemId();
        publicId = is.getPublicId();
        encoding = is.getEncoding();
        try {
            streamSetup(is);
            ch.setDocumentLocator(this);
            try {
                ch.startDocument();
                parse();
                tif.flushStack();
            } finally {
                ch.endDocument();
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (reader != null) {
                reader.close();
            }
            reader = null;
            stream = null;
        }
    }

    /**
     * @param is
     * @param swallowBom
     * @throws IOException
     * @throws SAXException
     */
    private void streamSetup(InputSource is) throws SAXException, IOException {
        boolean swallowBom = true;
        if (reader == null) {
            stream = is.getByteStream();
            if (stream == null) {
                throw new IllegalArgumentException(
                        "InputSource had neither a character stream nor a byt stream.");
            }
            if (encoding == null) {
                // sniff BOM
                if (!stream.markSupported()) {
                    stream = new BufferedInputStream(stream);
                }
                stream.mark(1);
                int b = stream.read();
                if (b == -1) {
                    throw new IOException("Premature end of file.");
                } else if (b == 0xFE) {
                    // first byte big endian
                    b = stream.read();
                    if (b == -1) {
                        throw new IOException("Premature end of file.");
                    } else if (b == 0xFF) {
                        swallowBom = false;
                        encoding = "UTF-16";
                        reader = draconianInputStreamReader("UTF-16BE", stream, false);
                    } else {
                        cannotDetermineEncoding();
                    }
                } else if (b == 0xFF) {
                    // first byte little endian
                    b = stream.read();
                    if (b == -1) {
                        throw new IOException("Premature end of file.");
                    } else if (b == 0xFE) {
                        swallowBom = false;
                        encoding = "UTF-16";
                        reader = draconianInputStreamReader("UTF-16LE", stream, false);
                    } else {
                        cannotDetermineEncoding();
                    }
                } else if (b == 0xEF) {
                    // first byte UTF-8
                    b = stream.read();
                    if (b == -1) {
                        throw new IOException("Premature end of file.");
                    } else if (b == 0xBB) {
                        b = stream.read();
                        if (b == -1) {
                            throw new IOException("Premature end of file.");
                        } else if (b == 0xBF) {
                            swallowBom = false;
                            encoding = "UTF-8";
                            reader = draconianInputStreamReader("UTF-8", stream, false);
                        } else {
                            cannotDetermineEncoding();
                        }
                    } else {
                        cannotDetermineEncoding();
                    }
                } else if (b < 0x80) {
                    // no BOM
                    swallowBom = false;
                    stream.reset();
                    reader = new NonBufferingAsciiInputStreamReader(stream);
                } else {
                    cannotDetermineEncoding();
                }
            } else {
                reader = draconianInputStreamReader(encoding, stream, false);
                if ("UTF-16BE".equalsIgnoreCase(encoding)
                        || "UTF-16LE".equalsIgnoreCase(encoding)
                        || "UTF-32BE".equalsIgnoreCase(encoding)
                        || "UTF-32LE".equalsIgnoreCase(encoding)) {
                    swallowBom = false;
                }
            }
        }
        if (swallowBom) {
            // won't happen if charecter encoding not determined yet
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            reader.mark(1);
            int c = reader.read();
            if (c != 0xFEFF) {
                reader.reset();
            }
        }
    }

    /**
     * @throws IOException
     *  
     */
    private void cannotDetermineEncoding() throws IOException {
        throw new IOException(
                "Unable to determine the character encoding of the document. No external encoding information was provided and the first byte was not an ASCII byte but did not constitute a part of the Byte Order Mark.");
    }

    /**
     *  
     */
    private Reader draconianInputStreamReader(String encoding,
            InputStream stream, boolean requireAsciiSuperset) throws SAXException {
        encoding = encoding.toUpperCase();
        try {
            Charset cs = Charset.forName(encoding);
            String canonName = cs.name();
            if (requireAsciiSuperset) {
                if (!EncodingInfo.isAsciiSuperset(canonName)) {
                    fatal("The encoding \u201C"
                            + encoding
                            + "\u201D is not an ASCII superset and, therefore, cannot be used in an internal encoding declaration.");
                }
            }
            if (canonName.startsWith("X-") || canonName.startsWith("x-")
                    || canonName.startsWith("Mac")) {
                if (encoding.startsWith("X-")) {
                    err(encoding
                            + " is not an IANA-registered encoding. (Charmod C022)");
                } else {
                    err(encoding
                            + "is not an IANA-registered encoding and did\u2019t start with \u201CX-\u201D. (Charmod C023)");
                }
            } else if (!canonName.equalsIgnoreCase(encoding)) {
                err(encoding
                        + " is not the preferred name of the character encoding in use. The preferred name is "
                        + canonName + ". (Charmod C024)");
            }
            if (EncodingInfo.isObscure(canonName)) {
                warn("Character encoding " + encoding + " is not widely supported. Better interoperability may be achieved by using UTF-8.");
            }
            CharsetDecoder decoder = cs.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return new InputStreamReader(stream, decoder);
        } catch (IllegalCharsetNameException e) {
            fatal("Illegal character encoding name: " + encoding);
        } catch (UnsupportedCharsetException e) {
            fatal("Unsupported character encoding: " + encoding);
        }
        return null; // keep the compiler happy
    }

    /**
     * @see org.xml.sax.XMLReader#parse(java.lang.String)
     */
    public void parse(String url) throws IOException, SAXException {
        // FIXME b0rked if no resolver
        parse(entityResolver.resolveEntity(url, null));
    }

    /**
     * @param string
     * @throws
     * @throws SAXException
     */
    void setEncoding(String enc) throws SAXException {
        if (enc == null) {
            if (encoding == null) {
                if (stream != null) {
                    // XXX should the parser default to US-ASCII instead?
                    fatal("Character encoding information not available.");
                }
            }
        } else {
            if (encoding == null) {
                encoding = enc;
                if (stream != null) {
                    reader = draconianInputStreamReader(encoding, stream, true);
                }
            } else {
                if (!encoding.equalsIgnoreCase(enc)) {
                    err("Internal character encoding information is inconsistent with external information or the BOM.");
                }
            }
        }
    }

    void setNonWhiteSpaceAllowed(boolean allow) {
        nonWhiteSpaceAllowed = allow;
    }

    /**
     * Returns the doctypeMode.
     * 
     * @return the doctypeMode
     */
    public int getDoctypeMode() {
        return doctypeMode;
    }

    /**
     * Sets the doctypeMode.
     * 
     * @param doctypeMode the doctypeMode to set
     */
    public void setDoctypeMode(int doctypeMode) {
        this.doctypeMode = doctypeMode;
    }

    /**
     * Returns the doctypeHandler.
     * 
     * @return the doctypeHandler
     */
    public DoctypeHandler getDoctypeHandler() {
        return doctypeHandler;
    }

    /**
     * Sets the doctypeHandler.
     * 
     * @param doctypeHandler the doctypeHandler to set
     */
    public void setDoctypeHandler(DoctypeHandler doctypeHandler) {
        this.doctypeHandler = doctypeHandler;
    }

    public void refireStart() throws SAXException {
        ch.setDocumentLocator(this);
        ch.startDocument();
    }
}
