/*
 * Copyright (c) 2007 Henri Sivonen
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

package nu.validator.htmlparser.dom;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nu.validator.htmlparser.Tokenizer;
import nu.validator.htmlparser.XmlViolationPolicy;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlDocumentBuilder extends DocumentBuilder {

    /**
     * @return
     */
    private static DOMImplementation jaxpDOMImplementation() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return builder.getDOMImplementation();
    }

    private final Tokenizer tokenizer;

    private final DOMTreeBuilder domTreeBuilder;

    private final DOMImplementation implementation;

    /**
     * @param implementation
     */
    public HtmlDocumentBuilder(DOMImplementation implementation) {
        this.implementation = implementation;
        this.domTreeBuilder = new DOMTreeBuilder(implementation);
        this.tokenizer = new Tokenizer(domTreeBuilder);
        this.tokenizer.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
    }

    public HtmlDocumentBuilder() {
        this(jaxpDOMImplementation());
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return implementation;
    }

    @Override
    public boolean isNamespaceAware() {
        return true;
    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public Document newDocument() {
        return implementation.createDocument(null, null, null);
    }

    @Override
    public Document parse(InputSource is) throws SAXException, IOException {
        if (is.getByteStream() == null || is.getCharacterStream() == null) {
            String systemId = is.getSystemId();
            is = new InputSource();
            is.setSystemId(systemId);
            is.setByteStream(new URL(systemId).openStream());
        }
        tokenizer.tokenize(is);
        return domTreeBuilder.getDocument();
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        domTreeBuilder.setErrorHandler(errorHandler);
        tokenizer.setErrorHandler(errorHandler);
    }

}
