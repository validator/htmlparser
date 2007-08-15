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
import java.net.MalformedURLException;
import java.net.URL;

import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.common.DocumentModeHandler;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.impl.Tokenizer;
import nu.validator.htmlparser.impl.TreeBuilder;
import nu.validator.saxtree.Document;
import nu.validator.saxtree.DocumentFragment;
import nu.validator.saxtree.TreeParser;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class implements an HTML5 parser that exposes data through the SAX2 
 * interface. 
 * 
 * <p>By default, when using the constructor without arguments, the 
 * this parser treats XML 1.0-incompatible infosets as fatal errors in 
 * order to adhere to the SAX2 API contract strictly. This corresponds to 
 * <code>FATAL</code> as the general XML violation policy. To make the parser 
 * support non-conforming HTML fully per the HTML 5 spec while on the other 
 * hand potentially violating the SAX2 API contract, set the general XML 
 * violation policy to <code>ALLOW</code>. Handling all input without fatal 
 * errors and without violating the SAX2 API contract is possible by setting 
 * the general XML violation policy to <code>ALTER_INFOSET</code>. <em>This 
 * makes the parser non-conforming</em> but is probably the most useful 
 * setting for most applications.
 * 
 * <p>By default, this parser doesn't do true streaming but buffers everything 
 * first. The parser can be made truly streaming by calling 
 * <code>setStreamabilityViolationPolicy(XmlViolationPolicy.FATAL)</code>. This 
 * has the consequence that errors that require non-streamable recovery are 
 * treated as fatal.
 * 
 * <p>By default, in order to make the parse events emulate the parse events 
 * for a DTDless XML document, the parser does not report the doctype through 
 * <code>LexicalHandler</code>. Doctype reporting through 
 * <code>LexicalHandler</code> can be turned on by calling 
 * <code>setReportingDoctype(true)</code>.
 * 
 * @version $Id$
 * @author hsivonen
 */
public class HtmlParser implements XMLReader {

    private Tokenizer tokenizer = null;

    private TreeBuilder<?> treeBuilder = null;

    private SAXStreamer saxStreamer = null; // work around javac bug

    private SAXTreeBuilder saxTreeBuilder = null; // work around javac bug

    private ContentHandler contentHandler = null;

    private LexicalHandler lexicalHandler = null;

    private DTDHandler dtdHandler = null;

    private EntityResolver entityResolver = null;

    private ErrorHandler errorHandler = null;

    private DocumentModeHandler documentModeHandler = null;

    private DoctypeExpectation doctypeExpectation = DoctypeExpectation.HTML;

    private boolean checkingNormalization = false;

    private boolean scriptingEnabled = false;

    private XmlViolationPolicy contentSpacePolicy = XmlViolationPolicy.FATAL;

    private XmlViolationPolicy contentNonXmlCharPolicy = XmlViolationPolicy.FATAL;

    private XmlViolationPolicy commentPolicy = XmlViolationPolicy.FATAL;

    private XmlViolationPolicy namePolicy = XmlViolationPolicy.FATAL;

    private XmlViolationPolicy streamabilityViolationPolicy = XmlViolationPolicy.ALLOW;
    
    private boolean html4ModeCompatibleWithXhtml1Schemata;

    private boolean mappingLangToXmlLang;

    private XmlViolationPolicy xmlnsPolicy;

    private boolean reportingDoctype = true;


    public HtmlParser() {
        this(XmlViolationPolicy.FATAL);
    }
    
    public HtmlParser(XmlViolationPolicy xmlPolicy) {
        setXmlPolicy(xmlPolicy);
    }    

