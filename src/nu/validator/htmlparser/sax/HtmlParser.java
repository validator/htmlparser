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

package nu.validator.htmlparser.sax;

import java.io.IOException;

import nu.validator.htmlparser.ContentModelFlag;
import nu.validator.htmlparser.DoctypeExpectation;
import nu.validator.htmlparser.DocumentModeHandler;
import nu.validator.htmlparser.Tokenizer;
import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;
import nu.validator.saxtree.Document;
import nu.validator.saxtree.DocumentFragment;
import nu.validator.saxtree.TreeParser;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlParser implements XMLReader {

    private Tokenizer tokenizer = null;

    private TreeBuilder<?> treeBuilder = null;

    private ContentHandler contentHandler = null;

    private LexicalHandler lexicalHandler = null;

    private DTDHandler dtdHandler = null;

    private EntityResolver entityResolver = null;

    private ErrorHandler errorHandler = null;

    private DocumentModeHandler documentModeHandler = null;

    private DoctypeExpectation doctypeExpectation = DoctypeExpectation.HTML;
    
    private boolean checkingNormalization = false;
    
    private boolean scriptingEnabled = false;

    private XmlViolationPolicy contentSpacePolicy = XmlViolationPolicy.ALLOW;

    private XmlViolationPolicy contentNonXmlCharPolicy = XmlViolationPolicy.ALLOW;

    private XmlViolationPolicy commentPolicy = XmlViolationPolicy.ALLOW;

    private XmlViolationPolicy streamabilityViolationPolicy = XmlViolationPolicy.ALLOW;

    public HtmlParser() {
    }

    private void lazyInit() {
        if (tokenizer == null) {
            if (streamabilityViolationPolicy == XmlViolationPolicy.ALLOW) {
                this.treeBuilder = new SAXTreeBuilder();
            } else {
                this.treeBuilder = new SAXStreamer();
            }
            this.tokenizer = new Tokenizer(treeBuilder);
            this.tokenizer.setErrorHandler(errorHandler);
            this.treeBuilder.setErrorHandler(errorHandler);
            this.tokenizer.setCheckingNormalization(checkingNormalization);
            this.tokenizer.setCommentPolicy(commentPolicy);
            this.tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
            this.tokenizer.setContentSpacePolicy(contentSpacePolicy);
            this.treeBuilder.setDoctypeExpectation(doctypeExpectation);
            this.treeBuilder.setDocumentModeHandler(documentModeHandler);
            this.treeBuilder.setIgnoringComments(lexicalHandler == null);
            this.treeBuilder.setScriptingEnabled(scriptingEnabled);
            if (treeBuilder instanceof SAXStreamer) {
                SAXStreamer streamer = (SAXStreamer) treeBuilder;
                streamer.setContentHandler(contentHandler);
                streamer.setLexicalHandler(lexicalHandler);
            }
        }
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getProperty(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    public void parse(InputSource input) throws IOException, SAXException {
        lazyInit();
        try {
            treeBuilder.setFragmentContext(null);
            tokenizer.tokenize(input);
        } finally {
            if (treeBuilder instanceof SAXTreeBuilder) {
                Document document = ((SAXTreeBuilder) treeBuilder).getDocument();
                new TreeParser(contentHandler, lexicalHandler).parse(document);
            }
        }
    }

    public void parseFragment(InputSource input, String context)
            throws IOException, SAXException {
        lazyInit();
        try {
            treeBuilder.setFragmentContext(context);
            tokenizer.tokenize(input);
        } finally {
            if (treeBuilder instanceof SAXTreeBuilder) {
                DocumentFragment fragment = ((SAXTreeBuilder) treeBuilder).getDocumentFragment();
                new TreeParser(contentHandler, lexicalHandler).parse(fragment);
            }
        }
    }

    public void parse(String systemId) throws IOException, SAXException {
        InputSource is;
        if (entityResolver == null) {
            is = new DefaultHandler().resolveEntity(null, systemId);
        } else {
            is = entityResolver.resolveEntity(null, systemId);
        }
        parse(is);
    }

    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
        if (treeBuilder != null) {
            if (treeBuilder instanceof SAXStreamer) {
                ((SAXStreamer)treeBuilder).setContentHandler(handler);
            }
        }
    }

    public void setLexicalHandler(LexicalHandler handler) {
        lexicalHandler = handler;
        if (treeBuilder != null) {
            treeBuilder.setIgnoringComments(handler == null);
            if (treeBuilder instanceof SAXStreamer) {
                ((SAXStreamer)treeBuilder).setLexicalHandler(handler);
            }
        }
    }

    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    public void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
        if (tokenizer != null) {
            tokenizer.setErrorHandler(handler);
            treeBuilder.setErrorHandler(handler);
        }
    }

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            setLexicalHandler((LexicalHandler) value);
        } else {
            throw new SAXNotRecognizedException();
        }
    }

    /**
     * @return
     * @see nu.validator.htmlparser.Tokenizer#isCheckingNormalization()
     */
    public boolean isCheckingNormalization() {
        return tokenizer.isCheckingNormalization();
    }

    /**
     * @param enable
     * @see nu.validator.htmlparser.Tokenizer#setCheckingNormalization(boolean)
     */
    public void setCheckingNormalization(boolean enable) {
        tokenizer.setCheckingNormalization(enable);
    }

    /**
     * @param commentPolicy
     * @see nu.validator.htmlparser.Tokenizer#setCommentPolicy(nu.validator.htmlparser.XmlViolationPolicy)
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        tokenizer.setCommentPolicy(commentPolicy);
    }

    /**
     * @param contentModelFlag
     * @param contentModelElement
     * @see nu.validator.htmlparser.Tokenizer#setContentModelFlag(nu.validator.htmlparser.ContentModelFlag, java.lang.String)
     */
    public void setContentModelFlag(ContentModelFlag contentModelFlag,
            String contentModelElement) {
        tokenizer.setContentModelFlag(contentModelFlag, contentModelElement);
    }

    /**
     * @param contentNonXmlCharPolicy
     * @see nu.validator.htmlparser.Tokenizer#setContentNonXmlCharPolicy(nu.validator.htmlparser.XmlViolationPolicy)
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
    }

    /**
     * @param contentSpacePolicy
     * @see nu.validator.htmlparser.Tokenizer#setContentSpacePolicy(nu.validator.htmlparser.XmlViolationPolicy)
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        tokenizer.setContentSpacePolicy(contentSpacePolicy);
    }

    /**
     * @return
     * @see nu.validator.htmlparser.TreeBuilder#isScriptingEnabled()
     */
    public boolean isScriptingEnabled() {
        return scriptingEnabled;
    }

    /**
     * @param scriptingEnabled
     * @see nu.validator.htmlparser.TreeBuilder#setScriptingEnabled(boolean)
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        this.scriptingEnabled = scriptingEnabled;
        if (treeBuilder != null) {
            treeBuilder.setScriptingEnabled(scriptingEnabled);
        }
    }

    /**
     * Returns the doctypeExpectation.
     * 
     * @return the doctypeExpectation
     */
    public DoctypeExpectation getDoctypeExpectation() {
        return doctypeExpectation;
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
     * Returns the documentModeHandler.
     * 
     * @return the documentModeHandler
     */
    public DocumentModeHandler getDocumentModeHandler() {
        return documentModeHandler;
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
     * Returns the streamabilityViolationPolicy.
     * 
     * @return the streamabilityViolationPolicy
     */
    public XmlViolationPolicy getStreamabilityViolationPolicy() {
        return streamabilityViolationPolicy;
    }

    /**
     * Sets the streamabilityViolationPolicy.
     * 
     * @param streamabilityViolationPolicy the streamabilityViolationPolicy to set
     */
    public void setStreamabilityViolationPolicy(
            XmlViolationPolicy streamabilityViolationPolicy) {
        this.streamabilityViolationPolicy = streamabilityViolationPolicy;
    }

}
