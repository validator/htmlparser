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

package nu.validator.htmlparser.xom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import nu.validator.htmlparser.Tokenizer;
import nu.validator.htmlparser.XmlViolationPolicy;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class HtmlBuilder extends Builder {

    private final Tokenizer tokenizer;

    private final XOMTreeBuilder xomTreeBuilder;

    private final SimpleNodeFactory simpleNodeFactory;

    private EntityResolver entityResolver;

    /**
     * 
     */
    public HtmlBuilder() {
        this(new SimpleNodeFactory());
    }

    /**
     * @param arg0
     */
    public HtmlBuilder(SimpleNodeFactory nodeFactory) {
        super();
        this.simpleNodeFactory = nodeFactory;
        this.xomTreeBuilder = new XOMTreeBuilder(nodeFactory);
        this.tokenizer = new Tokenizer(xomTreeBuilder);
        this.tokenizer.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
    }

    private void tokenize(InputSource is) throws ParsingException, IOException,
            MalformedURLException {
        try {
            if (is == null) {
                throw new IllegalArgumentException("Null input.");
            }
            if (is.getByteStream() == null || is.getCharacterStream() == null) {
                String systemId = is.getSystemId();
                if (systemId == null) {
                    throw new IllegalArgumentException(
                            "No byte stream, no character stream nor URI.");
                }
                if (entityResolver != null) {
                    is = entityResolver.resolveEntity(is.getPublicId(),
                            systemId);
                }
                if (is.getByteStream() == null
                        || is.getCharacterStream() == null) {
                    is = new InputSource();
                    is.setSystemId(systemId);
                    is.setByteStream(new URL(systemId).openStream());
                }
            }
            tokenizer.tokenize(is);
        } catch (SAXParseException e) {
            throw new ParsingException(e.getMessage(), e.getSystemId(), e.getLineNumber(),
                    e.getColumnNumber(), e);
        } catch (SAXException e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    public Document build(InputSource is) throws ParsingException, IOException {
        xomTreeBuilder.setFragmentContext(null);
        tokenize(is);
        return xomTreeBuilder.getDocument();
    }

    public Nodes buildFragment(InputSource is, String context)
            throws IOException, ParsingException {
        xomTreeBuilder.setFragmentContext(context);
        tokenize(is);
        return xomTreeBuilder.getDocumentFragment();
    }

    
    /**
     * @see nu.xom.Builder#build(java.io.File)
     */
    @Override
    public Document build(File file) throws ParsingException,
            ValidityException, IOException {
        return build(new FileInputStream(file), file.toURI().toASCIIString());
    }

    /**
     * @see nu.xom.Builder#build(java.io.InputStream, java.lang.String)
     */
    @Override
    public Document build(InputStream stream, String uri)
            throws ParsingException, ValidityException, IOException {
        InputSource is = new InputSource(stream);
        is.setSystemId(uri);
        return build(is);
    }

    /**
     * @see nu.xom.Builder#build(java.io.InputStream)
     */
    @Override
    public Document build(InputStream stream) throws ParsingException,
            ValidityException, IOException {
        return build(new InputSource(stream));
    }

    /**
     * @see nu.xom.Builder#build(java.io.Reader, java.lang.String)
     */
    @Override
    public Document build(Reader stream, String uri) throws ParsingException,
            ValidityException, IOException {
        InputSource is = new InputSource(stream);
        is.setSystemId(uri);
        return build(is);
    }

    /**
     * @see nu.xom.Builder#build(java.io.Reader)
     */
    @Override
    public Document build(Reader stream) throws ParsingException,
            ValidityException, IOException {
        return build(new InputSource(stream));
    }

    /**
     * @see nu.xom.Builder#build(java.lang.String, java.lang.String)
     */
    @Override
    public Document build(String content, String uri) throws ParsingException,
            ValidityException, IOException {
        return build(new StringReader(content), uri);
    }

    /**
     * @see nu.xom.Builder#build(java.lang.String)
     */
    @Override
    public Document build(String uri) throws ParsingException,
            ValidityException, IOException {
        return build(new InputSource(uri));
    }

    /**
     * @see nu.xom.Builder#getNodeFactory()
     */
    public SimpleNodeFactory getSimpleNodeFactory() {
        return simpleNodeFactory;
    }

    public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        xomTreeBuilder.setErrorHandler(errorHandler);
        tokenizer.setErrorHandler(errorHandler);
    }
}
