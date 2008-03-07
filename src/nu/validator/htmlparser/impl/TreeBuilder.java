/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2007-2008 Mozilla Foundation
 * Portions of comments Copyright 2004-2008 Apple Computer, Inc., Mozilla 
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
 * comment are quotes from the WHATWG HTML 5 spec as of 27 June 2007 
 * amended as of June 28 2007.
 * That document came with this statement:
 * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
 * Opera Software ASA. You are granted a license to use, reproduce and 
 * create derivative works of this document."
 */

package nu.validator.htmlparser.impl;

import java.util.Arrays;

import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.common.DocumentMode;
import nu.validator.htmlparser.common.DocumentModeHandler;
import nu.validator.htmlparser.common.XmlViolationPolicy;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class TreeBuilder<T> implements TokenHandler {

    private enum InsertionMode {
        INITIAL, BEFORE_HTML, BEFORE_HEAD, IN_HEAD, IN_HEAD_NOSCRIPT, AFTER_HEAD, IN_BODY, IN_TABLE, IN_CAPTION, IN_COLUMN_GROUP, IN_TABLE_BODY, IN_ROW, IN_CELL, IN_SELECT, IN_SELECT_IN_TABLE, AFTER_BODY, IN_FRAMESET, AFTER_FRAMESET, AFTER_AFTER_BODY, AFTER_AFTER_FRAMESET
    }

    private class StackNode<S> {
        final String name;

        final S node;
        
        final boolean scoping;
        
        final boolean special;

        final boolean fosterParenting;
        
        boolean tainted = false;
        
        /**
         * @param name
         * @param node
         * @param scoping
         * @param special
         */
        StackNode(final String name, final S node, final boolean scoping, final boolean special, final boolean fosterParenting) {
            this.name = name;
            this.node = node;
            this.scoping = scoping;
            this.special = special;
            this.fosterParenting = fosterParenting;
        }

        /**
         * @param name
         * @param node
         */
        StackNode(final String name, final S node) {
            this.name = name;
            this.node = node;
            this.scoping = ("table" == name || "caption" == name || "td" == name || "th" == name || "button" == name || "marquee" == name || "object" == name);
            this.special = ("address" == name || "area" == name || "base" == name || "basefont" == name || "bgsound" == name || "blockquote" == name || "body" == name || "br" == name || "center" == name || "col" == name || "colgroup" == name || "dd" == name || "dir" == name || "div" == name || "dl" == name || "dt" == name || "embed" == name || "fieldset" == name || "form" == name || "frame" == name || "frameset" == name || "h1" == name || "h2" == name || "h3" == name || "h4" == name || "h5" == name || "h6" == name || "head" == name || "hr" == name || "iframe" == name || "image" == name || "img" == name || "input" == name || "isindex" == name || "li" == name || "link" == name || "listing" == name || "menu" == name || "meta" == name || "noembed" == name || "noframes" == name || "noscript" == name || "ol" == name || "optgroup" == name || "option" == name || "p" == name || "param" == name || "plaintext" == name || "pre" == name || "script" == name || "select" == name || "spacer" == name || "style" == name || "tbody" == name || "textarea" == name || "tfoot" == name || "thead" == name || "title" == name || "tr" == name || "ul" == name ||  "wbr" == name);
            this.fosterParenting = ("table" == name || "tbody" == name || "tfoot" == name || "thead" == name || "tr" == name);
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return name;
        }
    }
    
    private final static char[] ISINDEX_PROMPT = "This is a searchable index. Insert your search keywords here: ".toCharArray();

    private final static String[] HTML4_PUBLIC_IDS = {
        "-//W3C//DTD HTML 4.0 Frameset//EN",
        "-//W3C//DTD HTML 4.0 Transitional//EN",
        "-//W3C//DTD HTML 4.0//EN",
        "-//W3C//DTD HTML 4.01 Frameset//EN",
        "-//W3C//DTD HTML 4.01 Transitional//EN",
        "-//W3C//DTD HTML 4.01//EN"
    };
    
    private final static String[] QUIRKY_PUBLIC_IDS = {
            "+//silmaril//dtd html pro v0r11 19970101//en",
            "-//advasoft ltd//dtd html 3.0 aswedit + extensions//en",
            "-//as//dtd html 3.0 aswedit + extensions//en",
            "-//ietf//dtd html 2.0 level 1//en",
            "-//ietf//dtd html 2.0 level 2//en",
            "-//ietf//dtd html 2.0 strict level 1//en",
            "-//ietf//dtd html 2.0 strict level 2//en",
            "-//ietf//dtd html 2.0 strict//en", "-//ietf//dtd html 2.0//en",
            "-//ietf//dtd html 2.1e//en", "-//ietf//dtd html 3.0//en",
            "-//ietf//dtd html 3.0//en//", "-//ietf//dtd html 3.2 final//en",
            "-//ietf//dtd html 3.2//en", "-//ietf//dtd html 3//en",
            "-//ietf//dtd html level 0//en",
            "-//ietf//dtd html level 0//en//2.0",
            "-//ietf//dtd html level 1//en",
            "-//ietf//dtd html level 1//en//2.0",
            "-//ietf//dtd html level 2//en",
            "-//ietf//dtd html level 2//en//2.0",
            "-//ietf//dtd html level 3//en",
            "-//ietf//dtd html level 3//en//3.0",
            "-//ietf//dtd html strict level 0//en",
            "-//ietf//dtd html strict level 0//en//2.0",
            "-//ietf//dtd html strict level 1//en",
            "-//ietf//dtd html strict level 1//en//2.0",
            "-//ietf//dtd html strict level 2//en",
            "-//ietf//dtd html strict level 2//en//2.0",
            "-//ietf//dtd html strict level 3//en",
            "-//ietf//dtd html strict level 3//en//3.0",
            "-//ietf//dtd html strict//en",
            "-//ietf//dtd html strict//en//2.0",
            "-//ietf//dtd html strict//en//3.0", "-//ietf//dtd html//en",
            "-//ietf//dtd html//en//2.0", "-//ietf//dtd html//en//3.0",
            "-//metrius//dtd metrius presentational//en",
            "-//microsoft//dtd internet explorer 2.0 html strict//en",
            "-//microsoft//dtd internet explorer 2.0 html//en",
            "-//microsoft//dtd internet explorer 2.0 tables//en",
            "-//microsoft//dtd internet explorer 3.0 html strict//en",
            "-//microsoft//dtd internet explorer 3.0 html//en",
            "-//microsoft//dtd internet explorer 3.0 tables//en",
            "-//netscape comm. corp.//dtd html//en",
            "-//netscape comm. corp.//dtd strict html//en",
            "-//o'reilly and associates//dtd html 2.0//en",
            "-//o'reilly and associates//dtd html extended 1.0//en",
            "-//spyglass//dtd html 2.0 extended//en",
            "-//sq//dtd html 2.0 hotmetal + extensions//en",
            "-//sun microsystems corp.//dtd hotjava html//en",
            "-//sun microsystems corp.//dtd hotjava strict html//en",
            "-//w3c//dtd html 3 1995-03-24//en",
            "-//w3c//dtd html 3.2 draft//en", "-//w3c//dtd html 3.2 final//en",
            "-//w3c//dtd html 3.2//en", "-//w3c//dtd html 3.2s draft//en",
            "-//w3c//dtd html 4.0 frameset//en",
            "-//w3c//dtd html 4.0 transitional//en",
            "-//w3c//dtd html experimental 19960712//en",
            "-//w3c//dtd html experimental 970421//en",
            "-//w3c//dtd w3 html//en", "-//w3o//dtd w3 html 3.0//en",
            "-//w3o//dtd w3 html 3.0//en//",
            "-//w3o//dtd w3 html strict 3.0//en//",
            "-//webtechs//dtd mozilla html 2.0//en",
            "-//webtechs//dtd mozilla html//en",
            "-/w3c/dtd html 4.0 transitional/en", "html" };

    private static final int NOT_FOUND_ON_STACK = Integer.MAX_VALUE;
    
    private final StackNode<T> MARKER = new StackNode<T>(null, null);

    private final boolean nonConformingAndStreaming;

    private final boolean conformingAndStreaming;
    
    private final boolean coalescingText;   
    
    private InsertionMode mode = InsertionMode.INITIAL;

    protected Tokenizer tokenizer;

    private ErrorHandler errorHandler;

    private DocumentModeHandler documentModeHandler;

    private DoctypeExpectation doctypeExpectation = DoctypeExpectation.HTML;

    private int cdataOrRcdataTimesToPop;

    private boolean scriptingEnabled = false;
    
    private boolean needToDropLF;

    private boolean wantingComments;

    private String context;
    
    private StackNode<T>[] stack;
    
    private int currentPtr = -1;

    private StackNode<T>[] listOfActiveFormattingElements;

    private int listPtr = -1;
    
    private T formPointer;

    private T headPointer;

    private boolean reportingDoctype = true;
    
    private char[] charBuffer;
    
    private int charBufferLen = 0;

    protected TreeBuilder(XmlViolationPolicy streamabilityViolationPolicy, boolean coalescingText) {
        this.conformingAndStreaming = streamabilityViolationPolicy == XmlViolationPolicy.FATAL;
        this.nonConformingAndStreaming = streamabilityViolationPolicy == XmlViolationPolicy.ALTER_INFOSET;
        this.coalescingText = coalescingText;
        if (coalescingText) {
            charBuffer = new char[1024];        
        }
    }
    
    /**
     * Reports an condition that would make the infoset incompatible with XML
     * 1.0 as fatal.
     * 
     * @throws SAXException
     * @throws SAXParseException
     */
    protected final void fatal() throws SAXException {
        SAXParseException spe = new SAXParseException("Last error required non-streamable recovery.", tokenizer);
        if (errorHandler != null) {
            errorHandler.fatalError(spe);
        }
        throw spe;
    }
    protected final void fatal(Exception e) throws SAXException {
        SAXParseException spe = new SAXParseException(e.getMessage(), tokenizer, e);;
        if (errorHandler != null) {
            errorHandler.fatalError(spe);
        }
        throw spe;
    }
    
    /**
     * Reports a Parse Error.
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected final void err(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.error(spe);
    }

    /**
     * Reports a warning
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected final void warn(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.warning(spe);
    }

    public final void start(Tokenizer self) throws SAXException {
        tokenizer = self;
        stack  = new StackNode[64];
        listOfActiveFormattingElements  = new StackNode[64];
        needToDropLF = false;
        cdataOrRcdataTimesToPop = 0;
        currentPtr = -1;
        formPointer = null;
        wantingComments = wantsComments();
        start(context != null);
        if (context == null) {
            mode = InsertionMode.INITIAL;
        } else {
            T elt = createHtmlElementSetAsRoot(tokenizer.newAttributes());
            StackNode<T> node = new StackNode<T>("html", elt);
            currentPtr++;
            stack[currentPtr] = node;
            resetTheInsertionMode();
            if ("title" == context || "textarea" == context) {
                tokenizer.setContentModelFlag(ContentModelFlag.RCDATA, context);
            } else if ("style" == context || "script" == context || "xmp" == context || "iframe" == context || "noembed" == context || "noframes" == context || (scriptingEnabled && "noscript" == context)) {
                tokenizer.setContentModelFlag(ContentModelFlag.CDATA, context);                
            } else if ("plaintext" == context) {
                tokenizer.setContentModelFlag(ContentModelFlag.PLAINTEXT, context);                       
            } else {
                tokenizer.setContentModelFlag(ContentModelFlag.PCDATA, context);                                       
            }
        }
    }

    public final void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean correct) throws SAXException {
        needToDropLF = false;
        switch (mode) {
            case INITIAL:
                /*
                 * A DOCTYPE token If the DOCTYPE token's name does not
                 * case-insensitively match the string "HTML", or if the token's
                 * public identifier is not missing, or if the token's system
                 * identifier is not missing, then there is a parse error.
                 * Conformance checkers may, instead of reporting this error,
                 * switch to a conformance checking mode for another language
                 * (e.g. based on the DOCTYPE token a conformance checker could
                 * recognise that the document is an HTML4-era document, and
                 * defer to an HTML4 conformance checker.)
                 * 
                 * Append a DocumentType node to the Document node, with the
                 * name attribute set to the name given in the DOCTYPE token;
                 * the publicId attribute set to the public identifier given in
                 * the DOCTYPE token, or the empty string if the public
                 * identifier was not set; the systemId attribute set to the
                 * system identifier given in the DOCTYPE token, or the empty
                 * string if the system identifier was not set; and the other
                 * attributes specific to DocumentType objects set to null and
                 * empty lists as appropriate. Associate the DocumentType node
                 * with the Document object so that it is returned as the value
                 * of the doctype attribute of the Document object.
                 */
                if (reportingDoctype ) {
                appendDoctypeToDocument(name, publicIdentifier == null ? ""
                        : publicIdentifier, systemIdentifier == null ? ""
                        : systemIdentifier);
                }
                /*
                 * Then, if the DOCTYPE token matches one of the conditions in
                 * the following list, then set the document to quirks mode:
                 * 
                 * Otherwise, if the DOCTYPE token matches one of the conditions
                 * in the following list, then set the document to limited
                 * quirks mode: + The public identifier is set to: "-//W3C//DTD
                 * XHTML 1.0 Frameset//EN" + The public identifier is set to:
                 * "-//W3C//DTD XHTML 1.0 Transitional//EN" + The system
                 * identifier is not missing and the public identifier is set
                 * to: "-//W3C//DTD HTML 4.01 Frameset//EN" + The system
                 * identifier is not missing and the public identifier is set
                 * to: "-//W3C//DTD HTML 4.01 Transitional//EN"
                 * 
                 * The name, system identifier, and public identifier strings
                 * must be compared to the values given in the lists above in a
                 * case-insensitive manner.
                 */
                String publicIdentifierLC = toAsciiLowerCase(publicIdentifier);
                String systemIdentifierLC = toAsciiLowerCase(systemIdentifier);
                switch (doctypeExpectation) {
                    case HTML:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentModeInternal(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            if (!(publicIdentifier == null && systemIdentifier == null)) {
                                err("Legacy doctype.");
                            }
                            documentModeInternal(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                    case HTML401_STRICT:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentModeInternal(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("The doctype was not the HTML 4.01 Strict doctype.");
                            }
                            documentModeInternal(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case HTML401_TRANSITIONAL:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)
                                    && systemIdentifier != null) {
                                if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("The doctype was not a non-quirky HTML 4.01 Transitional doctype.");
                            }
                            documentModeInternal(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            err("The doctype was not the HTML 4.01 Transitional doctype.");
                            documentModeInternal(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case AUTO:
                        boolean html4 = isHtml4Doctype(publicIdentifier);
                        if (html4) {
                            tokenizer.turnOnAdditionalHtml4Errors();
                        }
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("Almost standards mode doctype.");
                            }
                            documentModeInternal(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        } else {
                            if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                if (!(publicIdentifier == null && systemIdentifier == null)) {
                                    err("Legacy doctype.");
                                }
                            }
                            documentModeInternal(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        }
                        break;
                    case NO_DOCTYPE_ERRORS:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            documentModeInternal(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            documentModeInternal(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                }

                /*
                 * 
                 * Then, switch to the root element mode of the tree
                 * construction stage.
                 * 
                 * 
                 */
                mode = InsertionMode.BEFORE_HTML;
                return;
            default:
                /*
                 * A DOCTYPE token Parse error.
                 */
                err("Stray doctype.");
                /*
                 * Ignore the token.
                 */
                return;
        }
    }

    private boolean isHtml4Doctype(String publicIdentifier) {
        if (publicIdentifier != null
                && (Arrays.binarySearch(HTML4_PUBLIC_IDS, publicIdentifier) > -1)) {
            return true;
        }
        return false;
    }

    public final void comment(char[] buf, int length) throws SAXException {
        needToDropLF = false;
        if (wantingComments) {
            switch (mode) {
                case INITIAL:
                case BEFORE_HTML:
                case AFTER_AFTER_BODY:
                case AFTER_AFTER_FRAMESET:
                    /*
                     * A comment token Append a Comment node to the Document
                     * object with the data attribute set to the data given in
                     * the comment token.
                     */
                    appendCommentToDocument(buf, 0, length);
                    return;
                case AFTER_BODY:
                    /*
                     * * A comment token Append a Comment node to the first
                     * element in the stack of open elements (the html element),
                     * with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    flushCharacters();
                    appendComment(stack[0].node, buf, 0, length);
                    return;
                default:
                    /*
                     * * A comment token Append a Comment node to the current
                     * node with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    flushCharacters();
                    appendComment(stack[currentPtr].node, buf, 0, length);
                    return;
            }
        }
    }

    /**
     * @see nu.validator.htmlparser.impl.TokenHandler#characters(char[], int, int)
     */
    public final void characters(char[] buf, int start, int length)
            throws SAXException {
        if (needToDropLF) {
            if (buf[start] == '\n') {
                start++;
                length--;
                if (length == 0) {
                    return;
                }
            }
            needToDropLF = false;
        } else if (cdataOrRcdataTimesToPop > 0) {
            accumulateCharacters(buf, start, length);
            return;
        }

        // optimize the most common case
        if (mode == InsertionMode.IN_BODY || mode == InsertionMode.IN_CELL
                || mode == InsertionMode.IN_CAPTION) {
            reconstructTheActiveFormattingElements();
            accumulateCharacters(buf, start, length);
            return;
        }

        int end = start + length;
        loop: for (int i = start; i < end; i++) {
            switch (buf[i]) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * A character token that is one of one of U+0009 CHARACTER
                     * TABULATION, U+000A LINE FEED (LF), U+000B LINE
                     * TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
                     */
                    switch (mode) {
                        case INITIAL:
                        case BEFORE_HTML:
                        case BEFORE_HEAD:
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case IN_TABLE:
                        case IN_TABLE_BODY:
                        case IN_ROW:
                            if (isTainted()) {
                                if (start < i) {
                                    accumulateCharacters(buf, start, i - start);
                                }
                                reconstructTheActiveFormattingElements();
                                appendCharMayFoster(buf, i);
                                start = i + 1;
                            }
                            continue;
                        case IN_HEAD:
                        case IN_HEAD_NOSCRIPT:
                        case AFTER_HEAD:
                        case IN_COLUMN_GROUP:
                        case IN_FRAMESET:
                        case AFTER_FRAMESET:
                            /*
                             * Append the character to the current node.
                             */
                            continue;
                        case IN_BODY:
                        case IN_CELL:
                        case IN_CAPTION:
                            // XXX is this dead code?
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }

                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            break loop;
                        case IN_SELECT:
                        case IN_SELECT_IN_TABLE:
                            break loop;
                        case AFTER_BODY:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            // XXX bug?
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            continue;
                        case AFTER_AFTER_BODY:
                        case AFTER_AFTER_FRAMESET:
                            if (conformingAndStreaming) {
                                // XXX why? is this a bug?
                                return;
                            }
                            if (start < i) {
                                accumulateCharacters(buf, start, i - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            // XXX bug?
                            reconstructTheActiveFormattingElements();
                            /*
                             * Append the token's character to the current node.
                             */
                            continue;
                    }
                default:
                    /*
                     * A character token that is not one of one of U+0009
                     * CHARACTER TABULATION, U+000A LINE FEED (LF), U+000B LINE
                     * TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
                     */
                    switch (mode) {
                        case INITIAL:
                            /*
                             * Parse error.
                             */
                            if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                                err("Non-space characters found without seeing a doctype first.");
                            }
                            /*
                             * 
                             * Set the document to quirks mode.
                             */
                            documentModeInternal(DocumentMode.QUIRKS_MODE, null, null,
                                    false);
                            /*
                             * Then, switch to the root element mode of the
                             * tree construction stage
                             */
                            mode = InsertionMode.BEFORE_HTML;
                            /*
                             * and reprocess the current token.
                             * 
                             * 
                             */
                            i--;
                            continue;
                        case BEFORE_HTML:
                            /*
                             * Create an HTMLElement node with the tag name
                             * html, in the HTML namespace. Append it to the
                             * Document object.
                             */
                            appendHtmlElementToDocumentAndPush();
                            /* Switch to the main mode */
                            mode = InsertionMode.BEFORE_HEAD;
                            /*
                             * reprocess the current token.
                             * 
                             */
                            i--;
                            continue;
                        case BEFORE_HEAD:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * /*Act as if a start tag token with the tag name
                             * "head" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                            mode = InsertionMode.IN_HEAD;
                            /*
                             * then reprocess the current token.
                             * 
                             * This will result in an empty head element being
                             * generated, with the current token being
                             * reprocessed in the "after head" insertion mode.
                             */
                            i--;
                            continue;
                        case IN_HEAD:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if an end tag token with the tag name
                             * "head" had been seen,
                             */
                            pop();
                            mode = InsertionMode.AFTER_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_HEAD_NOSCRIPT:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error. Act as if an end tag with the tag
                             * name "noscript" had been seen
                             */
                            err("Non-space character inside \u201Cnoscript\u201D inside \u201Chead\u201D.");
                            pop();
                            mode = InsertionMode.IN_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case AFTER_HEAD:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if a start tag token with the tag name
                             * "body" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushBodyElement();
                            mode = InsertionMode.IN_BODY;
                            /*
                             * and then reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_BODY:
                        case IN_CELL:
                        case IN_CAPTION:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            break loop;
                        case IN_TABLE:
                        case IN_TABLE_BODY:
                        case IN_ROW:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                            }
                            reconstructTheActiveFormattingElements();
                            appendCharMayFoster(buf, i);
                            start = i + 1;
                            continue;
                        case IN_COLUMN_GROUP:
                            /*
                             * Act as if an end tag with the tag name "colgroup"
                             * had been seen, and then, if that token wasn't
                             * ignored, reprocess the current token.
                             */
                            if (currentPtr == 0) {
                                err("Non-space in \u201Ccolgroup\u201D when parsing fragment.");
                                continue;
                            }
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            i--;
                            continue;
                        case IN_SELECT:
                        case IN_SELECT_IN_TABLE:
                            break loop;
                        case AFTER_BODY:
                            err("Non-space character after body.");
                            if (conformingAndStreaming) {
                                fatal();
                            }
                            mode = InsertionMode.IN_BODY;
                            i--;
                            continue;
                        case IN_FRAMESET:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error.
                             */
                            err("Non-space in \u201Cframeset\u201D.");
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case AFTER_FRAMESET:
                            if (start < i) {
                                accumulateCharacters(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error.
                             */
                            err("Non-space after \u201Cframeset\u201D.");
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case AFTER_AFTER_BODY:
                            /*
                             * Parse error.
                             */
                            err("Non-space character in page trailer.");
                            if (conformingAndStreaming) {
                                fatal();
                            }
                            /*
                             * Switch back to the main mode and reprocess the
                             * token.
                             */
                            mode = InsertionMode.IN_BODY;
                            i--;
                            continue;
                        case AFTER_AFTER_FRAMESET:
                            /*
                             * Parse error.
                             */
                            err("Non-space character in page trailer.");
                            if (conformingAndStreaming) {
                                fatal();
                            }
                            /*
                             * Switch back to the main mode and reprocess the
                             * token.
                             */
                            mode = InsertionMode.IN_FRAMESET;
                            i--;
                            continue;
                    }
            }
        }
        if (start < end) {
            accumulateCharacters(buf, start, end - start);
        }
    }

    public final void eof() throws SAXException {
        try {
            flushCharacters();
            eofloop: for (;;) {
                switch (mode) {
                    case INITIAL:
                        /*
                         * Parse error.
                         */
                        if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                            err("End of file seen without seeing a doctype first.");
                        }
                        /*
                         * 
                         * Set the document to quirks mode.
                         */
                        documentModeInternal(DocumentMode.QUIRKS_MODE, null, null,
                                false);
                        /*
                         * Then, switch to the root element mode of the tree
                         * construction stage
                         */
                        mode = InsertionMode.BEFORE_HTML;
                        /*
                         * and reprocess the current token.
                         */
                        continue;
                    case BEFORE_HTML:
                        /*
                         * Create an HTMLElement node with the tag name html, in
                         * the HTML namespace. Append it to the Document object.
                         */
                        appendHtmlElementToDocumentAndPush();
                        /* Switch to the main mode */
                        mode = InsertionMode.BEFORE_HEAD;
                        /*
                         * reprocess the current token.
                         */
                        continue;
                    case BEFORE_HEAD:
                        appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                        mode = InsertionMode.IN_HEAD;
                        continue;
                    case IN_HEAD:
                        if (currentPtr > 1) {
                            err("End of file seen and there were open elements.");
                        }
                        while (currentPtr > 0) {
                            pop();
                        }
                        mode = InsertionMode.AFTER_HEAD;
                        continue;
                    case IN_HEAD_NOSCRIPT:
                        err("End of file seen and there were open elements.");
                        while (currentPtr > 1) {
                            pop();
                        }
                        mode = InsertionMode.IN_HEAD;
                        continue;
                    case AFTER_HEAD:
                        appendToCurrentNodeAndPushBodyElement();
                        mode = InsertionMode.IN_BODY;
                        continue;
                    case IN_BODY:
                    case IN_TABLE:
                    case IN_CAPTION:
                    case IN_COLUMN_GROUP:
                    case IN_TABLE_BODY:
                    case IN_ROW:
                    case IN_CELL:
                    case IN_SELECT:
                    case IN_SELECT_IN_TABLE:
                        /*
                         * Generate implied end tags.
                         */
                        generateImpliedEndTags();
                        /*
                         * If there are more than two nodes on the stack of open
                         * elements,
                         */
                        if (currentPtr > 1) {
                            err("End of file seen and there were open elements.");
                        } else if (currentPtr == 1 && stack[1].name != "body") {
                            /*
                             * or if there are two nodes but the second node is
                             * not a body node, this is a parse error.
                             */
                            err("End of file seen and there were open elements.");
                        }
                        if (context != null) {
                            if (currentPtr > 0 && stack[1].name != "body") {
                                /*
                                 * Otherwise, if the parser was originally
                                 * created as part of the HTML fragment parsing
                                 * algorithm, and there's more than one element
                                 * in the stack of open elements, and the second
                                 * node on the stack of open elements is not a
                                 * body node, then this is a parse error.
                                 * (fragment case)
                                 */
                                err("End of file seen and there were open elements.");
                            }
                        }

                        /* Stop parsing. */
                        if (context == null) {
                            bodyClosed(stack[1].node);
                        }
                        mode = InsertionMode.AFTER_BODY;
                        continue;
                    /*
                     * This fails because it doesn't imply HEAD and BODY tags.
                     * We should probably expand out the insertion modes and
                     * merge them with phases and then put the three things here
                     * into each insertion mode instead of trying to factor them
                     * out so carefully.
                     * 
                     */
                    case IN_FRAMESET:
                        err("End of file seen and there were open elements.");
                        break eofloop;                        
                    case AFTER_BODY:
                    case AFTER_FRAMESET:
                        if (context == null) {
                            htmlClosed(stack[0].node);
                        }
                    case AFTER_AFTER_BODY:
                    case AFTER_AFTER_FRAMESET:
                        break eofloop;                        
                }
            }
        } finally {
            // XXX close elts for SAX
            /* Stop parsing. */
            stack = null;
            listOfActiveFormattingElements = null;
            end();
        }
    }

    public final void startTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        for (;;) {
            switch (mode) {
                case IN_TABLE_BODY:
                    if ("tr" == name) {
                        clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_ROW;
                        return;
                    } else if ("td" == name || "th" == name) {
                        err("\u201C" + name + "\u201D start tag in table body.");
                        clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                        appendToCurrentNodeAndPushElement("tr",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        mode = InsertionMode.IN_ROW;
                        continue;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name) {
                        int eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                        if (eltPos == 0) {
                            err("Stray \u201C" + name + "\u201D start tag.");
                            return;
                        } else {
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            continue;
                        }
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_ROW:
                    if ("td" == name || "th" == name) {
                        clearStackBackTo(findLastOrRoot("tr"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_CELL;
                        insertMarker();
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name
                            || "tr" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert context != null;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE_BODY;
                        continue;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("caption" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        insertMarker();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_CAPTION;
                        return;
                    } else if ("colgroup" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_COLUMN_GROUP;
                        return;
                    } else if ("col" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement("colgroup",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        mode = InsertionMode.IN_COLUMN_GROUP;
                        continue;
                    } else if ("tbody" == name || "tfoot" == name
                            || "thead" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_TABLE_BODY;
                        return;
                    } else if ("td" == name || "tr" == name || "th" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement("tbody",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        mode = InsertionMode.IN_TABLE_BODY;
                        continue;
                    } else if ("table" == name) {
                        err("Start tag for \u201Ctable\u201D seen but the previous \u201Ctable\u201D is still open.");
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert context != null;
                            return;
                        }
                        generateImpliedEndTags();
                        // XXX is the next if dead code?
                        if (!isCurrent("table")) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        resetTheInsertionMode();
                        continue;
                    } else if (("script" == name || "style" == name) && !isTainted()) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("input" == name && !isTainted() && equalsIgnoreAsciiCase("hidden", attributes.getValue("", "type"))) {
                        appendVoidElementToCurrent(name, attributes, formPointer);
                        return;
                    } else {
                        err("Start tag \u201C" + name
                                + "\u201D seen in \u201Ctable\u201D.");
                        // fall through to IN_BODY
                    }
                case IN_CAPTION:
                    if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "td" == name || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        err("Stray \u201C" + name
                                + "\u201D start tag in \u201Ccaption\u201D.");
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        mode = InsertionMode.IN_TABLE;
                        continue;
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "td" == name || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        int eltPos = findLastInTableScopeTdTh();
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("No cell to close.");
                            return;
                        } else {
                            closeTheCell(eltPos);
                            continue;
                        }
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_BODY:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("base" == name || "link" == name || "meta" == name
                            || "style" == name || "script" == name || "title" == name) {
                        // Fall through to IN_HEAD
                    } else if ("body" == name) {
                        err("\u201Cbody\u201D start tag found but the \u201Cbody\u201D element is already open.");
                        addAttributesToBody(attributes);
                        return;
                    } else if ("p" == name || "div" == name || "h1" == name
                            || "h2" == name || "h3" == name || "h4" == name
                            || "h5" == name || "h6" == name
                            || "blockquote" == name || "ol" == name
                            || "ul" == name || "dl" == name
                            || "fieldset" == name || "address" == name
                            || "menu" == name || "center" == name
                            || "dir" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        return;
                    } else if ("pre" == name || "listing" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        needToDropLF = true;
                        return;
                    } else if ("form" == name) {
                        if (formPointer != null) {
                            err("Saw a \u201Cform\u201D start tag, but there was already an active \u201Cform\u201D element.");
                            return;
                        } else {
                            implicitlyCloseP();
                            appendToCurrentNodeAndPushFormElementMayFoster(attributes);
                            return;
                        }
                    } else if ("li" == name) {
                        implicitlyCloseP();
                        int eltPos = findLiToPop();
                        if (eltPos < currentPtr) {
                            err("A \u201Cli\u201D start tag was seen but the previous \u201Cli\u201D element had open children.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        return;
                    } else if ("dd" == name || "dt" == name) {
                        implicitlyCloseP();
                        int eltPos = findDdOrDtToPop();
                        if (eltPos < currentPtr) {
                            err("A definition list item start tag was seen but the previous definition list item element had open children.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        return;
                    } else if ("plaintext" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        tokenizer.setContentModelFlag(
                                ContentModelFlag.PLAINTEXT, name);
                        return;
                    } else if ("a" == name) {
                        int activeAPos = findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker("a");
                        if (activeAPos != -1) {
                            err("An \u201Ca\u201D start tag seen with already an active \u201Ca\u201D element.");
                            StackNode<T> activeA = listOfActiveFormattingElements[activeAPos];
                            adoptionAgencyEndTag("a");
                            removeFromStack(activeA);
                            activeAPos = findInListOfActiveFormattingElements(activeA);
                            if (activeAPos != -1) {
                                removeFromListOfActiveFormattingElements(activeAPos);
                            }
                        }
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElementMayFoster(name,
                                attributes);
                        return;
                    } else if ("i" == name || "b" == name || "em" == name
                            || "strong" == name || "font" == name
                            || "big" == name || "s" == name || "small" == name
                            || "strike" == name || "tt" == name || "u" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElementMayFoster(name,
                                attributes);
                        return;
                    } else if ("nobr" == name) {
                        reconstructTheActiveFormattingElements();
                        if (NOT_FOUND_ON_STACK != findLastInScope("nobr")) {
                            err("\u201Cnobr\u201D start tag seen when there was an open \u201Cnobr\u201D element in scope.");
                            adoptionAgencyEndTag("nobr");
                        }
                        appendToCurrentNodeAndPushFormattingElementMayFoster(name,
                                attributes);
                        return;
                    } else if ("button" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            err("\u201Cbutton\u201D start tag seen when there was an open \u201Cbutton\u201D element in scope.");
                            generateImpliedEndTags();
                            if (!isCurrent("button")) {
                                err("There was an open \u201Cbutton\u201D element in scope with unclosed children.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                            clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                            continue;
                        } else {
                            reconstructTheActiveFormattingElements();
                            appendToCurrentNodeAndPushElementMayFoster(name, attributes, formPointer);
                            insertMarker();
                            return;
                        }
                    } else if ("object" == name || "marquee" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        insertMarker();
                        return;
                    } else if ("xmp" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("table" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        mode = InsertionMode.IN_TABLE;
                        return;
                    } else if ("br" == name || "img" == name || "embed" == name
                            || "param" == name || "area" == name
                            || "basefont" == name || "bgsound" == name
                            || "spacer" == name || "wbr" == name) {
                        reconstructTheActiveFormattingElements();
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else if ("hr" == name) {
                        implicitlyCloseP();
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else if ("image" == name) {
                        err("Saw a start tag \u201Cimage\u201D.");
                        name = "img";
                        continue;
                    } else if ("input" == name) {
                        reconstructTheActiveFormattingElements();
                        appendVoidElementToCurrentMayFoster(name, attributes, formPointer);
                        return;
                    } else if ("isindex" == name) {
                        err("\u201Cisindex\u201D seen.");
                        if (formPointer != null) {
                            return;
                        }
                        implicitlyCloseP();
                        AttributesImpl formAttrs = tokenizer.newAttributes();
                        int actionIndex = attributes.getIndex("action");
                        if (actionIndex > -1) {
                            formAttrs.addAttribute("action",
                                    attributes.getValue(actionIndex));
                        }
                        appendToCurrentNodeAndPushFormElementMayFoster(formAttrs);
                        appendVoidElementToCurrentMayFoster("hr", EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElementMayFoster("p",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElementMayFoster("label",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        int promptIndex = attributes.getIndex("prompt");
                        if (promptIndex > -1) {
                            char[] prompt = attributes.getValue(promptIndex).toCharArray();
                            appendCharacters(stack[currentPtr].node, prompt,
                                    0, prompt.length);
                        } else {
                            // XXX localization
                            appendCharacters(stack[currentPtr].node, ISINDEX_PROMPT,
                                    0, ISINDEX_PROMPT.length);
                        }
                        AttributesImpl inputAttributes = tokenizer.newAttributes();
                        inputAttributes.addAttribute("name", "isindex");
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attributeQName = attributes.getQName(i);
                            if (!("name".equals(attributeQName)
                                    || "action".equals(attributeQName) || "prompt".equals(attributeQName))) {
                                inputAttributes.addAttribute(attributeQName,
                                        attributes.getValue(i));
                            }
                        }
                        appendVoidElementToCurrentMayFoster("input", inputAttributes, formPointer);
                        // XXX localization
                        pop(); // label
                        pop(); // p
                        appendVoidElementToCurrentMayFoster("hr", EmptyAttributes.EMPTY_ATTRIBUTES);
                        pop(); // form
                        return;
                    } else if ("textarea" == name) {
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes, formPointer);
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        cdataOrRcdataTimesToPop = 1;
                        needToDropLF = true;
                        return;
                    } else if ("iframe" == name || "noembed" == name
                            || "noframes" == name
                            || ("noscript" == name && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("select" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElementMayFoster(name,
                                attributes, formPointer);
                        switch (mode) {
                            case IN_TABLE:
                            case IN_CAPTION:
                            case IN_COLUMN_GROUP:
                            case IN_TABLE_BODY:
                            case IN_ROW:
                            case IN_CELL:
                                mode = InsertionMode.IN_SELECT_IN_TABLE;
                                break;
                            default:
                                mode = InsertionMode.IN_SELECT;
                                break;
                        }
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "frame" == name
                            || "frameset" == name || "head" == name
                            || "option" == name || "optgroup" == name
                            || "tbody" == name || "td" == name
                            || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        err("Stray start tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        return;
                    }
                case IN_HEAD:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("base" == name) {
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else if ("meta" == name || "link" == name) {
                        // Fall through to IN_HEAD_NOSCRIPT
                    } else if ("title" == name) {
                        appendToCurrentNodeAndPushElementMayFoster(name,
                                attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else if ("noscript" == name) {
                        if (scriptingEnabled) {
                            appendToCurrentNodeAndPushElement(name, attributes);
                            cdataOrRcdataTimesToPop = 1;
                            tokenizer.setContentModelFlag(
                                    ContentModelFlag.CDATA, name);
                        } else {
                            appendToCurrentNodeAndPushElementMayFoster(name,
                                    attributes);
                            mode = InsertionMode.IN_HEAD_NOSCRIPT;
                        }
                        return;
                    } else if ("script" == name || "style" == name) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElementMayFoster(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("head" == name) {
                        /* Parse error. */
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        /* Ignore the token. */
                        return;
                    } else {
                        pop();
                        mode = InsertionMode.AFTER_HEAD;
                        continue;
                    }
                case IN_HEAD_NOSCRIPT:
                    // XXX did Hixie really mean to omit "base" here?
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("link" == name) {
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else if ("meta" == name) {
                        errIfInconsistentCharset(attributes);
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else if ("style" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("head" == name) {
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        return;
                    } else if ("noscript" == name) {
                        err("Start tag for \u201Cnoscript\u201D seen when \u201Cnoscript\u201D was already open.");
                        return;
                    } else {
                        err("Bad start tag in \u201Cnoscript\u201D in \u201Chead\u201D.");
                        pop();
                        mode = InsertionMode.IN_HEAD;
                        continue;
                    }
                case IN_COLUMN_GROUP:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("col" == name) {
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else {
                        if (currentPtr == 0) {
                            assert context != null;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        pop();
                        mode = InsertionMode.IN_TABLE;
                        continue;
                    }
                case IN_SELECT_IN_TABLE:
                    if ("caption" == name || "table" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name
                            || "tr" == name || "td" == name || "th" == name) {
                        err("\u201C" + name + "\u201D start tag with \u201Cselect\u201D open.");
                        endSelect();
                        continue;
                    } else {
                        // fall through to IN_SELECT
                    }
                case IN_SELECT:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("option" == name) {
                        if (isCurrent("option")) {
                            pop();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("optgroup" == name) {
                        if (isCurrent("option")) {
                            pop();
                        }
                        if (isCurrent("optgroup")) {
                            pop();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("select" == name) {
                        err("\u201Cselect\u201D start tag where end tag expected.");
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert context != null;
                            err("No \u201Cselect\u201D in table scope.");
                            return;
                        } else {
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                            resetTheInsertionMode();
                            return;
                        }
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        return;
                    }
                case AFTER_BODY:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        if (conformingAndStreaming) {
                            fatal();
                        }
                        mode = InsertionMode.IN_BODY;
                        continue;
                    }
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("frame" == name) {
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        return;
                    } else {
                        // fall through to AFTER_FRAMESET
                    }
                case AFTER_FRAMESET:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("noframes" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        return;
                    }
                case INITIAL:
                    /*
                     * Parse error.
                     */
                    if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                        err("Start tag seen without seeing a doctype first.");
                    }
                    /*
                     * 
                     * Set the document to quirks mode.
                     */
                    documentModeInternal(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element mode of the tree
                     * construction stage
                     */
                    mode = InsertionMode.BEFORE_HTML;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case BEFORE_HTML:
                    // optimize error check and streaming SAX by hoisting
                    // "html" handling here.
                    if ("html" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendHtmlElementToDocumentAndPush();
                        } else {
                            appendHtmlElementToDocumentAndPush(attributes);
                        }
                        // XXX application cache should fire here
                        mode = InsertionMode.BEFORE_HEAD;
                        return;
                    } else {
                        /*
                         * Create an HTMLElement node with the tag name html, in
                         * the HTML namespace. Append it to the Document object.
                         */
                        appendHtmlElementToDocumentAndPush();
                        /* Switch to the main mode */
                        mode = InsertionMode.BEFORE_HEAD;
                        /*
                         * reprocess the current token.
                         * 
                         */
                        continue;
                    }
                case BEFORE_HEAD:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("head" == name) {
                        /*
                         * A start tag whose tag name is "head"
                         * 
                         * Create an element for the token.
                         * 
                         * Set the head element pointer to this new element
                         * node.
                         * 
                         * Append the new element to the current node and push
                         * it onto the stack of open elements.
                         */
                        appendToCurrentNodeAndPushHeadElement(attributes);
                        /*
                         * 
                         * Change the insertion mode to "in head".
                         * 
                         */
                        mode = InsertionMode.IN_HEAD;
                        return;
                    }

                    /*
                     * Any other start tag token
                     */

                    /*
                     * Act as if a start tag token with the tag name "head" and
                     * no attributes had been seen,
                     */
                    appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                    mode = InsertionMode.IN_HEAD;
                    /*
                     * then reprocess the current token.
                     * 
                     * This will result in an empty head element being
                     * generated, with the current token being reprocessed in
                     * the "after head" insertion mode.
                     */
                    continue;
                case AFTER_HEAD:
                    if ("html" == name) {
                        err("Stray \u201Chtml\u201D start tag.");
                        addAttributesToElement(stack[0].node, attributes);
                        return;
                    } else if ("body" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendToCurrentNodeAndPushBodyElement();
                        } else {
                            appendToCurrentNodeAndPushBodyElement(attributes);
                        }
                        mode = InsertionMode.IN_BODY;
                        return;
                    } else if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        mode = InsertionMode.IN_FRAMESET;
                        return;
                    } else if ("base" == name) {
                        err("\u201Cbase\u201D element outside \u201Chead\u201D.");
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        if (!nonConformingAndStreaming) {
                            pop(); // head
                        }
                        return;
                    } else if ("link" == name) {
                        err("\u201Clink\u201D element outside \u201Chead\u201D.");
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        if (!nonConformingAndStreaming) {
                            pop(); // head
                        }
                        return;
                    } else if ("meta" == name) {
                        err("\u201Cmeta\u201D element outside \u201Chead\u201D.");
                        errIfInconsistentCharset(attributes);
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendVoidElementToCurrentMayFoster(name, attributes);
                        if (!nonConformingAndStreaming) {
                            pop(); // head
                        }
                        return;
                    } else if ("script" == name) {
                        err("\u201Cscript\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("style" == name) {
                        err("\u201Cstyle\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("title" == name) {
                        err("\u201Ctitle\u201D element outside \u201Chead\u201D.");
                        if (!nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else {
                        appendToCurrentNodeAndPushBodyElement();
                        mode = InsertionMode.IN_BODY;
                        continue;
                    }
                case AFTER_AFTER_BODY:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    if (conformingAndStreaming) {
                        fatal();
                    }
                    mode = InsertionMode.IN_BODY;
                    continue;
                case AFTER_AFTER_FRAMESET:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    if (conformingAndStreaming) {
                        fatal();
                    }
                    mode = InsertionMode.IN_FRAMESET;
                    continue;
            }
        }
    }

    private boolean equalsIgnoreAsciiCase(CharSequence one, CharSequence other) {
        if (other == null && one == null) {
            return true;
        }
        if (other == null || one == null) {
            return false;
        }
        if (one.length() != other.length()) {
            return false;
        }
        for (int i = 0; i < other.length(); i++) {
            char c0 = one.charAt(i);
            if (c0 >= 'A' && c0 <= 'Z') {
                c0 += 0x20;
            }
            char c1 = other.charAt(i);
            if (c1 >= 'A' && c1 <= 'Z') {
                c1 += 0x20;
            }
            if (c0 != c1) {
                return false;
            }
        }
        return true;
    }
    
    private void errIfInconsistentCharset(Attributes attributes) throws SAXException {
        String content = attributes.getValue("", "content");
        String internalCharset = null;
        if (content != null) {
            internalCharset = MetaSniffer.extractCharsetFromContent(content);
            if (internalCharset != null) {
                if (!equalsIgnoreAsciiCase("content-type", attributes.getValue("", "http-equiv"))) {
                    warn("Attribute \u201Ccontent\u201D would be sniffed as an internal character encoding declaration but there was no matching \u201Chttp-equiv='Content-Type'\u201D attribute.");
                }
            }
        }
        if (internalCharset == null) {
            internalCharset = attributes.getValue("", "charset");
        }
        String externalCharset = tokenizer.getExternalCharset();
        if (internalCharset != null && externalCharset != null) {
            if (!equalsIgnoreAsciiCase(externalCharset, internalCharset)) {
                err("The internally declared character encoding \u201C"
                        + internalCharset
                        + "\u201D does not match the external declaration \u201C"
                        + externalCharset
                        + "\u201D. The external declaration takes precedence.");
            }
        }
    }
    
    public final void endTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        if (cdataOrRcdataTimesToPop > 0) {
            while (cdataOrRcdataTimesToPop > 0) {
                pop();
                cdataOrRcdataTimesToPop--;
            }
            return;
        }

        for (;;) {
            switch (mode) {
                case IN_ROW:
                    if ("tr" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert context != null;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE_BODY;
                        return;
                    } else if ("table" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert context != null;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE_BODY;
                        continue;
                    } else if ("tbody" == name || "thead" == name || "tfoot" == name) {
                        if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                            return;
                        }
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert context != null;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE_BODY;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");                            
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE_BODY:
                    if ("tbody" == name || "tfoot" == name || "thead" == name) {
                        int eltPos = findLastOrRoot(name);
                        if (eltPos == 0) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                            int eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                            if (eltPos == 0) {
                            assert context != null;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        pop();
                        mode = InsertionMode.IN_TABLE;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("table" == name) {
                        int eltPos = findLast("table");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert context != null;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        resetTheInsertionMode();
                        return;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "tbody" == name || "td" == name || "tfoot" == name || "th" == name || "thead" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");                        
                        // fall through to IN_BODY
                    }
                case IN_CAPTION:
                    if ("caption" == name) {
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        mode = InsertionMode.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                        err("\u201Ctable\u201D closed but \u201Ccaption\u201D was still open.");
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        mode = InsertionMode.IN_TABLE;
                        continue;
                    } else if ("body" == name || "col" == name || "colgroup" == name || "html" == name || "tbody" == name || "td" == name || "tfoot" == name || "th" == name || "thead" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                                                                                                        
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("td" == name || "th" == name) {
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                            
                        }
                        generateImpliedEndTags();
                        if (!isCurrent(name)) {
                            err("Unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            pop();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        mode = InsertionMode.IN_ROW;
                        return;
                    } else if ("table" == name || "tbody" == name || "tfoot" == name || "thead" == name || "tr" == name) {
                        if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                                                        
                        }
                        closeTheCell(findLastInTableScopeTdTh());
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                                                                                
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_BODY:
                    if ("body" == name) {
                        if (!isSecondOnStackBody()) {
                            assert context != null;
                            err("Stray end tag \u201Cbody\u201D.");
                            return;
                        }
                        assert currentPtr >= 1;
                        for (int i = 2; i <= currentPtr; i++) {
                            String stackName = stack[i].name;
                            if (!("dd" == stackName || "dt" == stackName || "li" == stackName
                                    || "p" == stackName)) {
                                err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        if (conformingAndStreaming) {
                            while(currentPtr > 1) {
                                pop();
                            }
                        }
                        if (context == null) {
                            bodyClosed(stack[1].node);
                        }
                        mode = InsertionMode.AFTER_BODY;
                        return;
                    } else if ("html" == name) {
                        if (!isSecondOnStackBody()) {
                            assert context != null;
                            err("Stray end tag \u201Chtml\u201D.");
                            return;
                        }
                        for (int i = 0; i <= currentPtr; i++) {
                            String stackName = stack[i].name;
                            if (!("dd" == stackName || "dt" == stackName || "li" == stackName
                                    || "p" == stackName || "tbody" == stackName || "td" == stackName
                                    || "tfoot" == stackName || "th" == stackName || "thead" == stackName || "tr" == stackName || "body" == stackName || "html" == stackName)) {
                                err("End tag for \u201Chtml\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        if (context == null) {
                            bodyClosed(stack[1].node);
                        }
                        mode = InsertionMode.AFTER_BODY;
                        continue;
                    } else if ("div" == name || "blockquote" == name
                            || "ul" == name || "ol" == name || "pre" == name
                            || "dl" == name || "fieldset" == name
                            || "address" == name || "center" == name
                            || "dir" == name || "listing" == name
                            || "menu" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                        } else {
                            generateImpliedEndTags();
                            if (!isCurrent(name)) {
                                err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }                            
                        }
                        return;
                    } else if ("form" == name) {
                        formPointer = null;
                        int eltPos = findLastInScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                        } else {
                            generateImpliedEndTags();
                            if (!isCurrent(name)) {
                                err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }                            
                        }
                        return;
                    } else if ("p" == name) {
                        if (!isCurrent(name)) {
                            err("End tag \u201Cp\u201D seen but there were unclosed elements.");
                        }
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                        } else {
                            appendVoidElementToCurrentMayFoster(name, EmptyAttributes.EMPTY_ATTRIBUTES);
                        }
                        return;
                    } else if ("dd" == name || "dt" == name || "li" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                                                        
                        } else {
                            generateImpliedEndTagsExceptFor(name);
                            if (!isCurrent(name)) {
                                err("End tag \u201C"
                                        + name
                                        + "\u201D seen but there were unclosed elements.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                        }
                        return;
                    } else if ("h1" == name || "h2" == name || "h3" == name
                            || "h4" == name || "h5" == name || "h6" == name) {
                        int eltPos = findLastInScopeHn();
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                        } else {
                            generateImpliedEndTags();
                            if (!isCurrent(name)) {
                                err("End tag \u201C"
                                        + name
                                        + "\u201D seen but there were unclosed elements.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                        }
                        return;
                    } else if ("a" == name || "b" == name || "big" == name || "em" == name || "font" == name || "i" == name || "nobr" == name || "s" == name || "small" == name || "strike" == name || "strong" == name || "tt" == name || "u" == name) {
                        adoptionAgencyEndTag(name);
                        return;
                    } else if ("button" == name || "marquee" == name || "object" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                        } else {
                            generateImpliedEndTags();
                            if (!isCurrent(name)) {
                                err("End tag \u201C"
                                        + name
                                        + "\u201D seen but there were unclosed elements.");
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                            clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        }
                        return;
                    } else if ("br" == name) {
                        err("End tag \u201Cbr\u201D.");
                        reconstructTheActiveFormattingElements();
                        appendVoidElementToCurrentMayFoster(name, EmptyAttributes.EMPTY_ATTRIBUTES);
                        return;
                    } else if ("area" == name || "basefont" == name || "bgsound" == name || "embed" == name || "hr" == name || "iframe" == name || "image" == name || "img" == name || "input" == name || "isindex" == name || "noembed" == name || "noframes" == name || "param" == name || "select" == name || "spacer" == name || "table" == name || "textarea" == name || "wbr" == name || (scriptingEnabled && "noscript" == name)) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        if (isCurrent(name)) {
                            pop();
                            return;
                        }
                        for(;;) {
                            generateImpliedEndTags();
                            if (isCurrent(name)) {
                                pop();
                                return;
                            }
                            StackNode<T> node = stack[currentPtr];
                            if (!(node.scoping || node.special)) {
                                err("Unclosed element \u201C" + node.name
                                        + "\u201D.");
                                pop();
                            } else {
                                err("Stray end tag \u201C" + name
                                        + "\u201D.");
                                return;
                            }
                        }
                    }
                case IN_COLUMN_GROUP:
                    if ("colgroup" == name) {
                        if (currentPtr == 0) {
                            assert context != null;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        pop();
                        mode = InsertionMode.IN_TABLE;
                        return;                    
                    } else if ("col" == name) {
                        err("Stray end tag \u201Ccol\u201D.");                        
                        return;
                    } else {
                        if (currentPtr == 0) {
                            assert context != null;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        pop();
                        mode = InsertionMode.IN_TABLE;
                        continue;                   
                    }
                case IN_SELECT_IN_TABLE:
                    if ("caption" == name || "table" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name
                            || "tr" == name || "td" == name || "th" == name) {
                        err("\u201C" + name + "\u201D end tag with \u201Cselect\u201D open.");
                        if (findLastInTableScope(name) != NOT_FOUND_ON_STACK) {
                            endSelect();
                            continue;                            
                        } else {
                            return;
                        }
                    } else {
                        // fall through to IN_SELECT
                    }
                case IN_SELECT:
                    if ("option" == name) {
                        if (isCurrent("option")) {
                            pop();
                            return;
                        } else {
                            err("Stray end tag \u201Coption\u201D");
                            return;
                        }
                    } else if ("optgroup" == name) {
                        if (isCurrent("option") && "optgroup" == stack[currentPtr - 1].name) {
                            pop();
                        }
                        if (isCurrent("optgroup")) {
                            pop();
                        } else {
                            err("Stray end tag \u201Coptgroup\u201D");
                        }
                        return;                            
                    } else if ("select" == name) {
                        endSelect();
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;
                    }
                case AFTER_BODY:
                    if ("html" == name) {
                        if (context != null) {
                            err("Stray end tag \u201Chtml\u201D");
                            return;                            
                        } else {
                            if (context == null) {
                                htmlClosed(stack[0].node);
                            }
                            mode = InsertionMode.AFTER_AFTER_BODY;
                            return;
                        }
                    } else {
                        err("Saw an end tag after \u201Cbody\u201D had been closed.");
                        if (conformingAndStreaming) {
                            fatal();
                        }
                        mode = InsertionMode.IN_BODY;
                        continue;
                    }
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        if (currentPtr == 0) {
                            assert context != null;
                            err("Stray end tag \u201Cframeset\u201D");
                            return;
                        }
                        pop();
                        if ((context == null) && !isCurrent("frameset")) {
                            mode = InsertionMode.AFTER_FRAMESET;                            
                        }
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;                        
                    }
                case AFTER_FRAMESET:
                    if ("html" == name) {
                        if (context == null) {
                            htmlClosed(stack[0].node);
                        }
                        mode = InsertionMode.AFTER_AFTER_FRAMESET;
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;                        
                    }
                case INITIAL:
                    /*
                     * Parse error.
                     */
                    if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                        err("End tag seen without seeing a doctype first.");
                    }
                    /*
                     * 
                     * Set the document to quirks mode.
                     */
                    documentModeInternal(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element mode of the tree
                     * construction stage
                     */
                    mode = InsertionMode.BEFORE_HTML;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case BEFORE_HTML:
                    /*
                     * Create an HTMLElement node with the tag name html, in the
                     * HTML namespace. Append it to the Document object.
                     */
                    appendHtmlElementToDocumentAndPush();
                    /* Switch to the main mode */
                    mode = InsertionMode.BEFORE_HEAD;
                    /*
                     * reprocess the current token.
                     * 
                     */
                    continue;
                case BEFORE_HEAD:
                    if ("head" == name || "body" == name || "html" == name || "p" == name || "br" == name) {
                        appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                        mode = InsertionMode.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case IN_HEAD:
                    if ("head" == name) {
                        pop();
                        mode = InsertionMode.AFTER_HEAD;
                        return;
                    } else if ("body" == name || "html" == name || "p" == name || "br" == name) {
                        pop();
                        mode = InsertionMode.AFTER_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                        
                    }
                case IN_HEAD_NOSCRIPT:
                    if ("noscript" == name) {
                        pop();
                        mode = InsertionMode.IN_HEAD;
                        return;
                    } else if ("p" == name || "br" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        pop();
                        mode = InsertionMode.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case AFTER_HEAD:
                    appendToCurrentNodeAndPushBodyElement();
                    mode = InsertionMode.IN_BODY;
                    continue;
                case AFTER_AFTER_BODY:
                    err("Stray \u201C" + name + "\u201D end tag.");
                    if (conformingAndStreaming) {
                        fatal();
                    }
                    mode = InsertionMode.IN_BODY;
                    continue;
                case AFTER_AFTER_FRAMESET:
                    err("Stray \u201C" + name + "\u201D end tag.");
                    if (conformingAndStreaming) {
                        fatal();
                    }
                    mode = InsertionMode.IN_FRAMESET;
                    continue;
            }
        }
    }

    /**
     * @throws SAXException
     */
    private void endSelect() throws SAXException {
        int eltPos = findLastInTableScope("select");
        if (eltPos == NOT_FOUND_ON_STACK) {
            assert context != null;
            err("Stray end tag \u201Cselect\u201D");
            return;                                                        
        }
        while (currentPtr >= eltPos) {
            pop();
        }
        resetTheInsertionMode();
    }

    private int findLastInTableScopeOrRootTbodyTheadTfoot() {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == "tbody" || stack[i].name == "thead" || stack[i].name == "tfoot") {
                return i;
            }
        }
        return 0;
    }

    private int findLast(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            }
        }
        return NOT_FOUND_ON_STACK;
    }
    
    private int findLastInTableScope(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            } else if (stack[i].name == "table") {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLastInScope(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            } else if (stack[i].scoping) {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLastInScopeHn() {
        for (int i = currentPtr; i > 0; i--) {
            String name = stack[i].name;
            if ("h1" == name || "h2" == name || "h3" == name || "h4" == name
                    || "h5" == name || "h6" == name) {
                return i;
            } else if (stack[i].scoping) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private void generateImpliedEndTagsExceptFor(String name) throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if (name != stackName && ("p" == stackName || "li" == stackName || "dd" == stackName || "dt" == stackName)) {
                pop();
            } else {
                return;
            }
        }
    }
    
    private void generateImpliedEndTags() throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if ("p" == stackName || "li" == stackName || "dd" == stackName || "dt" == stackName) {
                pop();
            } else {
                return;
            }
        }
    }

    private boolean isSecondOnStackBody() {
        return currentPtr >= 1 && stack[1].name == "body";
    }

    private void documentModeInternal(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) throws SAXException {
        if (documentModeHandler != null) {
            documentModeHandler.documentMode(mode, publicIdentifier,
                    systemIdentifier, html4SpecificAdditionalErrorChecks);
        }
        documentMode(mode, publicIdentifier,
                systemIdentifier, html4SpecificAdditionalErrorChecks);
    }

    private boolean isAlmostStandards(String publicIdentifierLC,
            String systemIdentifierLC) {
        if ("-//w3c//dtd xhtml 1.0 transitional//en".equals(publicIdentifierLC)) {
            return true;
        }
        if ("-//w3c//dtd xhtml 1.0 frameset//en".equals(publicIdentifierLC)) {
            return true;
        }
        if (systemIdentifierLC != null) {
            if ("-//w3c//dtd html 4.01 transitional//en".equals(publicIdentifierLC)) {
                return true;
            }
            if ("-//w3c//dtd html 4.01 frameset//en".equals(publicIdentifierLC)) {
                return true;
            }
        }
        return false;
    }

    private boolean isQuirky(String name, String publicIdentifierLC,
            String systemIdentifierLC, boolean correct) {
        if (!correct) {
            return true;
        }
        if (!"HTML".equalsIgnoreCase(name)) {
            return true;
        }
        if (publicIdentifierLC != null
                && (Arrays.binarySearch(QUIRKY_PUBLIC_IDS, publicIdentifierLC) > -1)) {
            return true;
        }
        if (systemIdentifierLC == null) {
            if ("-//w3c//dtd html 4.01 transitional//en".equals(publicIdentifierLC)) {
                return true;
            } else if ("-//w3c//dtd html 4.01 frameset//en".equals(publicIdentifierLC)) {
                return true;
            }
        } else if ("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd".equals(systemIdentifierLC)) {
            return true;
        }
        return false;
    }

    private String toAsciiLowerCase(String str) {
        if (str == null) {
            return null;
        }
        char[] buf = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
            }
            buf[i] = c;
        }
        return new String(buf);
    }

    private void closeTheCell(int eltPos) throws SAXException {
        generateImpliedEndTags();
        if (eltPos != currentPtr) {
            err("Unclosed elements.");
        }
        while (currentPtr >= eltPos) {
            pop();
        }
        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
        mode = InsertionMode.IN_ROW;
        return;
    }

    private int findLastInTableScopeTdTh() {
        for (int i = currentPtr; i > 0; i--) {
            String name = stack[i].name;
            if ("td" == name || "th" == name) {
                return i;
            } else if (name == "table") {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private void clearStackBackTo(int eltPos) throws SAXException {
        if (eltPos != currentPtr) {
            while(currentPtr > eltPos) { // > not >= intentional
                pop();
            }
        }
    }

    private void resetTheInsertionMode() {
        String name;
        for (int i = currentPtr; i >= 0; i--) {
            name = stack[i].name;
            if (i == 0) {
                if (!(context == "td" || context == "th")) {
                    name = context;
                }
            }
            if ("select" == name) {
                mode = InsertionMode.IN_SELECT;
                return;
            } else if ("td" == name || "th" == name) {
                mode = InsertionMode.IN_CELL;
                return;
            } else if ("tr" == name) {
                mode = InsertionMode.IN_ROW;
                return;
            } else if ("tbody" == name || "thead" == name || "tfoot" == name) {
                mode = InsertionMode.IN_TABLE_BODY;
                return;
            } else if ("caption" == name) {
                mode = InsertionMode.IN_CAPTION;
                return;
            } else if ("colgroup" == name) {
                mode = InsertionMode.IN_COLUMN_GROUP;
                return;
            } else if ("table" == name) {
                mode = InsertionMode.IN_TABLE;
                return;
            } else if ("head" == name) {
                mode = InsertionMode.IN_BODY; // really
                return;
            } else if ("body" == name) {
                mode = InsertionMode.IN_BODY;
                return;
            } else if ("frameset" == name) {
                mode = InsertionMode.IN_FRAMESET;
                return;
            } else if ("html" == name) {
                if (headPointer == null) {
                    mode = InsertionMode.BEFORE_HEAD;                    
                } else {
                    mode = InsertionMode.AFTER_HEAD;
                }
                return;
            } else if (i == 0) {
                mode = InsertionMode.IN_BODY;
                return;
            } 
        }
    }

    /**
     * @throws SAXException 
     * 
     */
    private void implicitlyCloseP() throws SAXException {
        int eltPos = findLastInScope("p");
        if (eltPos == NOT_FOUND_ON_STACK) {
            return;
        }
        if (currentPtr != eltPos) {
            err("Unclosed elements.");
        }
        while (currentPtr >= eltPos) {
            pop();
        }
    }

    private boolean clearLastStackSlot() {
        stack[currentPtr] = null;
        return true;
    }

    private boolean clearLastListSlot() {
        listOfActiveFormattingElements[listPtr] = null;
        return true;
    }
    
    private void push(StackNode<T> node) throws SAXException {
        currentPtr++;
        if (currentPtr == stack.length) {
            StackNode<T>[] newStack = new StackNode[stack.length + 64];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[currentPtr] = node;
        elementPushed(node.name, node.node);
    }

    private void append(StackNode<T> node) {
        listPtr++;
        if (listPtr == listOfActiveFormattingElements.length) {
            StackNode<T>[] newList = new StackNode[listOfActiveFormattingElements.length + 64];
            System.arraycopy(listOfActiveFormattingElements, 0, newList, 0, listOfActiveFormattingElements.length);
            listOfActiveFormattingElements = newList;
        }
        listOfActiveFormattingElements[listPtr] = node;
    }
    
    private void insertMarker() {
        append(MARKER);
    }

    private void clearTheListOfActiveFormattingElementsUpToTheLastMarker() {
        while (listPtr > -1) {
            if (listOfActiveFormattingElements[listPtr--] == MARKER) {
                return;
            }
        }
    }

    private boolean isCurrent(String name) {
        return name == stack[currentPtr].name;
    }

    private void removeFromStack(int pos) throws SAXException {
        if (currentPtr == pos) {
            pop();
        } else {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                throw new UnsupportedOperationException();
            } else {
                System.arraycopy(stack, pos + 1, stack, pos, currentPtr - pos);
                assert clearLastStackSlot();
                currentPtr--;
            }
        }
    }
    
    private void removeFromStack(StackNode<T> node) throws SAXException {
        if (stack[currentPtr] == node) {
            pop();
        } else {
            int pos = currentPtr - 1;
            while (pos >= 0 && stack[pos] != node) {
                pos--;
            }
            if (pos == -1) {
                // dead code?
                return;
            }
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                throw new UnsupportedOperationException();
            } else {
                System.arraycopy(stack, pos + 1, stack, pos, currentPtr - pos);
                currentPtr--;
            }
        }
    }

    private void removeFromListOfActiveFormattingElements(int pos) {
        if (pos == listPtr) {
            assert clearLastListSlot();
            listPtr--;
            return;
        }
        assert pos < listPtr;
        System.arraycopy(listOfActiveFormattingElements, pos + 1, listOfActiveFormattingElements, pos, listPtr - pos);
        assert clearLastListSlot();
        listPtr--;
    }

    private void adoptionAgencyEndTag(String name) throws SAXException {
        flushCharacters();
        for (;;) {
            int formattingEltListPos = listPtr;
            while (formattingEltListPos > -1) {
                String listName = listOfActiveFormattingElements[formattingEltListPos].name;
                if (listName == name) {
                    break;
                } else if (listName == null) {
                    formattingEltListPos = -1;
                    break;
                }
                formattingEltListPos--;
            }
            if (formattingEltListPos == -1) {
                err("No element \u201C" + name + "\u201D to close.");
                return;
            }
            StackNode<T> formattingElt = listOfActiveFormattingElements[formattingEltListPos];
            int formattingEltStackPos = currentPtr;
            boolean inScope = true;
            while (formattingEltStackPos > -1) {
                StackNode<T> node = stack[formattingEltStackPos];
                if (node == formattingElt) {
                    break;
                } else if (node.scoping) {
                    inScope = false;
                }
                formattingEltStackPos--;
            }
            if (formattingEltStackPos == -1) {
                err("No element \u201C" + name + "\u201D to close.");
                removeFromListOfActiveFormattingElements(formattingEltListPos);
                return;
            }
            if (!inScope) {
                err("No element \u201C" + name + "\u201D to close.");
                return;
            }
            // stackPos now points to the formatting element and it is in scope
            if (formattingEltStackPos != currentPtr) {
                err("End tag \u201C" + name + "\u201D violates nesting rules.");
            }
            int furthestBlockPos = formattingEltStackPos + 1;
            while (furthestBlockPos <= currentPtr) {
                StackNode<T> node = stack[furthestBlockPos];
                if (node.scoping || node.special) {
                    break;
                }
                furthestBlockPos++;
            }
            if (furthestBlockPos > currentPtr) {
                // no furthest block
                while (currentPtr >= formattingEltStackPos) {
                    pop();
                }
                removeFromListOfActiveFormattingElements(formattingEltListPos);
                return;
            }
            StackNode<T> commonAncestor = stack[formattingEltStackPos - 1];
            StackNode<T> furthestBlock = stack[furthestBlockPos];
            detachFromParent(furthestBlock.node);
            int bookmark = formattingEltListPos;
            int nodePos = furthestBlockPos;
            StackNode<T> lastNode = furthestBlock;
            for(;;) {
                nodePos--;
                StackNode<T> node = stack[nodePos];
                int nodeListPos = findInListOfActiveFormattingElements(node);
                if (nodeListPos == -1) {
                    assert formattingEltStackPos < nodePos;
                    assert bookmark < nodePos;
                    assert furthestBlockPos > nodePos;
                    removeFromStack(nodePos);
                    furthestBlockPos--;
                    continue;
                }
                if (nodePos == formattingEltStackPos) {
                    break;
                }
                if (nodePos == furthestBlockPos) {
                    bookmark = nodeListPos + 1;
                }
                if (hasChildren(node.node)) {
                    assert node == listOfActiveFormattingElements[nodeListPos];
                    assert node == stack[nodePos];
                    T clone = shallowClone(node.node);
                    node = new StackNode<T>(node.name, clone, node.scoping, node.special, node.fosterParenting);
                    listOfActiveFormattingElements[nodeListPos] = node;
                    stack[nodePos] = node;                    
                }
                detachFromParentAndAppendToNewParent(lastNode.node, node.node);
                lastNode = node;
            }
            if (commonAncestor.fosterParenting) {
                if (conformingAndStreaming) {
                    fatal();
                }
                insertIntoFosterParent(lastNode.node);
            } else {
                detachFromParentAndAppendToNewParent(lastNode.node, commonAncestor.node);
            }
            T clone = shallowClone(formattingElt.node);
            StackNode<T> formattingClone = new StackNode<T>(formattingElt.name, clone, formattingElt.scoping, formattingElt.special, formattingElt.fosterParenting);
            appendChildrenToNewParent(furthestBlock.node, clone);
            detachFromParentAndAppendToNewParent(clone, furthestBlock.node);
            removeFromListOfActiveFormattingElements(formattingEltListPos);
            insertIntoListOfActiveFormattingElements(formattingClone, bookmark);
            assert formattingEltStackPos < furthestBlockPos;
            removeFromStack(formattingEltStackPos);
            // furthestBlockPos is now off by one and points to the slot after it
            insertIntoStack(formattingClone, furthestBlockPos);
        }
    }

    private void insertIntoStack(StackNode<T> node, int position) throws SAXException {
        assert currentPtr + 1 < stack.length;
        assert position <= currentPtr + 1;
        if (position == currentPtr + 1) {
            flushCharacters();
            push(node);
        } else {
            System.arraycopy(stack, position, stack, position + 1, (currentPtr - position) + 1);
            currentPtr++;
            stack[position] = node;        
        }
    }

    private void insertIntoListOfActiveFormattingElements(StackNode<T> formattingClone, int bookmark) {
        assert listPtr + 1 < listOfActiveFormattingElements.length;
        if (bookmark <= listPtr) {
            System.arraycopy(listOfActiveFormattingElements, bookmark, listOfActiveFormattingElements, bookmark + 1, (listPtr - bookmark) + 1);
        }
        listPtr++;
        listOfActiveFormattingElements[bookmark] = formattingClone;
    }

    private int findInListOfActiveFormattingElements(StackNode<T> node) {
        for (int i = listPtr; i >= 0; i--) {
            if (node == listOfActiveFormattingElements[i]) {
                return i;
            }
        }
        return -1;
    }

    private int findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker(
            String name) {
        for (int i = listPtr; i >= 0; i--) {
            StackNode<T> node = listOfActiveFormattingElements[i];
            if (node.name == name) {
                return i;
            } else if (node == MARKER) {
                return -1;
            }
        }        
        return -1;
    }

    private int findDdOrDtToPop() {
        for (int i = currentPtr; i >= 0; i--) {
            StackNode<T> node = stack[i];
            if ("dd" == node.name || "dt" == node.name) {
                return i;
            } else if ((node.scoping || node.special) && !("div" == node.name || "address" == node.name)) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLiToPop() {
        for (int i = currentPtr; i >= 0; i--) {
            StackNode<T> node = stack[i];
            if ("li" == node.name) {
                return i;
            } else if ((node.scoping || node.special) && !("div" == node.name || "address" == node.name)) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLastOrRoot(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            }
        }
        return 0;
    }

    private void addAttributesToBody(Attributes attributes) throws SAXException {
        if (currentPtr >= 1) {
            StackNode<T> body = stack[1];
            if (body.name == "body") {
                addAttributesToElement(body.node, attributes);                
            }
        }
    }

    private void pushHeadPointerOntoStack() throws SAXException {
        flushCharacters();
        if (conformingAndStreaming) {
            fatal();
        }
        if (headPointer == null) {
            assert context != null;
            push(stack[currentPtr]);            
        } else {
            push(new StackNode<T>("head", headPointer));
        }
    }

    /**
     * @throws SAXException 
     * 
     */
    private void reconstructTheActiveFormattingElements() throws SAXException {
        if (listPtr == -1) {
            return;
        }
        StackNode<T> mostRecent = listOfActiveFormattingElements[listPtr];
        if (mostRecent == MARKER || isInStack(mostRecent)) {
            return;
        }
        int entryPos = listPtr;
        for(;;) {
            entryPos--;
            if (entryPos == -1) {
                break;
            }
            if (listOfActiveFormattingElements[entryPos] == MARKER) {
                break;
            }
            if (isInStack(listOfActiveFormattingElements[entryPos])) {
                break;
            }
        }
        if (entryPos < listPtr) {
            flushCharacters();
        }
        while (entryPos < listPtr) {
            entryPos++;
            StackNode<T> entry = listOfActiveFormattingElements[entryPos];
            T clone = shallowClone(entry.node);
            StackNode<T> entryClone = new StackNode<T>(entry.name, clone, entry.scoping, entry.special, entry.fosterParenting);
            StackNode<T> currentNode = stack[currentPtr];
            if (currentNode.fosterParenting) {
                insertIntoFosterParent(clone);                
            } else {
                detachFromParentAndAppendToNewParent(clone, currentNode.node);
            }
            push(entryClone);
            listOfActiveFormattingElements[entryPos] = entryClone;
        }
    }

    private void insertIntoFosterParent(T child) throws SAXException {
        int eltPos = findLastOrRoot("table");
        StackNode<T> node = stack[eltPos];
        node.tainted = true;
        T elt = node.node;
        if (eltPos == 0) {
            detachFromParentAndAppendToNewParent(child, elt);
            return;
        }
        T parent = parentElementFor(elt);
        if (parent == null) {
            detachFromParentAndAppendToNewParent(child, stack[eltPos - 1].node);            
        } else {
            insertBefore(child, elt, parent);            
        }
    }

    private boolean isInStack(StackNode<T> node) {
        for (int i = currentPtr; i >= 0; i--) {
            if (stack[i] == node) {
                return true;
            }
        }
        return false;
    }

    private void pop() throws SAXException {
        flushCharacters();
        StackNode<T> node = stack[currentPtr];
        assert clearLastStackSlot();
        currentPtr--;
        elementPopped(node.name, node.node);
    }

    private void appendCharMayFoster(char[] buf, int i) throws SAXException {
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                int eltPos = findLastOrRoot("table");
                StackNode<T> node = stack[eltPos];
                node.tainted = true;
                T elt = node.node;
                if (eltPos == 0) {
                    appendCharacters(elt, buf, i, 1);
                    return;
                }
                T parent = parentElementFor(elt);
                if (parent == null) {
                    appendCharacters(stack[eltPos - 1].node, buf, i, 1);
                } else {
                    insertCharactersBefore(buf, i, 1, elt, parent);            
                }
            }
        } else {
            accumulateCharacters(buf, i, 1);
        }
    }
    
    private boolean isTainted() {
        int eltPos = findLastOrRoot("table");
        StackNode<T> node = stack[eltPos];
        return node.tainted;
    }

    private void appendHtmlElementToDocumentAndPush(Attributes attributes) throws SAXException {
        T elt = createHtmlElementSetAsRoot(attributes);
        StackNode<T> node = new StackNode<T>("html", elt);
        push(node);
    }

    private void appendHtmlElementToDocumentAndPush() throws SAXException {
        appendHtmlElementToDocumentAndPush(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushHeadElement(
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement("head", attributes);
        detachFromParentAndAppendToNewParent(elt, stack[currentPtr].node);
        headPointer = elt;
        StackNode<T> node = new StackNode<T>("head", elt);
        push(node);
    }
    
    private void appendToCurrentNodeAndPushBodyElement(
            Attributes attributes) throws SAXException {
        appendToCurrentNodeAndPushElement("body", attributes);
    }

    private void appendToCurrentNodeAndPushBodyElement() throws SAXException {
        appendToCurrentNodeAndPushBodyElement(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushFormElementMayFoster(Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement("form", attributes);
        formPointer = elt;
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        StackNode<T> node = new StackNode<T>("form", elt);
        push(node);
    }
    
    private void appendToCurrentNodeAndPushFormattingElementMayFoster(String name,
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes, formPointer);
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);
        append(node);
    }

    private void appendToCurrentNodeAndPushElement(String name,
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes);
        detachFromParentAndAppendToNewParent(elt, stack[currentPtr].node);
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);        
    }

    private void appendToCurrentNodeAndPushElementMayFoster(String name,
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes);
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);
    }
    
    private void appendToCurrentNodeAndPushElementMayFoster(String name, Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes, formPointer);
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);
    }

    private void appendVoidElementToCurrentMayFoster(String name,
            Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes, formPointer);
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        if (conformingAndStreaming || nonConformingAndStreaming) {
            elementPushed(name, (T) attributes);
            elementPopped(name, null);
        }
    }
    
    private void appendVoidElementToCurrentMayFoster(String name, Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes);
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                insertIntoFosterParent(elt);
            }
        } else {
            detachFromParentAndAppendToNewParent(elt, current.node);
        }
        if (conformingAndStreaming || nonConformingAndStreaming) {
            elementPushed(name, (T) attributes);
            elementPopped(name, null);
        }
    }
       
    private void appendVoidElementToCurrent(String name,
            Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement(name, attributes, formPointer);
        StackNode<T> current = stack[currentPtr];
        detachFromParentAndAppendToNewParent(elt, current.node);
        if (conformingAndStreaming || nonConformingAndStreaming) {
            elementPushed(name, (T) attributes);
            elementPopped(name, null);
        }
    }

    
    private void accumulateCharacters(char[] buf, int start, int length) throws SAXException {
        if (coalescingText) {
            int newLen = charBufferLen + length;
            if (newLen > charBuffer.length) {
                char[] newBuf = new char[newLen];
                System.arraycopy(charBuffer, 0, newBuf, 0, charBuffer.length);
                charBuffer = newBuf;
            }
            System.arraycopy(buf, start, charBuffer, charBufferLen, length);
            charBufferLen = newLen;
        } else {
            appendCharacters(stack[currentPtr].node, buf, start, length);
        }
    }
    
    private void flushCharacters() throws SAXException {
        if (charBufferLen > 0) {
            appendCharacters(stack[currentPtr].node, charBuffer, 0, charBufferLen);
            charBufferLen = 0;
        }
    }
    
    // ------------------------------- //
    
    protected abstract T createElement(String name, Attributes attributes) throws SAXException;    
    
    protected T createElement(String name, Attributes attributes, T form) throws SAXException {
        return createElement(name, attributes);
    }
    
    protected abstract T createHtmlElementSetAsRoot(Attributes attributes) throws SAXException;
    
    protected abstract void detachFromParent(T element) throws SAXException;

    protected abstract boolean hasChildren(T element) throws SAXException;
    
    protected abstract T shallowClone(T element) throws SAXException;
    
    protected abstract void detachFromParentAndAppendToNewParent(T child, T newParent) throws SAXException;

    protected abstract void appendChildrenToNewParent(T oldParent, T newParent) throws SAXException;
    
    /**
     * Get the parent element. MUST return <code>null</code> if there is no parent
     * <em>or</em> the parent is not an element.
     */
    protected abstract T parentElementFor(T child) throws SAXException;
    
    protected abstract void insertBefore(T child, T sibling, T parent) throws SAXException;
    
    protected abstract void insertCharactersBefore(char[] buf, int start, int length, T sibling, T parent) throws SAXException;
    
    protected abstract void appendCharacters(T parent,
            char[] buf, int start, int length) throws SAXException;
    
    protected abstract void appendComment(T parent, char[] buf, int start, int length) throws SAXException;

    protected abstract void appendCommentToDocument(char[] buf, int start, int length) throws SAXException;

    protected abstract void addAttributesToElement(T element, Attributes attributes) throws SAXException;

    protected void start(boolean fragment) throws SAXException {
        
    }

    protected void end() throws SAXException {
        
    }

    protected void bodyClosed(T body) throws SAXException {
        
    }

    protected void htmlClosed(T html) throws SAXException {
        
    }
    
    protected void appendDoctypeToDocument(String name,
            String publicIdentifier, String systemIdentifier) throws SAXException {
        
    }
    
    protected void elementPushed(String name, T node) throws SAXException {
        
    }

    protected void elementPopped(String name, T node) throws SAXException {
        
    }

    protected void documentMode(DocumentMode mode, String publicIdentifier, String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) throws SAXException {

    }

    /**
     * @see nu.validator.htmlparser.impl.TokenHandler#wantsComments()
     */
    public boolean wantsComments() {
        return wantingComments;
    }

    public void setIgnoringComments(boolean ignoreComments) {
        wantingComments = !ignoreComments;
    }
    
    /**
     * Sets the errorHandler.
     * 
     * @param errorHandler the errorHandler to set
     */
    public final void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    public final void setFragmentContext(String context) {
        this.context = context == null ? null : context.intern();
    }
    
    protected final T currentNode() {
        return stack[currentPtr].node;
    }

    /**
     * Returns the scriptingEnabled.
     * 
     * @return the scriptingEnabled
     */
    public boolean isScriptingEnabled() {
        return scriptingEnabled;
    }

    /**
     * Sets the scriptingEnabled.
     * 
     * @param scriptingEnabled the scriptingEnabled to set
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        this.scriptingEnabled = scriptingEnabled;
    }

    /**
     * Sets the doctypeExpectation.
     * 
     * @param doctypeExpectation the doctypeExpectation to set
     */
    public void setDoctypeExpectation(DoctypeExpectation doctypeExpectation) {
        this.doctypeExpectation = doctypeExpectation;
    }

    /**
     * Sets the documentModeHandler.
     * 
     * @param documentModeHandler the documentModeHandler to set
     */
    public void setDocumentModeHandler(DocumentModeHandler documentModeHandler) {
        this.documentModeHandler = documentModeHandler;
    }

    /**
     * Sets the reportingDoctype.
     * 
     * @param reportingDoctype the reportingDoctype to set
     */
    public void setReportingDoctype(boolean reportingDoctype) {
        this.reportingDoctype = reportingDoctype;
    }
}