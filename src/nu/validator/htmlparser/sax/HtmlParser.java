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
import nu.validator.htmlparser.Tokenizer;
import nu.validator.htmlparser.XmlViolationPolicy;
import nu.validator.saxtree.Document;
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

public class HtmlParser implements XMLReader {

    private Tokenizer tokenizer;
    
    private SaxTreeBuilder treeBuilder;
    
    private ContentHandler contentHandler;
    
    private LexicalHandler lexicalHandler;
    
    public HtmlParser() {
        this.treeBuilder = new SaxTreeBuilder();
        this.tokenizer = new Tokenizer(treeBuilder);
    }
    
    public ContentHandler getContentHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public DTDHandler getDTDHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public EntityResolver getEntityResolver() {
        // TODO Auto-generated method stub
        return null;
    }

    public ErrorHandler getErrorHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    public void parse(InputSource input) throws IOException, SAXException {
        try {
            tokenizer.tokenize(input);
        } finally {
            Document document = treeBuilder.getDocument();
            new TreeParser(contentHandler, lexicalHandler).parse(document);
        }
    }

    public void parse(String systemId) throws IOException, SAXException {
        // TODO Auto-generated method stub
        
    }

    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
    }

    public void setLexicalHandler(LexicalHandler handler) {
        treeBuilder.setIgnoringComments(handler == null);
        lexicalHandler = handler;
    }
    
    public void setDTDHandler(DTDHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void setEntityResolver(EntityResolver resolver) {
        // TODO Auto-generated method stub
        
    }

    public void setErrorHandler(ErrorHandler handler) {
        tokenizer.setErrorHandler(handler);
        treeBuilder.setErrorHandler(handler);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
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
    public void setContentModelFlag(ContentModelFlag contentModelFlag, String contentModelElement) {
        tokenizer.setContentModelFlag(contentModelFlag, contentModelElement);
    }

    /**
     * @param contentNonXmlCharPolicy
     * @see nu.validator.htmlparser.Tokenizer#setContentNonXmlCharPolicy(nu.validator.htmlparser.XmlViolationPolicy)
     */
    public void setContentNonXmlCharPolicy(XmlViolationPolicy contentNonXmlCharPolicy) {
        tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
    }

    /**
     * @param contentSpacePolicy
     * @see nu.validator.htmlparser.Tokenizer#setContentSpacePolicy(nu.validator.htmlparser.XmlViolationPolicy)
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        tokenizer.setContentSpacePolicy(contentSpacePolicy);
    }

}
