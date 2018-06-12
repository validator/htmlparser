/*
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

package nu.validator.htmlparser.lang;

/**
 * Indicates a request for character encoding sniffer choice.
 *
 * @version $Id$
 * @author hsivonen
 */
public enum MessageEnum {
    UNCLOSED_ELEMENT("error.unclosed_element"),
    EOF_EXPECTED("error.eof_expected"),
    OPENED_ELEMENTS_UNCLOSED("error.opened_elements_unclosed"),
    IMPLIED("error.implied"),
    CELL_IMPLICITLY_CLOSE("error.cell_implicitly_close"),
    FOO_BETWEEN_HEAD_AND_BODY("error.fooBetweenHeadAndBody"),
    START_TAG_WITH_SELECT_OPEN("error.startTagWithSelectOpen"),
    BAD_START_TAG_IN_HEAD("error.badStartTagInHead"),
    FOO_SEEN_WHEN_FOO_OPEN("error.fooSeenWhenFooOpen"),
    START_TAG_IN_TABLE("error.startTagInTable"),
    START_TAG_IN_TABLE_BODY("error.startTagInTableBody"),
    END_TAG_SEEN_WITH_SELECT_OPEN("error.endTagSeenWithSelectOpen"),
    NO_ELEMENT_TO_CLOSE_BUT_END_TAG_SEEN("error.noElementToCloseButEndTagSeen"),
    HTML_START_TAG_IN_FOREIGN_CONTEXT("error.htmlStartTagInForeignContext"),
    UNCLOSED_CHILDREN_IN_RUBY("error.unclosedChildrenInRuby"),
    START_TAG_SEEN_WITHOUT_RUBY("error.startTagSeenWithoutRuby"),
    SELF_CLOSING("error.selfClosing"),
    UNCLOSED_ELEMENTS_ON_STACK("error.unclosedElementsOnStack"),
    END_TAG_NOT_MATCH_CURRENT_OPEN_ELEMENT("error.endTagDidNotMatchCurrentOpenElement"),
    END_TAG_VIOLATES_NESTING_RULES("error.endTagViolatesNestingRules"),
    EOF_WITH_UNCLOSED_ELEMENTS("error.eofWithUnclosedElements"),
    END_WITH_UNCLOSED_ELEMENTS("error.endWithUnclosedElements"),
    NON_SPACE_CHARS_WITHOUT_DOC_FIRST("error.nonSpaceCharsWithoutDoctypeFirst");
    private String prop;
    private MessageEnum(String prop) {
        this.prop = prop;
    }
    public String getProp() {
        return prop;
    }
}
