/*
 * Copyright (c) 2020 Anthony Vanelverdinghe
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

/**
 * Provides an implementation of the HTML5 parsing algorithm in Java for applications.
 * The parser is designed to work as a drop-in replacement for the XML parser in applications
 * that already support XHTML 1.x content with an XML parser and use XOM to interface with the parser.
 */
@SuppressWarnings({"requires-automatic", "requires-transitive-automatic"})
module nu.validator.htmlparser.xom {
    requires transitive java.xml;
    requires transitive nu.xom;
    requires transitive nu.validator.htmlparser;

    exports nu.validator.htmlparser.xom;
}
