/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2007-2008 Mozilla Foundation
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

package nu.validator.htmlparser.gwt;

import nu.validator.htmlparser.impl.Tokenizer;
import nu.validator.htmlparser.impl.UTF16Buffer;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class implements an HTML5 parser that exposes data through the DOM 
 * interface. 
 * 
 * <p>By default, when using the constructor without arguments, the 
 * this parser treats XML 1.0-incompatible infosets as fatal errors. 
 * This corresponds to 
 * <code>FATAL</code> as the general XML violation policy. To make the parser 
 * support non-conforming HTML fully per the HTML 5 spec while on the other 
 * hand potentially violating the DOM API contract, set the general XML 
 * violation policy to <code>ALLOW</code>. This does not work with a standard 
 * DOM implementation. Handling all input without fatal errors and without 
 * violating the DOM API contract is possible by setting 
 * the general XML violation policy to <code>ALTER_INFOSET</code>. <em>This 
 * makes the parser non-conforming</em> but is probably the most useful 
 * setting for most applications.
 * 
 * <p>The doctype is not represented in the tree.
 * 
 * <p>The document mode is represented as user data <code>DocumentMode</code> 
 * object with the key <code>nu.validator.document-mode</code> on the document 
 * node. 
 * 
 * <p>The form pointer is also stored as user data with the key 
 * <code>nu.validator.form-pointer</code>.
 * 
 * @version $Id: HtmlDocumentBuilder.java 255 2008-05-29 08:57:38Z hsivonen $
 * @author hsivonen
 */
public class HtmlParser {

    private final Tokenizer tokenizer;

    private final BrowserTreeBuilder domTreeBuilder;

    /**
     * Instantiates the parser
     * 
     * @param implementation
     *            the DOM implementation
     *            @param xmlPolicy the policy
     */
    public HtmlParser(JavaScriptObject document) {
        this.domTreeBuilder = new BrowserTreeBuilder(document);
        this.tokenizer = new Tokenizer(domTreeBuilder);
    }

    /**
     * Parses a document from a SAX <code>InputSource</code>.
     * @param is the source
     * @return the doc
     * @see javax.xml.parsers.DocumentBuilder#parse(org.xml.sax.InputSource)
     */
    public JavaScriptObject parse(String source) throws SAXException {
        domTreeBuilder.setFragmentContext(null);
        tokenize(source, null);
        return domTreeBuilder.getDocument();
    }

    /**
     * Parses a document fragment from a SAX <code>InputSource</code>.
     * @param is the source
     * @param context the context element name
     * @return the doc
     * @throws IOException
     * @throws SAXException
     */
    public JavaScriptObject parseFragment(String source, String context)
            throws SAXException {
        domTreeBuilder.setFragmentContext(context);
        tokenize(source, context);
        return domTreeBuilder.getDocumentFragment();
    }

    /**
     * @param is
     * @throws SAXException
     * @throws IOException
     * @throws MalformedURLException
     */
    private void tokenize(String source, String context) throws SAXException {
        boolean lastWasCR = false;
        UTF16Buffer buffer = new UTF16Buffer(source.toCharArray(), 0, source.length());
        domTreeBuilder.setFragmentContext(context);
        try {
            tokenizer.start();
            while (buffer.hasMore()) {
                buffer.adjust(lastWasCR);
                lastWasCR = false;
                if (buffer.hasMore()) {
                    lastWasCR = tokenizer.tokenizeBuffer(buffer);                    
                }
            }
            tokenizer.eof();
        } finally {
            tokenizer.end();
        }
    }

    /**
     * @see javax.xml.parsers.DocumentBuilder#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        domTreeBuilder.setErrorHandler(errorHandler);
        tokenizer.setErrorHandler(errorHandler);
    }

    /**
     * Sets whether comment nodes appear in the tree.
     * @param ignoreComments <code>true</code> to ignore comments
     * @see nu.validator.htmlparser.impl.TreeBuilder#setIgnoringComments(boolean)
     */
    public void setIgnoringComments(boolean ignoreComments) {
        domTreeBuilder.setIgnoringComments(ignoreComments);
    }

    /**
     * Sets whether the parser considers scripting to be enabled for noscript treatment.
     * @param scriptingEnabled <code>true</code> to enable
     * @see nu.validator.htmlparser.impl.TreeBuilder#setScriptingEnabled(boolean)
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        domTreeBuilder.setScriptingEnabled(scriptingEnabled);
    }
    
}
