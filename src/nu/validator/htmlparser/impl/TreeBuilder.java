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

    final static int OTHER = 0;

    final static int A = 1;

    final static int BASE = 2;

    final static int BODY = 3;

    final static int BR = 4;

    final static int BUTTON = 5;

    final static int CAPTION = 6;

    final static int COL = 7;

    final static int COLGROUP = 8;

    final static int FORM = 9;

    final static int FRAME = 10;

    final static int FRAMESET = 11;

    final static int IMAGE = 12;

    final static int INPUT = 13;

    final static int ISINDEX = 14;

    final static int LI = 15;

    final static int LINK = 16;

    final static int MATH = 17;

    final static int META = 18;

    final static int SVG = 19;

    final static int HEAD = 20;

    final static int HR = 22;

    final static int HTML = 23;

    final static int NOBR = 24;

    final static int NOFRAMES = 25;

    final static int NOSCRIPT = 26;

    final static int OPTGROUP = 27;

    final static int OPTION = 28;

    final static int P = 29;

    final static int PLAINTEXT = 30;

    final static int SCRIPT = 31;

    final static int SELECT = 32;

    final static int STYLE = 33;

    final static int TABLE = 34;

    final static int TEXTAREA = 35;

    final static int TITLE = 36;

    final static int TR = 37;

    final static int XMP = 38;

    final static int TBODY_OR_THEAD_OR_TFOOT = 39;

    final static int TD_OR_TH = 40;

    final static int DD_OR_DT = 41;

    final static int H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6 = 42;

    final static int OBJECT_OR_MARQUEE_OR_APPLET = 43;

    final static int PRE_OR_LISTING = 44;

    final static int B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U = 45;

    final static int UL_OR_OL_OR_DL = 46;

    final static int IFRAME_OR_NOEMBED = 47;

    final static int EMBED_OR_IMG = 48;

    final static int AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR = 49;

    final static int DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU = 50;

    final static int FIELDSET_OR_ADDRESS_OR_DIR = 51;

    final static int CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR = 52;

    private enum InsertionMode {
        INITIAL, BEFORE_HTML, BEFORE_HEAD, IN_HEAD, IN_HEAD_NOSCRIPT, AFTER_HEAD, IN_BODY, IN_TABLE, IN_CAPTION, IN_COLUMN_GROUP, IN_TABLE_BODY, IN_ROW, IN_CELL, IN_SELECT, IN_SELECT_IN_TABLE, AFTER_BODY, IN_FRAMESET, AFTER_FRAMESET, AFTER_AFTER_BODY, AFTER_AFTER_FRAMESET
    }

    private class StackNode<S> {
        final int magic;
        
        final String name;
        
        final String popName;

        final String ns;

        final S node;

        final boolean scoping;

        final boolean special;

        final boolean fosterParenting;

        boolean tainted = false;

        /**
         * @param magic TODO
         * @param name
         * @param node
         * @param scoping
         * @param special
         * @param popName TODO
         */
        StackNode(int magic, final String ns, final String name,
                final S node, final boolean scoping,
                final boolean special, final boolean fosterParenting, String popName) {
            this.magic = magic;
            this.name = name;
            this.ns = ns;
            this.node = node;
            this.scoping = scoping;
            this.special = special;
            this.fosterParenting = fosterParenting;
            this.popName = popName;
        }

        /**
         * @param elementName TODO
         * @param node
         */
        StackNode(final String ns, ElementName elementName, final S node) {
            this.magic = elementName.magic;
            this.name = elementName.name;
            this.popName = elementName.name;
            this.ns = ns;
            this.node = node;
            this.scoping = elementName.scoping;
            this.special = elementName.special;
            this.fosterParenting = elementName.fosterParenting;
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
            "-//W3C//DTD HTML 4.0//EN", "-//W3C//DTD HTML 4.01 Frameset//EN",
            "-//W3C//DTD HTML 4.01 Transitional//EN",
            "-//W3C//DTD HTML 4.01//EN" };

    private final static String[] QUIRKY_PUBLIC_IDS = {
            "+//silmaril//dtd html pro v0r11 19970101//en",
            "-//advasoft ltd//dtd html 3.0 aswedit + extensions//en",
            "-//as//dtd html 3.0 aswedit + extensions//en",
            "-//ietf//dtd html 2.0 level 1//en",
            "-//ietf//dtd html 2.0 level 2//en",
            "-//ietf//dtd html 2.0 strict level 1//en",
            "-//ietf//dtd html 2.0 strict level 2//en",
            "-//ietf//dtd html 2.0 strict//en",
            "-//ietf//dtd html 2.0//en",
            "-//ietf//dtd html 2.1e//en",
            "-//ietf//dtd html 3.0//en",
            "-//ietf//dtd html 3.0//en//",
            "-//ietf//dtd html 3.2 final//en",
            "-//ietf//dtd html 3.2//en",
            "-//ietf//dtd html 3//en",
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
            "-//ietf//dtd html strict//en//3.0",
            "-//ietf//dtd html//en",
            "-//ietf//dtd html//en//2.0",
            "-//ietf//dtd html//en//3.0",
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
            "-//o'reilly and associates//dtd html extended relaxed 1.0//en",
            "-//softquad software//dtd hotmetal pro 6.0::19990601::extensions to html 4.0//en",
            "-//softquad//dtd hotmetal pro 4.0::19971010::extensions to html 4.0//en",
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

    private static final int IN_FOREIGN = 0;

    private static final int NOT_IN_FOREIGN = 1;

    private final StackNode<T> MARKER = new StackNode<T>(null, new ElementName(null), null);

    private final boolean nonConformingAndStreaming;

    private final boolean conformingAndStreaming;

    private final boolean coalescingText;

    private boolean bodyCloseReported = false;

    private boolean htmlCloseReported = false;

    private InsertionMode mode = InsertionMode.INITIAL;

    private int foreignFlag = NOT_IN_FOREIGN;

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

    protected TreeBuilder(XmlViolationPolicy streamabilityViolationPolicy,
            boolean coalescingText) {
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
        SAXParseException spe = new SAXParseException(
                "Last error required non-streamable recovery.", tokenizer);
        if (errorHandler != null) {
            errorHandler.fatalError(spe);
        }
        throw spe;
    }

    protected final void fatal(Exception e) throws SAXException {
        SAXParseException spe = new SAXParseException(e.getMessage(),
                tokenizer, e);
        ;
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
        stack = new StackNode[64];
        listOfActiveFormattingElements = new StackNode[64];
        needToDropLF = false;
        cdataOrRcdataTimesToPop = 0;
        currentPtr = -1;
        formPointer = null;
        wantingComments = wantsComments();
        start(context != null);
        if (context == null) {
            mode = InsertionMode.INITIAL;
            foreignFlag = NOT_IN_FOREIGN;
        } else {
            T elt = createHtmlElementSetAsRoot(tokenizer.newAttributes());
            StackNode<T> node = new StackNode<T>(
                    "http://www.w3.org/1999/xhtml", ElementName.HTML, elt);
            currentPtr++;
            stack[currentPtr] = node;
            resetTheInsertionMode();
            if ("title" == context || "textarea" == context) {
                tokenizer.setContentModelFlag(ContentModelFlag.RCDATA, context);
            } else if ("style" == context || "script" == context
                    || "xmp" == context || "iframe" == context
                    || "noembed" == context || "noframes" == context
                    || (scriptingEnabled && "noscript" == context)) {
                tokenizer.setContentModelFlag(ContentModelFlag.CDATA, context);
            } else if ("plaintext" == context) {
                tokenizer.setContentModelFlag(ContentModelFlag.PLAINTEXT,
                        context);
            } else {
                tokenizer.setContentModelFlag(ContentModelFlag.PCDATA, context);
            }
        }
    }

    public final void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean forceQuirks) throws SAXException {
        needToDropLF = false;
        doctypeloop: for (;;) {
            switch (foreignFlag) {
                case IN_FOREIGN:
                    break doctypeloop;
                default:
                    switch (mode) {
                        case INITIAL:
                            /*
                             * A DOCTYPE token If the DOCTYPE token's name does
                             * not case-insensitively match the string "HTML",
                             * or if the token's public identifier is not
                             * missing, or if the token's system identifier is
                             * not missing, then there is a parse error.
                             * Conformance checkers may, instead of reporting
                             * this error, switch to a conformance checking mode
                             * for another language (e.g. based on the DOCTYPE
                             * token a conformance checker could recognise that
                             * the document is an HTML4-era document, and defer
                             * to an HTML4 conformance checker.)
                             * 
                             * Append a DocumentType node to the Document node,
                             * with the name attribute set to the name given in
                             * the DOCTYPE token; the publicId attribute set to
                             * the public identifier given in the DOCTYPE token,
                             * or the empty string if the public identifier was
                             * not set; the systemId attribute set to the system
                             * identifier given in the DOCTYPE token, or the
                             * empty string if the system identifier was not
                             * set; and the other attributes specific to
                             * DocumentType objects set to null and empty lists
                             * as appropriate. Associate the DocumentType node
                             * with the Document object so that it is returned
                             * as the value of the doctype attribute of the
                             * Document object.
                             */
                            if (reportingDoctype) {
                                appendDoctypeToDocument(name,
                                        publicIdentifier == null ? ""
                                                : publicIdentifier,
                                        systemIdentifier == null ? ""
                                                : systemIdentifier);
                            }
                            /*
                             * Then, if the DOCTYPE token matches one of the
                             * conditions in the following list, then set the
                             * document to quirks mode:
                             * 
                             * Otherwise, if the DOCTYPE token matches one of
                             * the conditions in the following list, then set
                             * the document to limited quirks mode: + The public
                             * identifier is set to: "-//W3C//DTD XHTML 1.0
                             * Frameset//EN" + The public identifier is set to:
                             * "-//W3C//DTD XHTML 1.0 Transitional//EN" + The
                             * system identifier is not missing and the public
                             * identifier is set to: "-//W3C//DTD HTML 4.01
                             * Frameset//EN" + The system identifier is not
                             * missing and the public identifier is set to:
                             * "-//W3C//DTD HTML 4.01 Transitional//EN"
                             * 
                             * The name, system identifier, and public
                             * identifier strings must be compared to the values
                             * given in the lists above in a case-insensitive
                             * manner.
                             */
                            String publicIdentifierLC = toAsciiLowerCase(publicIdentifier);
                            String systemIdentifierLC = toAsciiLowerCase(systemIdentifier);
                            switch (doctypeExpectation) {
                                case HTML:
                                    if (isQuirky(name, publicIdentifierLC,
                                            systemIdentifierLC, forceQuirks)) {
                                        err("Quirky doctype.");
                                        documentModeInternal(
                                                DocumentMode.QUIRKS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
                                    } else if (isAlmostStandards(
                                            publicIdentifierLC,
                                            systemIdentifierLC)) {
                                        err("Almost standards mode doctype.");
                                        documentModeInternal(
                                                DocumentMode.ALMOST_STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
                                    } else {
                                        if (!(publicIdentifier == null && systemIdentifier == null)) {
                                            err("Legacy doctype.");
                                        }
                                        documentModeInternal(
                                                DocumentMode.STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
                                    }
                                    break;
                                case HTML401_STRICT:
                                    tokenizer.turnOnAdditionalHtml4Errors();
                                    if (isQuirky(name, publicIdentifierLC,
                                            systemIdentifierLC, forceQuirks)) {
                                        err("Quirky doctype.");
                                        documentModeInternal(
                                                DocumentMode.QUIRKS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    } else if (isAlmostStandards(
                                            publicIdentifierLC,
                                            systemIdentifierLC)) {
                                        err("Almost standards mode doctype.");
                                        documentModeInternal(
                                                DocumentMode.ALMOST_STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    } else {
                                        if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                                            if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                                warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                            }
                                        } else {
                                            err("The doctype was not the HTML 4.01 Strict doctype.");
                                        }
                                        documentModeInternal(
                                                DocumentMode.STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    }
                                    break;
                                case HTML401_TRANSITIONAL:
                                    tokenizer.turnOnAdditionalHtml4Errors();
                                    if (isQuirky(name, publicIdentifierLC,
                                            systemIdentifierLC, forceQuirks)) {
                                        err("Quirky doctype.");
                                        documentModeInternal(
                                                DocumentMode.QUIRKS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    } else if (isAlmostStandards(
                                            publicIdentifierLC,
                                            systemIdentifierLC)) {
                                        if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)
                                                && systemIdentifier != null) {
                                            if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                                warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                            }
                                        } else {
                                            err("The doctype was not a non-quirky HTML 4.01 Transitional doctype.");
                                        }
                                        documentModeInternal(
                                                DocumentMode.ALMOST_STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    } else {
                                        err("The doctype was not the HTML 4.01 Transitional doctype.");
                                        documentModeInternal(
                                                DocumentMode.STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, true);
                                    }
                                    break;
                                case AUTO:
                                    boolean html4 = isHtml4Doctype(publicIdentifier);
                                    if (html4) {
                                        tokenizer.turnOnAdditionalHtml4Errors();
                                    }
                                    if (isQuirky(name, publicIdentifierLC,
                                            systemIdentifierLC, forceQuirks)) {
                                        err("Quirky doctype.");
                                        documentModeInternal(
                                                DocumentMode.QUIRKS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, html4);
                                    } else if (isAlmostStandards(
                                            publicIdentifierLC,
                                            systemIdentifierLC)) {
                                        if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)) {
                                            tokenizer.turnOnAdditionalHtml4Errors();
                                            if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                                warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                            }
                                        } else {
                                            err("Almost standards mode doctype.");
                                        }
                                        documentModeInternal(
                                                DocumentMode.ALMOST_STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, html4);
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
                                        documentModeInternal(
                                                DocumentMode.STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, html4);
                                    }
                                    break;
                                case NO_DOCTYPE_ERRORS:
                                    if (isQuirky(name, publicIdentifierLC,
                                            systemIdentifierLC, forceQuirks)) {
                                        documentModeInternal(
                                                DocumentMode.QUIRKS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
                                    } else if (isAlmostStandards(
                                            publicIdentifierLC,
                                            systemIdentifierLC)) {
                                        documentModeInternal(
                                                DocumentMode.ALMOST_STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
                                    } else {
                                        documentModeInternal(
                                                DocumentMode.STANDARDS_MODE,
                                                publicIdentifier,
                                                systemIdentifier, false);
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
                            break doctypeloop;
                    }
            }

        }
        /*
         * A DOCTYPE token Parse error.
         */
        err("Stray doctype.");
        /*
         * Ignore the token.
         */
        return;
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
            commentloop: for (;;) {
                switch (foreignFlag) {
                    case IN_FOREIGN:
                        break commentloop;
                    default:
                        switch (mode) {
                            case INITIAL:
                            case BEFORE_HTML:
                            case AFTER_AFTER_BODY:
                            case AFTER_AFTER_FRAMESET:
                                /*
                                 * A comment token Append a Comment node to the
                                 * Document object with the data attribute set
                                 * to the data given in the comment token.
                                 */
                                appendCommentToDocument(buf, 0, length);
                                return;
                            case AFTER_BODY:
                                /*
                                 * * A comment token Append a Comment node to
                                 * the first element in the stack of open
                                 * elements (the html element), with the data
                                 * attribute set to the data given in the
                                 * comment token.
                                 * 
                                 */
                                flushCharacters();
                                appendComment(stack[0].node, buf, 0, length);
                                return;
                            default:
                                break commentloop;
                        }
                }
            }
        }
        /*
         * * A comment token Append a Comment node to the current node with the
         * data attribute set to the data given in the comment token.
         * 
         */
        flushCharacters();
        appendComment(stack[currentPtr].node, buf, 0, length);
        return;
    }

    /**
     * @see nu.validator.htmlparser.impl.TokenHandler#characters(char[], int,
     *      int)
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
        if (foreignFlag == IN_FOREIGN || mode == InsertionMode.IN_BODY
                || mode == InsertionMode.IN_CELL
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    null, null, false);
                            /*
                             * Then, switch to the root element mode of the tree
                             * construction stage
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
                            }
                            reconstructTheActiveFormattingElements();
                            appendCharMayFoster(buf, i);
                            start = i + 1;
                            continue;
                        case IN_COLUMN_GROUP:
                            if (start < i) {
                                accumulateCharacters(buf, start, i - start);
                                start = i;
                            }
                            /*
                             * Act as if an end tag with the tag name "colgroup"
                             * had been seen, and then, if that token wasn't
                             * ignored, reprocess the current token.
                             */
                            if (currentPtr == 0) {
                                err("Non-space in \u201Ccolgroup\u201D when parsing fragment.");
                                start = i + 1;
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
                                accumulateCharacters(buf, start, i - start);
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
                                accumulateCharacters(buf, start, i - start);
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
            switch (foreignFlag) {
                case IN_FOREIGN:
                    err("End of file in a foreign namespace context.");
                    while (stack[currentPtr].ns != "http://www.w3.org/1999/xhtml") {
                        pop();
                    }
                    foreignFlag = NOT_IN_FOREIGN;
                default:
            }
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
                        documentModeInternal(DocumentMode.QUIRKS_MODE, null,
                                null, false);
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
                        // XXX application cache manifest
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
                    case IN_COLUMN_GROUP:
                        if (currentPtr == 0) {
                            assert context != null;
                            break eofloop;
                        } else {
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            continue;
                        }
                    case IN_CAPTION:
                    case IN_CELL:
                    case IN_BODY:
                        for (int i = currentPtr; i >= 0; i--) {
                            String name = stack[i].name;
                            if (!("dd" == name || "dt" == name || "li" == name
                                    || "p" == name || "tbody" == name
                                    || "td" == name || "tfoot" == name
                                    || "th" == name || "thead" == name
                                    || "body" == name || "html" == name)) {
                                err("End of file seen and there were open elements.");
                                break;
                            }
                        }
                        break eofloop;
                    case IN_TABLE_BODY:
                    case IN_ROW:
                    case IN_TABLE:
                    case IN_SELECT:
                    case IN_SELECT_IN_TABLE:
                    case IN_FRAMESET:
                        if (currentPtr > 0) {
                            err("End of file seen and there were open elements.");
                        }
                        break eofloop;
                    case AFTER_BODY:
                    case AFTER_FRAMESET:
                    case AFTER_AFTER_BODY:
                    case AFTER_AFTER_FRAMESET:
                        break eofloop;
                }
            }
            while (currentPtr > 1) {
                pop();
            }
            if (currentPtr == 1) {
                if (stack[1].name == "body") {
                    if (!bodyCloseReported) {
                        bodyClosed(stack[1].node);
                    }
                } else {
                    pop();
                }
            }
            if (context == null && !htmlCloseReported) {
                htmlClosed(stack[0].node);
            }
            /* Stop parsing. */
        } finally {
            stack = null;
            listOfActiveFormattingElements = null;
            end();
        }
    }

    public final void startTag(ElementName elementName, Attributes attributes,
            boolean selfClosing) throws SAXException {
        int eltPos;
        needToDropLF = false;
        boolean needsPostProcessing = false;
        starttagloop: for (;;) {
            int magic = elementName.magic;
            String name = elementName.name;
            switch (foreignFlag) {
                case IN_FOREIGN:
                    StackNode<T> currentNode = stack[currentPtr];
                    String currNs = currentNode.ns;
                    String currName = currentNode.name;
                    if (("http://www.w3.org/1999/xhtml" == currNs)
                            || ((!("mglyph" == name || "malignmark" == name))
                                    && "http://www.w3.org/1998/Math/MathML" == currNs && ("mi" == currName
                                    || "mo" == currName
                                    || "mn" == currName
                                    || "ms" == currName || "mtext" == currName))) {
                        needsPostProcessing = true;
                        // fall through to normal stuff under default
                    } else {
                        switch (magic) {
                            case B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U:
                            case DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU:
                            case BODY:
                            case BR:
                            case CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR:
                            case DD_OR_DT:
                            case EMBED_OR_IMG:
                            case H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6:
                            case HEAD:
                            case HR:
                            case LI:
                            case META:
                            case NOBR:
                            case P:
                            case PRE_OR_LISTING:
                                err("HTML start tag \u201C"
                                        + name
                                        + "\u201D in a foreign namespace context.");
                                while (stack[currentPtr].ns != "http://www.w3.org/1999/xhtml") {
                                    pop();
                                }
                                foreignFlag = NOT_IN_FOREIGN;
                                continue starttagloop;
                            default:
                                attributes = adjustForeignAttributes(attributes);
                                if (selfClosing) {
                                    appendVoidElementToCurrentMayFoster(currNs,
                                            name, attributes);
                                    selfClosing = false;
                                } else {
                                    appendToCurrentNodeAndPushElementMayFoster(
                                            currNs, elementName, attributes);
                                }
                                break starttagloop;
                        }
                    }
                default:
                    switch (mode) {
                        case IN_TABLE_BODY:
                            switch (magic) {
                                case TR:
                                    clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    mode = InsertionMode.IN_ROW;
                                    break starttagloop;
                                case TD_OR_TH:
                                    err("\u201C" + name
                                            + "\u201D start tag in table body.");
                                    clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            ElementName.TR,
                                            EmptyAttributes.EMPTY_ATTRIBUTES);
                                    mode = InsertionMode.IN_ROW;
                                    continue;
                                case CAPTION:
                                case COL:
                                case COLGROUP:
                                case TBODY_OR_THEAD_OR_TFOOT:
                                    eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                                    if (eltPos == 0) {
                                        err("Stray \u201C" + name
                                                + "\u201D start tag.");
                                        break starttagloop;
                                    } else {
                                        clearStackBackTo(eltPos);
                                        pop();
                                        mode = InsertionMode.IN_TABLE;
                                        continue;
                                    }
                                default:
                                    // fall through to IN_TABLE
                            }
                        case IN_ROW:
                            switch (magic) {
                                case TD_OR_TH:
                                    clearStackBackTo(findLastOrRoot(TR));
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    mode = InsertionMode.IN_CELL;
                                    insertMarker();
                                    break starttagloop;
                                case CAPTION:
                                case COL:
                                case COLGROUP:
                                case TBODY_OR_THEAD_OR_TFOOT:
                                case TR:
                                    eltPos = findLastOrRoot(TR);
                                    if (eltPos == 0) {
                                        assert context != null;
                                        err("No table row to close.");
                                        break starttagloop;
                                    }
                                    clearStackBackTo(eltPos);
                                    pop();
                                    mode = InsertionMode.IN_TABLE_BODY;
                                    continue;
                                default:
                                    // fall through to IN_TABLE
                            }
                        case IN_TABLE:
                            intableloop: for (;;) {
                                switch (magic) {
                                    case CAPTION:
                                        clearStackBackTo(findLastOrRoot(TABLE));
                                        insertMarker();
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        mode = InsertionMode.IN_CAPTION;
                                        break starttagloop;
                                    case COLGROUP:
                                        clearStackBackTo(findLastOrRoot(TABLE));
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        mode = InsertionMode.IN_COLUMN_GROUP;
                                        break starttagloop;
                                    case COL:
                                        clearStackBackTo(findLastOrRoot(TABLE));
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                ElementName.COLGROUP,
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        mode = InsertionMode.IN_COLUMN_GROUP;
                                        continue starttagloop;
                                    case TBODY_OR_THEAD_OR_TFOOT:
                                        clearStackBackTo(findLastOrRoot(TABLE));
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        mode = InsertionMode.IN_TABLE_BODY;
                                        break starttagloop;
                                    case TR:
                                    case TD_OR_TH:
                                        clearStackBackTo(findLastOrRoot(TABLE));
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                ElementName.TBODY,
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        mode = InsertionMode.IN_TABLE_BODY;
                                        continue starttagloop;
                                    case TABLE:
                                        err("Start tag for \u201Ctable\u201D seen but the previous \u201Ctable\u201D is still open.");
                                        eltPos = findLastInTableScope(name);
                                        if (eltPos == NOT_FOUND_ON_STACK) {
                                            assert context != null;
                                            break starttagloop;
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
                                        continue starttagloop;
                                    case SCRIPT:
                                    case STYLE:
                                        if (isTainted()) {
                                            break intableloop;
                                        }
                                        // XXX need to manage much more stuff
                                        // here if
                                        // supporting
                                        // document.write()
                                        appendToCurrentNodeAndPushElement(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        cdataOrRcdataTimesToPop = 1;
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.CDATA, elementName);
                                        break starttagloop;
                                    case INPUT:
                                        if (isTainted()
                                                || !equalsIgnoreAsciiCase(
                                                        "hidden",
                                                        attributes.getValue("",
                                                                "type"))) {
                                            break intableloop;
                                        }
                                        appendVoidElementToCurrent(
                                                "http://www.w3.org/1999/xhtml",
                                                name, attributes, formPointer);
                                        selfClosing = false;
                                        break starttagloop;
                                    default:
                                        err("Start tag \u201C"
                                                + name
                                                + "\u201D seen in \u201Ctable\u201D.");
                                        // fall through to IN_BODY
                                        break intableloop;
                                }
                            }
                        case IN_CAPTION:
                            switch (magic) {
                                case CAPTION:
                                case COL:
                                case COLGROUP:
                                case TBODY_OR_THEAD_OR_TFOOT:
                                case TR:
                                case TD_OR_TH:
                                    err("Stray \u201C"
                                            + name
                                            + "\u201D start tag in \u201Ccaption\u201D.");
                                    eltPos = findLastInTableScope("caption");
                                    if (eltPos == NOT_FOUND_ON_STACK) {
                                        break starttagloop;
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
                                default:
                                    // fall through to IN_BODY
                            }
                        case IN_CELL:
                            switch (magic) {
                                case CAPTION:
                                case COL:
                                case COLGROUP:
                                case TBODY_OR_THEAD_OR_TFOOT:
                                case TR:
                                case TD_OR_TH:
                                    eltPos = findLastInTableScopeTdTh();
                                    if (eltPos == NOT_FOUND_ON_STACK) {
                                        err("No cell to close.");
                                        break starttagloop;
                                    } else {
                                        closeTheCell(eltPos);
                                        continue;
                                    }
                                default:
                                    // fall through to IN_BODY
                            }
                        case IN_BODY:
                            inbodyloop: for (;;) {
                                switch (magic) {
                                    case HTML:
                                        err("Stray \u201Chtml\u201D start tag.");
                                        addAttributesToElement(stack[0].node,
                                                attributes);
                                        break starttagloop;
                                    case BASE:
                                    case LINK:
                                    case META:
                                    case STYLE:
                                    case SCRIPT:
                                    case TITLE:
                                        // Fall through to IN_HEAD
                                        break inbodyloop;
                                    case BODY:
                                        err("\u201Cbody\u201D start tag found but the \u201Cbody\u201D element is already open.");
                                        addAttributesToBody(attributes);
                                        break starttagloop;
                                    case P:
                                    case DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU:
                                    case H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6:
                                    case UL_OR_OL_OR_DL:
                                    case FIELDSET_OR_ADDRESS_OR_DIR:
                                        implicitlyCloseP();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case PRE_OR_LISTING:
                                        implicitlyCloseP();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        needToDropLF = true;
                                        break starttagloop;
                                    case FORM:
                                        if (formPointer != null) {
                                            err("Saw a \u201Cform\u201D start tag, but there was already an active \u201Cform\u201D element.");
                                            break starttagloop;
                                        } else {
                                            implicitlyCloseP();
                                            appendToCurrentNodeAndPushFormElementMayFoster(attributes);
                                            break starttagloop;
                                        }
                                    case LI:
                                        implicitlyCloseP();
                                        eltPos = findLiToPop();
                                        if (eltPos < currentPtr) {
                                            err("A \u201Cli\u201D start tag was seen but the previous \u201Cli\u201D element had open children.");
                                        }
                                        while (currentPtr >= eltPos) {
                                            pop();
                                        }
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case DD_OR_DT:
                                        implicitlyCloseP();
                                        eltPos = findDdOrDtToPop();
                                        if (eltPos < currentPtr) {
                                            err("A definition list item start tag was seen but the previous definition list item element had open children.");
                                        }
                                        while (currentPtr >= eltPos) {
                                            pop();
                                        }
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case PLAINTEXT:
                                        implicitlyCloseP();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.PLAINTEXT,
                                                elementName);
                                        break starttagloop;
                                    case A:
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
                                        appendToCurrentNodeAndPushFormattingElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U:
                                        reconstructTheActiveFormattingElements();
                                        appendToCurrentNodeAndPushFormattingElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case NOBR:
                                        reconstructTheActiveFormattingElements();
                                        if (NOT_FOUND_ON_STACK != findLastInScope("nobr")) {
                                            err("\u201Cnobr\u201D start tag seen when there was an open \u201Cnobr\u201D element in scope.");
                                            adoptionAgencyEndTag("nobr");
                                        }
                                        appendToCurrentNodeAndPushFormattingElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                    case BUTTON:
                                        eltPos = findLastInScope(name);
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
                                            continue starttagloop;
                                        } else {
                                            reconstructTheActiveFormattingElements();
                                            appendToCurrentNodeAndPushElementMayFoster(
                                                    "http://www.w3.org/1999/xhtml",
                                                    elementName, attributes,
                                                    formPointer);
                                            insertMarker();
                                            break starttagloop;
                                        }
                                    case OBJECT_OR_MARQUEE_OR_APPLET:
                                        reconstructTheActiveFormattingElements();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        insertMarker();
                                        break starttagloop;
                                    case XMP:
                                        reconstructTheActiveFormattingElements();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        cdataOrRcdataTimesToPop = 1;
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.CDATA, elementName);
                                        break starttagloop;
                                    case TABLE:
                                        implicitlyCloseP();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        mode = InsertionMode.IN_TABLE;
                                        break starttagloop;
                                    case BR:
                                    case EMBED_OR_IMG:
                                    case AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR:
                                        reconstructTheActiveFormattingElements();
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                name, attributes);
                                        selfClosing = false;
                                        break starttagloop;
                                    case HR:
                                        implicitlyCloseP();
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                name, attributes);
                                        selfClosing = false;
                                        break starttagloop;
                                    case IMAGE:
                                        err("Saw a start tag \u201Cimage\u201D.");
                                        elementName = ElementName.IMG;
                                        continue starttagloop;
                                    case INPUT:
                                        reconstructTheActiveFormattingElements();
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                name, attributes, formPointer);
                                        selfClosing = false;
                                        break starttagloop;
                                    case ISINDEX:
                                        err("\u201Cisindex\u201D seen.");
                                        if (formPointer != null) {
                                            break starttagloop;
                                        }
                                        implicitlyCloseP();
                                        AttributesImpl formAttrs = tokenizer.newAttributes();
                                        int actionIndex = attributes.getIndex("action");
                                        if (actionIndex > -1) {
                                            formAttrs.addAttribute(
                                                    "action",
                                                    attributes.getValue(actionIndex));
                                        }
                                        appendToCurrentNodeAndPushFormElementMayFoster(formAttrs);
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                "hr",
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                ElementName.P,
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                ElementName.LABEL,
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        int promptIndex = attributes.getIndex("prompt");
                                        if (promptIndex > -1) {
                                            char[] prompt = attributes.getValue(
                                                    promptIndex).toCharArray();
                                            appendCharacters(
                                                    stack[currentPtr].node,
                                                    prompt, 0, prompt.length);
                                        } else {
                                            // XXX localization
                                            appendCharacters(
                                                    stack[currentPtr].node,
                                                    ISINDEX_PROMPT, 0,
                                                    ISINDEX_PROMPT.length);
                                        }
                                        AttributesImpl inputAttributes = tokenizer.newAttributes();
                                        inputAttributes.addAttribute("name",
                                                "isindex");
                                        for (int i = 0; i < attributes.getLength(); i++) {
                                            String attributeQName = attributes.getQName(i);
                                            if (!("name".equals(attributeQName)
                                                    || "action".equals(attributeQName) || "prompt".equals(attributeQName))) {
                                                inputAttributes.addAttribute(
                                                        attributeQName,
                                                        attributes.getValue(i));
                                            }
                                        }
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                "input", inputAttributes,
                                                formPointer);
                                        // XXX localization
                                        pop(); // label
                                        pop(); // p
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                "hr",
                                                EmptyAttributes.EMPTY_ATTRIBUTES);
                                        pop(); // form
                                        selfClosing = false;
                                        break starttagloop;
                                    case TEXTAREA:
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes, formPointer);
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.RCDATA, elementName);
                                        cdataOrRcdataTimesToPop = 1;
                                        needToDropLF = true;
                                        break starttagloop;
                                    case NOSCRIPT:
                                        if (!scriptingEnabled) {
                                            reconstructTheActiveFormattingElements();
                                            appendToCurrentNodeAndPushElementMayFoster(
                                                    "http://www.w3.org/1999/xhtml",
                                                    elementName, attributes);
                                            break starttagloop;
                                        } else {
                                            // fall through
                                        }
                                    case IFRAME_OR_NOEMBED:
                                    case NOFRAMES:
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        cdataOrRcdataTimesToPop = 1;
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.CDATA, elementName);
                                        break starttagloop;
                                    case SELECT:
                                        reconstructTheActiveFormattingElements();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes, formPointer);
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
                                        break starttagloop;
                                    case MATH:
                                        reconstructTheActiveFormattingElements();
                                        attributes = adjustForeignAttributes(attributes);
                                        if (selfClosing) {
                                            appendVoidElementToCurrentMayFoster(
                                                    "http://www.w3.org/1998/Math/MathML",
                                                    name, attributes);
                                            selfClosing = false;
                                        } else {
                                            appendToCurrentNodeAndPushElementMayFoster(
                                                    "http://www.w3.org/1998/Math/MathML",
                                                    elementName, attributes);
                                            foreignFlag = IN_FOREIGN;
                                        }
                                        break starttagloop;
                                    case CAPTION:
                                    case COL:
                                    case COLGROUP:
                                    case TBODY_OR_THEAD_OR_TFOOT:
                                    case TR:
                                    case TD_OR_TH:
                                    case FRAME:
                                    case FRAMESET:
                                    case HEAD:
                                    case OPTION:
                                    case OPTGROUP:
                                        err("Stray start tag \u201C" + name
                                                + "\u201D.");
                                        break starttagloop;
                                    default:
                                        reconstructTheActiveFormattingElements();
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        break starttagloop;
                                }
                            }
                        case IN_HEAD:
                            inheadloop: for (;;) {
                                switch (magic) {
                                    case HTML:
                                        err("Stray \u201Chtml\u201D start tag.");
                                        addAttributesToElement(stack[0].node,
                                                attributes);
                                        break starttagloop;
                                    case BASE:
                                        appendVoidElementToCurrentMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                name, attributes);
                                        selfClosing = false;
                                        break starttagloop;
                                    case META:
                                    case LINK:
                                        // Fall through to IN_HEAD_NOSCRIPT
                                        break inheadloop;
                                    case TITLE:
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        cdataOrRcdataTimesToPop = 1;
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.RCDATA, elementName);
                                        break starttagloop;
                                    case NOSCRIPT:
                                        if (scriptingEnabled) {
                                            appendToCurrentNodeAndPushElement(
                                                    "http://www.w3.org/1999/xhtml",
                                                    elementName, attributes);
                                            cdataOrRcdataTimesToPop = 1;
                                            tokenizer.setContentModelFlag(
                                                    ContentModelFlag.CDATA,
                                                    elementName);
                                        } else {
                                            appendToCurrentNodeAndPushElementMayFoster(
                                                    "http://www.w3.org/1999/xhtml",
                                                    elementName, attributes);
                                            mode = InsertionMode.IN_HEAD_NOSCRIPT;
                                        }
                                        break starttagloop;
                                    case SCRIPT:
                                    case STYLE:
                                        // XXX need to manage much more stuff
                                        // here if
                                        // supporting
                                        // document.write()
                                        appendToCurrentNodeAndPushElementMayFoster(
                                                "http://www.w3.org/1999/xhtml",
                                                elementName, attributes);
                                        cdataOrRcdataTimesToPop = 1;
                                        tokenizer.setContentModelFlag(
                                                ContentModelFlag.CDATA, elementName);
                                        break starttagloop;
                                    case HEAD:
                                        /* Parse error. */
                                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                                        /* Ignore the token. */
                                        break starttagloop;
                                    default:
                                        pop();
                                        mode = InsertionMode.AFTER_HEAD;
                                        continue starttagloop;
                                }
                            }
                        case IN_HEAD_NOSCRIPT:
                            switch (magic) {
                                case HTML:
                                    // XXX did Hixie really mean to omit "base"
                                    // here?
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case LINK:
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    break starttagloop;
                                case META:
                                    checkMetaCharset(attributes);
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    break starttagloop;
                                case STYLE:
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    cdataOrRcdataTimesToPop = 1;
                                    tokenizer.setContentModelFlag(
                                            ContentModelFlag.CDATA, elementName);
                                    break starttagloop;
                                case HEAD:
                                    err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                                    break starttagloop;
                                case NOSCRIPT:
                                    err("Start tag for \u201Cnoscript\u201D seen when \u201Cnoscript\u201D was already open.");
                                    break starttagloop;
                                default:
                                    err("Bad start tag in \u201Cnoscript\u201D in \u201Chead\u201D.");
                                    pop();
                                    mode = InsertionMode.IN_HEAD;
                                    continue;
                            }
                        case IN_COLUMN_GROUP:
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case COL:
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    break starttagloop;
                                default:
                                    if (currentPtr == 0) {
                                        assert context != null;
                                        err("Garbage in \u201Ccolgroup\u201D fragment.");
                                        break starttagloop;
                                    }
                                    pop();
                                    mode = InsertionMode.IN_TABLE;
                                    continue;
                            }
                        case IN_SELECT_IN_TABLE:
                            switch (magic) {
                                case CAPTION:
                                case TBODY_OR_THEAD_OR_TFOOT:
                                case TR:
                                case TD_OR_TH:
                                case TABLE:
                                    err("\u201C"
                                            + name
                                            + "\u201D start tag with \u201Cselect\u201D open.");
                                    endSelect();
                                    continue;
                                default:
                                    // fall through to IN_SELECT
                            }
                        case IN_SELECT:
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case OPTION:
                                    if (isCurrent("option")) {
                                        pop();
                                    }
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    break starttagloop;
                                case OPTGROUP:
                                    if (isCurrent("option")) {
                                        pop();
                                    }
                                    if (isCurrent("optgroup")) {
                                        pop();
                                    }
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    break starttagloop;
                                case SELECT:
                                    err("\u201Cselect\u201D start tag where end tag expected.");
                                    eltPos = findLastInTableScope(name);
                                    if (eltPos == NOT_FOUND_ON_STACK) {
                                        assert context != null;
                                        err("No \u201Cselect\u201D in table scope.");
                                        break starttagloop;
                                    } else {
                                        while (currentPtr >= eltPos) {
                                            pop();
                                        }
                                        resetTheInsertionMode();
                                        break starttagloop;
                                    }
                                case INPUT:
                                    err("\u201Cinput\u201D start tag seen in \u201Cselect\2201D.");
                                    endSelect();
                                    continue;
                                default:
                                    err("Stray \u201C" + name
                                            + "\u201D start tag.");
                                    break starttagloop;
                            }
                        case AFTER_BODY:
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                default:
                                    err("Stray \u201C" + name
                                            + "\u201D start tag.");
                                    if (conformingAndStreaming) {
                                        fatal();
                                    }
                                    mode = InsertionMode.IN_BODY;
                                    continue;
                            }
                        case IN_FRAMESET:
                            switch (magic) {
                                case FRAMESET:
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    break starttagloop;
                                case FRAME:
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    break starttagloop;
                                default:
                                    // fall through to AFTER_FRAMESET
                            }
                        case AFTER_FRAMESET:
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case NOFRAMES:
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    cdataOrRcdataTimesToPop = 1;
                                    tokenizer.setContentModelFlag(
                                            ContentModelFlag.CDATA, elementName);
                                    break starttagloop;
                                default:
                                    err("Stray \u201C" + name
                                            + "\u201D start tag.");
                                    break starttagloop;
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
                            documentModeInternal(DocumentMode.QUIRKS_MODE,
                                    null, null, false);
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
                            switch (magic) {
                                case HTML:
                                    // optimize error check and streaming SAX by
                                    // hoisting
                                    // "html" handling here.
                                    if (attributes.getLength() == 0) {
                                        // This has the right magic side effect
                                        // that
                                        // it
                                        // makes attributes in SAX Tree mutable.
                                        appendHtmlElementToDocumentAndPush();
                                    } else {
                                        appendHtmlElementToDocumentAndPush(attributes);
                                    }
                                    // XXX application cache should fire here
                                    mode = InsertionMode.BEFORE_HEAD;
                                    break starttagloop;
                                default:
                                    /*
                                     * Create an HTMLElement node with the tag
                                     * name html, in the HTML namespace. Append
                                     * it to the Document object.
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
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case HEAD:
                                    /*
                                     * A start tag whose tag name is "head"
                                     * 
                                     * Create an element for the token.
                                     * 
                                     * Set the head element pointer to this new
                                     * element node.
                                     * 
                                     * Append the new element to the current
                                     * node and push it onto the stack of open
                                     * elements.
                                     */
                                    appendToCurrentNodeAndPushHeadElement(attributes);
                                    /*
                                     * 
                                     * Change the insertion mode to "in head".
                                     * 
                                     */
                                    mode = InsertionMode.IN_HEAD;
                                    break starttagloop;
                                default:

                                    /*
                                     * Any other start tag token
                                     */

                                    /*
                                     * Act as if a start tag token with the tag
                                     * name "head" and no attributes had been
                                     * seen,
                                     */
                                    appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                                    mode = InsertionMode.IN_HEAD;
                                    /*
                                     * then reprocess the current token.
                                     * 
                                     * This will result in an empty head element
                                     * being generated, with the current token
                                     * being reprocessed in the "after head"
                                     * insertion mode.
                                     */
                                    continue;
                            }
                        case AFTER_HEAD:
                            switch (magic) {
                                case HTML:
                                    err("Stray \u201Chtml\u201D start tag.");
                                    addAttributesToElement(stack[0].node,
                                            attributes);
                                    break starttagloop;
                                case BODY:
                                    if (attributes.getLength() == 0) {
                                        // This has the right magic side effect
                                        // that
                                        // it
                                        // makes attributes in SAX Tree mutable.
                                        appendToCurrentNodeAndPushBodyElement();
                                    } else {
                                        appendToCurrentNodeAndPushBodyElement(attributes);
                                    }
                                    mode = InsertionMode.IN_BODY;
                                    break starttagloop;
                                case FRAMESET:
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    mode = InsertionMode.IN_FRAMESET;
                                    break starttagloop;
                                case BASE:
                                    err("\u201Cbase\u201D element outside \u201Chead\u201D.");
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    if (!nonConformingAndStreaming) {
                                        pop(); // head
                                    }
                                    break starttagloop;
                                case LINK:
                                    err("\u201Clink\u201D element outside \u201Chead\u201D.");
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    if (!nonConformingAndStreaming) {
                                        pop(); // head
                                    }
                                    break starttagloop;
                                case META:
                                    err("\u201Cmeta\u201D element outside \u201Chead\u201D.");
                                    checkMetaCharset(attributes);
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendVoidElementToCurrentMayFoster(
                                            "http://www.w3.org/1999/xhtml",
                                            name, attributes);
                                    selfClosing = false;
                                    if (!nonConformingAndStreaming) {
                                        pop(); // head
                                    }
                                    break starttagloop;
                                case SCRIPT:
                                    err("\u201Cscript\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                            : 2; // pops head
                                    tokenizer.setContentModelFlag(
                                            ContentModelFlag.CDATA, elementName);
                                    break starttagloop;
                                case STYLE:
                                    err("\u201Cstyle\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                            : 2; // pops head
                                    tokenizer.setContentModelFlag(
                                            ContentModelFlag.CDATA, elementName);
                                    break starttagloop;
                                case TITLE:
                                    err("\u201Ctitle\u201D element outside \u201Chead\u201D.");
                                    if (!nonConformingAndStreaming) {
                                        pushHeadPointerOntoStack();
                                    }
                                    appendToCurrentNodeAndPushElement(
                                            "http://www.w3.org/1999/xhtml",
                                            elementName, attributes);
                                    cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                            : 2; // pops head
                                    tokenizer.setContentModelFlag(
                                            ContentModelFlag.RCDATA, elementName);
                                    break starttagloop;
                                default:
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
        if (needsPostProcessing && foreignFlag == IN_FOREIGN
                && !hasForeignInScope()) {
            /*
             * If, after doing so, the insertion mode is still "in foreign
             * content", but there is no element in scope that has a namespace
             * other than the HTML namespace, switch the insertion mode to the
             * secondary insertion mode.
             */
            foreignFlag = NOT_IN_FOREIGN;
        }
        if (selfClosing) {
            err("Self-closing syntax (\u201C/>\u201D) used on a non-void HTML element.");
        }
    }

    private Attributes adjustForeignAttributes(Attributes attributes) {
        // TODO Auto-generated method stub
        return attributes;
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

    private void checkMetaCharset(Attributes attributes) throws SAXException {
        String content = attributes.getValue("", "content");
        String internalCharset = null;
        if (content != null) {
            internalCharset = MetaSniffer.extractCharsetFromContent(content);
            if (internalCharset != null) {
                if (!equalsIgnoreAsciiCase("content-type", attributes.getValue(
                        "", "http-equiv"))) {
                    warn("Attribute \u201Ccontent\u201D would be sniffed as an internal character encoding declaration but there was no matching \u201Chttp-equiv='Content-Type'\u201D attribute.");
                }
            }
        }
        if (internalCharset == null) {
            internalCharset = attributes.getValue("", "charset");
        }
        if (internalCharset != null) {
            tokenizer.internalEncodingDeclaration(internalCharset);
        }
    }

    public final void endTag(ElementName elementName)
            throws SAXException {
        needToDropLF = false;
        if (cdataOrRcdataTimesToPop > 0) {
            while (cdataOrRcdataTimesToPop > 0) {
                pop();
                cdataOrRcdataTimesToPop--;
            }
            return;
        }

        int eltPos;
        endtagloop: for (;;) {
            int magic = elementName.magic;
            String name = elementName.name;
            switch (mode) {
                case IN_ROW:
                    switch (magic) {
                        case TR:
                            eltPos = findLastOrRoot(TR);
                            if (eltPos == 0) {
                                assert context != null;
                                err("No table row to close.");
                                break endtagloop;
                            }
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE_BODY;
                            break endtagloop;
                        case TABLE:
                            eltPos = findLastOrRoot(TR);
                            if (eltPos == 0) {
                                assert context != null;
                                err("No table row to close.");
                                break endtagloop;
                            }
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE_BODY;
                            continue;
                        case TBODY_OR_THEAD_OR_TFOOT:
                            if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                                err("Stray end tag \u201C" + name + "\u201D.");
                                break endtagloop;
                            }
                            eltPos = findLastOrRoot(TR);
                            if (eltPos == 0) {
                                assert context != null;
                                err("No table row to close.");
                                break endtagloop;
                            }
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE_BODY;
                            continue;
                        case BODY:
                        case CAPTION:
                        case COL:
                        case COLGROUP:
                        case HTML:
                        case TD_OR_TH:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            // fall through to IN_TABLE
                    }
                case IN_TABLE_BODY:
                    switch (magic) {
                        case TBODY_OR_THEAD_OR_TFOOT:
                            eltPos = findLastOrRoot(name);
                            if (eltPos == 0) {
                                err("Stray end tag \u201C" + name + "\u201D.");
                                break endtagloop;
                            }
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            break endtagloop;
                        case TABLE:
                            eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                            if (eltPos == 0) {
                                assert context != null;
                                err("Stray end tag \u201Ctable\u201D.");
                                break endtagloop;
                            }
                            clearStackBackTo(eltPos);
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            continue;
                        case BODY:
                        case CAPTION:
                        case COL:
                        case COLGROUP:
                        case HTML:
                        case TD_OR_TH:
                        case TR:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    switch (magic) {
                        case TABLE:
                            eltPos = findLast("table");
                            if (eltPos == NOT_FOUND_ON_STACK) {
                                assert context != null;
                                err("Stray end tag \u201Ctable\u201D.");
                                break endtagloop;
                            }
                            while (currentPtr >= eltPos) {
                                pop();
                            }
                            resetTheInsertionMode();
                            break endtagloop;
                        case BODY:
                        case CAPTION:
                        case COL:
                        case COLGROUP:
                        case HTML:
                        case TBODY_OR_THEAD_OR_TFOOT:
                        case TD_OR_TH:
                        case TR:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            // fall through to IN_BODY
                    }
                case IN_CAPTION:
                    switch (magic) {
                        case CAPTION:
                            eltPos = findLastInTableScope("caption");
                            if (eltPos == NOT_FOUND_ON_STACK) {
                                break endtagloop;
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
                            break endtagloop;
                        case TABLE:
                            err("\u201Ctable\u201D closed but \u201Ccaption\u201D was still open.");
                            eltPos = findLastInTableScope("caption");
                            if (eltPos == NOT_FOUND_ON_STACK) {
                                break endtagloop;
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
                        case BODY:
                        case COL:
                        case COLGROUP:
                        case HTML:
                        case TBODY_OR_THEAD_OR_TFOOT:
                        case TD_OR_TH:
                        case TR:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            // fall through to IN_BODY
                    }
                case IN_CELL:
                    switch (magic) {
                        case TD_OR_TH:
                            eltPos = findLastInTableScope(name);
                            if (eltPos == NOT_FOUND_ON_STACK) {
                                err("Stray end tag \u201C" + name + "\u201D.");
                                break endtagloop;
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
                            break endtagloop;
                        case TABLE:
                        case TBODY_OR_THEAD_OR_TFOOT:
                        case TR:
                            if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                                err("Stray end tag \u201C" + name + "\u201D.");
                                break endtagloop;
                            }
                            closeTheCell(findLastInTableScopeTdTh());
                            continue;
                        case BODY:
                        case CAPTION:
                        case COL:
                        case COLGROUP:
                        case HTML:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            // fall through to IN_BODY
                    }
                case IN_BODY:
                    switch (magic) {
                        case BODY:
                            if (!isSecondOnStackBody()) {
                                assert context != null;
                                err("Stray end tag \u201Cbody\u201D.");
                                break endtagloop;
                            }
                            assert currentPtr >= 1;
                            uncloseloop1: for (int i = 2; i <= currentPtr; i++) {
                                switch (stack[i].magic) {
                                    case DD_OR_DT:
                                    case LI:
                                    case P:
                                        break;
                                    default:
                                        err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                        break uncloseloop1;
                                }
                            }
                            if (conformingAndStreaming) {
                                while (currentPtr > 1) {
                                    pop();
                                }
                            }
                            if (context == null) {
                                bodyCloseReported = true;
                                bodyClosed(stack[1].node);
                            }
                            mode = InsertionMode.AFTER_BODY;
                            break endtagloop;
                        case HTML:
                            if (!isSecondOnStackBody()) {
                                assert context != null;
                                err("Stray end tag \u201Chtml\u201D.");
                                break endtagloop;
                            }
                            uncloseloop2: for (int i = 0; i <= currentPtr; i++) {
                                switch (stack[i].magic) {
                                    case DD_OR_DT:
                                    case LI:
                                    case P:
                                    case TBODY_OR_THEAD_OR_TFOOT:
                                    case TD_OR_TH:
                                    case BODY:
                                    case HTML:
                                        break;
                                    default:
                                        err("End tag for \u201Chtml\u201D seen but there were unclosed elements.");
                                        break uncloseloop2;
                                }
                            }
                            if (context == null) {
                                bodyCloseReported = true;
                                bodyClosed(stack[1].node);
                            }
                            mode = InsertionMode.AFTER_BODY;
                            continue;
                        case DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU:
                        case UL_OR_OL_OR_DL:
                        case PRE_OR_LISTING:
                        case FIELDSET_OR_ADDRESS_OR_DIR:
                            eltPos = findLastInScope(name);
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
                            break endtagloop;
                        case FORM:
                            formPointer = null;
                            eltPos = findLastInScope(name);
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
                            break endtagloop;
                        case P:
                            if (!isCurrent(name)) {
                                err("End tag \u201Cp\u201D seen but there were unclosed elements.");
                            }
                            eltPos = findLastInScope(name);
                            if (eltPos != NOT_FOUND_ON_STACK) {
                                while (currentPtr >= eltPos) {
                                    pop();
                                }
                            } else {
                                appendVoidElementToCurrentMayFoster(
                                        "http://www.w3.org/1999/xhtml", name,
                                        EmptyAttributes.EMPTY_ATTRIBUTES);
                            }
                            break endtagloop;
                        case DD_OR_DT:
                        case LI:
                            eltPos = findLastInScope(name);
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
                            break endtagloop;
                        case H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6:
                            eltPos = findLastInScopeHn();
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
                            break endtagloop;
                        case A:
                        case B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U:
                        case NOBR:
                            adoptionAgencyEndTag(name);
                            break endtagloop;
                        case BUTTON:
                        case OBJECT_OR_MARQUEE_OR_APPLET:
                            eltPos = findLastInScope(name);
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
                            break endtagloop;
                        case BR:
                            err("End tag \u201Cbr\u201D.");
                            reconstructTheActiveFormattingElements();
                            appendVoidElementToCurrentMayFoster(
                                    "http://www.w3.org/1999/xhtml", name,
                                    EmptyAttributes.EMPTY_ATTRIBUTES);
                            break endtagloop;
                        case AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR:
                        case EMBED_OR_IMG:
                        case IMAGE:
                        case INPUT:
                        case HR:
                        case ISINDEX:
                        case IFRAME_OR_NOEMBED: // XXX???
                        case NOFRAMES: // XXX??
                        case SELECT:
                        case TABLE:
                        case TEXTAREA: // XXX??
                        case NOSCRIPT: // XXX??
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                        default:
                            if (isCurrent(name)) {
                                pop();
                                break endtagloop;
                            }
                            for (;;) {
                                generateImpliedEndTags();
                                if (isCurrent(name)) {
                                    pop();
                                    break endtagloop;
                                }
                                StackNode<T> node = stack[currentPtr];
                                if (!(node.scoping || node.special)) {
                                    err("Unclosed element \u201C" + node.name
                                            + "\u201D.");
                                    pop();
                                } else {
                                    err("Stray end tag \u201C" + name
                                            + "\u201D.");
                                    break endtagloop;
                                }
                            }
                    }
                case IN_COLUMN_GROUP:
                    switch (magic) {
                        case COLGROUP:
                            if (currentPtr == 0) {
                                assert context != null;
                                err("Garbage in \u201Ccolgroup\u201D fragment.");
                                break endtagloop;
                            }
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            break endtagloop;
                        case COL:
                            err("Stray end tag \u201Ccol\u201D.");
                            break endtagloop;
                        default:
                            if (currentPtr == 0) {
                                assert context != null;
                                err("Garbage in \u201Ccolgroup\u201D fragment.");
                                break endtagloop;
                            }
                            pop();
                            mode = InsertionMode.IN_TABLE;
                            continue;
                    }
                case IN_SELECT_IN_TABLE:
                    switch (magic) {
                        case CAPTION:
                        case TABLE:
                        case TBODY_OR_THEAD_OR_TFOOT:
                        case TR:
                        case TD_OR_TH:
                            err("\u201C"
                                    + name
                                    + "\u201D end tag with \u201Cselect\u201D open.");
                            if (findLastInTableScope(name) != NOT_FOUND_ON_STACK) {
                                endSelect();
                                continue;
                            } else {
                                break endtagloop;
                            }
                        default:
                            // fall through to IN_SELECT
                    }
                case IN_SELECT:
                    switch (magic) {
                        case OPTION:
                            if (isCurrent("option")) {
                                pop();
                                break endtagloop;
                            } else {
                                err("Stray end tag \u201Coption\u201D");
                                break endtagloop;
                            }
                        case OPTGROUP:
                            if (isCurrent("option")
                                    && "optgroup" == stack[currentPtr - 1].name) {
                                pop();
                            }
                            if (isCurrent("optgroup")) {
                                pop();
                            } else {
                                err("Stray end tag \u201Coptgroup\u201D");
                            }
                            break endtagloop;
                        case SELECT:
                            endSelect();
                            break endtagloop;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D");
                            break endtagloop;
                    }
                case AFTER_BODY:
                    switch (magic) {
                        case HTML:
                            if (context != null) {
                                err("Stray end tag \u201Chtml\u201D");
                                break endtagloop;
                            } else {
                                if (context == null) {
                                    htmlCloseReported = true;
                                    htmlClosed(stack[0].node);
                                }
                                mode = InsertionMode.AFTER_AFTER_BODY;
                                break endtagloop;
                            }
                        default:
                            err("Saw an end tag after \u201Cbody\u201D had been closed.");
                            if (conformingAndStreaming) {
                                fatal();
                            }
                            mode = InsertionMode.IN_BODY;
                            continue;
                    }
                case IN_FRAMESET:
                    switch (magic) {
                        case FRAMESET:
                            if (currentPtr == 0) {
                                assert context != null;
                                err("Stray end tag \u201Cframeset\u201D");
                                break endtagloop;
                            }
                            pop();
                            if ((context == null) && !isCurrent("frameset")) {
                                mode = InsertionMode.AFTER_FRAMESET;
                            }
                            break endtagloop;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D");
                            break endtagloop;
                    }
                case AFTER_FRAMESET:
                    switch (magic) {
                        case HTML:
                            if (context == null) {
                                htmlCloseReported = true;
                                htmlClosed(stack[0].node);
                            }
                            mode = InsertionMode.AFTER_AFTER_FRAMESET;
                            break endtagloop;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D");
                            break endtagloop;
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
                    switch (magic) {
                        case HEAD:
                        case BODY:
                        case HTML:
                        case P:
                        case BR:
                            appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                            mode = InsertionMode.IN_HEAD;
                            continue;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                    }
                case IN_HEAD:
                    switch (magic) {
                        case HEAD:
                            pop();
                            mode = InsertionMode.AFTER_HEAD;
                            break endtagloop;
                        case BODY:
                        case HTML:
                        case P:
                        case BR:
                            pop();
                            mode = InsertionMode.AFTER_HEAD;
                            continue;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
                    }
                case IN_HEAD_NOSCRIPT:
                    switch (magic) {
                        case NOSCRIPT:
                            pop();
                            mode = InsertionMode.IN_HEAD;
                            break endtagloop;
                        case P:
                        case BR:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            pop();
                            mode = InsertionMode.IN_HEAD;
                            continue;
                        default:
                            err("Stray end tag \u201C" + name + "\u201D.");
                            break endtagloop;
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
        if (foreignFlag == IN_FOREIGN && !hasForeignInScope()) {
            /*
             * If, after doing so, the insertion mode is still "in foreign
             * content", but there is no element in scope that has a namespace
             * other than the HTML namespace, switch the insertion mode to the
             * secondary insertion mode.
             */
            foreignFlag = NOT_IN_FOREIGN;
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
            if (stack[i].magic == TBODY_OR_THEAD_OR_TFOOT) {
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
            if (stack[i].magic == H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6) {
                return i;
            } else if (stack[i].scoping) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private boolean hasForeignInScope() {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].ns != "http://www.w3.org/1999/xhtml") {
                return true;
            } else if (stack[i].scoping) {
                return false;
            }
        }
        return false;
    }

    private void generateImpliedEndTagsExceptFor(String name)
            throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if (name != stackName
                    && ("p" == stackName || "li" == stackName
                            || "dd" == stackName || "dt" == stackName)) {
                pop();
            } else {
                return;
            }
        }
    }

    private void generateImpliedEndTags() throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if ("p" == stackName || "li" == stackName || "dd" == stackName
                    || "dt" == stackName) {
                pop();
            } else {
                return;
            }
        }
    }

    private boolean isSecondOnStackBody() {
        return currentPtr >= 1 && stack[1].magic == BODY;
    }

    private void documentModeInternal(DocumentMode mode,
            String publicIdentifier, String systemIdentifier,
            boolean html4SpecificAdditionalErrorChecks) throws SAXException {
        if (documentModeHandler != null) {
            documentModeHandler.documentMode(mode, publicIdentifier,
                    systemIdentifier, html4SpecificAdditionalErrorChecks);
        }
        documentMode(mode, publicIdentifier, systemIdentifier,
                html4SpecificAdditionalErrorChecks);
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
            String systemIdentifierLC, boolean forceQuirks) {
        if (forceQuirks) {
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
            while (currentPtr > eltPos) { // > not >= intentional
                pop();
            }
        }
    }

    private void resetTheInsertionMode() {
        StackNode<T> node;
        String name;
        for (int i = currentPtr; i >= 0; i--) {
            node = stack[i];
            name = node.name;
            if (i == 0) {
                if (!(context == "td" || context == "th")) {
                    name = context;
                } else {
                    mode = InsertionMode.IN_BODY; // XXX from Hixie's email
                    return;
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
            } else if ("http://www.w3.org/1999/xhtml" != node.ns) {
                foreignFlag = IN_FOREIGN;
                mode = InsertionMode.IN_BODY;
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
        elementPushed("http://www.w3.org/1999/xhtml", node.name, node.node);
    }

    private void append(StackNode<T> node) {
        listPtr++;
        if (listPtr == listOfActiveFormattingElements.length) {
            StackNode<T>[] newList = new StackNode[listOfActiveFormattingElements.length + 64];
            System.arraycopy(listOfActiveFormattingElements, 0, newList, 0,
                    listOfActiveFormattingElements.length);
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
        System.arraycopy(listOfActiveFormattingElements, pos + 1,
                listOfActiveFormattingElements, pos, listPtr - pos);
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
            for (;;) {
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
                    node = new StackNode<T>(node.magic, node.ns, node.name,
                            clone, node.scoping, node.special, node.fosterParenting, node.popName);
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
                detachFromParentAndAppendToNewParent(lastNode.node,
                        commonAncestor.node);
            }
            T clone = shallowClone(formattingElt.node);
            StackNode<T> formattingClone = new StackNode<T>(formattingElt.magic,
                    formattingElt.ns, formattingElt.name, clone,
                    formattingElt.scoping, formattingElt.special, formattingElt.fosterParenting, formattingElt.popName);
            appendChildrenToNewParent(furthestBlock.node, clone);
            detachFromParentAndAppendToNewParent(clone, furthestBlock.node);
            removeFromListOfActiveFormattingElements(formattingEltListPos);
            insertIntoListOfActiveFormattingElements(formattingClone, bookmark);
            assert formattingEltStackPos < furthestBlockPos;
            removeFromStack(formattingEltStackPos);
            // furthestBlockPos is now off by one and points to the slot after
            // it
            insertIntoStack(formattingClone, furthestBlockPos);
        }
    }

    private void insertIntoStack(StackNode<T> node, int position)
            throws SAXException {
        assert currentPtr + 1 < stack.length;
        assert position <= currentPtr + 1;
        if (position == currentPtr + 1) {
            flushCharacters();
            push(node);
        } else {
            System.arraycopy(stack, position, stack, position + 1,
                    (currentPtr - position) + 1);
            currentPtr++;
            stack[position] = node;
        }
    }

    private void insertIntoListOfActiveFormattingElements(
            StackNode<T> formattingClone, int bookmark) {
        assert listPtr + 1 < listOfActiveFormattingElements.length;
        if (bookmark <= listPtr) {
            System.arraycopy(listOfActiveFormattingElements, bookmark,
                    listOfActiveFormattingElements, bookmark + 1,
                    (listPtr - bookmark) + 1);
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
            if (DD_OR_DT == node.magic) {
                return i;
            } else if ((node.scoping || node.special)
                    && !("div" == node.name || "address" == node.name)) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLiToPop() {
        for (int i = currentPtr; i >= 0; i--) {
            StackNode<T> node = stack[i];
            if (LI == node.magic) {
                return i;
            } else if ((node.scoping || node.special)
                    && !("div" == node.name || "address" == node.name)) {
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

    private int findLastOrRoot(int magic) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].magic == magic) {
                return i;
            }
        }
        return 0;
    }
    
    private void addAttributesToBody(Attributes attributes) throws SAXException {
        if (currentPtr >= 1) {
            StackNode<T> body = stack[1];
            if (body.magic == BODY) {
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
            push(new StackNode<T>("http://www.w3.org/1999/xhtml", ElementName.HEAD,
                    headPointer));
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
        for (;;) {
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
            StackNode<T> entryClone = new StackNode<T>(entry.magic, entry.ns,
                    entry.name, clone, entry.scoping, entry.special, entry.fosterParenting, entry.popName);
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
        int eltPos = findLastOrRoot(TABLE);
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
        elementPopped("http://www.w3.org/1999/xhtml", node.name, node.node);
    }

    private void appendCharMayFoster(char[] buf, int i) throws SAXException {
        StackNode<T> current = stack[currentPtr];
        if (current.fosterParenting) {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                return;
            } else {
                int eltPos = findLastOrRoot(TABLE);
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
        int eltPos = findLastOrRoot(TABLE);
        StackNode<T> node = stack[eltPos];
        return node.tainted;
    }

    private void appendHtmlElementToDocumentAndPush(Attributes attributes)
            throws SAXException {
        T elt = createHtmlElementSetAsRoot(attributes);
        StackNode<T> node = new StackNode<T>("http://www.w3.org/1999/xhtml",
                ElementName.HTML, elt);
        push(node);
    }

    private void appendHtmlElementToDocumentAndPush() throws SAXException {
        appendHtmlElementToDocumentAndPush(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushHeadElement(Attributes attributes)
            throws SAXException {
        flushCharacters();
        T elt = createElement("http://www.w3.org/1999/xhtml", "head",
                attributes);
        detachFromParentAndAppendToNewParent(elt, stack[currentPtr].node);
        headPointer = elt;
        StackNode<T> node = new StackNode<T>("http://www.w3.org/1999/xhtml",
                ElementName.HEAD, elt);
        push(node);
    }

    private void appendToCurrentNodeAndPushBodyElement(Attributes attributes)
            throws SAXException {
        appendToCurrentNodeAndPushElement("http://www.w3.org/1999/xhtml",
                ElementName.BODY, attributes);
    }

    private void appendToCurrentNodeAndPushBodyElement() throws SAXException {
        appendToCurrentNodeAndPushBodyElement(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushFormElementMayFoster(
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement("http://www.w3.org/1999/xhtml", "form",
                attributes);
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
        StackNode<T> node = new StackNode<T>("http://www.w3.org/1999/xhtml",
                ElementName.FORM, elt);
        push(node);
    }

    private void appendToCurrentNodeAndPushFormattingElementMayFoster(
            String ns, ElementName elementName, Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, elementName.name, attributes, formPointer);
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
        StackNode<T> node = new StackNode<T>(ns, elementName, elt);
        push(node);
        append(node);
    }

    private void appendToCurrentNodeAndPushElement(String ns, ElementName elementName,
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, elementName.name, attributes);
        detachFromParentAndAppendToNewParent(elt, stack[currentPtr].node);
        StackNode<T> node = new StackNode<T>(ns, elementName, elt);
        push(node);
    }

    private void appendToCurrentNodeAndPushElementMayFoster(String ns,
            ElementName elementName, Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, elementName.name, attributes);
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
        StackNode<T> node = new StackNode<T>(ns, elementName, elt);
        push(node);
    }

    private void appendToCurrentNodeAndPushElementMayFoster(String ns,
            ElementName elementName, Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, elementName.name, attributes, formPointer);
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
        StackNode<T> node = new StackNode<T>(ns, elementName, elt);
        push(node);
    }

    private void appendVoidElementToCurrentMayFoster(String ns, String name,
            Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, name, attributes, formPointer);
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
            elementPushed(ns, name, (T) attributes);
            elementPopped(ns, name, null);
        }
    }

    private void appendVoidElementToCurrentMayFoster(String ns, String name,
            Attributes attributes) throws SAXException {
        flushCharacters();
        T elt = createElement(ns, name, attributes);
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
            elementPushed(ns, name, (T) attributes);
            elementPopped(ns, name, null);
        }
    }

    private void appendVoidElementToCurrent(String ns, String name,
            Attributes attributes, T form) throws SAXException {
        flushCharacters();
        T elt = createElement("http://www.w3.org/1999/xhtml", name, attributes,
                formPointer);
        StackNode<T> current = stack[currentPtr];
        detachFromParentAndAppendToNewParent(elt, current.node);
        if (conformingAndStreaming || nonConformingAndStreaming) {
            elementPushed("http://www.w3.org/1999/xhtml", name, (T) attributes);
            elementPopped("http://www.w3.org/1999/xhtml", name, null);
        }
    }

    private void accumulateCharacters(char[] buf, int start, int length)
            throws SAXException {
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
            appendCharacters(stack[currentPtr].node, charBuffer, 0,
                    charBufferLen);
            charBufferLen = 0;
        }
    }

    // ------------------------------- //

    protected abstract T createElement(String ns, String name,
            Attributes attributes) throws SAXException;

    protected T createElement(String ns, String name, Attributes attributes,
            T form) throws SAXException {
        return createElement("http://www.w3.org/1999/xhtml", name, attributes);
    }

    protected abstract T createHtmlElementSetAsRoot(Attributes attributes)
            throws SAXException;

    protected abstract void detachFromParent(T element) throws SAXException;

    protected abstract boolean hasChildren(T element) throws SAXException;

    protected abstract T shallowClone(T element) throws SAXException;

    protected abstract void detachFromParentAndAppendToNewParent(T child,
            T newParent) throws SAXException;

    protected abstract void appendChildrenToNewParent(T oldParent, T newParent)
            throws SAXException;

    /**
     * Get the parent element. MUST return <code>null</code> if there is no
     * parent <em>or</em> the parent is not an element.
     */
    protected abstract T parentElementFor(T child) throws SAXException;

    protected abstract void insertBefore(T child, T sibling, T parent)
            throws SAXException;

    protected abstract void insertCharactersBefore(char[] buf, int start,
            int length, T sibling, T parent) throws SAXException;

    protected abstract void appendCharacters(T parent, char[] buf, int start,
            int length) throws SAXException;

    protected abstract void appendComment(T parent, char[] buf, int start,
            int length) throws SAXException;

    protected abstract void appendCommentToDocument(char[] buf, int start,
            int length) throws SAXException;

    protected abstract void addAttributesToElement(T element,
            Attributes attributes) throws SAXException;

    protected void start(boolean fragment) throws SAXException {

    }

    protected void end() throws SAXException {

    }

    protected void bodyClosed(T body) throws SAXException {

    }

    protected void htmlClosed(T html) throws SAXException {

    }

    protected void appendDoctypeToDocument(String name,
            String publicIdentifier, String systemIdentifier)
            throws SAXException {

    }

    protected void elementPushed(String ns, String name, T node)
            throws SAXException {

    }

    protected void elementPopped(String ns, String name, T node)
            throws SAXException {

    }

    protected void documentMode(DocumentMode m, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks)
            throws SAXException {

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
     * @param errorHandler
     *            the errorHandler to set
     */
    public final void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Returns the errorHandler.
     * 
     * @return the errorHandler
     */
    ErrorHandler getErrorHandler() {
        return errorHandler;
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
     * @param scriptingEnabled
     *            the scriptingEnabled to set
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        this.scriptingEnabled = scriptingEnabled;
    }

    /**
     * Sets the doctypeExpectation.
     * 
     * @param doctypeExpectation
     *            the doctypeExpectation to set
     */
    public void setDoctypeExpectation(DoctypeExpectation doctypeExpectation) {
        this.doctypeExpectation = doctypeExpectation;
    }

    /**
     * Sets the documentModeHandler.
     * 
     * @param documentModeHandler
     *            the documentModeHandler to set
     */
    public void setDocumentModeHandler(DocumentModeHandler documentModeHandler) {
        this.documentModeHandler = documentModeHandler;
    }

    /**
     * Sets the reportingDoctype.
     * 
     * @param reportingDoctype
     *            the reportingDoctype to set
     */
    public void setReportingDoctype(boolean reportingDoctype) {
        this.reportingDoctype = reportingDoctype;
    }
}