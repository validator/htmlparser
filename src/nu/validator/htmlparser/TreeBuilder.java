/*
 * Copyright (c) 2007 Henri Sivonen
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
 * comment are quotes from the WHATWG HTML 5 spec as of 27 June 2007 
 * amended as of June 28 2007.
 * That document came with this statement:
 * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
 * Opera Software ASA. You are granted a license to use, reproduce and 
 * create derivative works of this document."
 */

package nu.validator.htmlparser;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fi.iki.hsivonen.xml.EmptyAttributes;

public abstract class TreeBuilder implements TokenHandler {

    private enum Phase {
        INITIAL, ROOT_ELEMENT, BEFORE_HEAD, IN_HEAD, IN_HEAD_NOSCRIPT, AFTER_HEAD, IN_BODY, IN_TABLE, IN_CAPTION, IN_COLUMN_GROUP, IN_TABLE_BODY, IN_ROW, IN_CELL, IN_SELECT, AFTER_BODY, IN_FRAMESET, AFTER_FRAMESET, TRAILING_END
    }

    private class StackNode {
        final String name;

        final Object node;
        
        final boolean impliedEndTag;

        /**
         * @param name
         * @param node
         */
        StackNode(final String name, final Object node) {
            this.name = name;
            this.node = node;
            this.impliedEndTag = ("dd" == name || "dt" == name || "li" == name
                    || "p" == name);
        }

        public StackNode clone() {
            return new StackNode(this.name, this.node);
        }
    }

    private final static char[] ISINDEX_PROMPT = "This is a searchable index. Insert your search keywords here: ".toCharArray();

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

    private Phase phase = Phase.INITIAL;

    private Phase phaseBeforeSwitchingToTrailingEnd;

    private Tokenizer tokenizer;

    private ErrorHandler errorHandler;

    private DocumentModeHandler documentModeHandler;

    private DoctypeExpectation doctypeExpectation;

    private int cdataOrRcdataTimesToPop;

    private boolean scriptingEnabled;

    private boolean nonConformingAndStreaming;

    private boolean needToDropLF;

    private boolean wantingComments;

    private boolean fragment;

    private Phase previousPhaseBeforeTrailingEnd;
    
    private StackNode[] stack = new StackNode[64];
    
    private int currentPtr = -1;

    private StackNode formPointer;

    /**
     * Reports an condition that would make the infoset incompatible with XML
     * 1.0 as fatal.
     * 
     * @param message
     *            the message
     * @throws SAXException
     * @throws SAXParseException
     */
    protected void fatal(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, tokenizer);
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
    protected void err(String message) throws SAXException {
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
    protected void warn(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.warning(spe);
    }

    public final void start(Tokenizer self) throws SAXException {
        // TODO Auto-generated method stub
        needToDropLF = false;
        cdataOrRcdataTimesToPop = 0;
    }

    /**
     * @see nu.validator.htmlparser.TokenHandler#wantsComments()
     */
    public abstract boolean wantsComments() throws SAXException;

    public final void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean correct) throws SAXException {
        needToDropLF = false;
        switch (phase) {
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
                appendDoctypeToDocument(name, publicIdentifier == null ? ""
                        : publicIdentifier, systemIdentifier == null ? ""
                        : systemIdentifier);
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
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            if (!(publicIdentifier == null && systemIdentifier == null)) {
                                err("Legacy doctype.");
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                    case HTML401_STRICT:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("The doctype was not the HTML 4.01 Strict doctype.");
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case HTML401_TRANSITIONAL:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
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
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            err("The doctype was not the HTML 4.01 Transitional doctype.");
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case AUTO:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            boolean html4 = "-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier);
                            if (html4) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("Almost standards mode doctype.");
                            }
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        } else {
                            boolean html4 = "-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier);
                            if (html4) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                if (!(publicIdentifier == null && systemIdentifier == null)) {
                                    err("Legacy doctype.");
                                }
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        }
                        break;
                    case NO_DOCTYPE_ERRORS:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                }

                /*
                 * 
                 * Then, switch to the root element phase of the tree
                 * construction stage.
                 * 
                 * 
                 */
                phase = Phase.ROOT_ELEMENT;
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

