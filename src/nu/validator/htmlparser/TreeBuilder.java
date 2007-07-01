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

    private Phase phase = Phase.INITIAL;

    private Phase phaseBeforeSwitchingToTrailingEnd;

    private Tokenizer tokenizer;

    private ErrorHandler errorHandler;

    private DocumentModeHandler documentModeHandler;

    private DoctypeExpectation doctypeExpectation;

    private int cdataOrRcdataTimesToPop;
    
    private boolean scriptingEnabled;

    private boolean nonConformingAndStreaming;

    private boolean previousTokenWasPreStart;

    private boolean wantingComments;

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

    private void documentMode(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) {
        if (documentModeHandler != null) {
            documentModeHandler.documentMode(mode, publicIdentifier,
                    systemIdentifier, html4SpecificAdditionalErrorChecks);
        }
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
        previousTokenWasPreStart = false;
        cdataOrRcdataTimesToPop = 0;
    }

    /**
     * @see nu.validator.htmlparser.TokenHandler#wantsComments()
     */
    public abstract boolean wantsComments() throws SAXException;

    public final void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean correct) throws SAXException {
        previousTokenWasPreStart = false;
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

    public final void comment(char[] buf, int length) throws SAXException {
        previousTokenWasPreStart = false;
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

    protected abstract void appendCommentToCurrentNode(char[] buf, int length);

    protected abstract void appendCommentToDocument(char[] buf, int length);

    protected abstract void appendCommentToRootElement(char[] buf, int length);

    
    //XXX intern element names and use ==
    
    public final void startTag(String name, Attributes attributes)
            throws SAXException {
        previousTokenWasPreStart = false;
        for (;;) {
            switch (phase) {
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
                     * Then, switch to the root element phase of the
                     * tree construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    // optimize error check and streaming SAX by hoisting
                    // "html" handling here.
                    if ("html".equals(name)) {
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
                    if ("head".equals(name)) {
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
                case IN_HEAD:
                    if ("base".equals(name) || "link".equals(name)) {
                        /*
                         * A start tag whose tag name is one of: "base", "link"

    Insert an HTML element for the token.

                         */
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;
                    } else if ("meta".equals(name)) {
                        // XXX do charset stuff
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;                        
                    } else if ("title".equals(name)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA, name);
                        return;                                               
                    } else if ("style".equals(name) || ("noscript".equals(name) && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("noscript".equals(name) && !scriptingEnabled) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_HEAD_NOSCRIPT;
                        return;
                    } else if ("script".equals(name)) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("head".equals(name)) {
                        /*Parse error.*/
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        /* Ignore the token.*/
                        return;
                    } else {
                        /* Act as if an end tag token with the tag name "head" had been seen, */
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        /* and reprocess the current token.  */
                        continue;
                    }
                case IN_HEAD_NOSCRIPT:
                    // XXX did Hixie really mean to omit "base" here?
                    if ("link".equals(name)) {
                        /*
                         * Insert an HTML element for the token.
                         */
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;
                    } else if ("meta".equals(name)) {
                        // XXX do charset stuff
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;                        
                    } else if ("style".equals(name)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("head".equals(name)) {
                        /*Parse error.*/
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        /* Ignore the token.*/
                        return;
                    } else if ("noscript".equals(name)) {
                        /*Parse error.*/
                        err("Start tag for \u201Cnoscript\u201D seen when \u201Cnoscript\u201D was already open.");
                        /* Ignore the token.*/
                        return;
                    } else {
                        /*
                         * Parse error.*/
                        err("Bad start tag in \u201Cnoscript\u201D in \u201Chead\u201D.");
                        /* Act as if an end tag with the tag name "noscript" had been seen 
                         */
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        /*
                         * and reprocess the current token.
                         */
                        continue;
                    }
                case AFTER_HEAD:
                    if ("body".equals(name)) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendToCurrentNodeAndPushBodyElement();
                        } else {
                            appendToCurrentNodeAndPushBodyElement(attributes);
                        }
                        phase = Phase.IN_BODY;
                        return;
                    } else if ("frameset".equals(name)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_FRAMESET;
                        return;
                    } else if ("base".equals(name)) {
                        err("\u201Cbase\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("link".equals(name)) {
                        err("\u201Clink\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("meta".equals(name)) {
                        err("\u201Cmeta\u201D element outside \u201Chead\u201D.");
                        // XXX do chaset stuff
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("script".equals(name)) {
                        err("\u201Cscript\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1 : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("style".equals(name)) {
                        err("\u201Cstyle\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1 : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("title".equals(name)) {
                        err("\u201Ctitle\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1 : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA, name);
                        return;                                                                       
                    } else {
                        appendToCurrentNodeAndPushBodyElement();
                        phase = Phase.IN_BODY;
                        continue;
                    }
                case IN_BODY:
                    if ("base".equals(name) || "link".equals(name)) {
                        /*
                         * A start tag whose tag name is one of: "base", "link"

    Insert an HTML element for the token.

                         */
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;
                    } else if ("meta".equals(name)) {
                        // XXX do charset stuff
                        appendToCurrentNodeAndPushElement(name, attributes);
                        popCurrentNode(); // XXX not in spec
                        return;                        
                    } else if ("style".equals(name)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("script".equals(name)) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA, name);
                        return;                                                                       
                    } else if ("title".equals(name)) {
                        err("\u201Ctitle\u201D element found inside \u201Cbody\201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1 : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA, name);
                        return;                                                                       
                    } else if ("body".equals(name)) {
                        err("\u201Cbody\u201D start tag found but the \u201Cbody\201D element is already open.");
                        addAttributesToBody(attributes);
                        return;
                    } else if ("p".equals(name) || "div".equals(name) || "h1".equals(name) || "h2".equals(name) || "h3".equals(name) || "h4".equals(name) || "h5".equals(name) || "h6".equals(name) || "blockquote".equals(name) || "ol".equals(name) ||  "ul".equals(name) || "dl".equals(name) || "fieldset".equals(name) || "address".equals(name) || "menu".equals(name)  || "center".equals(name) || "dir".equals(name) || "listing".equals(name)) {
                        if(stackHasInScope("p")) {
                            endP();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("pre".equals(name)) {
                        if(stackHasInScope("p")) {
                            endP();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);                        
                        previousTokenWasPreStart = true;
                        return;
                    } else if ("form".equals(name)) {
                        if (isFormPointerNull()) {
                            err("Saw a \u201Cform\u201D start tag, but there was already an active \u201Cform\u201D element.");
                            return;
                        } else {
                            if(stackHasInScope("p")) {
                                endP();
                            }
                            appendToCurrentNodeAndPushFormElement(attributes);
                            return;                            
                        }
                    } else if ("li".equals(name)) {
                        if(stackHasInScope("p")) {
                            endP();
                        }
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
                    } else if ("dd".equals(name) || "dt".equals(name)) {
                        if(stackHasInScope("p")) {
                            endP();
                        }
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
                    } else if ("plaintext".equals(name)) {
                        if(stackHasInScope("p")) {
                            endP();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        tokenizer.setContentModelFlag(ContentModelFlag.PLAINTEXT, name);
                        return;
                    } else if ("a".equals(name)) {
                        // TODO
                        /*
                         * 

    If the list of active formatting elements contains an element whose tag name is "a" between the end of the list and the last marker on the list (or the start of the list if there is no marker on the list), then this is a parse error; act as if an end tag with the tag name "a" had been seen, then remove that element from the list of active formatting elements and the stack of open elements if the end tag didn't already remove it (it might not have if the element is not in table scope).

    In the non-conforming stream <a href="a">a<table><a href="b">b</table>x, the first a element would be closed upon seeing the second one, and the "x" character would be inside a link to "b", not to "a". This is despite the fact that the outer a element is not in table scope (meaning that a regular </a> end tag at the start of the table wouldn't close the outer a element).

    Reconstruct the active formatting elements, if any.

    Insert an HTML element for the token. Add that element to the list of active formatting elements.

                         * 
                         */
                    }
                case IN_TABLE:
                    // TODO
                    return;
                case IN_CAPTION:
                    // TODO
                    return;
                case IN_COLUMN_GROUP:
                    // TODO
                    return;
                case IN_TABLE_BODY:
                    // TODO
                    return;
                case IN_ROW:
                    // TODO
                    return;
                case IN_CELL:
                    // TODO
                    return;
                case IN_SELECT:
                    // TODO
                    return;
                case AFTER_BODY:
                    // TODO
                    return;
                case IN_FRAMESET:
                    // TODO
                    return;
                case AFTER_FRAMESET:
                    // TODO
                    return;
                case TRAILING_END:
                    // TODO
                    return;
            }
        }
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

    public final void endTag(String name, Attributes attributes)
            throws SAXException {
        previousTokenWasPreStart = false;
        if (cdataOrRcdataTimesToPop > 0) {
            while (cdataOrRcdataTimesToPop > 0) {
                popCurrentNode();
                cdataOrRcdataTimesToPop--;
            }
            return;
        }

        for (;;) {
            switch (phase) {
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
                     * Then, switch to the root element phase of the
                     * tree construction stage
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
                    // TODO
                    return;
                case IN_HEAD:
                    // TODO
                    return;
                case IN_HEAD_NOSCRIPT:
                    // TODO
                    return;
                case AFTER_HEAD:
                    // TODO
                    return;
                case IN_BODY:
                    // TODO
                    return;
                case IN_TABLE:
                    // TODO
                    return;
                case IN_CAPTION:
                    // TODO
                    return;
                case IN_COLUMN_GROUP:
                    // TODO
                    return;
                case IN_TABLE_BODY:
                    // TODO
                    return;
                case IN_ROW:
                    // TODO
                    return;
                case IN_CELL:
                    // TODO
                    return;
                case IN_SELECT:
                    // TODO
                    return;
                case AFTER_BODY:
                    // TODO
                    return;
                case IN_FRAMESET:
                    // TODO
                    return;
                case AFTER_FRAMESET:
                    // TODO
                    return;
                case TRAILING_END:
                    // TODO
                    return;
            }
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
                     * Then, switch to the root element phase of the
                     * tree construction stage
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
                     /* If there are more than two nodes on the stack of open
                     * elements, */
                    if (stackSize() > 2) {
                        err("End of file seen and there were open elements.");
                    } else if (stackSize() == 2 && !"body".equals(nameOfCurrentNode())) {
                        /*or if there are two nodes but the second node
                         * is not a body node, this is a parse error.
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
                    
                    
                     /* Stop parsing.*/
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
                    /*Stop parsing.*/
                    return;
            }
        }
    }

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

    /**
     * @see nu.validator.htmlparser.TokenHandler#characters(char[], int, int)
     */
    public final void characters(char[] buf, int start, int length)
            throws SAXException {
        if (previousTokenWasPreStart) {
            if (buf[start] == '\n') {
                start++;
                length--;
                if (length == 0) {
                    return;
                }
            }
            previousTokenWasPreStart = false;
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
                                 * Reconstruct the active formatting elements, if
                                 * any.
                                 */
                                reconstructTheActiveFormattingElements();
                                /* Append the token's character to the current node. */
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
                             * Act as if an end tag with the tag name "colgroup" had been seen, and then, if that token wasn't ignored, reprocess the current token.
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
                             * Parse error. */
                            err("Non-space character in page trailer.");
                            /*Switch back to the main phase and reprocess the token.
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