    /**
     * This class wraps differnt tree builders depending on configuration. This 
     * method does the work of hiding this from the user of the class.
     */
    private void lazyInit() {
        if (tokenizer == null) {
            if (streamabilityViolationPolicy == XmlViolationPolicy.ALLOW) {
                this.saxTreeBuilder = new SAXTreeBuilder();
                this.treeBuilder = this.saxTreeBuilder;
                this.saxStreamer = null;
            } else {
                this.saxStreamer = new SAXStreamer();
                this.treeBuilder = this.saxStreamer;
                this.saxTreeBuilder = null;
            }
            this.tokenizer = new Tokenizer(treeBuilder);
            this.tokenizer.setErrorHandler(errorHandler);
            this.treeBuilder.setErrorHandler(errorHandler);
            this.tokenizer.setCheckingNormalization(checkingNormalization);
            this.tokenizer.setCommentPolicy(commentPolicy);
            this.tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
            this.tokenizer.setContentSpacePolicy(contentSpacePolicy);
            this.tokenizer.setHtml4ModeCompatibleWithXhtml1Schemata(html4ModeCompatibleWithXhtml1Schemata);
            this.tokenizer.setMappingLangToXmlLang(mappingLangToXmlLang);
            this.tokenizer.setXmlnsPolicy(xmlnsPolicy);
            this.treeBuilder.setDoctypeExpectation(doctypeExpectation);
            this.treeBuilder.setDocumentModeHandler(documentModeHandler);
            this.treeBuilder.setIgnoringComments(lexicalHandler == null);
            this.treeBuilder.setScriptingEnabled(scriptingEnabled);
            this.treeBuilder.setReportingDoctype(reportingDoctype);
            if (saxStreamer != null) {
                saxStreamer.setContentHandler(contentHandler == null ? new DefaultHandler()
                        : contentHandler);
                saxStreamer.setLexicalHandler(lexicalHandler);
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

    /**
     * Exposes the configuration of the emulated XML parser as well as
     * boolean-valued configuration without using non-<code>XMLReader</code>
     * getters directly.
     * 
     * <dl>
     * <dt><code>http://xml.org/sax/features/external-general-entities</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/external-parameter-entities</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/is-standalone</code></dt>
     * <dd><code>true</code></dd>
     * <dt><code>http://xml.org/sax/features/lexical-handler/parameter-entities</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/namespaces</code></dt>
     * <dd><code>true</code></dd>
     * <dt><code>http://xml.org/sax/features/namespace-prefixes</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/resolve-dtd-uris</code></dt>
     * <dd><code>true</code></dd>
     * <dt><code>http://xml.org/sax/features/string-interning</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/unicode-normalization-checking</code></dt>
     * <dd><code>isCheckingNormalization</code></dd>
     * <dt><code>http://xml.org/sax/features/use-attributes2</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/use-locator2</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/use-entity-resolver2</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/validation</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/xmlns-uris</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://xml.org/sax/features/xml-1.1</code></dt>
     * <dd><code>false</code></dd>
     * <dt><code>http://validator.nu/features/html4-mode-compatible-with-xhtml1-schemata</code></dt>
     * <dd><code>isHtml4ModeCompatibleWithXhtml1Schemata</code></dd>
     * <dt><code>http://validator.nu/features/mapping-lang-to-xml-lang</code></dt>
     * <dd><code>isMappingLangToXmlLang</code></dd>
     * <dt><code>http://validator.nu/features/scripting-enabled</code></dt>
     * <dd><code>isScriptingEnabled</code></dd>
     * </dl>
     * 
     * @param name
     *            feature URI string
     * @return a value per the list above
     * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
     */
    public boolean getFeature(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if ("http://xml.org/sax/features/external-general-entities".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/external-parameter-entities".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/is-standalone".equals(name)) {
            return true;
        } else if ("http://xml.org/sax/features/lexical-handler/parameter-entities".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/namespaces".equals(name)) {
            return true;
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/resolve-dtd-uris".equals(name)) {
            return true; // default value--applicable scenario never happens
        } else if ("http://xml.org/sax/features/string-interning".equals(name)) {
            return false; // XXX revisit
        } else if ("http://xml.org/sax/features/unicode-normalization-checking".equals(name)) {
            return isCheckingNormalization(); // the checks aren't really per
            // XML 1.1
        } else if ("http://xml.org/sax/features/use-attributes2".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/use-locator2".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/use-entity-resolver2".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/validation".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/xmlns-uris".equals(name)) {
            return false;
        } else if ("http://xml.org/sax/features/xml-1.1".equals(name)) {
            return false;
        } else if ("http://validator.nu/features/html4-mode-compatible-with-xhtml1-schemata".equals(name)) {
            return isHtml4ModeCompatibleWithXhtml1Schemata();
        } else if ("http://validator.nu/features/mapping-lang-to-xml-lang".equals(name)) {
            return isMappingLangToXmlLang();
        } else if ("http://validator.nu/features/scripting-enabled".equals(name)) {
            return isScriptingEnabled();
        } else {
            throw new SAXNotRecognizedException();
        }
    }

    /**
     * Allows <code>XMLReader</code>-level access to non-boolean valued
     * getters.
     * 
     * <p>
     * The properties are mapped as follows:
     * 
     * <dl>
     * <dt><code>http://xml.org/sax/properties/document-xml-version</code></dt>
     * <dd><code>"1.0"</code></dd>
     * <dt><code>http://xml.org/sax/properties/lexical-handler</code></dt>
     * <dd><code>getLexicalHandler</code></dd>
     * <dt><code>http://validator.nu/properties/content-space-policy</code></dt>
     * <dd><code>getContentSpacePolicy</code></dd>
     * <dt><code>http://validator.nu/properties/content-non-xml-char-policy</code></dt>
     * <dd><code>getContentNonXmlCharPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/comment-policy</code></dt>
     * <dd><code>getCommentPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/xmlns-policy</code></dt>
     * <dd><code>getXmlnsPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/name-policy</code></dt>
     * <dd><code>getNamePolicy</code></dd>
     * <dt><code>http://validator.nu/properties/streamability-violation-policy</code></dt>
     * <dd><code>getStreamabilityViolationPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/document-mode-handler</code></dt>
     * <dd><code>getDocumentModeHandler</code></dd>
     * <dt><code>http://validator.nu/properties/doctype-expectation</code></dt>
     * <dd><code>getDoctypeExpectation</code></dd>
     * <dt><code>http://xml.org/sax/features/unicode-normalization-checking</code></dt>
     * </dl>
     * 
     * @param name
     *            property URI string
     * @return a value per the list above
     * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
     */
    public Object getProperty(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if ("http://xml.org/sax/properties/declaration-handler".equals(name)) {
            throw new SAXNotSupportedException(
                    "This parser does not suppert DeclHandler.");
        } else if ("http://xml.org/sax/properties/document-xml-version".equals(name)) {
            return "1.0"; // Emulating an XML 1.1 parser is not supported.
        } else if ("http://xml.org/sax/properties/dom-node".equals(name)) {
            throw new SAXNotSupportedException(
                    "This parser does not walk the DOM.");
        } else if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return getLexicalHandler();
        } else if ("http://xml.org/sax/properties/xml-string".equals(name)) {
            throw new SAXNotSupportedException(
                    "This parser does not expose the source as a string.");
        } else if ("http://validator.nu/properties/content-space-policy".equals(name)) {
            return getContentSpacePolicy();
        } else if ("http://validator.nu/properties/content-non-xml-char-policy".equals(name)) {
            return getContentNonXmlCharPolicy();
        } else if ("http://validator.nu/properties/comment-policy".equals(name)) {
            return getCommentPolicy();
        } else if ("http://validator.nu/properties/xmlns-policy".equals(name)) {
            return getXmlnsPolicy();
        } else if ("http://validator.nu/properties/name-policy".equals(name)) {
            return getNamePolicy();
        } else if ("http://validator.nu/properties/streamability-violation-policy".equals(name)) {
            return getStreamabilityViolationPolicy();
        } else if ("http://validator.nu/properties/document-mode-handler".equals(name)) {
            return getDocumentModeHandler();
        } else if ("http://validator.nu/properties/doctype-expectation".equals(name)) {
            return getDoctypeExpectation();
        } else if ("http://validator.nu/properties/xml-policy".equals(name)) {
            throw new SAXNotSupportedException(
                    "Cannot get a convenience setter.");
        } else {
            throw new SAXNotRecognizedException();
        }
    }

    public void parse(InputSource input) throws IOException, SAXException {
        lazyInit();
        try {
            treeBuilder.setFragmentContext(null);
            tokenize(input);
        } finally {
            if (saxTreeBuilder != null) {
                Document document = saxTreeBuilder.getDocument();
                if (document != null) {
                    new TreeParser(contentHandler, lexicalHandler).parse(document);
                }
            }
        }
    }

    public void parseFragment(InputSource input, String context)
            throws IOException, SAXException {
        lazyInit();
        try {
            treeBuilder.setFragmentContext(context);
            tokenize(input);
        } finally {
            if (saxTreeBuilder != null) {
                DocumentFragment fragment = saxTreeBuilder.getDocumentFragment();
                new TreeParser(contentHandler, lexicalHandler).parse(fragment);
            }
        }
    }
    
    /**
     * @param is
     * @throws SAXException
     * @throws IOException
     * @throws MalformedURLException
     */
    private void tokenize(InputSource is) throws SAXException, IOException, MalformedURLException {
        if (is == null) {
            throw new IllegalArgumentException("Null input.");            
        }
        if (is.getByteStream() == null || is.getCharacterStream() == null) {
            String systemId = is.getSystemId();
            if (systemId == null) {
                throw new IllegalArgumentException("No byte stream, no character stream nor URI.");
            }
            if (entityResolver != null) {
                is = entityResolver.resolveEntity(is.getPublicId(), systemId);
            }
            if (is.getByteStream() == null || is.getCharacterStream() == null) {
                is = new InputSource();
                is.setSystemId(systemId);
                is.setByteStream(new URL(systemId).openStream());
            }
        }
        tokenizer.tokenize(is);
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
        if (saxStreamer != null) {
            saxStreamer.setContentHandler(contentHandler == null ? new DefaultHandler()
                    : contentHandler);
        }
    }

    public void setLexicalHandler(LexicalHandler handler) {
        lexicalHandler = handler;
        if (treeBuilder != null) {
            treeBuilder.setIgnoringComments(handler == null);
            if (saxStreamer != null) {
                saxStreamer.setLexicalHandler(handler);
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

    /**
     * Sets a boolean feature without having to use non-<code>XMLReader</code>
     * setters directly.
     * 
     * <p>
     * The supported features are:
     * 
     * <dl>
     * <dt><code>http://xml.org/sax/features/unicode-normalization-checking</code></dt>
     * <dd><code>setCheckingNormalization</code></dd>
     * <dt><code>http://validator.nu/features/html4-mode-compatible-with-xhtml1-schemata</code></dt>
     * <dd><code>setHtml4ModeCompatibleWithXhtml1Schemata</code></dd>
     * <dt><code>http://validator.nu/features/mapping-lang-to-xml-lang</code></dt>
     * <dd><code>setMappingLangToXmlLang</code></dd>
     * <dt><code>http://validator.nu/features/scripting-enabled</code></dt>
     * <dd><code>setScriptingEnabled</code></dd>
     * </dl>
     * 
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/features/external-general-entities".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/external-parameter-entities".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/is-standalone".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/lexical-handler/parameter-entities".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/namespaces".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/resolve-dtd-uris".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/string-interning".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/unicode-normalization-checking".equals(name)) {
            setCheckingNormalization(value);
        } else if ("http://xml.org/sax/features/use-attributes2".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/use-locator2".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/use-entity-resolver2".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/validation".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/xmlns-uris".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://xml.org/sax/features/xml-1.1".equals(name)) {
            throw new SAXNotSupportedException("Cannot set " + name + ".");
        } else if ("http://validator.nu/features/html4-mode-compatible-with-xhtml1-schemata".equals(name)) {
            setHtml4ModeCompatibleWithXhtml1Schemata(value);
        } else if ("http://validator.nu/features/mapping-lang-to-xml-lang".equals(name)) {
            setMappingLangToXmlLang(value);
        } else if ("http://validator.nu/features/scripting-enabled".equals(name)) {
            setScriptingEnabled(value);
        } else {
            throw new SAXNotRecognizedException();
        }
    }

    /**
     * Sets a non-boolean property without having to use non-<code>XMLReader</code>
     * setters directly.
     * 
     * <dl>
     * <dt><code>http://xml.org/sax/properties/lexical-handler</code></dt>
     * <dd><code>setLexicalHandler</code></dd>
     * <dt><code>http://validator.nu/properties/content-space-policy</code></dt>
     * <dd><code>setContentSpacePolicy</code></dd>
     * <dt><code>http://validator.nu/properties/content-non-xml-char-policy</code></dt>
     * <dd><code>setContentNonXmlCharPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/comment-policy</code></dt>
     * <dd><code>setCommentPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/xmlns-policy</code></dt>
     * <dd><code>setXmlnsPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/name-policy</code></dt>
     * <dd><code>setNamePolicy</code></dd>
     * <dt><code>http://validator.nu/properties/streamability-violation-policy</code></dt>
     * <dd><code>setStreamabilityViolationPolicy</code></dd>
     * <dt><code>http://validator.nu/properties/document-mode-handler</code></dt>
     * <dd><code>setDocumentModeHandler</code></dd>
     * <dt><code>http://validator.nu/properties/doctype-expectation</code></dt>
     * <dd><code>setDoctypeExpectation</code></dd>
     * <dt><code>http://validator.nu/properties/xml-policy</code></dt>
     * <dd><code>setXmlPolicy</code></dd>
     * </dl>
     * 
     * @see org.xml.sax.XMLReader#setProperty(java.lang.String,
     *      java.lang.Object)
     */
    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/declaration-handler".equals(name)) {
            throw new SAXNotSupportedException(
                    "This parser does not suppert DeclHandler.");
        } else if ("http://xml.org/sax/properties/document-xml-version".equals(name)) {
            throw new SAXNotSupportedException(
                    "Can't set document-xml-version.");
        } else if ("http://xml.org/sax/properties/dom-node".equals(name)) {
            throw new SAXNotSupportedException("Can't set dom-node.");
        } else if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            setLexicalHandler((LexicalHandler) value);
        } else if ("http://xml.org/sax/properties/xml-string".equals(name)) {
            throw new SAXNotSupportedException("Can't set xml-string.");
        } else if ("http://validator.nu/properties/content-space-policy".equals(name)) {
            setContentSpacePolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/content-non-xml-char-policy".equals(name)) {
            setContentNonXmlCharPolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/comment-policy".equals(name)) {
            setCommentPolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/xmlns-policy".equals(name)) {
            setXmlnsPolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/name-policy".equals(name)) {
            setNamePolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/streamability-violation-policy".equals(name)) {
            setStreamabilityViolationPolicy((XmlViolationPolicy) value);
        } else if ("http://validator.nu/properties/document-mode-handler".equals(name)) {
            setDocumentModeHandler((DocumentModeHandler) value);
        } else if ("http://validator.nu/properties/doctype-expectation".equals(name)) {
            setDoctypeExpectation((DoctypeExpectation) value);
        } else if ("http://validator.nu/properties/xml-policy".equals(name)) {
            setXmlPolicy((XmlViolationPolicy) value);
        } else {
            throw new SAXNotRecognizedException();
        }
    }

    /**
     * @return
     * @see nu.validator.htmlparser.impl.Tokenizer#isCheckingNormalization()
     */
    public boolean isCheckingNormalization() {
        return checkingNormalization;
    }

    /**
     * @param enable
     * @see nu.validator.htmlparser.impl.Tokenizer#setCheckingNormalization(boolean)
     */
    public void setCheckingNormalization(boolean enable) {
        this.checkingNormalization = enable;
        if (tokenizer != null) {
            tokenizer.setCheckingNormalization(checkingNormalization);
        }
    }

    /**
     * @param commentPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setCommentPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        this.commentPolicy = commentPolicy;
        if (tokenizer != null) {
            tokenizer.setCommentPolicy(commentPolicy);
        }
    }

    /**
     * @param contentNonXmlCharPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentNonXmlCharPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        this.contentNonXmlCharPolicy = contentNonXmlCharPolicy;
        if (tokenizer != null) {
            tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
        }
    }

    /**
     * @param contentSpacePolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentSpacePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        this.contentSpacePolicy = contentSpacePolicy;
        if (tokenizer != null) {
            tokenizer.setContentSpacePolicy(contentSpacePolicy);
        }
    }

    /**
     * @return
     * @see nu.validator.htmlparser.impl.TreeBuilder#isScriptingEnabled()
     */
    public boolean isScriptingEnabled() {
        return scriptingEnabled;
    }

    /**
     * @param scriptingEnabled
     * @see nu.validator.htmlparser.impl.TreeBuilder#setScriptingEnabled(boolean)
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
     * @param doctypeExpectation
     *            the doctypeExpectation to set
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
     * @param documentModeHandler
     *            the documentModeHandler to set
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
     * @param streamabilityViolationPolicy
     *            the streamabilityViolationPolicy to set
     */
    public void setStreamabilityViolationPolicy(
            XmlViolationPolicy streamabilityViolationPolicy) {
        this.streamabilityViolationPolicy = streamabilityViolationPolicy;
    }

    public void setHtml4ModeCompatibleWithXhtml1Schemata(
            boolean html4ModeCompatibleWithXhtml1Schemata) {
        this.html4ModeCompatibleWithXhtml1Schemata = html4ModeCompatibleWithXhtml1Schemata;
        if (tokenizer != null) {
            tokenizer.setHtml4ModeCompatibleWithXhtml1Schemata(html4ModeCompatibleWithXhtml1Schemata);
        }
    }

    public Locator getDocumentLocator() {
        return tokenizer;
    }

    /**
     * Returns the html4ModeCompatibleWithXhtml1Schemata.
     * 
     * @return the html4ModeCompatibleWithXhtml1Schemata
     */
    public boolean isHtml4ModeCompatibleWithXhtml1Schemata() {
        return html4ModeCompatibleWithXhtml1Schemata;
    }

    /**
     * @param mappingLangToXmlLang
     * @see nu.validator.htmlparser.impl.Tokenizer#setMappingLangToXmlLang(boolean)
     */
    public void setMappingLangToXmlLang(boolean mappingLangToXmlLang) {
        this.mappingLangToXmlLang = mappingLangToXmlLang;
        if (tokenizer != null) {
            tokenizer.setMappingLangToXmlLang(mappingLangToXmlLang);
        }
    }

    /**
     * Returns the mappingLangToXmlLang.
     * 
     * @return the mappingLangToXmlLang
     */
    public boolean isMappingLangToXmlLang() {
        return mappingLangToXmlLang;
    }

    /**
     * @param xmlnsPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setXmlnsPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setXmlnsPolicy(XmlViolationPolicy xmlnsPolicy) {
        if (xmlnsPolicy == XmlViolationPolicy.FATAL) {
            throw new IllegalArgumentException("Can't use FATAL here.");
        }
        this.xmlnsPolicy = xmlnsPolicy;
        if (tokenizer != null) {
            tokenizer.setXmlnsPolicy(xmlnsPolicy);
        }
    }

    /**
     * Returns the xmlnsPolicy.
     * 
     * @return the xmlnsPolicy
     */
    public XmlViolationPolicy getXmlnsPolicy() {
        return xmlnsPolicy;
    }

    /**
     * Returns the lexicalHandler.
     * 
     * @return the lexicalHandler
     */
    public LexicalHandler getLexicalHandler() {
        return lexicalHandler;
    }

    /**
     * Returns the commentPolicy.
     * 
     * @return the commentPolicy
     */
    public XmlViolationPolicy getCommentPolicy() {
        return commentPolicy;
    }

    /**
     * Returns the contentNonXmlCharPolicy.
     * 
     * @return the contentNonXmlCharPolicy
     */
    public XmlViolationPolicy getContentNonXmlCharPolicy() {
        return contentNonXmlCharPolicy;
    }

    /**
     * Returns the contentSpacePolicy.
     * 
     * @return the contentSpacePolicy
     */
    public XmlViolationPolicy getContentSpacePolicy() {
        return contentSpacePolicy;
    }

    /**
     * @param reportingDoctype
     * @see nu.validator.htmlparser.impl.TreeBuilder#setReportingDoctype(boolean)
     */
    public void setReportingDoctype(boolean reportingDoctype) {
        this.reportingDoctype = reportingDoctype;
        if (treeBuilder != null) {
            treeBuilder.setReportingDoctype(reportingDoctype);
        }
    }

    /**
     * Returns the reportingDoctype.
     * 
     * @return the reportingDoctype
     */
    public boolean isReportingDoctype() {
        return reportingDoctype;
    }

    /**
     * @param namePolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setNamePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setNamePolicy(XmlViolationPolicy namePolicy) {
        this.namePolicy = namePolicy;
        if (tokenizer != null) {
            tokenizer.setNamePolicy(namePolicy);
        }
    }

    /**
     * This is a catch-all convenience method for setting name, xmlns, content space, 
     * content non-XML char and comment policies in one go. This does not affect the 
     * streamability policy or doctype reporting.
     * 
     * @param xmlPolicy
     */
    public void setXmlPolicy(XmlViolationPolicy xmlPolicy) {
        setNamePolicy(xmlPolicy);
        setXmlnsPolicy(xmlPolicy == XmlViolationPolicy.FATAL ? XmlViolationPolicy.ALTER_INFOSET : xmlPolicy);
        setContentSpacePolicy(xmlPolicy);
        setContentNonXmlCharPolicy(xmlPolicy);
        setCommentPolicy(xmlPolicy);
    }

    /**
     * Returns the namePolicy.
     * 
     * @return the namePolicy
     */
    public XmlViolationPolicy getNamePolicy() {
        return namePolicy;
    }
}
