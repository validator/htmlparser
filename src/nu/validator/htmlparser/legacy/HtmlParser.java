/*
 * Copyright (c) 2005, 2006 Henri Sivonen
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

package nu.validator.htmlparser.legacy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import nu.validator.htmlparser.Tokenizer;

import org.whattf.checker.NormalizationChecker;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fi.iki.hsivonen.io.EncodingInfo;
import fi.iki.hsivonen.io.NonBufferingAsciiInputStreamReader;
import fi.iki.hsivonen.xml.AttributesImpl;
import fi.iki.hsivonen.xml.ContentHandlerFilter;
import fi.iki.hsivonen.xml.EmptyAttributes;
import fi.iki.hsivonen.xml.SilentDraconianErrorHandler;
import fi.iki.hsivonen.xml.XhtmlSaxEmitter;

/**
 * WARNING: This parser is incomplete. It does not perform tag inference, yet. It does not yet perform 
 * case folding for attribute value like method="POST".
 * 
 * @version $Id$
 * @author hsivonen
 */
public final class HtmlParser implements XMLReader {

    private ErrorHandler eh;

    private ContentHandler ch;
    
    private DoctypeHandler doctypeHandler;

    private LegacyTokenHandler lth;
    
    private Tokenizer tokenizer;

    private boolean doctypeSeen;
    
    private int doctypeMode;
    
    private boolean html5;

    private DTDHandler dtdHandler;

    private EmptyElementFilter eef;

    private TagInferenceFilter tif;

    private CharacterEncodingDeclarationFilter cedf;

    private ContentHandlerFilter pipelineLast;

    private EntityResolver entityResolver = null;

    public HtmlParser() {
        eef = new EmptyElementFilter();
        lth = new LegacyTokenHandler(eef);
        tif = new TagInferenceFilter(lth);
        cedf = new CharacterEncodingDeclarationFilter(this);
        ch = eef;
        eef.setContentHandler(tif);
        tif.setContentHandler(cedf);
        pipelineLast = cedf;
        setErrorHandler(new SilentDraconianErrorHandler());
        setContentHandler(new DefaultHandler());
    }

//    /**
//     * @throws SAXException 
//     * 
//     */
//    private void sawHtml5Doctype() throws SAXException {
//        html5 = true;
//        switch (doctypeMode) {
//            case DoctypeHandler.ANY_DOCTYPE:
//                if (doctypeHandler != null) {
//                    doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML5);
//                }
//                break;
//            case DoctypeHandler.DOCTYPE_HTML5:
//                return;
//            case DoctypeHandler.DOCTYPE_HTML401_STRICT:
//                err("Expected an HTML 4.01 Strict document but saw the HTML5 doctype.");
//                break;
//            case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
//                err("Expected an HTML 4.01 Transitional document but saw the HTML5 doctype.");
//                break;
//            default:
//                throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
//        }
//    }
//
//    /**
//     * @param publicId
//     * @param systemId
//     * @throws SAXException
//     */
//    private void checkPublicAndSystemIds(String publicId, String systemId)
//            throws SAXException, IOException {
//        if ("-//W3C//DTD HTML 4.01//EN".equals(publicId)) {
//            switch (doctypeMode) {
//                case DoctypeHandler.ANY_DOCTYPE:
//                    if (doctypeHandler != null) {
//                        doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML401_STRICT);
//                    }
//                    break;
//                case DoctypeHandler.DOCTYPE_HTML401_STRICT:
//                    return;
//                case DoctypeHandler.DOCTYPE_HTML5:
//                    err("Expected an HTML5 document but saw an HTML 4.01 Strict doctype.");
//                    break;
//                case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
//                    err("Expected HTML 4.01 Transitional document but saw an HTML 4.01 Strict doctype.");
//                    break;
//                default:
//                    throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
//            }
//            if (systemId == null) {
//                // XXX err, because HTML 4.01 says "must"?
//                warn("The Strict doctype lacks the system id (URI). This kind of Strict doctype is considered quirky by Mac IE 5. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\u201D.");
//            } else if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemId)) {
//                // XXX err, because HTML 4.01 says "must"?
//                warn("The Strict doctype has a non-canonical system id (URI). The form required by the HTML 4.01 specification is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\u201D.");
//            }
//        } else if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicId)) {
//            switch (doctypeMode) {
//                case DoctypeHandler.ANY_DOCTYPE:
//                    if (doctypeHandler != null) {
//                        doctypeHandler.doctype(DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL);
//                    }
//                    break;
//                case DoctypeHandler.DOCTYPE_HTML401_TRANSITIONAL:
//                    return;
//                case DoctypeHandler.DOCTYPE_HTML401_STRICT:
//                    err("Expected an HTML 4.01 Strict document but saw an HTML 4.01 Transitional doctype.");
//                    break;
//                case DoctypeHandler.DOCTYPE_HTML5:
//                    err("Expected an HTML5 document but saw an HTML 4.01 Transitional doctype.");
//                    break;
//                default:
//                    throw new RuntimeException("Bug in HtmlParser: doctypeMode out of range.");
//            }
//            if (systemId == null) {
//                err("The Transitional doctype lacks the system id (URI). This kind of Transitional doctype is considered quirky by browsers. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\u201D.");
//            } else if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemId)) {
//                // XXX err, because HTML 4.01 says "must"?
//                warn("The Transitional doctype has a non-canonical system id (URI). This kind of Transitional doctype may be considered quirky by some legacy browsers. The preferred non-quirky form (also required by the HTML 4.01 specification) is \u201C<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\u201D.");
//            }
//        } else if (publicId.startsWith("-//W3C//DTD XHTML ")) {
//            fatal("XHTML public id seen. XHTML documents are not conforming HTML5 or HTML 4.01 documents.");
//        } else {
//            err("Legacy doctype or inappropriate doctype. This parser is designed for HTML5 and also supports the HTML5-like subset of HTML 4.01.");
//        }
//    }