    public final void comment(char[] buf, int length) throws SAXException {
        needToDropLF = false;
        if (wantingComments) {
            switch (phase) {
                case INITIAL:
                case ROOT_ELEMENT:
                case TRAILING_END:
                    /*
                     * A comment token Append a Comment node to the Document
                     * object with the data attribute set to the data given in
                     * the comment token.
                     */
                    appendCommentToDocument(buf, length);
                    return;
                case AFTER_BODY:
                    /*
                     * * A comment token Append a Comment node to the first
                     * element in the stack of open elements (the html element),
                     * with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    appendCommentToRootElement(buf, length);
                    return;
                default:
                    /*
                     * * A comment token Append a Comment node to the current
                     * node with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    appendCommentToCurrentNode(buf, length);
                    return;
            }
        }
    }

    /**
     * @see nu.validator.htmlparser.TokenHandler#characters(char[], int, int)
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
            appendCharactersToCurrentNode(buf, start, length);
            return;
        }

        // optimize the most common case
        if (phase == Phase.IN_BODY || phase == Phase.IN_CELL
                || phase == Phase.IN_CAPTION) {
            reconstructTheActiveFormattingElements();
            appendCharactersToCurrentNode(buf, start, length);
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
                    switch (phase) {
                        case INITIAL:
                        case ROOT_ELEMENT:
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case BEFORE_HEAD:
                        case IN_HEAD:
                        case IN_HEAD_NOSCRIPT:
                        case AFTER_HEAD:
                        case IN_TABLE:
                        case IN_COLUMN_GROUP:
                        case IN_TABLE_BODY:
                        case IN_ROW:
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
                                appendCharactersToCurrentNode(buf, start, i
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
                            break loop;
                        case AFTER_BODY:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            continue;
                        case TRAILING_END:
                            if (phaseBeforeSwitchingToTrailingEnd == Phase.AFTER_FRAMESET) {
                                continue;
                            } else {
                                if (start < i) {
                                    appendCharactersToCurrentNode(buf, start, i
                                            - start);
                                    start = i;
                                }
                                /*
                                 * Reconstruct the active formatting elements,
                                 * if any.
                                 */
                                reconstructTheActiveFormattingElements();
                                /*
                                 * Append the token's character to the current
                                 * node.
                                 */
                                continue;
                            }
                    }
                default:
                    /*
                     * A character token that is not one of one of U+0009
                     * CHARACTER TABULATION, U+000A LINE FEED (LF), U+000B LINE
                     * TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
                     */
                    switch (phase) {
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
                            documentMode(DocumentMode.QUIRKS_MODE, null, null,
                                    false);
                            /*
                             * Then, switch to the root element phase of the
                             * tree construction stage
                             */
                            phase = Phase.ROOT_ELEMENT;
                            /*
                             * and reprocess the current token.
                             * 
                             * 
                             */
                            i--;
                            continue;
                        case ROOT_ELEMENT:
                            /*
                             * Create an HTMLElement node with the tag name
                             * html, in the HTML namespace. Append it to the
                             * Document object.
                             */
                            appendHtmlElementToDocument();
                            /* Switch to the main phase */
                            phase = Phase.BEFORE_HEAD;
                            /*
                             * reprocess the current token.
                             * 
                             */
                            i--;
                            continue;
                        case BEFORE_HEAD:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * /*Act as if a start tag token with the tag name
                             * "head" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                            phase = Phase.IN_HEAD;
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
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if an end tag token with the tag name
                             * "head" had been seen,
                             */
                            popCurrentNode();
                            phase = Phase.AFTER_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_HEAD_NOSCRIPT:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error. Act as if an end tag with the tag
                             * name "noscript" had been seen
                             */
                            err("Non-space character inside \u201Cnoscript\u201D inside \u201Chead\u201D.");
                            popCurrentNode();
                            phase = Phase.IN_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case AFTER_HEAD:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if a start tag token with the tag name
                             * "body" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushBodyElement();
                            phase = Phase.IN_BODY;
                            /*
                             * and then reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_BODY:
                        case IN_CELL:
                        case IN_CAPTION:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
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
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                            }
                            reconstructTheActiveFormattingElementsWithFosterParent();
                            appendCharToFosterParent(buf[i]);
                            start = i + 1;
                            continue;
                        case IN_COLUMN_GROUP:
                            /*
                             * Act as if an end tag with the tag name "colgroup"
                             * had been seen, and then, if that token wasn't
                             * ignored, reprocess the current token.
                             */
                            if (isCurrentRoot()) {
                                err("Non-space in \u201Ccolgroup\u201D when parsing fragment.");
                                continue;
                            }
                            popCurrentNode();
                            phase = Phase.IN_TABLE;
                            i--;
                            continue;
                        case IN_SELECT:
                            break loop;
                        case AFTER_BODY:
                            err("Non-space character after body.");
                            phase = Phase.IN_BODY;
                            i--;
                            continue;
                        case IN_FRAMESET:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
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
                                appendCharactersToCurrentNode(buf, start, i
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
                        case TRAILING_END:
                            /*
                             * Parse error.
                             */
                            err("Non-space character in page trailer.");
                            /*
                             * Switch back to the main phase and reprocess the
                             * token.
                             */
                            phase = phaseBeforeSwitchingToTrailingEnd;
                            i--;
                            continue;
                    }
            }
        }
        if (start < end) {
            appendCharactersToCurrentNode(buf, start, end - start);
        }
    }

    public final void eof() throws SAXException {
        for (;;) {
            switch (phase) {
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
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    /*
                     * Create an HTMLElement node with the tag name html, in the
                     * HTML namespace. Append it to the Document object.
                     */
                    appendHtmlElementToDocument();
                    /* Switch to the main phase */
                    phase = Phase.BEFORE_HEAD;
                    /*
                     * reprocess the current token.
                     */
                    continue;
                case BEFORE_HEAD:
                case IN_HEAD:
                case IN_HEAD_NOSCRIPT:
                case AFTER_HEAD:
                case IN_BODY:
                case IN_TABLE:
                case IN_CAPTION:
                case IN_COLUMN_GROUP:
                case IN_TABLE_BODY:
                case IN_ROW:
                case IN_CELL:
                case IN_SELECT:
                case AFTER_BODY:
                case IN_FRAMESET:
                case AFTER_FRAMESET:
                    /*
                     * Generate implied end tags.
                     */
                    generateImpliedEndTags();
                    /*
                     * If there are more than two nodes on the stack of open
                     * elements,
                     */
                    if (stackSize() > 2) {
                        err("End of file seen and there were open elements.");
                    } else if (stackSize() == 2
                            && !"body".equals(nameOfCurrentNode())) {
                        /*
                         * or if there are two nodes but the second node is not
                         * a body node, this is a parse error.
                         */
                        err("End of file seen and there were open elements.");
                    }

                    // XXX fragments
                    /*
                     * Otherwise, if the parser was originally created as part
                     * of the HTML fragment parsing algorithm, and there's more
                     * than one element in the stack of open elements, and the
                     * second node on the stack of open elements is not a body
                     * node, then this is a parse error. (fragment case)
                     */

                    /* Stop parsing. */
                    return;
                    /*
                     * This fails because it doesn't imply HEAD and BODY tags.
                     * We should probably expand out the insertion modes and
                     * merge them with phases and then put the three things here
                     * into each insertion mode instead of trying to factor them
                     * out so carefully.
                     * 
                     */
                case TRAILING_END:
                    /* Stop parsing. */
                    return;
            }
        }
    }

    public final void startTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        for (;;) {
            switch (phase) {
                case IN_TABLE_BODY:
                    if ("tr" == name) {
                        clearTheStackBackToATableBodyContext();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_ROW;
                        return;
                    } else if ("td" == name || "th" == name) {
                        err("\u201C" + name + "\u201D start tag in table body.");
                        clearTheStackBackToATableBodyContext();
                        appendToCurrentNodeAndPushElement("tr",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_ROW;
                        continue;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name) {
                        if (!(stackHasInTableScope("tbody")
                                || stackHasInTableScope("thead") || stackHasInTableScope("tfoot"))) {
                            err("Stray \u201C" + name + "\u201D start tag.");
                            return;
                        } else {
                            clearTheStackBackToATableBodyContext();
                            popCurrentNode();
                            phase = Phase.IN_TABLE;
                            continue;
                        }
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_ROW:
                    if ("td" == name || "th" == name) {
                        clearTheStackBackToATableRowContext();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_CELL;
                        insertMarker();
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name
                            || "tr" == name) {
                        if (!stackHasInTableScope("tr")) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearTheStackBackToATableRowContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("caption" == name) {
                        clearTheStackBackToATableContext();
                        insertMarker();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_CAPTION;
                        return;
                    } else if ("colgroup" == name) {
                        clearTheStackBackToATableContext();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_COLUMN_GROUP;
                        return;
                    } else if ("col" == name) {
                        clearTheStackBackToATableContext();
                        appendToCurrentNodeAndPushElement("colgroup",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_COLUMN_GROUP;
                        continue;
                    } else if ("tbody" == name || "tfoot" == name
                            || "thead" == name) {
                        clearTheStackBackToATableContext();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_TABLE_BODY;
                        return;
                    } else if ("td" == name || "tr" == name || "th" == name) {
                        clearTheStackBackToATableContext();
                        appendToCurrentNodeAndPushElement("tbody",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("table" == name) {
                        err("Start tag for \u201Ctable\u201D seen but the previous \u201Ctable\u201D is still open.");
                        if (!stackHasInTableScope("table")) {
                            assert fragment;
                            return;
                        }
                        generateImpliedEndTags();
                        // XXX is the next if dead code?
                        if (!isCurrent("table")) {
                            err("Unclosed elements on stack.");
                        }
                        popUntilElementHasBeenPopped("table");
                        resetTheInsertionMode();
                        continue;
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
                        int eltPos = getInTableScopeNode("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "td" == name || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        if (!(stackHasInScope("td") || stackHasInTableScope("th"))) {
                            err("No cell to close.");
                            return;
                        } else {
                            closeTheCell();
                            continue;
                        }
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_BODY:
                    if ("base" == name || "link" == name || "meta" == name
                            || "style" == name || "script" == name) {
                        // Fall through to IN_HEAD
                    } else if ("title" == name) {
                        err("\u201Ctitle\u201D element found inside \u201Cbody\201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else if ("body" == name) {
                        err("\u201Cbody\u201D start tag found but the \u201Cbody\201D element is already open.");
                        addAttributesToBody(attributes);
                        return;
                    } else if ("p" == name || "div" == name || "h1" == name
                            || "h2" == name || "h3" == name || "h4" == name
                            || "h5" == name || "h6" == name
                            || "blockquote" == name || "ol" == name
                            || "ul" == name || "dl" == name
                            || "fieldset" == name || "address" == name
                            || "menu" == name || "center" == name
                            || "dir" == name || "listing" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("pre" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        needToDropLF = true;
                        return;
                    } else if ("form" == name) {
                        if (isFormPointerNull()) {
                            err("Saw a \u201Cform\u201D start tag, but there was already an active \u201Cform\u201D element.");
                            return;
                        } else {
                            implicitlyCloseP();
                            appendToCurrentNodeAndPushFormElement(attributes);
                            return;
                        }
                    } else if ("li" == name) {
                        implicitlyCloseP();
                        int timesToPop = timesNeededToPopInOrderToPopUptoAndIncludingLi();
                        if (timesToPop > 1) {
                            err("A \u201Cli\u201D start tag was seen but the previous \u201Cli\u201D element had open children.");
                        }
                        while (timesToPop > 0) {
                            popCurrentNode();
                            timesToPop--;
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("dd" == name || "dt" == name) {
                        implicitlyCloseP();
                        int timesToPop = timesNeededToPopInOrderToPopUptoAndIncludingDdOrDt();
                        if (timesToPop > 1) {
                            err("A definition list item start tag was seen but the previous definition list item element had open children.");
                        }
                        while (timesToPop > 0) {
                            popCurrentNode();
                            timesToPop--;
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("plaintext" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        tokenizer.setContentModelFlag(
                                ContentModelFlag.PLAINTEXT, name);
                        return;
                    } else if ("a" == name) {
                        StackNode activeA = findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker("a");
                        if (activeA != null) {
                            err("An \u201Ca\u201D start tag seen with already an active \u201Ca\u201D element.");
                            adoptionAgencyEndTag("a");
                            removeFromListOfActiveFormattingElements(activeA);
                            removeFromStack(activeA);
                        }
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("i" == name || "b" == name || "em" == name
                            || "strong" == name || "font" == name
                            || "big" == name || "s" == name || "small" == name
                            || "strike" == name || "tt" == name || "u" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("nobr" == name) {
                        reconstructTheActiveFormattingElements();
                        if (stackHasInScope("nobr")) {
                            err("\u201Cnobr\u201D start tag seen when there was an open \u201Cnobr\u201D element in scope.");
                            adoptionAgencyEndTag("nobr");
                        }
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("button" == name) {
                        if (stackHasInScope("button")) {
                            err("\u201Cbutton\u201D start tag seen when there was an open \u201Cbutton\u201D element in scope.");
                            generateImpliedEndTags();
                            if (!isCurrent("button")) {
                                err("There was an open \u201Cbutton\u201D element in scope with unclosed children.");
                            }
                            StackNode buttonInScope = getInScopeNode("button");
                            if (buttonInScope != null) {
                                popUpToAndIncluding(buttonInScope);
                                clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                            }
                            continue;
                        } else {
                            reconstructTheActiveFormattingElements();
                            appendToCurrentNodeAndPushElement(name, attributes);
                            insertMarker();
                            return;
                        }
                    } else if ("object" == name || "marquee" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        insertMarker();
                        return;
                    } else if ("xmp" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("table" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("br" == name || "img" == name || "embed" == name
                            || "param" == name || "area" == name
                            || "basefont" == name || "bgsound" == name
                            || "spacer" == name || "wbr" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else if ("hr" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else if ("image" == name) {
                        err("Saw a start tag \u201Cimage\201D.");
                        name = "img";
                        continue;
                    } else if ("input" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeVoidElementAssociateWithForm(name,
                                attributes);
                        return;
                    } else if ("isindex" == name) {
                        err("\u201Cisindex\201D seen.");
                        if (isFormPointerNull()) {
                            return;
                        }
                        implicitlyCloseP();
                        AttributesImpl formAttrs = newAttributes();
                        int actionIndex = attributes.getIndex("action");
                        if (actionIndex > -1) {
                            formAttrs.addAttribute("action",
                                    attributes.getValue(actionIndex));
                        }
                        appendToCurrentNodeAndPushFormElement(formAttrs);
                        appendToCurrentNodeVoidElement("hr",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElement("p",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElement("label",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        int promptIndex = attributes.getIndex("prompt");
                        if (promptIndex > -1) {
                            char[] prompt = attributes.getValue(promptIndex).toCharArray();
                            appendCharactersToCurrentNode(prompt, 0,
                                    prompt.length);
                        } else {
                            // XXX localization
                            appendCharactersToCurrentNode(ISINDEX_PROMPT, 0,
                                    ISINDEX_PROMPT.length);
                        }
                        AttributesImpl inputAttributes = newAttributes();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attributeQName = attributes.getQName(i);
                            if (!("name".equals(attributeQName)
                                    || "action".equals(attributeQName) || "prompt".equals(attributeQName))) {
                                inputAttributes.addAttribute(attributeQName,
                                        attributes.getValue(i));
                            }
                        }
                        appendToCurrentNodeVoidElementAssociateWithForm(
                                "input", inputAttributes);
                        // XXX localization
                        popCurrentNode(); // label
                        popCurrentNode(); // p
                        appendToCurrentNodeVoidElement("hr",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        popCurrentNode(); // form
                        return;
                    } else if ("textarea" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        associateCurrentNodeWithFormPointer();
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        cdataOrRcdataTimesToPop = 1;
                        needToDropLF = true;
                        return;
                    } else if ("iframe" == name || "noembed" == name
                            || "noframes" == name
                            || ("noscript" == name && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("select" == name) {
                        reconstructTheActiveFormattingElements();
                        // XXX form pointer
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_SELECT;
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "frame" == name
                            || "frameset" == name || "head" == name
                            || "option" == name || "optgroup" == name
                            || "tbody" == name || "td" == name
                            || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        err("Stay start tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    }
                case IN_HEAD:
                    if ("base" == name) {
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else if ("meta" == name || "link" == name) {
                        // Fall through to IN_HEAD_NOSCRIPT
                    } else if ("title" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else if ("style" == name
                            || ("noscript" == name && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("noscript" == name && !scriptingEnabled) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_HEAD_NOSCRIPT;
                        return;
                    } else if ("script" == name) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElement(name, attributes);
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
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        continue;
                    }
                case IN_HEAD_NOSCRIPT:
                    // XXX did Hixie really mean to omit "base" here?
                    if ("link" == name) {
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else if ("meta" == name) {
                        // XXX do charset stuff
                        appendToCurrentNodeVoidElement(name, attributes);
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
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        continue;
                    }
                case IN_COLUMN_GROUP:
                    if ("col" == name) {
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else {
                        if (isCurrentRoot()) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;
                    }
                case IN_SELECT:
                    if ("option" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("optgroup" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                        }
                        if (isCurrent("optgroup")) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("select" == name) {
                        err("\u201Cselect\u201D start tag where end tag expected.");
                        if (!stackHasInTableScope("select")) {
                            assert fragment;
                            err("No \u201Cselect\u201D in table scope.");
                            return;
                        } else {
                            popUntilElementHasBeenPopped("select");
                            resetTheInsertionMode();
                            return;
                        }
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        return;
                    }
                case AFTER_BODY:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    phase = Phase.IN_BODY;
                    continue;
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("frame" == name) {
                        appendToCurrentNodeVoidElement(name, attributes);
                        return;
                    } else {
                        // fall through to AFTER_FRAMESET
                    }
                case AFTER_FRAMESET:
                    if ("noframes" == name) {
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
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    // optimize error check and streaming SAX by hoisting
                    // "html" handling here.
                    if ("html" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendHtmlElementToDocument();
                        } else {
                            appendHtmlElementToDocument(attributes);
                        }
                        phase = Phase.BEFORE_HEAD;
                        return;
                    } else {
                        /*
                         * Create an HTMLElement node with the tag name html, in
                         * the HTML namespace. Append it to the Document object.
                         */
                        appendHtmlElementToDocument();
                        /* Switch to the main phase */
                        phase = Phase.BEFORE_HEAD;
                        /*
                         * reprocess the current token.
                         * 
                         */
                        continue;
                    }
                case BEFORE_HEAD:
                    if ("head" == name) {
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
                        phase = Phase.IN_HEAD;
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
                    phase = Phase.IN_HEAD;
                    /*
                     * then reprocess the current token.
                     * 
                     * This will result in an empty head element being
                     * generated, with the current token being reprocessed in
                     * the "after head" insertion mode.
                     */
                    continue;
                case AFTER_HEAD:
                    if ("body" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendToCurrentNodeAndPushBodyElement();
                        } else {
                            appendToCurrentNodeAndPushBodyElement(attributes);
                        }
                        phase = Phase.IN_BODY;
                        return;
                    } else if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_FRAMESET;
                        return;
                    } else if ("base" == name) {
                        err("\u201Cbase\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeVoidElement(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("link" == name) {
                        err("\u201Clink\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeVoidElement(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("meta" == name) {
                        err("\u201Cmeta\u201D element outside \u201Chead\u201D.");
                        // XXX do chaset stuff
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeVoidElement(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("script" == name) {
                        err("\u201Cscript\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (nonConformingAndStreaming) {
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
                        if (nonConformingAndStreaming) {
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
                        if (nonConformingAndStreaming) {
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
                        phase = Phase.IN_BODY;
                        continue;
                    }
                case TRAILING_END:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    phase = previousPhaseBeforeTrailingEnd;
                    continue;
            }
        }
    }

    public final void endTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        if (cdataOrRcdataTimesToPop > 0) {
            while (cdataOrRcdataTimesToPop > 0) {
                popCurrentNode();
                cdataOrRcdataTimesToPop--;
            }
            return;
        }

        for (;;) {
            switch (phase) {
                case IN_ROW:
                    if ("tr" == name) {
                        if (!stackHasInTableScope("tr")) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearTheStackBackToATableRowContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        return;
                    } else if ("table" == name) {
                        if (!stackHasInTableScope("tr")) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearTheStackBackToATableRowContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("tbody" == name || "thead" == name || "tfoot" == name) {
                        if (!stackHasInTableScope(name)) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                            return;
                        }
                        if (!stackHasInTableScope("tr")) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearTheStackBackToATableRowContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");                            
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE_BODY:
                    if ("tbody" == name || "tfoot" == name || "thead" == name) {
                        if (!stackHasInTableScope(name)) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;
                        }
                        clearTheStackBackToATableBodyContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                        if (!stackHasInTableScopeTbodyTheadTfoot()) {
                            assert fragment;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        clearTheStackBackToATableBodyContext();
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("table" == name) {
                        if (!stackHas("table")) {
                            assert fragment;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        // XXX struck useless stuff
                        popUntilElementHasBeenPopped("table");
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
                        int eltPos = getInTableScopeNode("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                        err("\u201Ctable\u201D closed but \u201Ccaption\u201D was still open.");
                        int eltPos = getInTableScopeNode("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else if ("body" == name || "col" == name || "colgroup" == name || "html" == name || "tbody" == name || "td" == name || "tfoot" == name || "th" == name || "thead" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                                                                                                        
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("td" == name || "th" == name) {
                        int eltPos = getInTableScopeNode(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                            
                        }
                        generateImpliedEndTags();
                        if (!isCurrent(name)) {
                            err("Unclosed elements.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_ROW;
                        return;
                    } else if ("table" == name || "tbody" == name || "tfoot" == name || "thead" == name || "tr" == name) {
                        if (!stackHasInTableScope(name)) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                                                        
                        }
                        closeTheCell();
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
                            assert fragment;
                            err("Stray end tag \u201Cbody\u201D.");
                            return;
                        }
                        assert currentPtr >= 1;
                        for (int i = 2; i <= currentPtr; i++) {
                            if (!stack[i].impliedEndTag) {
                                err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        phase = Phase.AFTER_BODY;
                        return;
                    } else if ("html" == name) {
                        if (!isSecondOnStackBody()) {
                            assert fragment;
                            err("Stray end tag \u201Chtml\u201D.");
                            return;
                        }
                        for (int i = 0; i <= currentPtr; i++) {
                            String stackName = stack[i].name;
                            if ("dd" == stackName || "dt" == stackName || "li" == stackName
                                    || "p" == stackName || "tbody" == stackName || "td" == stackName
                                    || "tfoot" == stackName || "th" == stackName || "thead" == stackName || "tr" == stackName || "body" == stackName || "html" == stackName) {
                                err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        phase = Phase.AFTER_BODY;
                        continue;
                    } else if ("div" == name || "blockquote" == name
                            || "ul" == name || "ol" == name || "pre" == name
                            || "dl" == name || "fieldset" == name
                            || "address" == name || "center" == name
                            || "dir" == name || "listing" == name
                            || "menu" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("form" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201Cform\u201D seen but there were unclosed elements.");
                        } else {
                            popCurrentNode();
                        }
                        formPointer = null;
                        return;
                    } else if ("p" == name) {
                        if (!isCurrent(name)) {
                            err("End tag \u201Cp\u201D seen but there were unclosed elements.");
                        }
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            eltPos--;
                            while (currentPtr > eltPos) {
                                popCurrentNode();
                            }
                        } else {
                            appendToCurrentNodeVoidElement(name, EmptyAttributes.EMPTY_ATTRIBUTES);
                        }
                        return;
                    } else if ("dd" == name || "dt" == name || "li" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTagsExceptFor(name);
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("h1" == name || "h2" == name || "h3" == name
                            || "h4" == name || "h5" == name || "h6" == name) {
                        int eltPos = findLastInScopeHn();
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("a" == name || "b" == name || "big" == name || "em" == name || "font" == name || "i" == name || "nobr" == name || "s" == name || "small" == name || "strike" == name || "strong" == name || "tt" == name || "u" == name) {
                        adoptionAgencyEndTag(name);
                        return;
                    } else if ("button" == name || "marquee" == name || "object" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        return;
                    } else if ("area" == name || "basefont" == name || "bgsound" == name || "br" == name || "embed" == name || "hr" == name || "iframe" == name || "image" == name || "img" == name || "input" == name || "isindex" == name || "noembed" == name || "noframes" == name || "param" == name || "select" == name || "spacer" == name || "table" == name || "textarea" == name || "wbr" == name || (scriptingEnabled && "noscript" == name)) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        if (isCurrent(name)) {
                            popCurrentNode();
                            return;
                        }
                        for(;;) {
                            generateImpliedEndTags();
                            if (isCurrent(name)) {
                                popCurrentNode();
                                return;
                            }
                            String stackName = stack[currentPtr].name;
                            if (!("address" == stackName || "area" == stackName || "base" == stackName || "basefont" == stackName || "bgsound" == stackName || "blockquote" == stackName || "body" == stackName || "br" == stackName || "center" == stackName || "col" == stackName || "colgroup" == stackName || "dd" == stackName || "dir" == stackName || "div" == stackName || "dl" == stackName || "dt" == stackName || "embed" == stackName || "fieldset" == stackName || "form" == stackName || "frame" == stackName || "frameset" == stackName || "h1" == stackName || "h2" == stackName || "h3" == stackName || "h4" == stackName || "h5" == stackName || "h6" == stackName || "head" == stackName || "hr" == stackName || "iframe" == stackName || "image" == stackName || "img" == stackName || "input" == stackName || "isindex" == stackName || "li" == stackName || "link" == stackName || "listing" == stackName || "menu" == stackName || "meta" == stackName || "noembed" == stackName || "noframes" == stackName || "noscript" == stackName || "ol" == stackName || "optgroup" == stackName || "option" == stackName || "p" == stackName || "param" == stackName || "plaintext" == stackName || "pre" == stackName || "script" == stackName || "select" == stackName || "spacer" == stackName || "style" == stackName || "tbody" == stackName || "textarea" == stackName || "tfoot" == stackName || "thead" == stackName || "title" == stackName || "tr" == stackName || "ul" == stackName || "wbr" == stackName || "button" == stackName || "caption" == stackName || "html" == stackName || "marquee" == stackName || "object" == stackName || "table" == stackName || "td" == stackName || "th" == stackName)) {
                                err("Unclosed element \u201C" + stackName + "\u201D.");
                                popCurrentNode();
                            } else {
                                return;
                            }
                        }
                    }
                case IN_COLUMN_GROUP:
                    if ("colgroup" == name) {
                        if (isCurrentRoot()) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        return;                    
                    } else if ("col" == name) {
                        err("Stray end tag \u201Ccol\u201D.");                        
                        return;
                    } else {
                        if (isCurrentRoot()) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;                   
                    }
                case IN_SELECT:
                    if ("option" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                            return;
                        } else {
                            err("Stray end tag \u201Coption\u201D");
                            return;
                        }
                    } else if ("optgroup" == name) {
                        if (isCurrent("option") && "optgroup" == stack[currentPtr - 1].name) {
                            popCurrentNode();
                        }
                        if (isCurrent("optgroup")) {
                            popCurrentNode();
                        } else {
                            err("Stray end tag \u201Coptgroup\u201D");
                            return;                            
                        }
                    } else if ("select" == name) {
                        int eltPos = getInTableScopeNode("select");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert fragment;
                            err("Stray end tag \u201Cselect\u201D");
                            return;                                                        
                        }
                        eltPos--;
                        while (currentPtr > eltPos) {
                            popCurrentNode();
                        }
                        resetTheInsertionMode();
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;
                    }
                case AFTER_BODY:
                    if ("html" == name) {
                        if (fragment) {
                            err("Stray end tag \u201Chtml\u201D");
                            return;                            
                        } else {
                            previousPhaseBeforeTrailingEnd = Phase.AFTER_BODY;
                            phase = Phase.TRAILING_END;
                            return;
                        }
                    } else {
                        err("Saw an end tag after \u201Cbody\u201D had been closed.");
                        phase = Phase.IN_BODY;
                        continue;
                    }
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        if (isCurrentRoot()) {
                            assert fragment;
                            err("Stray end tag \u201Cframeset\u201D");
                            return;
                        }
                        popCurrentNode();
                        if (!fragment && !isCurrent("frameset")) {
                            phase = Phase.AFTER_FRAMESET;                            
                        }
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;                        
                    }
                case AFTER_FRAMESET:
                    if ("html" == name) {
                        previousPhaseBeforeTrailingEnd = Phase.AFTER_FRAMESET;
                        phase = Phase.TRAILING_END;
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
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    /*
                     * Create an HTMLElement node with the tag name html, in the
                     * HTML namespace. Append it to the Document object.
                     */
                    appendHtmlElementToDocument();
                    /* Switch to the main phase */
                    phase = Phase.BEFORE_HEAD;
                    /*
                     * reprocess the current token.
                     * 
                     */
                    continue;
                case BEFORE_HEAD:
                    if ("head" == name || "body" == name || "html" == name || "p" == name || "br" == name) {
                        appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case IN_HEAD:
                    if ("head" == name) {
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        return;
                    } else if ("body" == name || "html" == name || "p" == name || "br" == name) {
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                        
                    }
                case IN_HEAD_NOSCRIPT:
                    if ("noscript" == name) {
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        return;
                    } else if ("p" == name || "br" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case AFTER_HEAD:
                    appendToCurrentNodeAndPushBodyElement();
                    phase = Phase.IN_BODY;
                    continue;
                case TRAILING_END:
                    err("Stray \u201C" + name + "\u201D end tag.");
                    phase = previousPhaseBeforeTrailingEnd;
                    continue;
            }
        }
    }

    private int getInTableScopeNode(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    private boolean stackHasInTableScopeTbodyTheadTfoot() {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean stackHas(String string) {
        // TODO Auto-generated method stub
        return false;
    }

    private int findLastInScope(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    private int findLastInScopeHn() {
        // TODO Auto-generated method stub
        return 0;
    }

    private boolean stackHasInScopeHn() {
        // TODO Auto-generated method stub
        return false;
    }

    private void generateImpliedEndTagsExceptFor(String name) {
        // TODO Auto-generated method stub
        
    }

    private boolean isSecondOnStackBody() {
        // TODO Auto-generated method stub
        return false;
    }

    private void documentMode(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) {
        if (documentModeHandler != null) {
            documentModeHandler.documentMode(mode, publicIdentifier,
                    systemIdentifier, html4SpecificAdditionalErrorChecks);
        }
    }

    protected void appendDoctypeToDocument(String name,
            String publicIdentifier, String systemIdentifier) {
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

    protected abstract void appendCommentToCurrentNode(char[] buf, int length);

    protected abstract void appendCommentToDocument(char[] buf, int length);

    protected abstract void appendCommentToRootElement(char[] buf, int length);

    private void closeTheCell() {
        // TODO Auto-generated method stub

    }

    private void clearTheStackBackToATableRowContext() {
        // TODO Auto-generated method stub

    }

    private void clearTheStackBackToATableBodyContext() {
        // TODO Auto-generated method stub

    }

    private boolean stackHasInTableScope(String string) {
        // TODO Auto-generated method stub
        return false;
    }

    private void popUntilElementHasBeenPopped(String string) {
        // TODO Auto-generated method stub

    }

    private void resetTheInsertionMode() {
        // TODO Auto-generated method stub

    }

    private void clearTheStackBackToATableContext() {
        // TODO Auto-generated method stub

    }

    private void associateCurrentNodeWithFormPointer() {
        // TODO Auto-generated method stub

    }

    private AttributesImpl newAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    private void appendToCurrentNodeVoidElementAssociateWithForm(String name,
            Attributes attributes) {
        // TODO Auto-generated method stub

    }

    /**
     * @param name
     * @param attributes
     */
    private void appendToCurrentNodeVoidElement(String name,
            Attributes attributes) {
        appendToCurrentNodeAndPushElement(name, attributes);
        popCurrentNode();
    }

    /**
     * 
     */
    private void implicitlyCloseP() {
        if (stackHasInScope("p")) {
            endP();
        }
    }

    private void insertMarker() {
        // TODO Auto-generated method stub

    }

    private void clearTheListOfActiveFormattingElementsUpToTheLastMarker() {
        // TODO Auto-generated method stub

    }

    private void popUpToAndIncluding(StackNode buttonInScope) {
        // TODO Auto-generated method stub

    }

    private StackNode getInScopeNode(String string) {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean isCurrent(String string) {
        // TODO Auto-generated method stub
        return false;
    }

    private void appendToCurrentNodeAndPushFormattingElement(String name,
            Attributes attributes) {
        // TODO Auto-generated method stub

    }

    private void removeFromStack(StackNode activeA) {
        // TODO Auto-generated method stub

    }

    private void removeFromListOfActiveFormattingElements(StackNode activeA) {
        // TODO Auto-generated method stub

    }

    private void adoptionAgencyEndTag(String string) {
        // TODO Auto-generated method stub

    }

    private StackNode findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker(
            String string) {
        // TODO Auto-generated method stub
        return null;
    }

    private int timesNeededToPopInOrderToPopUptoAndIncludingDdOrDt() {
        // TODO Auto-generated method stub
        return 0;
    }

    private int timesNeededToPopInOrderToPopUptoAndIncludingLi() {
        // TODO Auto-generated method stub
        return 0;
    }

    private void appendToCurrentNodeAndPushFormElement(Attributes attributes) {
        // TODO Auto-generated method stub

    }

    private boolean isFormPointerNull() {
        // TODO Auto-generated method stub
        return false;
    }

    private void endP() {
        // TODO Auto-generated method stub

    }

    private boolean stackHasInScope(String string) {
        // TODO Auto-generated method stub
        return false;
    }

    private void addAttributesToBody(Attributes attributes) {
        // TODO Auto-generated method stub

    }

    private void pushHeadPointerOntoStack() {
        // TODO Auto-generated method stub

    }

    protected abstract void appendHtmlElementToDocument(Attributes attributes);

    private Object nameOfCurrentNode() {
        // TODO Auto-generated method stub
        return null;
    }

    private int stackSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    private void generateImpliedEndTags() {
        // TODO Auto-generated method stub

    }

    private boolean isCurrentRoot() {
        // TODO Auto-generated method stub
        return false;
    }

    private void reconstructTheActiveFormattingElementsWithFosterParent() {
        // TODO Auto-generated method stub

    }

    private void appendCharToFosterParent(char c) {
        // TODO Auto-generated method stub

    }

    private void reconstructTheActiveFormattingElements() {
        // TODO Auto-generated method stub

    }

    protected abstract void popCurrentNode();

    protected abstract void appendCharactersToCurrentNode(char[] buf,
            int start, int length);

    protected abstract void appendToCurrentNodeAndPushHeadElement(
            Attributes attributes);

    protected abstract void appendToCurrentNodeAndPushBodyElement(
            Attributes attributes);

    protected abstract void appendToCurrentNodeAndPushBodyElement();

    protected abstract void appendToCurrentNodeAndPushElement(String name,
            Attributes attributes);

    protected abstract void appendHtmlElementToDocument();

}