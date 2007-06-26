package nu.validator.htmlparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class HtmlInputStreamReader extends Reader implements ByteReadable,
        Locator {

    private static final int SNIFFING_LIMIT = 512;

    private final InputStream inputStream;

    private final ErrorHandler errorHandler;

    private final Locator locator;

    private CharsetDecoder decoder = null;

    private boolean sniffing = true;

    private int limit = 0;

    private int position = 0;

    private int bytesRead = 0;

    private boolean eofSeen = false;

    private boolean shouldReadBytes = false;

    private boolean charsetBoundaryPassed = false;

    private final byte[] byteArray = new byte[4096]; // Length must be >=

    // SNIFFING_LIMIT

    private final ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);

    private boolean needToNotifyTokenizer = false;

    private boolean flushing = false;

    private int line = -1;

    private int col = -1;

    private int lineColPos;

    /**
     * @param inputStream
     * @param errorHandler
     * @param locator
     * @throws IOException
     * @throws SAXException
     */
    public HtmlInputStreamReader(InputStream inputStream,
            ErrorHandler errorHandler, Locator locator) throws SAXException,
            IOException {
        this.inputStream = inputStream;
        this.errorHandler = errorHandler;
        this.locator = locator;
        this.sniffing = true;
        this.decoder = (new BomSniffer(this)).sniff();
        if (this.decoder == null) {
            position = 0;
            this.decoder = (new MetaSniffer(this, errorHandler, this)).sniff();
            sniffing = false;
            // TODO chardet
            if (this.decoder == null) {
                err("Could not determine the character encoding of the document. Using \u201CWindows-1252\u201D.");
                this.decoder = Charset.forName("Windows-1252").newDecoder();
            }
        }
        sniffing = false;
        position = 0;
        bytesRead = 0;
        byteBuffer.position(position);
        byteBuffer.limit(limit);
        initDecoder();
    }

    /**
     * 
     */
    private void initDecoder() {
        if ("ISO-8859-1".equals(this.decoder.charset().name())) {
            this.decoder = Charset.forName("Windows-1252").newDecoder(); 
        }
        this.decoder.onMalformedInput(CodingErrorAction.REPORT);
        this.decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    public HtmlInputStreamReader(InputStream inputStream,
            ErrorHandler errorHandler, Locator locator, CharsetDecoder decoder) throws SAXException,
            IOException {
        this.inputStream = inputStream;
        this.errorHandler = errorHandler;
        this.locator = locator;
        this.decoder = decoder;
        this.sniffing = false;
        position = 0;
        bytesRead = 0;
        byteBuffer.position(0);
        byteBuffer.limit(0);
        shouldReadBytes = true;
        initDecoder();
    }

    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        inputStream.close();
    }

    @Override
    public int read(char[] charArray, int offset, int length)
            throws IOException {
        lineColPos = 0;
        if (sniffing) {
            throw new IllegalStateException(
                    "read() called when in the sniffing state.");
        }
        if (offset != 0) {
            // Deal only with buffers that start at zero
            throw new IllegalArgumentException("Offset was not zero.");
        }
        if (length < 2) {
            // Deal only with buffers that can take a surrogate
            throw new IllegalArgumentException("Length less than two.");
        }
        if (needToNotifyTokenizer) {
            // TODO NOTIFY
            needToNotifyTokenizer = false;
        }
        CharBuffer charBuffer = CharBuffer.wrap(charArray);
        charBuffer.limit(length);
        charBuffer.position(0);
        if (flushing) {
            decoder.flush(charBuffer);
            // return -1 if zero
            int cPos = charBuffer.position();
            return cPos == 0 ? -1 : cPos;                    
        }
        if (shouldReadBytes) {
            int oldLimit = byteBuffer.limit();
            int readLen;
            if (charsetBoundaryPassed) {
                readLen = byteArray.length - oldLimit;
            } else {
                readLen = SNIFFING_LIMIT - oldLimit;
            }
            int num = inputStream.read(byteArray, oldLimit, readLen);
            if (num == -1) {
                eofSeen = true;
                inputStream.close();
            } else {
                byteBuffer.position(0);
                byteBuffer.limit(oldLimit + num);
            }
            shouldReadBytes = false;
        }
        boolean finalDecode = false;
        for (;;) {
            int oldBytePos = byteBuffer.position();
            CoderResult cr = decoder.decode(byteBuffer, charBuffer, finalDecode);
            bytesRead += byteBuffer.position() - oldBytePos;
            if (cr == CoderResult.OVERFLOW) {
                // Decoder will remember surrogates
                return charBuffer.position();
            } else if (cr == CoderResult.UNDERFLOW) {
                // If the buffer was not fully consumed, there may be an
                // incomplete byte sequence that needs to seed the next
                // buffer.
                int remaining = byteBuffer.remaining();
                if (remaining > 0) {
                    System.arraycopy(byteArray, byteBuffer.position(),
                            byteArray, 0, remaining);
                }
                byteBuffer.position(0);
                byteBuffer.limit(remaining);
                if (!charsetBoundaryPassed) {
                    if (bytesRead + remaining >= SNIFFING_LIMIT) {
                        needToNotifyTokenizer = true;
                    }
                }
                if (flushing) {
                    // The final decode was successful. Not sure if this ever happens. 
                    // Let's get out in any case.
                    int cPos = charBuffer.position();
                    return cPos == 0 ? -1 : cPos;                    
                } else if (eofSeen) {
                    // If there's something left, it isn't something that would be 
                    // consumed in the middle of the stream. Rerun the loop once 
                    // in the final mode.
                    shouldReadBytes = false;
                    finalDecode = true;
                    flushing  = true;
                    continue;
                } else {
                    // The usual stuff. Want more bytes next time.
                    shouldReadBytes = true;
                    return charBuffer.position();
                }
            } else {
                // The result is in error. No need to test.
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cr.length(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append('\u201C');
                    sb.append(Integer.toHexString(byteBuffer.get() & 0xFF));
                    bytesRead++;
                    sb.append('\u201D');
                }
                charBuffer.put('\uFFFD');
                calculateLineAndCol(charBuffer);
                if (cr.isMalformed()) {
                    err("Malformed byte sequence: " + sb + ".");
                } else if (cr.isUnmappable()) {
                    err("Unmappable byte sequence: " + sb + ".");
                } else {
                    throw new RuntimeException(
                            "CoderResult was none of overflow, underflow, malformed or unmappable.");
                }
                if (finalDecode) {
                    // These were the last bytes of input. Return without relooping.
                    return charBuffer.position();
                }
            }
        }
    }

    private void calculateLineAndCol(CharBuffer charBuffer) {
        if (locator != null) {
            line = locator.getLineNumber();
            col = locator.getColumnNumber();
            char[] charArray = charBuffer.array();
            boolean prevWasCR = false;
            int i;
            for (i = lineColPos; i < charBuffer.position(); i++) {
                switch (charArray[i]) {
                    case '\n': // LF
                        if (!prevWasCR) {
                            line++;
                            col = 0;
                        }
                        prevWasCR = false;
                        break;
                    case '\r': // CR
                        line++;
                        col = 0;
                        prevWasCR = true;
                        break;
                    default:
                        col++;
                        prevWasCR = false;
                        break;
                }
            }
            lineColPos = i;
        }
    }

    public int readByte() throws IOException {
        if (!sniffing) {
            throw new IllegalStateException(
                    "readByte() called when not in the sniffing state.");
        }
        if (position == SNIFFING_LIMIT) {
            return -1;
        } else if (position < limit) {
            return byteArray[position++] & 0xFF;
        } else {
            int num = inputStream.read(byteArray, limit, SNIFFING_LIMIT - limit);
            if (num == -1) {
                return -1;
            } else {
                limit += num;
                return byteArray[position++] & 0xFF;
            }
        }
    }

    public static void main(String[] args) {
        CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPORT);
        dec.onUnmappableCharacter(CodingErrorAction.REPORT);
        byte[] bytes = { (byte) 0xF0, (byte) 0x9D, (byte) 0x80, (byte) 0x80 };
        byte[] bytes2 = { (byte) 0xB8, (byte) 0x80, 0x63, 0x64, 0x65 };
        ByteBuffer byteBuf = ByteBuffer.wrap(bytes);
        ByteBuffer byteBuf2 = ByteBuffer.wrap(bytes2);
        char[] chars = new char[1];
        CharBuffer charBuf = CharBuffer.wrap(chars);

        CoderResult cr = dec.decode(byteBuf, charBuf, false);
        System.out.println(cr);
        System.out.println(byteBuf);
        // byteBuf.get();
        cr = dec.decode(byteBuf2, charBuf, false);
        System.out.println(cr);
        System.out.println(byteBuf2);

    }

    public int getColumnNumber() {
        if (locator != null) {
            return col;
        }
        return -1;
    }

    public int getLineNumber() {
        if (locator != null) {
            return line;
        }
        return -1;
    }

    public String getPublicId() {
        if (locator != null) {
            return locator.getPublicId();
        }
        return null;
    }

    public String getSystemId() {
        if (locator != null) {
            return locator.getSystemId();
        }
        return null;
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void err(String message) throws IOException {
        // TODO remove wrapping when changing read() to take a CharBuffer
        try {
            if (errorHandler != null) {
                SAXParseException spe = new SAXParseException(message, this);
                errorHandler.error(spe);
            }
        } catch (SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * @param string
     * @throws SAXException
     */
    private void warn(String message) throws IOException {
        try {
            if (errorHandler != null) {
                SAXParseException spe = new SAXParseException(message, this);
                errorHandler.warning(spe);
            }
        } catch (SAXException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }
    
    public Charset getCharset() {
        return decoder.charset();
    }

}