    /**
     * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
     */
    public boolean getFeature(String key) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if ("http://xml.org/sax/features/namespaces".equals(key)) {
            return true;
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(key)) {
            return false;
        } else if ("http://hsivonen.iki.fi/checkers/nfc/".equals(key)) {
            return tokenizer.isCheckingNormalization();
        } else {
            throw new SAXNotRecognizedException(key);
        }
    }

    /**
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public void setFeature(String key, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/features/namespaces".equals(key)) {
            if (!value) {
                throw new SAXNotSupportedException(
                        "Cannot turn off namespace support.");
            }
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(key)) {
            if (value) {
                throw new SAXNotSupportedException("Cannot turn on prefixing.");
            }
        } else if ("http://hsivonen.iki.fi/checkers/nfc/".equals(key)) {
            tokenizer.setCheckingNormalization(value);
        } else {
            throw new SAXNotRecognizedException(key);
        }
    }

    /**
     * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
     */
    public Object getProperty(String key) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        throw new SAXNotRecognizedException(key);
    }

    /**
     * @see org.xml.sax.XMLReader#setProperty(java.lang.String,
     *      java.lang.Object)
     */
    public void setProperty(String key, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(key);
    }

    /**
     * @see org.xml.sax.XMLReader#setEntityResolver(org.xml.sax.EntityResolver)
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * @see org.xml.sax.XMLReader#getEntityResolver()
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * @see org.xml.sax.XMLReader#setDTDHandler(org.xml.sax.DTDHandler)
     */
    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    /**
     * @see org.xml.sax.XMLReader#getDTDHandler()
     */
    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    /**
     * @see org.xml.sax.XMLReader#setContentHandler(org.xml.sax.ContentHandler)
     */
    public void setContentHandler(ContentHandler ch) {
        pipelineLast.setContentHandler(ch);
    }

    /**
     * @see org.xml.sax.XMLReader#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return pipelineLast.getContentHandler();
    }

    /**
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler eh) {
        this.eh = eh;
        eef.setErrorHandler(eh);
        tif.setErrorHandler(eh);
        cedf.setErrorHandler(eh);
    }

    /**
     * @see org.xml.sax.XMLReader#getErrorHandler()
     */
    public ErrorHandler getErrorHandler() {
        return eh;
    }

    /**
     * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
     */
    public void parse(InputSource is) throws IOException, SAXException {
        tokenizer.tokenize(is);
    }


    /**
     * @see org.xml.sax.XMLReader#parse(java.lang.String)
     */
    public void parse(String url) throws IOException, SAXException {
        // FIXME b0rked if no resolver
        parse(entityResolver.resolveEntity(url, null));
    }

    /**
     * Returns the doctypeMode.
     * 
     * @return the doctypeMode
     */
    public int getDoctypeMode() {
        return doctypeMode;
    }

    /**
     * Sets the doctypeMode.
     * 
     * @param doctypeMode the doctypeMode to set
     */
    public void setDoctypeMode(int doctypeMode) {
        this.doctypeMode = doctypeMode;
    }

    /**
     * Returns the doctypeHandler.
     * 
     * @return the doctypeHandler
     */
    public DoctypeHandler getDoctypeHandler() {
        return doctypeHandler;
    }

    /**
     * Sets the doctypeHandler.
     * 
     * @param doctypeHandler the doctypeHandler to set
     */
    public void setDoctypeHandler(DoctypeHandler doctypeHandler) {
        this.doctypeHandler = doctypeHandler;
    }

}
