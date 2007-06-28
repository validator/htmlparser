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

package nu.validator.htmlparser;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class TreeBuilder implements TokenHandler {

    private enum Phase {
        INITIAL_PHASE, ROOT_ELEMENT, BEFORE_HEAD, IN_HEAD, AFTER_HEAD, IN_BODY, IN_TABLE, IN_CAPTION, IN_COLUMN_GROUP, IN_TABLE_BODY, IN_ROW, IN_CELL, IN_SELECT, AFTER_BODY, IN_FRAMESET, AFTER_FRAMESET, TRAILING_END
    }

    private Phase phase;
    private Tokenizer tokenizer;
    private ErrorHandler errorHandler;

    /**
     * Reports an condition that would make the infoset incompatible with XML 1.0 as fatal.
     * 
     * @param message the message
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
     * @param message the message
     * @throws SAXException
     */
    protected void err(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.error(spe);
    }

    /**
     * Reports a warning
     * 
     * @param message the message
     * @throws SAXException
     */
    protected void warn(String message) throws SAXException {
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.warning(spe);
    }
    
    public void start(Tokenizer self) throws SAXException {
        // TODO Auto-generated method stub

    }
    
    public abstract boolean wantsComments() throws SAXException;
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        outer : for (;;) {
            switch (phase) {
                case INITIAL_PHASE:
                    int end = start + length;
                    for (int i = start; i < end; i++) {
                        switch (buf[i]) {
                            case '\t':
                            case '\n':
                            case '\u000B':
                            case '\u000C':
                            case ' ':
                                /*
                                 * A character token that is one of one of
                                 * U+0009 CHARACTER TABULATION, U+000A LINE FEED
                                 * (LF), U+000B LINE TABULATION, U+000C FORM
                                 * FEED (FF), or U+0020 SPACE Ignore the token.
                                 * 
                                 */
                                continue;
                                default: 
                                    /*
                                     *      A character token that is not one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE TABULATION,
     U+000C FORM FEED (FF), or U+0020 SPACE
     Parse error.
     */
                                    err("Non-space characters found without seeing a doctype first.");
                                    /*

     Set the document to quirks mode.
*/
                                toQuirksMode();
                                /*
     Then, switch to the root element phase of the tree construction
     stage */
                                    phase = Phase.ROOT_ELEMENT;
                                    /*
                                     * and reprocess the current token.

                                     * 
                                     */
                                    start = i;
                                    length = end - i;
                                    continue outer;
                        }
                    }
                    return;
                case ROOT_ELEMENT:
                    //                  TODO
                    return;
                case BEFORE_HEAD:
                    //                  TODO
                    return;
                case IN_HEAD:
                    //                  TODO
                    return;
                case AFTER_HEAD:
                    //                  TODO
                    return;
                case IN_BODY:
                    //                  TODO
                    return;
                case IN_TABLE:
                    //                  TODO
                    return;
                case IN_CAPTION:
                    //                  TODO
                    return;
                case IN_COLUMN_GROUP:
                    //                  TODO
                    return;
                case IN_TABLE_BODY:
                    //                  TODO
                    return;
                case IN_ROW:
                    //                  TODO
                    return;
                case IN_CELL:
                    //                  TODO
                    return;
                case IN_SELECT:
                    //                  TODO
                    return;
                case AFTER_BODY:
                    //                  TODO
                    return;
                case IN_FRAMESET:
                    //                  TODO
                    return;
                case AFTER_FRAMESET:
                    //                  TODO
                    return;
                case TRAILING_END:
                    //                  TODO
                    return;
            }
        }
    }

    private void toQuirksMode() {
        // TODO Auto-generated method stub
        
    }

    public void comment(char[] buf, int length) throws SAXException {
        appendCommentToCurrentNode(buf, length);
    }

    protected abstract void appendCommentToCurrentNode(char[] buf, int length);

    public void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean correct) throws SAXException {
        for (;;) {
            switch (phase) {
                case INITIAL_PHASE:
                    //                  TODO
                    return;
                case ROOT_ELEMENT:
                    //                  TODO
                    return;
                case BEFORE_HEAD:
                    //                  TODO
                    return;
                case IN_HEAD:
                    //                  TODO
                    return;
                case AFTER_HEAD:
                    //                  TODO
                    return;
                case IN_BODY:
                    //                  TODO
                    return;
                case IN_TABLE:
                    //                  TODO
                    return;
                case IN_CAPTION:
                    //                  TODO
                    return;
                case IN_COLUMN_GROUP:
                    //                  TODO
                    return;
                case IN_TABLE_BODY:
                    //                  TODO
                    return;
                case IN_ROW:
                    //                  TODO
                    return;
                case IN_CELL:
                    //                  TODO
                    return;
                case IN_SELECT:
                    //                  TODO
                    return;
                case AFTER_BODY:
                    //                  TODO
                    return;
                case IN_FRAMESET:
                    //                  TODO
                    return;
                case AFTER_FRAMESET:
                    //                  TODO
                    return;
                case TRAILING_END:
                    //                  TODO
                    return;
            }
        }
    }

    public void startTag(String name, Attributes attributes) throws SAXException {
        for (;;) {
            switch (phase) {
                case INITIAL_PHASE:
                    //                  TODO
                    return;
                case ROOT_ELEMENT:
                    //                  TODO
                    return;
                case BEFORE_HEAD:
                    //                  TODO
                    return;
                case IN_HEAD:
                    //                  TODO
                    return;
                case AFTER_HEAD:
                    //                  TODO
                    return;
                case IN_BODY:
                    //                  TODO
                    return;
                case IN_TABLE:
                    //                  TODO
                    return;
                case IN_CAPTION:
                    //                  TODO
                    return;
                case IN_COLUMN_GROUP:
                    //                  TODO
                    return;
                case IN_TABLE_BODY:
                    //                  TODO
                    return;
                case IN_ROW:
                    //                  TODO
                    return;
                case IN_CELL:
                    //                  TODO
                    return;
                case IN_SELECT:
                    //                  TODO
                    return;
                case AFTER_BODY:
                    //                  TODO
                    return;
                case IN_FRAMESET:
                    //                  TODO
                    return;
                case AFTER_FRAMESET:
                    //                  TODO
                    return;
                case TRAILING_END:
                    //                  TODO
                    return;
            }
        }
        }
    
    public void endTag(String name, Attributes attributes) throws SAXException {
        for (;;) {
            switch (phase) {
                case INITIAL_PHASE:
                    //                  TODO
                    return;
                case ROOT_ELEMENT:
                    //                  TODO
                    return;
                case BEFORE_HEAD:
                    //                  TODO
                    return;
                case IN_HEAD:
                    //                  TODO
                    return;
                case AFTER_HEAD:
                    //                  TODO
                    return;
                case IN_BODY:
                    //                  TODO
                    return;
                case IN_TABLE:
                    //                  TODO
                    return;
                case IN_CAPTION:
                    //                  TODO
                    return;
                case IN_COLUMN_GROUP:
                    //                  TODO
                    return;
                case IN_TABLE_BODY:
                    //                  TODO
                    return;
                case IN_ROW:
                    //                  TODO
                    return;
                case IN_CELL:
                    //                  TODO
                    return;
                case IN_SELECT:
                    //                  TODO
                    return;
                case AFTER_BODY:
                    //                  TODO
                    return;
                case IN_FRAMESET:
                    //                  TODO
                    return;
                case AFTER_FRAMESET:
                    //                  TODO
                    return;
                case TRAILING_END:
                    //                  TODO
                    return;
            }
        }
    }

    public void eof() throws SAXException {
        for (;;) {
            switch (phase) {
                case INITIAL_PHASE:
                    //                  TODO
                    return;
                case ROOT_ELEMENT:
                    //                  TODO
                    return;
                case BEFORE_HEAD:
                    //                  TODO
                    return;
                case IN_HEAD:
                    //                  TODO
                    return;
                case AFTER_HEAD:
                    //                  TODO
                    return;
                case IN_BODY:
                    //                  TODO
                    return;
                case IN_TABLE:
                    //                  TODO
                    return;
                case IN_CAPTION:
                    //                  TODO
                    return;
                case IN_COLUMN_GROUP:
                    //                  TODO
                    return;
                case IN_TABLE_BODY:
                    //                  TODO
                    return;
                case IN_ROW:
                    //                  TODO
                    return;
                case IN_CELL:
                    //                  TODO
                    return;
                case IN_SELECT:
                    //                  TODO
                    return;
                case AFTER_BODY:
                    //                  TODO
                    return;
                case IN_FRAMESET:
                    //                  TODO
                    return;
                case AFTER_FRAMESET:
                    //                  TODO
                    return;
                case TRAILING_END:
                    //                  TODO
                    return;
            }
        }
    }

    /*
     * 
     #8.2.3. Tokenisation Table of contents 8.3. Namespaces

     WHATWG 

     HTML 5

     Working Draft — 27 June 2007

     < 8.2.3. Tokenisation – Table of contents – 8.3. Namespaces >

     8.2.4. Tree construction

     The input to the tree construction stage is a sequence of tokens from
     the tokenisation stage. The tree construction stage is associated with
     a DOM Document object when a parser is created. The "output" of this
     stage consists of dynamically modifying or extending that document's
     DOM tree.

     Tree construction passes through several phases. Initially, UAs must
     act according to the steps described as being those of the initial
     phase.

     This specification does not define when an interactive user agent has
     to render the Document available to the user, or when it has to begin
     accepting user input.

     When the steps below require the UA to append a character to a node,
     the UA must collect it and all subsequent consecutive characters that
     would be appended to that node, and insert one Text node whose data is
     the concatenation of all those characters.

     DOM mutation events must not fire for changes caused by the UA parsing
     the document. (Conceptually, the parser is not mutating the DOM, it is
     constructing it.) This includes the parsing of any content inserted
     using document.write() and document.writeln() calls. [DOM3EVENTS]

     Not all of the tag names mentioned below are conformant tag names in
     this specification; many are included to handle legacy content. They
     still form part of the algorithm that implementations are required to
     implement to claim conformance.

     The algorithm described below places no limit on the depth of the DOM
     tree generated, or on the length of tag names, attribute names,
     attribute values, text nodes, etc. While implementators are encouraged
     to avoid arbitrary limits, it is recognised that practical concerns
     will likely force user agents to impose nesting depths.

     8.2.4.1. The initial phase

     Initially, the tree construction stage must handle each token emitted
     from the tokenisation stage as follows:



     A DOCTYPE token
     If the DOCTYPE token's name does not case-insensitively match
     the string "HTML", or if the token's public identifier is not
     missing, or if the token's system identifier is not missing,
     then there is a parse error. Conformance checkers may, instead
     of reporting this error, switch to a conformance checking mode
     for another language (e.g. based on the DOCTYPE token a
     conformance checker could recognise that the document is an
     HTML4-era document, and defer to an HTML4 conformance checker.)

     Append a DocumentType node to the Document node, with the name
     attribute set to the name given in the DOCTYPE token; the
     publicId attribute set to the public identifier given in the
     DOCTYPE token, or the empty string if the public identifier was
     not set; the systemId attribute set to the system identifier
     given in the DOCTYPE token, or the empty string if the system
     identifier was not set; and the other attributes specific to
     DocumentType objects set to null and empty lists as
     appropriate. Associate the DocumentType node with the Document
     object so that it is returned as the value of the doctype
     attribute of the Document object.

     Then, if the DOCTYPE token matches one of the conditions in the
     following list, then set the document to quirks mode:

     + The correctness flag is set to incorrect.
     + The name is set to anything other than "HTML".
     + The public identifier is set to: "+//Silmaril//dtd html Pro
     v0r11 19970101//EN"
     + The public identifier is set to: "-//AdvaSoft Ltd//DTD HTML
     3.0 asWedit + extensions//EN"
     + The public identifier is set to: "-//AS//DTD HTML 3.0 asWedit
     + extensions//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0 Level
     1//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0 Level
     2//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0
     Strict Level 1//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0
     Strict Level 2//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0
     Strict//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.0//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 2.1E//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 3.0//EN"
     + The public identifier is set to: "-//IETF//DTD HTML
     3.0//EN//"
     + The public identifier is set to: "-//IETF//DTD HTML 3.2
     Final//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 3.2//EN"
     + The public identifier is set to: "-//IETF//DTD HTML 3//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     0//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     0//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     1//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     1//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     2//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     2//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     3//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Level
     3//EN//3.0"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 0//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 0//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 1//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 1//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 2//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 2//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 3//EN"
     + The public identifier is set to: "-//IETF//DTD HTML Strict
     Level 3//EN//3.0"
     + The public identifier is set to: "-//IETF//DTD HTML
     Strict//EN"
     + The public identifier is set to: "-//IETF//DTD HTML
     Strict//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML
     Strict//EN//3.0"
     + The public identifier is set to: "-//IETF//DTD HTML//EN"
     + The public identifier is set to: "-//IETF//DTD HTML//EN//2.0"
     + The public identifier is set to: "-//IETF//DTD HTML//EN//3.0"
     + The public identifier is set to: "-//Metrius//DTD Metrius
     Presentational//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 2.0 HTML Strict//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 2.0 HTML//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 2.0 Tables//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 3.0 HTML Strict//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 3.0 HTML//EN"
     + The public identifier is set to: "-//Microsoft//DTD Internet
     Explorer 3.0 Tables//EN"
     + The public identifier is set to: "-//Netscape Comm.
     Corp.//DTD HTML//EN"
     + The public identifier is set to: "-//Netscape Comm.
     Corp.//DTD Strict HTML//EN"
     + The public identifier is set to: "-//O'Reilly and
     Associates//DTD HTML 2.0//EN"
     + The public identifier is set to: "-//O'Reilly and
     Associates//DTD HTML Extended 1.0//EN"
     + The public identifier is set to: "-//Spyglass//DTD HTML 2.0
     Extended//EN"
     + The public identifier is set to: "-//SQ//DTD HTML 2.0
     HoTMetaL + extensions//EN"
     + The public identifier is set to: "-//Sun Microsystems
     Corp.//DTD HotJava HTML//EN"
     + The public identifier is set to: "-//Sun Microsystems
     Corp.//DTD HotJava Strict HTML//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 3
     1995-03-24//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 3.2
     Draft//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 3.2
     Final//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 3.2//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 3.2S
     Draft//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 4.0
     Frameset//EN"
     + The public identifier is set to: "-//W3C//DTD HTML 4.0
     Transitional//EN"
     + The public identifier is set to: "-//W3C//DTD HTML
     Experimental 19960712//EN"
     + The public identifier is set to: "-//W3C//DTD HTML
     Experimental 970421//EN"
     + The public identifier is set to: "-//W3C//DTD W3 HTML//EN"
     + The public identifier is set to: "-//W3O//DTD W3 HTML
     3.0//EN"
     + The public identifier is set to: "-//W3O//DTD W3 HTML
     3.0//EN//"
     + The public identifier is set to: "-//W3O//DTD W3 HTML Strict
     3.0//EN//"
     + The public identifier is set to: "-//WebTechs//DTD Mozilla
     HTML 2.0//EN"
     + The public identifier is set to: "-//WebTechs//DTD Mozilla
     HTML//EN"
     + The public identifier is set to: "-/W3C/DTD HTML 4.0
     Transitional/EN"
     + The public identifier is set to: "HTML"
     + The system identifier is set to:
     "http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd"
     + The system identifier is missing and the public identifier is
     set to: "-//W3C//DTD HTML 4.01 Frameset//EN"
     + The system identifier is missing and the public identifier is
     set to: "-//W3C//DTD HTML 4.01 Transitional//EN"

     Otherwise, if the DOCTYPE token matches one of the conditions
     in the following list, then set the document to limited quirks
     mode:

     + The public identifier is set to: "-//W3C//DTD XHTML 1.0
     Frameset//EN"
     + The public identifier is set to: "-//W3C//DTD XHTML 1.0
     Transitional//EN"
     + The system identifier is not missing and the public
     identifier is set to: "-//W3C//DTD HTML 4.01 Frameset//EN"
     + The system identifier is not missing and the public
     identifier is set to: "-//W3C//DTD HTML 4.01
     Transitional//EN"

     The name, system identifier, and public identifier strings must
     be compared to the values given in the lists above in a
     case-insensitive manner.

     Then, switch to the root element phase of the tree construction
     stage.

     A start tag token
     An end tag token


     An end-of-file token
     Parse error.

     Set the document to quirks mode.

     Then, switch to the root element phase of the tree construction
     stage and reprocess the current token.

     8.2.4.2. The root element phase

     After the initial phase, as each token is emitted from the
     tokenisation stage, it must be processed as described in this section.

     A DOCTYPE token
     Parse error. Ignore the token.

     A comment token
     Append a Comment node to the Document object with the data
     attribute set to the data given in the comment token.

     A character token that is one of one of U+0009 CHARACTER TABULATION,
     U+000A LINE FEED (LF), U+000B LINE TABULATION, U+000C FORM FEED
     (FF), or U+0020 SPACE
     Ignore the token.

     A character token that is not one of U+0009 CHARACTER TABULATION,
     U+000A LINE FEED (LF), U+000B LINE TABULATION, U+000C FORM FEED
     (FF), or U+0020 SPACE

     A start tag token
     An end tag token
     An end-of-file token
     Create an HTMLElement node with the tag name html, in the HTML
     namespace. Append it to the Document object. Switch to the main
     phase and reprocess the current token.

     Should probably make end tags be ignored, so that "</head><!--
     --><html>" puts the comment before the root node (or should
     we?)

     The root element can end up being removed from the Document object,
     e.g. by scripts; nothing in particular happens in such cases, content
     continues being appended to the nodes as described in the next
     section.

     8.2.4.3. The main phase

     After the root element phase, each token emitted from the tokenisation
     stage must be processed as described in this section. This is by far
     the most involved part of parsing an HTML document.

     The tree construction stage in this phase has several pieces of state:
     a stack of open elements, a list of active formatting elements, a head
     element pointer, a form element pointer, and an insertion mode.

     We could just fold insertion modes and phases into one concept (and
     duplicate the two rules common to all insertion modes into all of
     them).

     8.2.4.3.1. The stack of open elements

     Initially the stack of open elements contains just the html root
     element node created in the last phase before switching to this phase
     (or, in the fragment case, the html element created as part of that
     algorithm). That's the topmost node of the stack. It never gets popped
     off the stack. (This stack grows downwards.)

     The current node is the bottommost node in this stack.

     Elements in the stack fall into the following categories:

     Special
     The following HTML elements have varying levels of special
     parsing rules: address, area, base, basefont, bgsound,
     blockquote, body, br, center, col, colgroup, dd, dir, div, dl,
     dt, embed, fieldset, form, frame, frameset, h1, h2, h3, h4, h5,
     h6, head, hr, iframe, image, img, input, isindex, li, link,
     listing, menu, meta, noembed, noframes, noscript, ol, optgroup,
     option, p, param, plaintext, pre, script, select, spacer,
     style, tbody, textarea, tfoot, thead, title, tr, ul, and wbr.

     Scoping
     The following HTML elements introduce new scopes for various
     parts of the parsing: button, caption, html, marquee, object,
     table, td and th.

     Formatting
     The following HTML elements are those that end up in the list
     of active formatting elements: a, b, big, em, font, i, nobr, s,
     small, strike, strong, tt, and u.

     Phrasing
     All other elements found while parsing an HTML document.

     Still need to add these new elements to the lists: event-source,
     section, nav, article, aside, header, footer, datagrid, command

     The stack of open elements is said to have an element in scope or have
     an element in table scope when the following algorithm terminates in a
     match state:
     1. Initialise node to be the current node (the bottommost node of the
     stack).
     2. If node is the target node, terminate in a match state.
     3. Otherwise, if node is a table element, terminate in a failure
     state.
     4. Otherwise, if the algorithm is the "has an element in scope"
     variant (rather than the "has an element in table scope" variant),
     and node is one of the following, terminate in a failure state:
     + caption
     + td
     + th
     + button
     + marquee
     + object
     5. Otherwise, if node is an html element, terminate in a failure
     state. (This can only happen if the node is the topmost node of
     the stack of open elements, and prevents the next step from being
     invoked if there are no more elements in the stack.)
     6. Otherwise, set node to the previous entry in the stack of open
     elements and return to step 2. (This will never fail, since the
     loop will always terminate in the previous step if the top of the
     stack is reached.)

     Nothing happens if at any time any of the elements in the stack of
     open elements are moved to a new location in, or removed from, the
     Document tree. In particular, the stack is not changed in this
     situation. This can cause, amongst other strange effects, content to
     be appended to nodes that are no longer in the DOM.

     In some cases (namely, when closing misnested formatting elements),
     the stack is manipulated in a random-access fashion.

     8.2.4.3.2. The list of active formatting elements

     Initially the list of active formatting elements is empty. It is used
     to handle mis-nested formatting element tags.

     The list contains elements in the formatting category, and scope
     markers. The scope markers are inserted when entering buttons, object
     elements, marquees, table cells, and table captions, and are used to
     prevent formatting from "leaking" into tables, buttons, object
     elements, and marquees.

     When the steps below require the UA to reconstruct the active
     formatting elements, the UA must perform the following steps:
     1. If there are no entries in the list of active formatting elements,
     then there is nothing to reconstruct; stop this algorithm.
     2. If the last (most recently added) entry in the list of active
     formatting elements is a marker, or if it is an element that is in
     the stack of open elements, then there is nothing to reconstruct;
     stop this algorithm.
     3. Let entry be the last (most recently added) element in the list of
     active formatting elements.
     4. If there are no entries before entry in the list of active
     formatting elements, then jump to step 8.
     5. Let entry be the entry one earlier than entry in the list of
     active formatting elements.
     6. If entry is neither a marker nor an element that is also in the
     stack of open elements, go to step 4.
     7. Let entry be the element one later than entry in the list of
     active formatting elements.
     8. Perform a shallow clone of the element entry to obtain clone.
     [DOM3CORE]
     9. Append clone to the current node and push it onto the stack of
     open elements so that it is the new current node.
     10. Replace the entry for entry in the list with an entry for clone.
     11. If the entry for clone in the list of active formatting elements
     is not the last entry in the list, return to step 7.

     This has the effect of reopening all the formatting elements that were
     opened in the current body, cell, or caption (whichever is youngest)
     that haven't been explicitly closed.

     The way this specification is written, the list of active formatting
     elements always consists of elements in chronological order with the
     least recently added element first and the most recently added element
     last (except for while steps 8 to 11 of the above algorithm are being
     executed, of course).

     When the steps below require the UA to clear the list of active
     formatting elements up to the last marker, the UA must perform the
     following steps:
     1. Let entry be the last (most recently added) entry in the list of
     active formatting elements.
     2. Remove entry from the list of active formatting elements.
     3. If entry was a marker, then stop the algorithm at this point. The
     list has been cleared up to the last marker.
     4. Go to step 1.

     8.2.4.3.3. Creating and inserting HTML elements

     When the steps below require the UA to create an element for a token,
     the UA must create a node implementing the interface appropriate for
     the element type corresponding to the tag name of the token (as given
     in the section of this specification that defines that element, e.g.
     for an a element it would be the HTMLAnchorElement interface), with
     the tag name being the name of that element, with the node being in
     the HTML namespace, and with the attributes on the node being those
     given in the given token.

     When the steps below require the UA to insert an HTML element for a
     token, the UA must first create an element for the token, and then
     append this node to the current node, and push it onto the stack of
     open elements so that it is the new current node.

     The steps below may also require that the UA insert an HTML element in
     a particular place, in which case the UA must create an element for
     the token and then insert or append the new node in the location
     specified. (This happens in particular during the parsing of tables
     with invalid content.)

     The interface appropriate for an element that is not defined in this
     specification is HTMLElement.

     The generic CDATA parsing algorithm and the generic RCDATA parsing
     algorithm consist of the following steps. These algorithms are always
     invoked in response to a start tag token, and are always passed a
     context node, typically the current node, which is used as the place
     to insert the resulting element node.
     1. Create an element for the token.
     2. Append the new element to the given context node.
     3. If the algorithm that was invoked is the generic CDATA parsing
     algorithm, switch the tokeniser's content model flag to the CDATA
     state; otherwise the algorithm invoked was the generic RCDATA
     parsing algorithm, switch the tokeniser's content model flag to
     the RCDATA state.
     4. Then, collect all the character tokens that the tokeniser returns
     until it returns a token that is not a character token, or until
     it stops tokenising.
     5. If this process resulted in a collection of character tokens,
     append a single Text node, whose contents is the concatenation of
     all those tokens' characters, to the new element node.
     6. The tokeniser's content model flag will have switched back to the
     PCDATA state.
     7. If the next token is an end tag token with the same tag name as
     the start tag token, ignore it. Otherwise, this is a parse error.

     8.2.4.3.4. Closing elements that have implied end tags

     When the steps below require the UA to generate implied end tags,
     then, if the current node is a dd element, a dt element, an li
     element, a p element, a td element, a th element, or a tr element, the
     UA must act as if an end tag with the respective tag name had been
     seen and then generate implied end tags again.

     The step that requires the UA to generate implied end tags but lists
     an element to exclude from the process, then the UA must perform the
     above steps as if that element was not in the above list.

     8.2.4.3.5. The element pointers

     Initially the head element pointer and the form element pointer are
     both null.

     Once a head element has been parsed (whether implicitly or explicitly)
     the head element pointer gets set to point to this node.

     The form element pointer points to the last form element that was
     opened and whose end tag has not yet been seen. It is used to make
     form controls associate with forms in the face of dramatically bad
     markup, for historical reasons.

     8.2.4.3.6. The insertion mode

     Initially the insertion mode is "before head". It can change to "in
     head", "in head noscript", "after head", "in body", "in table", "in
     caption", "in column group", "in table body", "in row", "in cell", "in
     select", "after body", "in frameset", and "after frameset" during the
     course of the parsing, as described below. It affects how certain
     tokens are processed.

     If the tree construction stage is switched from the main phase to the
     trailing end phase and back again, the various pieces of state are not
     reset; the UA must act as if the state was maintained.

     When the steps below require the UA to reset the insertion mode
     appropriately, it means the UA must follow these steps:
     1. Let last be false.
     2. Let node be the last node in the stack of open elements.
     3. If node is the first node in the stack of open elements, then set
     last to true. If the context element of the HTML fragment parsing
     algorithm is neither a td element nor a th element, then set node
     to the context element. (fragment case)
     4. If node is a select element, then switch the insertion mode to "in
     select" and abort these steps. (fragment case)
     5. If node is a td or th element, then switch the insertion mode to
     "in cell" and abort these steps.
     6. If node is a tr element, then switch the insertion mode to "in
     row" and abort these steps.
     7. If node is a tbody, thead, or tfoot element, then switch the
     insertion mode to "in table body" and abort these steps.
     8. If node is a caption element, then switch the insertion mode to
     "in caption" and abort these steps.
     9. If node is a colgroup element, then switch the insertion mode to
     "in column group" and abort these steps. (fragment case)
     10. If node is a table element, then switch the insertion mode to "in
     table" and abort these steps.
     11. If node is a head element, then switch the insertion mode to "in
     body" ("in body"! not "in head"!) and abort these steps. (fragment
     case)
     12. If node is a body element, then switch the insertion mode to "in
     body" and abort these steps.
     13. If node is a frameset element, then switch the insertion mode to
     "in frameset" and abort these steps. (fragment case)
     14. If node is an html element, then: if the head element pointer is
     null, switch the insertion mode to "before head", otherwise,
     switch the insertion mode to "after head". In either case, abort
     these steps. (fragment case)
     15. If last is true, then set the insertion mode to "in body" and
     abort these steps. (fragment case)
     16. Let node now be the node before node in the stack of open
     elements.
     17. Return to step 3.

     8.2.4.3.7. How to handle tokens in the main phase

     Tokens in the main phase must be handled as follows:

     A DOCTYPE token
     Parse error. Ignore the token.

     A start tag whose tag name is "html"
     If this start tag token was not the first start tag token, then
     it is a parse error.

     For each attribute on the token, check to see if the attribute
     is already present on the top element of the stack of open
     elements. If it is not, add the attribute and its corresponding
     value to that element.

     An end-of-file token
     Generate implied end tags.

     If there are more than two nodes on the stack of open elements,
     or if there are two nodes but the second node is not a body
     node, this is a parse error.

     Otherwise, if the parser was originally created as part of the
     HTML fragment parsing algorithm, and there's more than one
     element in the stack of open elements, and the second node on
     the stack of open elements is not a body node, then this is a
     parse error. (fragment case)

     Stop parsing.

     This fails because it doesn't imply HEAD and BODY tags. We
     should probably expand out the insertion modes and merge them
     with phases and then put the three things here into each
     insertion mode instead of trying to factor them out so
     carefully.

     Anything else
     Depends on the insertion mode:

     If the insertion mode is "before head"
     Handle the token as follows:

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "head"
     Create an element for the token.

     Set the head element pointer to this new element
     node.

     Append the new element to the current node and push
     it onto the stack of open elements.

     Change the insertion mode to "in head".

     A start tag token whose tag name is one of: "base", "link",
     "meta", "script", "style", "title"
     Act as if a start tag token with the tag name
     "head" and no attributes had been seen, then
     reprocess the current token.

     This will result in a head element being generated,
     and with the current token being reprocessed in the
     "in head" insertion mode.

     An end tag whose tag name is one of: "head", "body", "html"
     Act as if a start tag token with the tag name
     "head" and no attributes had been seen, then
     reprocess the current token.

     Any other end tag
     Parse error. Ignore the token.

     Do we really want to ignore end tags here?

     A character token that is not one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE

     Any other start tag token
     Act as if a start tag token with the tag name
     "head" and no attributes had been seen, then
     reprocess the current token.

     This will result in an empty head element being
     generated, with the current token being reprocessed
     in the "after head" insertion mode.

     If the insertion mode is "in head"
     Handle the token as follows.

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is one of: "base", "link"
     Insert an HTML element for the token.

     A start tag whose tag name is "meta"
     Insert an HTML element for the token.

     If the element has a charset attribute, and its
     value is a supported encoding, and the confidence
     is currently tentative, then change the encoding to
     the encoding given by the value of the charset
     attribute.

     Otherwise, if the element has a content attribute,
     and applying the algorithm to extract an encoding
     from a Content-Type to its value returns a
     supported encoding encoding, and the confidence is
     currently tentative, then change the encoding to
     the encoding encoding.

     A start tag whose tag name is "title"
     Follow the generic RCDATA parsing algorithm, with
     the current node as the context node.

     A start tag whose tag name is "noscript", if scripting is
     enabled:

     A start tag whose tag name is "style"
     Follow the generic CDATA parsing algorithm, with
     the current node as the context node.

     A start tag whose tag name is "noscript", if scripting is
     disabled:
     Insert a noscript element for the token.

     Change the insertion mode to "in head noscript".

     A start tag whose tag name is "script"
     Create an element for the token.

     Mark the element as being "parser-inserted". This
     ensures that, if the script is external, any
     document.write() calls in the script will execute
     in-line, instead of blowing the document away, as
     would happen in most other cases.

     Switch the tokeniser's content model flag to the
     CDATA state.

     Then, collect all the character tokens that the
     tokeniser returns until it returns a token that is
     not a character token, or until it stops
     tokenising.

     If this process resulted in a collection of
     character tokens, append a single Text node to the
     script element node whose contents is the
     concatenation of all those tokens' characters.

     The tokeniser's content model flag will have
     switched back to the PCDATA state.

     If the next token is not an end tag token with the
     tag name "script", then this is a parse error; mark
     the script element as "already executed".
     Otherwise, the token is the script element's end
     tag, so ignore it.

     If the parser was originally created for the HTML
     fragment parsing algorithm, then mark the script
     element as "already executed", and skip the rest of
     the processing described for this token (including
     the part below where "scripts that will execute as
     soon as the parser resumes" are executed).
     (fragment case)

     Marking the script element as "already executed"
     prevents it from executing when it is inserted into
     the document a few paragraphs below. Thus, scripts
     missing their end tags and scripts that were
     inserted using innerHTML aren't executed.

     Let the old insertion point have the same value as
     the current insertion point. Let the insertion
     point be just before the next input character.

     Append the new element to the current node. Special
     processing occurs when a script element is inserted
     into a document that might cause some script to
     execute, which might cause new characters to be
     inserted into the tokeniser.

     Let the insertion point have the value of the old
     insertion point. (In other words, restore the
     insertion point to the value it had before the
     previous paragraph. This value might be the
     "undefined" value.)

     At this stage, if there is a script that will
     execute as soon as the parser resumes, then:

     If the tree construction stage is being called
     reentrantly, say from a call to
     document.write():
     Abort the processing of any nested
     invokations of the tokeniser, yielding
     control back to the caller. (Tokenisation
     will resume when the caller returns to the
     "outer" tree construction stage.)

     Otherwise:
     Follow these steps:

     1. Let the script be the script that will execute
     as soon as the parser resumes. There is no
     longer a script that will execute as soon as
     the parser resumes.
     2. Pause until the script has completed loading.
     3. Let the insertion point be just before the
     next input character.
     4. Execute the script.
     5. Let the insertion point be undefined again.
     6. If there is once again a script that will
     execute as soon as the parser resumes, then
     repeat these steps from step 1.

     An end tag whose tag name is "head"
     Pop the current node (which will be the head
     element) off the stack of open elements.

     Change the insertion mode to "after head".

     An end tag whose tag name is one of: "body", "html"
     Act as described in the "anything else" entry
     below.

     A start tag whose tag name is "head"
     Any other end tag
     Parse error. Ignore the token.

     Anything else
     Act as if an end tag token with the tag name "head"
     had been seen, and reprocess the current token.

     In certain UAs, some elements don't trigger the "in
     body" mode straight away, but instead get put into
     the head. Do we want to copy that?

     If the insertion mode is "in head noscript"

     An end tag whose tag name is "noscript"
     Pop the current node (which will be a noscript
     element) from the stack of open elements; the new
     current node will be a head element.

     Switch the insertion mode to "in head".

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE

     A comment token
     A start tag whose tag name is one of: "link", "meta",
     "style"
     Process the token as if the insertion mode had been
     "in head".

     A start tag whose tag name is one of: "head", "noscript"
     Any other end tag
     Parse error. Ignore the token.

     Anything else
     Parse error. Act as if an end tag with the tag name
     "noscript" had been seen and reprocess the current
     token.

     If the insertion mode is "after head"
     Handle the token as follows:

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "body"
     Insert a body element for the token.

     Change the insertion mode to "in body".

     A start tag whose tag name is "frameset"
     Insert a frameset element for the token.

     Change the insertion mode to "in frameset".

     A start tag token whose tag name is one of: "base", "link",
     "meta", "script", "style", "title"
     Parse error.

     Push the node pointed to by the head element
     pointer onto the stack of open elements.

     Process the token as if the insertion mode had been
     "in head".

     Pop the current node (which will be the node
     pointed to by the head element pointer) off the
     stack of open elements.

     Anything else
     Act as if a start tag token with the tag name
     "body" and no attributes had been seen, and then
     reprocess the current token.

     If the insertion mode is "in body"
     Handle the token as follows:

     A character token
     Reconstruct the active formatting elements, if any.

     Append the token's character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag token whose tag name is one of: "base", "link",
     "meta", "script", "style"
     Process the token as if the insertion mode had been
     "in head".

     A start tag whose tag name is "title"
     Parse error. Process the token as if the insertion
     mode had been "in head".

     A start tag whose tag name is "body"
     Parse error.

     If the second element on the stack of open elements
     is not a body element, or, if the stack of open
     elements has only one node on it, then ignore the
     token. (fragment case)

     Otherwise, for each attribute on the token, check
     to see if the attribute is already present on the
     body element (the second element) on the stack of
     open elements. If it is not, add the attribute and
     its corresponding value to that element.

     An end tag whose tag name is "body"
     If the second element in the stack of open elements
     is not a body element, this is a parse error.
     Ignore the token. (fragment case)

     Otherwise, if there is a node in the stack of open
     elements that is not either a dd element, a dt
     element, an li element, a p element, a td element,
     a th element, a tr element, the body element, or
     the html element, then this is a parse error.

     Change the insertion mode to "after body".

     An end tag whose tag name is "html"
     Act as if an end tag with tag name "body" had been
     seen, then, if that token wasn't ignored, reprocess
     the current token.

     The fake end tag token here can only be ignored in
     the fragment case.

     A start tag whose tag name is one of: "address",
     "blockquote", "center", "dir", "div", "dl",
     "fieldset", "listing", "menu", "ol", "p", "ul"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token.

     A start tag whose tag name is "pre"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token.

     If the next token is a U+000A LINE FEED (LF)
     character token, then ignore that token and move on
     to the next one. (Newlines at the start of pre
     blocks are ignored as an authoring convenience.)

     A start tag whose tag name is "form"
     If the form element pointer is not null, ignore the
     token with a parse error.

     Otherwise:

     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token, and set the
     form element pointer to point to the element
     created.

     A start tag whose tag name is "li"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Run the following algorithm:

     1. Initialise node to be the current node (the
     bottommost node of the stack).
     2. If node is an li element, then pop all the nodes
     from the current node up to node, including node,
     then stop this algorithm. If more than one node is
     popped, then this is a parse error.
     3. If node is not in the formatting category, and is
     not in the phrasing category, and is not an address
     or div element, then stop this algorithm.
     4. Otherwise, set node to the previous entry in the
     stack of open elements and return to step 2.

     Finally, insert an li element.

     A start tag whose tag name is one of: "dd", "dt"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Run the following algorithm:

     1. Initialise node to be the current node (the
     bottommost node of the stack).
     2. If node is a dd or dt element, then pop all the
     nodes from the current node up to node, including
     node, then stop this algorithm. If more than one
     node is popped, then this is a parse error.
     3. If node is not in the formatting category, and is
     not in the phrasing category, and is not an address
     or div element, then stop this algorithm.
     4. Otherwise, set node to the previous entry in the
     stack of open elements and return to step 2.

     Finally, insert an HTML element with the same tag
     name as the token's.

     A start tag whose tag name is "plaintext"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token.

     Switch the content model flag to the PLAINTEXT
     state.

     Once a start tag with the tag name "plaintext" has
     been seen, that will be the last token ever seen
     other than character tokens (and the end-of-file
     token), because there is no way to switch the
     content model flag out of the PLAINTEXT state.

     An end tag whose tag name is one of: "address",
     "blockquote", "center", "dir", "div", "dl",
     "fieldset", "listing", "menu", "ol", "pre", "ul"
     If the stack of open elements has an element in
     scope with the same tag name as that of the token,
     then generate implied end tags.

     Now, if the current node is not an element with the
     same tag name as that of the token, then this is a
     parse error.

     If the stack of open elements has an element in
     scope with the same tag name as that of the token,
     then pop elements from this stack until an element
     with that tag name has been popped from the stack.

     An end tag whose tag name is "form"
     If the stack of open elements has an element in
     scope with the same tag name as that of the token,
     then generate implied end tags.

     Now, if the current node is not an element with the
     same tag name as that of the token, then this is a
     parse error.

     Otherwise, if the current node is an element with
     the same tag name as that of the token pop that
     element from the stack.

     In any case, set the form element pointer to null.

     An end tag whose tag name is "p"
     If the stack of open elements has a p element in
     scope, then generate implied end tags, except for p
     elements.

     If the current node is not a p element, then this
     is a parse error.

     If the stack of open elements has a p element in
     scope, then pop elements from this stack until the
     stack no longer has a p element in scope.

     Otherwise, act as if a start tag with the tag name
     p had been seen, then reprocess the current token.

     An end tag whose tag name is one of: "dd", "dt", "li"
     If the stack of open elements has an element in
     scope whose tag name matches the tag name of the
     token, then generate implied end tags, except for
     elements with the same tag name as the token.

     If the current node is not an element with the same
     tag name as the token, then this is a parse error.

     If the stack of open elements has an element in
     scope whose tag name matches the tag name of the
     token, then pop elements from this stack until an
     element with that tag name has been popped from the
     stack.

     A start tag whose tag name is one of: "h1", "h2", "h3",
     "h4", "h5", "h6"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token.

     An end tag whose tag name is one of: "h1", "h2", "h3",
     "h4", "h5", "h6"
     If the stack of open elements has in scope an
     element whose tag name is one of "h1", "h2", "h3",
     "h4", "h5", or "h6", then generate implied end
     tags.

     Now, if the current node is not an element with the
     same tag name as that of the token, then this is a
     parse error.

     If the stack of open elements has in scope an
     element whose tag name is one of "h1", "h2", "h3",
     "h4", "h5", or "h6", then pop elements from the
     stack until an element with one of those tag names
     has been popped from the stack.

     A start tag whose tag name is "a"
     If the list of active formatting elements contains
     an element whose tag name is "a" between the end of
     the list and the last marker on the list (or the
     start of the list if there is no marker on the
     list), then this is a parse error; act as if an end
     tag with the tag name "a" had been seen, then
     remove that element from the list of active
     formatting elements and the stack of open elements
     if the end tag didn't already remove it (it might
     not have if the element is not in table scope).

     In the non-conforming stream
     <a href="a">a<table><a href="b">b</table>x, the
     first a element would be closed upon seeing the
     second one, and the "x" character would be inside a
     link to "b", not to "a". This is despite the fact
     that the outer a element is not in table scope
     (meaning that a regular </a> end tag at the start
     of the table wouldn't close the outer a element).

     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token. Add that
     element to the list of active formatting elements.

     A start tag whose tag name is one of: "b", "big", "em",
     "font", "i", "s", "small", "strike", "strong",
     "tt", "u"
     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token. Add that
     element to the list of active formatting elements.

     A start tag whose tag name is "nobr"
     Reconstruct the active formatting elements, if any.

     If the stack of open elements has a nobr element in
     scope, then act as if an end tag with the tag name
     nobr had been seen.

     Insert an HTML element for the token. Add that
     element to the list of active formatting elements.

     An end tag whose tag name is one of: "a", "b", "big", "em",
     "font", "i", "nobr", "s", "small", "strike",
     "strong", "tt", "u"
     Follow these steps:

     1. Let the formatting element be the last element in
     the list of active formatting elements that:
     @ is between the end of the list and the last
     scope marker in the list, if any, or the start
     of the list otherwise, and
     @ has the same tag name as the token.
     If there is no such node, or, if that node is also
     in the stack of open elements but the element is
     not in scope, then this is a parse error. Abort
     these steps. The token is ignored.
     Otherwise, if there is such a node, but that node
     is not in the stack of open elements, then this is
     a parse error; remove the element from the list,
     and abort these steps.
     Otherwise, there is a formatting element and that
     element is in the stack and is in scope. If the
     element is not the current node, this is a parse
     error. In any case, proceed with the algorithm as
     written in the following steps.
     2. Let the furthest block be the topmost node in the
     stack of open elements that is lower in the stack
     than the formatting element, and is not an element
     in the phrasing or formatting categories. There
     might not be one.
     3. If there is no furthest block, then the UA must
     skip the subsequent steps and instead just pop all
     the nodes from the bottom of the stack of open
     elements, from the current node up to and including
     the formatting element, and remove the formatting
     element from the list of active formatting
     elements.
     4. Let the common ancestor be the element immediately
     above the formatting element in the stack of open
     elements.
     5. If the furthest block has a parent node, then
     remove the furthest block from its parent node.
     6. Let a bookmark note the position of the formatting
     element in the list of active formatting elements
     relative to the elements on either side of it in
     the list.
     7. Let node and last node be the furthest block.
     Follow these steps:
     1. Let node be the element immediately prior to
     node in the stack of open elements.
     2. If node is not in the list of active
     formatting elements, then remove node from the
     stack of open elements and then go back to
     step 1.
     3. Otherwise, if node is the formatting element,
     then go to the next step in the overall
     algorithm.
     4. Otherwise, if last node is the furthest block,
     then move the aforementioned bookmark to be
     immediately after the node in the list of
     active formatting elements.
     5. If node has any children, perform a shallow
     clone of node, replace the entry for node in
     the list of active formatting elements with an
     entry for the clone, replace the entry for
     node in the stack of open elements with an
     entry for the clone, and let node be the
     clone.
     6. Insert last node into node, first removing it
     from its previous parent node if any.
     7. Let last node be node.
     8. Return to step 1 of this inner set of steps.
     8. Insert whatever last node ended up being in the
     previous step into the common ancestor node, first
     removing it from its previous parent node if any.
     9. Perform a shallow clone of the formatting element.
     10. Take all of the child nodes of the furthest block
     and append them to the clone created in the last
     step.
     11. Append that clone to the furthest block.
     12. Remove the formatting element from the list of
     active formatting elements, and insert the clone
     into the list of active formatting elements at the
     position of the aforementioned bookmark.
     13. Remove the formatting element from the stack of
     open elements, and insert the clone into the stack
     of open elements immediately after (i.e. in a more
     deeply nested position than) the position of the
     furthest block in that stack.
     14. Jump back to step 1 in this series of steps.

     The way these steps are defined, only elements in
     the formatting category ever get cloned by this
     algorithm.

     Because of the way this algorithm causes elements
     to change parents, it has been dubbed the "adoption
     agency algorithm" (in contrast with other possibly
     algorithms for dealing with misnested content,
     which included the "incest algorithm", the "secret
     affair algorithm", and the "Heisenberg algorithm").

     A start tag whose tag name is "button"
     If the stack of open elements has a button element
     in scope, then this is a parse error; act as if an
     end tag with the tag name "button" had been seen,
     then reprocess the token.

     Otherwise:

     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token.

     Insert a marker at the end of the list of active
     formatting elements.

     A start tag token whose tag name is one of: "marquee",
     "object"
     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token.

     Insert a marker at the end of the list of active
     formatting elements.

     An end tag token whose tag name is one of: "button",
     "marquee", "object"
     If the stack of open elements has in scope an
     element whose tag name is the same as the tag name
     of the token, then generate implied end tags.

     Now, if the current node is not an element with the
     same tag name as the token, then this is a parse
     error.

     Now, if the stack of open elements has an element
     in scope whose tag name matches the tag name of the
     token, then pop elements from the stack until that
     element has been popped from the stack, and clear
     the list of active formatting elements up to the
     last marker.

     A start tag whose tag name is "xmp"
     Reconstruct the active formatting elements, if any.

     Follow the generic CDATA parsing algorithm, with
     the current node as the context node.

     A start tag whose tag name is "table"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token.

     Change the insertion mode to "in table".

     A start tag whose tag name is one of: "area", "basefont",
     "bgsound", "br", "embed", "img", "param", "spacer",
     "wbr"
     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token. Immediately
     pop the current node off the stack of open
     elements.

     A start tag whose tag name is "hr"
     If the stack of open elements has a p element in
     scope, then act as if an end tag with the tag name
     p had been seen.

     Insert an HTML element for the token. Immediately
     pop the current node off the stack of open
     elements.

     A start tag whose tag name is "image"
     Parse error. Change the token's tag name to "img"
     and reprocess it. (Don't ask.)

     A start tag whose tag name is "input"
     Reconstruct the active formatting elements, if any.

     Insert an input element for the token.

     If the form element pointer is not null, then
     associate the input element with the form element
     pointed to by the form element pointer.

     Pop that input element off the stack of open
     elements.

     A start tag whose tag name is "isindex"
     Parse error.

     If the form element pointer is not null, then
     ignore the token.

     Otherwise:

     Act as if a start tag token with the tag name
     "form" had been seen.

     If the token has an attribute called "action", set
     the action attribute on the resulting form element
     to the value of the "action" attribute of the
     token.

     Act as if a start tag token with the tag name "hr"
     had been seen.

     Act as if a start tag token with the tag name "p"
     had been seen.

     Act as if a start tag token with the tag name
     "label" had been seen.

     Act as if a stream of character tokens had been
     seen (see below for what they should say).

     Act as if a start tag token with the tag name
     "input" had been seen, with all the attributes from
     the "isindex" token except "name", "action", and
     "prompt". Set the name attribute of the resulting
     input element to the value "isindex".

     Act as if a stream of character tokens had been
     seen (see below for what they should say).

     Act as if an end tag token with the tag name
     "label" had been seen.

     Act as if an end tag token with the tag name "p"
     had been seen.

     Act as if a start tag token with the tag name "hr"
     had been seen.

     Act as if an end tag token with the tag name "form"
     had been seen.

     If the token has an attribute with the name
     "prompt", then the first stream of characters must
     be the same string as given in that attribute, and
     the second stream of characters must be empty.
     Otherwise, the two streams of character tokens
     together should, together with the input element,
     express the equivalent of "This is a searchable
     index. Insert your search keywords here: (input
     field)" in the user's preferred language.

     Then need to specify that if the form submission
     causes just a single form control, whose name is
     "isindex", to be submitted, then we submit just the
     value part, not the "isindex=" part.

     A start tag whose tag name is "textarea"
     Create an element for the token.

     If the form element pointer is not null, then
     associate the textarea element with the form
     element pointed to by the form element pointer.

     Append the new element to the current node.

     Switch the tokeniser's content model flag to the
     RCDATA state.

     If the next token is a U+000A LINE FEED (LF)
     character token, then ignore that token and move on
     to the next one. (Newlines at the start of textarea
     elements are ignored as an authoring convenience.)

     Then, collect all the character tokens that the
     tokeniser returns until it returns a token that is
     not a character token, or until it stops
     tokenising.

     If this process resulted in a collection of
     character tokens, append a single Text node, whose
     contents is the concatenation of all those tokens'
     characters, to the new element node.

     The tokeniser's content model flag will have
     switched back to the PCDATA state.

     If the next token is an end tag token with the tag
     name "textarea", ignore it. Otherwise, this is a
     parse error.

     A start tag whose tag name is one of: "iframe", "noembed",
     "noframes"

     A start tag whose tag name is "noscript", if scripting is
     enabled:
     Follow the generic CDATA parsing algorithm, with
     the current node as the context node.

     A start tag whose tag name is "select"
     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token.

     Change the insertion mode to "in select".

     An end tag whose tag name is "br"
     Parse error. Act as if a start tag token with the
     tag name "br" had been seen. Ignore the end tag
     token.

     A start or end tag whose tag name is one of: "caption",
     "col", "colgroup", "frame", "frameset", "head",
     "option", "optgroup", "tbody", "td", "tfoot", "th",
     "thead", "tr"

     An end tag whose tag name is one of: "area", "basefont",
     "bgsound", "br", "embed", "hr", "iframe", "image",
     "img", "input", "isindex", "noembed", "noframes",
     "param", "select", "spacer", "table", "textarea",
     "wbr"

     An end tag whose tag name is "noscript", if scripting is
     enabled:
     Parse error. Ignore the token.

     A start or end tag whose tag name is one of:
     "event-source", "section", "nav", "article",
     "aside", "header", "footer", "datagrid", "command"
     Work in progress!

     A start tag token not covered by the previous entries
     Reconstruct the active formatting elements, if any.

     Insert an HTML element for the token.

     This element will be a phrasing element.

     An end tag token not covered by the previous entries
     Run the following algorithm:

     1. Initialise node to be the current node (the
     bottommost node of the stack).
     2. If node has the same tag name as the end tag token,
     then:
     1. Generate implied end tags.
     2. If the tag name of the end tag token does not
     match the tag name of the current node, this
     is a parse error.
     3. Pop all the nodes from the current node up to
     node, including node, then stop this
     algorithm.
     3. Otherwise, if node is in neither the formatting
     category nor the phrasing category, then this is a
     parse error. Stop this algorithm. The end tag token
     is ignored.
     4. Set node to the previous entry in the stack of open
     elements.
     5. Return to step 2.

     If the insertion mode is "in table"

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "caption"
     Clear the stack back to a table context. (See
     below.)

     Insert a marker at the end of the list of active
     formatting elements.

     Insert an HTML element for the token, then switch
     the insertion mode to "in caption".

     A start tag whose tag name is "colgroup"
     Clear the stack back to a table context. (See
     below.)

     Insert an HTML element for the token, then switch
     the insertion mode to "in column group".

     A start tag whose tag name is "col"
     Act as if a start tag token with the tag name
     "colgroup" had been seen, then reprocess the
     current token.

     A start tag whose tag name is one of: "tbody", "tfoot",
     "thead"
     Clear the stack back to a table context. (See
     below.)

     Insert an HTML element for the token, then switch
     the insertion mode to "in table body".

     A start tag whose tag name is one of: "td", "th", "tr"
     Act as if a start tag token with the tag name
     "tbody" had been seen, then reprocess the current
     token.

     A start tag whose tag name is "table"
     Parse error. Act as if an end tag token with the
     tag name "table" had been seen, then, if that token
     wasn't ignored, reprocess the current token.

     The fake end tag token here can only be ignored in
     the fragment case.

     An end tag whose tag name is "table"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.
     (fragment case)

     Otherwise:

     Generate implied end tags.

     Now, if the current node is not a table element,
     then this is a parse error.

     Pop elements from this stack until a table element
     has been popped from the stack.

     Reset the insertion mode appropriately.

     An end tag whose tag name is one of: "body", "caption",
     "col", "colgroup", "html", "tbody", "td", "tfoot",
     "th", "thead", "tr"
     Parse error. Ignore the token.

     Anything else
     Parse error. Process the token as if the insertion
     mode was "in body", with the following exception:

     If the current node is a table, tbody, tfoot,
     thead, or tr element, then, whenever a node would
     be inserted into the current node, it must instead
     be inserted into the foster parent element.

     The foster parent element is the parent element of
     the last table element in the stack of open
     elements, if there is a table element and it has
     such a parent element. If there is no table element
     in the stack of open elements (fragment case), then
     the foster parent element is the first element in
     the stack of open elements (the html element).
     Otherwise, if there is a table element in the stack
     of open elements, but the last table element in the
     stack of open elements has no parent, or its parent
     node is not an element, then the foster parent
     element is the element before the last table
     element in the stack of open elements.

     If the foster parent element is the parent element
     of the last table element in the stack of open
     elements, then the new node must be inserted
     immediately before the last table element in the
     stack of open elements in the foster parent
     element; otherwise, the new node must be appended
     to the foster parent element.

     When the steps above require the UA to clear the stack
     back to a table context, it means that the UA must, while
     the current node is not a table element or an html
     element, pop elements from the stack of open elements. If
     this causes any elements to be popped from the stack,
     then this is a parse error.

     The current node being an html element after this process
     is a fragment case.

     If the insertion mode is "in caption"

     An end tag whose tag name is "caption"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.
     (fragment case)

     Otherwise:

     Generate implied end tags.

     Now, if the current node is not a caption element,
     then this is a parse error.

     Pop elements from this stack until a caption
     element has been popped from the stack.

     Clear the list of active formatting elements up to
     the last marker.

     Switch the insertion mode to "in table".

     A start tag whose tag name is one of: "caption", "col",
     "colgroup", "tbody", "td", "tfoot", "th", "thead",
     "tr"

     An end tag whose tag name is "table"
     Parse error. Act as if an end tag with the tag name
     "caption" had been seen, then, if that token wasn't
     ignored, reprocess the current token.

     The fake end tag token here can only be ignored in
     the fragment case.

     An end tag whose tag name is one of: "body", "col",
     "colgroup", "html", "tbody", "td", "tfoot", "th",
     "thead", "tr"
     Parse error. Ignore the token.

     Anything else
     Process the token as if the insertion mode was "in
     body".

     If the insertion mode is "in column group"

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "col"
     Insert a col element for the token. Immediately pop
     the current node off the stack of open elements.

     An end tag whose tag name is "colgroup"
     If the current node is the root html element, then
     this is a parse error, ignore the token. (fragment
     case)

     Otherwise, pop the current node (which will be a
     colgroup element) from the stack of open elements.
     Switch the insertion mode to "in table".

     An end tag whose tag name is "col"
     Parse error. Ignore the token.

     Anything else
     Act as if an end tag with the tag name "colgroup"
     had been seen, and then, if that token wasn't
     ignored, reprocess the current token.

     The fake end tag token here can only be ignored in
     the fragment case.

     If the insertion mode is "in table body"

     A start tag whose tag name is "tr"
     Clear the stack back to a table body context. (See
     below.)

     Insert a tr element for the token, then switch the
     insertion mode to "in row".

     A start tag whose tag name is one of: "th", "td"
     Parse error. Act as if a start tag with the tag
     name "tr" had been seen, then reprocess the current
     token.

     An end tag whose tag name is one of: "tbody", "tfoot",
     "thead"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.

     Otherwise:

     Clear the stack back to a table body context. (See
     below.)

     Pop the current node from the stack of open
     elements. Switch the insertion mode to "in table".

     A start tag whose tag name is one of: "caption", "col",
     "colgroup", "tbody", "tfoot", "thead"

     An end tag whose tag name is "table"
     If the stack of open elements does not have a
     tbody, thead, or tfoot element in table scope, this
     is a parse error. Ignore the token. (fragment case)

     Otherwise:

     Clear the stack back to a table body context. (See
     below.)

     Act as if an end tag with the same tag name as the
     current node ("tbody", "tfoot", or "thead") had
     been seen, then reprocess the current token.

     An end tag whose tag name is one of: "body", "caption",
     "col", "colgroup", "html", "td", "th", "tr"
     Parse error. Ignore the token.

     Anything else
     Process the token as if the insertion mode was "in
     table".

     When the steps above require the UA to clear the stack
     back to a table body context, it means that the UA must,
     while the current node is not a tbody, tfoot, thead, or
     html element, pop elements from the stack of open
     elements. If this causes any elements to be popped from
     the stack, then this is a parse error.

     The current node being an html element after this process
     is a fragment case.

     If the insertion mode is "in row"

     A start tag whose tag name is one of: "th", "td"
     Clear the stack back to a table row context. (See
     below.)

     Insert an HTML element for the token, then switch
     the insertion mode to "in cell".

     Insert a marker at the end of the list of active
     formatting elements.

     An end tag whose tag name is "tr"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.
     (fragment case)

     Otherwise:

     Clear the stack back to a table row context. (See
     below.)

     Pop the current node (which will be a tr element)
     from the stack of open elements. Switch the
     insertion mode to "in table body".

     A start tag whose tag name is one of: "caption", "col",
     "colgroup", "tbody", "tfoot", "thead", "tr"

     An end tag whose tag name is "table"
     Act as if an end tag with the tag name "tr" had
     been seen, then, if that token wasn't ignored,
     reprocess the current token.

     The fake end tag token here can only be ignored in
     the fragment case.

     An end tag whose tag name is one of: "tbody", "tfoot",
     "thead"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.

     Otherwise, act as if an end tag with the tag name
     "tr" had been seen, then reprocess the current
     token.

     An end tag whose tag name is one of: "body", "caption",
     "col", "colgroup", "html", "td", "th"
     Parse error. Ignore the token.

     Anything else
     Process the token as if the insertion mode was "in
     table".

     When the steps above require the UA to clear the stack
     back to a table row context, it means that the UA must,
     while the current node is not a tr element or an html
     element, pop elements from the stack of open elements. If
     this causes any elements to be popped from the stack,
     then this is a parse error.

     The current node being an html element after this process
     is a fragment case.

     If the insertion mode is "in cell"

     An end tag whose tag name is one of: "td", "th"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     that of the token, then this is a parse error and
     the token must be ignored.

     Otherwise:

     Generate implied end tags, except for elements with
     the same tag name as the token.

     Now, if the current node is not an element with the
     same tag name as the token, then this is a parse
     error.

     Pop elements from this stack until an element with
     the same tag name as the token has been popped from
     the stack.

     Clear the list of active formatting elements up to
     the last marker.

     Switch the insertion mode to "in row". (The current
     node will be a tr element at this point.)

     A start tag whose tag name is one of: "caption", "col",
     "colgroup", "tbody", "td", "tfoot", "th", "thead",
     "tr"
     If the stack of open elements does not have a td or
     th element in table scope, then this is a parse
     error; ignore the token. (fragment case)

     Otherwise, close the cell (see below) and reprocess
     the current token.

     An end tag whose tag name is one of: "body", "caption",
     "col", "colgroup", "html"
     Parse error. Ignore the token.

     An end tag whose tag name is one of: "table", "tbody",
     "tfoot", "thead", "tr"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     that of the token (which can only happen for
     "tbody", "tfoot" and "thead", or, in the fragment
     case), then this is a parse error and the token
     must be ignored.

     Otherwise, close the cell (see below) and reprocess
     the current token.

     Anything else
     Process the token as if the insertion mode was "in
     body".

     Where the steps above say to close the cell, they mean to
     follow the following algorithm:

     1. If the stack of open elements has a td element in table
     scope, then act as if an end tag token with the tag name
     "td" had been seen.
     2. Otherwise, the stack of open elements will have a th
     element in table scope; act as if an end tag token with
     the tag name "th" had been seen.

     The stack of open elements cannot have both a td and a th
     element in table scope at the same time, nor can it have
     neither when the insertion mode is "in cell".

     If the insertion mode is "in select"
     Handle the token as follows:

     A character token
     Append the token's character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "option"
     If the current node is an option element, act as if
     an end tag with the tag name "option" had been
     seen.

     Insert an HTML element for the token.

     A start tag whose tag name is "optgroup"
     If the current node is an option element, act as if
     an end tag with the tag name "option" had been
     seen.

     If the current node is an optgroup element, act as
     if an end tag with the tag name "optgroup" had been
     seen.

     Insert an HTML element for the token.

     An end tag whose tag name is "optgroup"
     First, if the current node is an option element,
     and the node immediately before it in the stack of
     open elements is an optgroup element, then act as
     if an end tag with the tag name "option" had been
     seen.

     If the current node is an optgroup element, then
     pop that node from the stack of open elements.
     Otherwise, this is a parse error, ignore the token.

     An end tag whose tag name is "option"
     If the current node is an option element, then pop
     that node from the stack of open elements.
     Otherwise, this is a parse error, ignore the token.

     An end tag whose tag name is "select"
     If the stack of open elements does not have an
     element in table scope with the same tag name as
     the token, this is a parse error. Ignore the token.
     (fragment case)

     Otherwise:

     Pop elements from the stack of open elements until
     a select element has been popped from the stack.

     Reset the insertion mode appropriately.

     A start tag whose tag name is "select"
     Parse error. Act as if the token had been an end
     tag with the tag name "select" instead.

     An end tag whose tag name is one of: "caption", "table",
     "tbody", "tfoot", "thead", "tr", "td", "th"
     Parse error.

     If the stack of open elements has an element in
     table scope with the same tag name as that of the
     token, then act as if an end tag with the tag name
     "select" had been seen, and reprocess the token.
     Otherwise, ignore the token.

     Anything else
     Parse error. Ignore the token.

     If the insertion mode is "after body"
     Handle the token as follows:

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Process the token as it would be processed if the
     insertion mode was "in body".

     A comment token
     Append a Comment node to the first element in the
     stack of open elements (the html element), with the
     data attribute set to the data given in the comment
     token.

     An end tag whose tag name is "html"
     If the parser was originally created as part of the
     HTML fragment parsing algorithm, this is a parse
     error; ignore the token. (The element will be an
     html element in this case.) (fragment case)

     Otherwise, switch to the trailing end phase.

     Anything else
     Parse error. Set the insertion mode to "in body"
     and reprocess the token.

     If the insertion mode is "in frameset"
     Handle the token as follows:

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     A start tag whose tag name is "frameset"
     Insert a frameset element for the token.

     An end tag whose tag name is "frameset"
     If the current node is the root html element, then
     this is a parse error; ignore the token. (fragment
     case)

     Otherwise, pop the current node from the stack of
     open elements.

     If the parser was not originally created as part of
     the HTML fragment parsing algorithm (fragment
     case), and the current node is no longer a frameset
     element, then change the insertion mode to "after
     frameset".

     A start tag whose tag name is "frame"
     Insert an HTML element for the token. Immediately
     pop the current node off the stack of open
     elements.

     A start tag whose tag name is "noframes"
     Process the token as if the insertion mode had been
     "in body".

     Anything else
     Parse error. Ignore the token.

     If the insertion mode is "after frameset"
     Handle the token as follows:

     A character token that is one of one of U+0009 CHARACTER
     TABULATION, U+000A LINE FEED (LF), U+000B LINE
     TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
     Append the character to the current node.

     A comment token
     Append a Comment node to the current node with the
     data attribute set to the data given in the comment
     token.

     An end tag whose tag name is "html"
     Switch to the trailing end phase.

     A start tag whose tag name is "noframes"
     Process the token as if the insertion mode had been
     "in body".

     Anything else
     Parse error. Ignore the token.

     This doesn't handle UAs that don't support frames, or that do support
     frames but want to show the NOFRAMES content. Supporting the former is
     easy; supporting the latter is harder.

     8.2.4.4. The trailing end phase

     After the main phase, as each token is emitted from the tokenisation
     stage, it must be processed as described in this section.

     A DOCTYPE token
     Parse error. Ignore the token.

     A comment token
     Append a Comment node to the Document object with the data
     attribute set to the data given in the comment token.

     A character token that is one of one of U+0009 CHARACTER TABULATION,
     U+000A LINE FEED (LF), U+000B LINE TABULATION, U+000C FORM FEED
     (FF), or U+0020 SPACE
     Process the token as it would be processed in the main phase.

     A character token that is not one of U+0009 CHARACTER TABULATION,
     U+000A LINE FEED (LF), U+000B LINE TABULATION, U+000C FORM FEED
     (FF), or U+0020 SPACE

     A start tag token
     An end tag token
     Parse error. Switch back to the main phase and reprocess the
     token.

     An end-of-file token
     Stop parsing.

     8.2.5. The End

     Once the user agent stops parsing the document, the user agent must
     follow the steps in this section.

     First, the rules for when a script completes loading start applying
     (script execution is no longer managed by the parser).

     If any of the scripts in the list of scripts that will execute as soon
     as possible have completed loading, or if the list of scripts that
     will execute asynchronously is not empty and the first script in that
     list has completed loading, then the user agent must act as if those
     scripts just completed loading, following the rules given for that in
     the script element definition.

     Then, if the list of scripts that will execute when the document has
     finished parsing is not empty, and the first item in this list has
     already completed loading, then the user agent must act as if that
     script just finished loading.

     By this point, there will be no scripts that have loaded but have not
     yet been executed.

     The user agent must then fire a simple event called DOMContentLoaded
     at the Document.

     Once everything that delays the load event has completed, the user
     agent must fire a load event at the body element.

     */
}
