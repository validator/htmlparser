/*
 * Copyright (c) 2005, 2006, 2007 Henri Sivonen
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

package nu.validator.htmlparser.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.UnsupportedCharsetException;

import nu.validator.htmlparser.common.CharacterHandler;
import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.TokenHandler;
import nu.validator.htmlparser.extra.NormalizationChecker;
import nu.validator.htmlparser.impl.Confidence;
import nu.validator.htmlparser.impl.Tokenizer;
import nu.validator.htmlparser.impl.TreeBuilder;
import nu.validator.htmlparser.impl.UTF16Buffer;
import nu.validator.htmlparser.rewindable.RewindableInputStream;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Driver extends Tokenizer {

    /**
     * The input UTF-16 code unit stream. If a byte stream was given, this
     * object is an instance of <code>HtmlInputStreamReader</code>.
     */
    private Reader reader;
    /**
     * The reference to the rewindable byte stream. <code>null</code> if p
     * rohibited or no longer needed.
     */
    private RewindableInputStream rewindableInputStream;
    private boolean swallowBom;
    private Encoding characterEncoding;
    private boolean allowRewinding = true;
    private Heuristics heuristics = Heuristics.NONE;

    public Driver(TokenHandler tokenHandler) {
        super(tokenHandler);
    }

    /**
     * Returns the allowRewinding.
     * 
     * @return the allowRewinding
     */
    public boolean isAllowRewinding() {
        return allowRewinding;
    }

    /**
     * Sets the allowRewinding.
     * 
     * @param allowRewinding
     *            the allowRewinding to set
     */
    public void setAllowRewinding(boolean allowRewinding) {
        this.allowRewinding = allowRewinding;
    }

    /**
     * Turns NFC checking on or off.
     * 
     * @param enable
     *            <code>true</code> if checking on
     */
    public void setCheckingNormalization(boolean enable) {
        if (enable) {
            if (isCheckingNormalization()) {
                return;
            } else {
                NormalizationChecker normalizationChecker = new NormalizationChecker(
                        this);
                normalizationChecker.setErrorHandler(errorHandler);
    
            }
        } else {
            if (isCheckingNormalization()) {
                CharacterHandler[] newHandlers = new CharacterHandler[characterHandlers.length - 1];
                boolean skipped = false;
                int j = 0;
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    if (!(!skipped && (ch instanceof NormalizationChecker))) {
                        newHandlers[j] = ch;
                        j++;
                    }
                }
                characterHandlers = newHandlers;
            } else {
                return;
            }
        }
    }

    public void addCharacterHandler(CharacterHandler characterHandler) {
        if (characterHandler == null) {
            throw new IllegalArgumentException("Null argument.");
        }
        CharacterHandler[] newHandlers = new CharacterHandler[characterHandlers.length + 1];
        System.arraycopy(characterHandlers, 0, newHandlers, 0,
                characterHandlers.length);
        newHandlers[characterHandlers.length] = characterHandler;
        characterHandlers = newHandlers;
    }

    /**
     * Query if checking normalization.
     * 
     * @return <code>true</code> if checking on
     */
    public boolean isCheckingNormalization() {
        for (int i = 0; i < characterHandlers.length; i++) {
            CharacterHandler ch = characterHandlers[i];
            if (ch instanceof NormalizationChecker) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the tokenization. This is the main entry point.
     * 
     * @param is
     *            the input source
     * @throws SAXException
     *             on fatal error (if configured to treat XML violations as
     *             fatal) or if the token handler threw
     * @throws IOException
     *             if the stream threw
     */
    public void tokenize(InputSource is) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource was null.");
        }
        confidence = Confidence.TENTATIVE;
        swallowBom = true;
        rewindableInputStream = null;
        this.systemId = is.getSystemId();
        this.publicId = is.getPublicId();
        this.reader = is.getCharacterStream();
        this.characterEncoding = encodingFromExternalDeclaration(is.getEncoding());
        if (this.reader == null) {
            InputStream inputStream = is.getByteStream();
            if (inputStream == null) {
                throw new SAXException("Both streams in InputSource were null.");
            }
            if (this.characterEncoding == null) {
                if (allowRewinding) {
                    inputStream = rewindableInputStream = new RewindableInputStream(
                            inputStream);
                }
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this, heuristics);
            } else {
                becomeConfident();
                this.reader = new HtmlInputStreamReader(inputStream,
                        errorHandler, this, this, this.characterEncoding);
            }
        } else {
            becomeConfident();
        }
        Throwable t = null;
        try {
            for (;;) {
                try {
                    for (int i = 0; i < characterHandlers.length; i++) {
                        CharacterHandler ch = characterHandlers[i];
                        ch.start();
                    }
                    runStates();
                    if (confidence == Confidence.TENTATIVE
                            && !alreadyComplainedAboutNonAscii) {
                        warnWithoutLocation("The character encoding of the document was not declared.");
                    }
                    break;
                } catch (ReparseException e) {
                    if (rewindableInputStream == null) {
                        fatal("Changing encoding at this point would need non-streamable behavior.");
                    } else {
                        rewindableInputStream.rewind();
                        becomeConfident();
                        this.reader = new HtmlInputStreamReader(
                                rewindableInputStream, errorHandler, this,
                                this, this.characterEncoding);
                    }
                    continue;
                }
            }
        } catch (Throwable tr) {
            t = tr;
        } finally {
            try {
                end();
                characterEncoding = null;
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    ch.end();
                }
                reader.close();
                reader = null;
                rewindableInputStream = null;
            } catch (Throwable tr) {
                if (t == null) {
                    t = tr;
                } // else drop the later throwable
            }
            if (t != null) {
                if (t instanceof IOException) {
                    throw (IOException) t;
                } else if (t instanceof SAXException) {
                    throw (SAXException) t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    // impossible
                    throw new RuntimeException(t);
                }
            }
        }
    }

    void dontSwallowBom() {
        swallowBom = false;
    }

    private void runStates() throws SAXException, IOException {
        start();
    
        char[] buffer = new char[2048];
        UTF16Buffer bufr = new UTF16Buffer(buffer, 0, 0);
        boolean lastWasCR = false;
        int len = -1;
        while ((len = reader.read(buffer)) != -1) {
            assert len > 0;
            int offset = 0;
            int length = len;
            if (swallowBom) {
                swallowBom = false;
                if (buffer[0] == '\uFEFF') {
                    offset = 1;
                    length--;
                }
            }
            for (int i = 0; i < characterHandlers.length; i++) {
                CharacterHandler ch = characterHandlers[i];
                ch.characters(buffer, offset, length);
            }
            bufr.setOffset(offset);
            bufr.setLength(length);
            lastWasCR = normalizeLineBreaks(bufr, lastWasCR);
            tokenizeBuffer(bufr);
        }
        eof();
    }

    public void setEncoding(Encoding encoding, Confidence confidence) {
        this.characterEncoding = encoding;
        if (confidence == Confidence.CERTAIN) {
            becomeConfident();
        }
    }

    public void internalEncodingDeclaration(String internalCharset) throws SAXException {
        try {
            internalCharset = Encoding.toAsciiLowerCase(internalCharset);
            Encoding cs;
            if ("utf-16".equals(internalCharset)
                    || "utf-16be".equals(internalCharset)
                    || "utf-16le".equals(internalCharset)) {
                cs = Encoding.UTF8;
                errTreeBuilder("Internal encoding declaration specified \u201C"
                        + internalCharset
                        + "\u201D which is not an ASCII superset. Continuing as if the encoding had been \u201Cutf-8\u201D.");
            } else {
                cs = Encoding.forName(internalCharset);
            }
            Encoding actual = cs.getActualHtmlEncoding();
            if (actual == null) {
                actual = cs;
            }
            if (!actual.isAsciiSuperset()) {
                errTreeBuilder("Internal encoding declaration specified \u201C"
                        + internalCharset
                        + "\u201D which is not an ASCII superset. Not changing the encoding.");
                return;
            }
            if (characterEncoding == null) {
                // Reader case
                return;
            }
            if (characterEncoding == actual) {
                becomeConfident();
                return;
            }
            if (confidence == Confidence.CERTAIN) {
                errTreeBuilder("Internal encoding declaration \u201C"
                        + internalCharset
                        + "\u201D disagrees with the actual encoding of the document (\u201C"
                        + characterEncoding.getCanonName() + "\u201D).");
            } else {
                Encoding newEnc = whineAboutEncodingAndReturnActual(
                        internalCharset, cs);
                errTreeBuilder("Changing character encoding \u201C"
                        + internalCharset + "\u201D and reparsing.");
                characterEncoding = newEnc;
                throw new ReparseException();
            }
        } catch (UnsupportedCharsetException e) {
            errTreeBuilder("Internal encoding declaration named an unsupported chararacter encoding \u201C"
                    + internalCharset + "\u201D.");
        }
    }

    /**
     * 
     */
    private void becomeConfident() {
        if (rewindableInputStream != null) {
            rewindableInputStream.willNotRewind();
        }
        confidence = Confidence.CERTAIN;
    }

    /**
     * Sets the encoding sniffing heuristics.
     * 
     * @param heuristics
     *            the heuristics to set
     */
    public void setHeuristics(Heuristics heuristics) {
        this.heuristics = heuristics;
    }

    protected void errTreeBuilder(String message) throws SAXException {
        ErrorHandler eh = null;
        if (tokenHandler instanceof TreeBuilder<?>) {
            TreeBuilder<?> treeBuilder = (TreeBuilder<?>) tokenHandler;
            eh = treeBuilder.getErrorHandler();
        }
        if (eh == null) {
            eh = errorHandler;
        }
        if (eh == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, this);
        eh.error(spe);
    }

    /**
     * Reports a warning without line/col
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected void warnWithoutLocation(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, null,
                getSystemId(), -1, -1);
        errorHandler.warning(spe);
    }

    /**
     * Initializes a decoder from external decl.
     */
    protected Encoding encodingFromExternalDeclaration(String encoding)
            throws SAXException {
                if (encoding == null) {
                    return null;
                }
                encoding = Encoding.toAsciiLowerCase(encoding);
                try {
                    Encoding cs = Encoding.forName(encoding);
                    if ("utf-16".equals(cs.getCanonName())
                            || "utf-32".equals(cs.getCanonName())) {
                        swallowBom = false;
                    }
                    return whineAboutEncodingAndReturnActual(encoding, cs);
                } catch (UnsupportedCharsetException e) {
                    err("Unsupported character encoding name: \u201C" + encoding
                            + "\u201D. Will sniff.");
                    swallowBom = true;
                }
                return null; // keep the compiler happy
            }

    /**
     * @param encoding
     * @param cs
     * @return
     * @throws SAXException
     */
    protected Encoding whineAboutEncodingAndReturnActual(String encoding, Encoding cs)
            throws SAXException {
                String canonName = cs.getCanonName();
                if (!cs.isRegistered()) {
                    if (encoding.startsWith("x-")) {
                        err("The encoding \u201C"
                                + encoding
                                + "\u201D is not an IANA-registered encoding. (Charmod C022)");
                    } else {
                        err("The encoding \u201C"
                                + encoding
                                + "\u201D is not an IANA-registered encoding and did not use the \u201Cx-\u201D prefix. (Charmod C023)");
                    }
                } else if (!canonName.equals(encoding)) {
                    err("The encoding \u201C"
                            + encoding
                            + "\u201D is not the preferred name of the character encoding in use. The preferred name is \u201C"
                            + canonName + "\u201D. (Charmod C024)");
                }
                if (cs.isShouldNot()) {
                    warn("Authors should not use the character encoding \u201C"
                            + encoding
                            + "\u201D. It is recommended to use \u201CUTF-8\u201D.");
                } else if (cs.isLikelyEbcdic()) {
                    warn("Authors should not use EBCDIC-based encodings. It is recommended to use \u201CUTF-8\u201D.");
                } else if (cs.isObscure()) {
                    warn("The character encoding \u201C"
                            + encoding
                            + "\u201D is not widely supported. Better interoperability may be achieved by using \u201CUTF-8\u201D.");
                }
                Encoding actual = cs.getActualHtmlEncoding();
                if (actual == null) {
                    return cs;
                } else {
                    warn("Using \u201C" + actual.getCanonName()
                            + "\u201D instead of the declared encoding \u201C"
                            + encoding + "\u201D.");
                    return actual;
                }
            }

    protected void complainAboutNonAscii() throws SAXException {
        err("The character encoding of the document was not explicit (assumed \u201C"
                + characterEncoding.getCanonName()
                + "\u201D) but the document contains non-ASCII.");
    }

    private class ReparseException extends SAXException {

    }
}
