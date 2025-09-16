/*
 * Copyright (c) 2005, 2006, 2007 Henri Sivonen
 * Copyright (c) 2007-2017 Mozilla Foundation
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

/*
 * The comments following this one that use the same comment syntax as this
 * comment are quotes from the HTML Standard at https://html.spec.whatwg.org/
 * as of 10 September 2020. That document came with this statement:
 * Copyright © WHATWG (Apple, Google, Mozilla, Microsoft). This work is
 * licensed under a Creative Commons Attribution 4.0 International License.
 */

package nu.validator.htmlparser.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.UnsupportedCharsetException;

import nu.validator.htmlparser.common.CharacterHandler;
import nu.validator.htmlparser.common.EncodingDeclarationHandler;
import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.TransitionHandler;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.extra.NormalizationChecker;
import nu.validator.htmlparser.impl.ErrorReportingTokenizer;
import nu.validator.htmlparser.impl.Tokenizer;
import nu.validator.htmlparser.impl.UTF16Buffer;
import nu.validator.htmlparser.rewindable.RewindableInputStream;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Driver implements EncodingDeclarationHandler {

    /**
     * The input UTF-16 code unit stream. If a byte stream was given, this
     * object is an instance of <code>HtmlInputStreamReader</code>.
     */
    private Reader reader;

    /**
     * The reference to the rewindable byte stream. <code>null</code> if 
     * prohibited or no longer needed.
     */
    private RewindableInputStream rewindableInputStream;

    private boolean swallowBom = true;

    private Encoding characterEncoding;

    private boolean allowRewinding = true;

    private Heuristics heuristics = Heuristics.NONE;
    
    private final Tokenizer tokenizer;
    
    private Confidence confidence;

    /**
     * Used for NFC checking if non-<code>null</code>, source code capture,
     * etc.
     */
    private CharacterHandler[] characterHandlers = new CharacterHandler[0];

    public Driver(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        tokenizer.setEncodingDeclarationHandler(this);
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
                NormalizationChecker normalizationChecker = new NormalizationChecker(tokenizer);
                normalizationChecker.setErrorHandler(tokenizer.getErrorHandler());

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
        int bufferSize = 2048;
        tokenize(is, bufferSize);
    }
    /**
     * Runs the tokenization. This is the main entry point.
     *
     * @param is
     *            the input source
     * @param bufferSize
     *            the size of the buffer to feed to the tokenizer
     * @throws SAXException
     *             on fatal error (if configured to treat XML violations as
     *             fatal) or if the token handler threw
     * @throws IOException
     *             if the stream threw
     */
    public void tokenize(InputSource is, int bufferSize)
            throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource was null.");
        }
        tokenizer.start();
        confidence = Confidence.TENTATIVE;
        rewindableInputStream = null;
        tokenizer.initLocation(is.getPublicId(), is.getSystemId());
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
                        tokenizer.getErrorHandler(), tokenizer, this, heuristics);
            } else {
                if (this.characterEncoding != Encoding.UTF8) {
                    errorWithoutLocation(Encoding.msgLegacyEncoding(
                            this.characterEncoding.getCanonName()));
                }
                becomeConfident();
                this.reader = new HtmlInputStreamReader(inputStream,
                        tokenizer.getErrorHandler(), tokenizer, this, this.characterEncoding);
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
                    runStates(bufferSize);
                    break;
                } catch (ReparseException e) {
                    if (rewindableInputStream == null) {
                        tokenizer.fatal("Changing encoding at this point would need non-streamable behavior.");
                    } else {
                        rewindableInputStream.rewind();
                        becomeConfident();
                        this.reader = new HtmlInputStreamReader(
                                rewindableInputStream, tokenizer.getErrorHandler(), tokenizer,
                                this, this.characterEncoding);
                    }
                    continue;
                }
            }
        } catch (Throwable tr) {
            t = tr;
        } finally {
            try {
                tokenizer.end();
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

    public void dontSwallowBom() {
        swallowBom = false;
    }

    private void runStates(int bufferSize) throws SAXException, IOException {
        char[] buffer = new char[bufferSize];
        UTF16Buffer bufr = new UTF16Buffer(buffer, 0, 0);
        boolean lastWasCR = false;
        int len = -1;
        if ((len = reader.read(buffer)) != -1) {
            assert len > 0;
            int streamOffset = 0;
            int offset = 0;
            int length = len;
            if (swallowBom) {
                if (buffer[0] == '\uFEFF') {
                    streamOffset = -1;
                    offset = 1;
                    length--;
                }
            }
            if (length > 0) {
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    ch.characters(buffer, offset, length);
                }
                tokenizer.setTransitionBaseOffset(streamOffset);
                bufr.setStart(offset);
                bufr.setEnd(offset + length);
                while (bufr.hasMore()) {
                    bufr.adjust(lastWasCR);
                    lastWasCR = false;
                    if (bufr.hasMore()) {
                        lastWasCR = tokenizer.tokenizeBuffer(bufr);                    
                    }
                }
            }
            streamOffset = length;
            while ((len = reader.read(buffer)) != -1) {
                assert len > 0;
                for (int i = 0; i < characterHandlers.length; i++) {
                    CharacterHandler ch = characterHandlers[i];
                    ch.characters(buffer, 0, len);
                }
                tokenizer.setTransitionBaseOffset(streamOffset);
                bufr.setStart(0);
                bufr.setEnd(len);
                while (bufr.hasMore()) {
                    bufr.adjust(lastWasCR);
                    lastWasCR = false;
                    if (bufr.hasMore()) {
                        lastWasCR = tokenizer.tokenizeBuffer(bufr);                    
                    }
                }
                streamOffset += len;
            }
        }
        tokenizer.eof();
    }

    public void setEncoding(Encoding encoding, Confidence confidence) {
        this.characterEncoding = encoding;
        if (confidence == Confidence.CERTAIN) {
            becomeConfident();
        }
    }

    private void errInternalActualDiffer(String internalCharset, String actual)
            throws SAXException {
        if (!internalCharset.equals(actual)) {
            tokenizer.errTreeBuilder(
                    "Ignoring internal encoding declaration \u201C"
                            + internalCharset + "\u201D, which disagrees with"
                            + " the actual encoding of the document (\u201C"
                            + actual + "\u201D).");
        }
    }

    public boolean internalEncodingDeclaration(String internalCharset)
            throws SAXException {
        String actual = characterEncoding.getCanonName();
        if (confidence == Confidence.CERTAIN) {
            errInternalActualDiffer(internalCharset, actual);
            return true;
        }
        /* https://html.spec.whatwg.org/#changing-the-encoding-while-parsing */
        try {
            if ("utf-16be".equals(actual) || "utf-16le".equals(actual)) {
                errInternalActualDiffer(internalCharset, actual);
                /*
                 * 1. If the encoding that is already being used to interpret
                 * the input stream is a UTF-16 encoding, then set the
                 * confidence to certain and return. The new encoding is ignored
                 * becomeConfident();
                 */
                return true;
            }
            internalCharset = internalCharset.toLowerCase();
            Encoding cs = Encoding.forName(internalCharset);
            if ("utf-16be".equals(internalCharset)
                    || "utf-16le".equals(internalCharset)) {
                /*
                 * 2. If the new encoding is a UTF-16 encoding, then change it
                 * to UTF-8.
                 */
                tokenizer.errTreeBuilder(
                        Encoding.msgIgnoredCharset(internalCharset, "utf-8"));
                cs = Encoding.UTF8;
                internalCharset = "utf-8";
            } else if ("x-user-defined".equals(internalCharset)) {
                /*
                 * 3. If the new encoding is x-user-defined, then change it to
                 * windows-1252.
                 */
                tokenizer.errTreeBuilder(Encoding.msgIgnoredCharset(
                        "x-user-defined", "windows-1252"));
                cs = Encoding.WINDOWS1252;
                internalCharset = "windows-1252";
            }
            if (characterEncoding == null) {
                // Reader case
                return true;
            }
            if (characterEncoding == cs) {
                /*
                 * 4. If the new encoding is identical or equivalent to the
                 * encoding that is already being used to interpret the input
                 * stream, then set the confidence to certain and return.
                 */
                becomeConfident();
                return true;
            }
            /*
             * 6. Otherwise, navigate to the document again, with
             * historyHandling set to "replace", and using the same source
             * browsing context, but this time skip the encoding sniffing
             * algorithm and instead just set the encoding to the new encoding
             */
            Encoding newEnc = whineAboutEncodingAndReturnCanonical(
                    internalCharset, cs);
            tokenizer.errTreeBuilder("Changing character encoding to \u201C"
                    + internalCharset + "\u201D and reparsing.");
            characterEncoding = newEnc;
            // Note: We intentionally don’t call becomeConfident() at this
            // point. If we did, it would end up causing the exception
            // java.lang.IllegalStateException: rewind() after willNotRewind()
            // to be thrown later. So we are departing here from strictly
            // following the ordering in the corresponding spec language, which
            // specifies setting the confidence to "certain" at this point.
            throw new ReparseException();
        } catch (UnsupportedCharsetException e) {
            tokenizer.errTreeBuilder(
                    Encoding.msgBadInternalCharset(internalCharset));
            return false;
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
        tokenizer.becomeConfident();
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

    /**
     * Reports a warning without line/col
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected void errorWithoutLocation(String message) throws SAXException {
        ErrorHandler errorHandler = tokenizer.getErrorHandler();
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, null,
                tokenizer.getSystemId(), -1, -1);
        errorHandler.error(spe);
    }

    /**
     * Initializes a decoder from external decl.
     */
    protected Encoding encodingFromExternalDeclaration(String encoding)
            throws SAXException {
        if (encoding == null) {
            return null;
        }
        encoding = encoding.toLowerCase();
        try {
            Encoding cs = Encoding.forName(encoding);
            if ("utf-16be".equals(cs.getCanonName())
                    || "utf-16le".equals(cs.getCanonName())) {
                swallowBom = false;
            }
            return whineAboutEncodingAndReturnCanonical(encoding, cs);
        } catch (UnsupportedCharsetException e) {
            tokenizer.err(Encoding.msgBadEncoding(encoding) + " Will sniff.");
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
    protected Encoding whineAboutEncodingAndReturnCanonical(String encoding,
            Encoding cs) throws SAXException {
        String canonName = cs.getCanonName();
        if (!canonName.equals(encoding)) {
            tokenizer.err(Encoding.msgNotCanonicalName(encoding, canonName));
        }
        return cs;
    }

    private class ReparseException extends SAXException {

    }

    void notifyAboutMetaBoundary() {
        tokenizer.notifyAboutMetaBoundary();
    }

    /**
     * @param commentPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setCommentPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
        tokenizer.setCommentPolicy(commentPolicy);
    }

    /**
     * @param contentNonXmlCharPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentNonXmlCharPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentNonXmlCharPolicy(
            XmlViolationPolicy contentNonXmlCharPolicy) {
        tokenizer.setContentNonXmlCharPolicy(contentNonXmlCharPolicy);
    }

    /**
     * @param contentSpacePolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setContentSpacePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
        tokenizer.setContentSpacePolicy(contentSpacePolicy);
    }

    /**
     * @param eh
     * @see nu.validator.htmlparser.impl.Tokenizer#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler eh) {
        tokenizer.setErrorHandler(eh);
        for (int i = 0; i < characterHandlers.length; i++) {
            CharacterHandler ch = characterHandlers[i];
            if (ch instanceof NormalizationChecker) {
                NormalizationChecker nc = (NormalizationChecker) ch;
                nc.setErrorHandler(eh);
            }
        }
    }
    
    public void setTransitionHandler(TransitionHandler transitionHandler) {
        if (tokenizer instanceof ErrorReportingTokenizer) {
            ErrorReportingTokenizer ert = (ErrorReportingTokenizer) tokenizer;
            ert.setTransitionHandler(transitionHandler);
        } else if (transitionHandler != null) {
            throw new IllegalStateException("Attempt to set a transition handler on a plain tokenizer.");
        }
    }

    /**
     * @param mappingLangToXmlLang
     * @see nu.validator.htmlparser.impl.Tokenizer#setMappingLangToXmlLang(boolean)
     */
    public void setMappingLangToXmlLang(boolean mappingLangToXmlLang) {
        tokenizer.setMappingLangToXmlLang(mappingLangToXmlLang);
    }

    /**
     * @param namePolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setNamePolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setNamePolicy(XmlViolationPolicy namePolicy) {
        tokenizer.setNamePolicy(namePolicy);
    }

    /**
     * @param xmlnsPolicy
     * @see nu.validator.htmlparser.impl.Tokenizer#setXmlnsPolicy(nu.validator.htmlparser.common.XmlViolationPolicy)
     */
    public void setXmlnsPolicy(XmlViolationPolicy xmlnsPolicy) {
        tokenizer.setXmlnsPolicy(xmlnsPolicy);
    }

    public String getCharacterEncoding() throws SAXException {
        return characterEncoding == null ? null : characterEncoding.getCanonName();
    }

    public Locator getDocumentLocator() {
        return tokenizer;
    }
}
