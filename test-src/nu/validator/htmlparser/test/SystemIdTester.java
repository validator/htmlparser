/*
 * Copyright Mozilla Foundation
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

package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

/**
 * Regression test for
 * <a href="https://github.com/validator/htmlparser/issues/123">issue&nbsp;#123</a>:
 * the document system identifier must be available to a caller-supplied
 * {@link org.xml.sax.ContentHandler} by the time {@code startDocument()} fires.
 *
 * <p>Both SAX configurations are exercised, because they surface the system
 * identifier by different mechanisms: the default buffered path snapshots the
 * locator when the root node is created, whereas the streaming path hands the
 * live tokenizer to the handler directly.
 */
public class SystemIdTester {

    private static final String SYSTEM_ID = "http://example.org/doc.html";

    private static final String DOCUMENT = "<!DOCTYPE html><html><head><title>Test</title></head><body><p>Hello</p></body></html>";

    /**
     * Records the system identifier reported by the {@link Locator} at
     * {@code startDocument()} time, i.e. the earliest point at which a
     * content handler can reasonably read it.
     */
    private static final class SystemIdRecorder extends DefaultHandler {

        private Locator locator;

        private String systemIdAtStartDocument = "<startDocument() not called>";

        @Override public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override public void startDocument() throws SAXException {
            systemIdAtStartDocument = (locator == null) ? null
                    : locator.getSystemId();
        }
    }

    private static void check(XmlViolationPolicy streamabilityViolationPolicy,
            String label) throws SAXException, IOException {
        HtmlParser parser = new HtmlParser();
        parser.setStreamabilityViolationPolicy(streamabilityViolationPolicy);
        SystemIdRecorder recorder = new SystemIdRecorder();
        parser.setContentHandler(recorder);

        InputSource in = new InputSource(new StringReader(DOCUMENT));
        in.setSystemId(SYSTEM_ID);
        parser.parse(in);

        if (!SYSTEM_ID.equals(recorder.systemIdAtStartDocument)) {
            throw new AssertionError(label
                    + ": expected system identifier \"" + SYSTEM_ID
                    + "\" at startDocument() but got \""
                    + recorder.systemIdAtStartDocument + "\"");
        }
        System.out.println(label + ": OK");
    }

    public static void main(String[] args) throws SAXException, IOException {
        // Default configuration: the SAXTreeBuilder path, which replays a
        // buffered tree to the content handler.
        check(XmlViolationPolicy.ALLOW, "buffered (SAXTreeBuilder)");
        // Any non-ALLOW policy selects the direct SAXStreamer path.
        check(XmlViolationPolicy.ALTER_INFOSET, "streaming (SAXStreamer)");
        System.out.println("issue #123 regression test passed");
    }
}
