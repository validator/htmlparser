package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.InputStream;

public class UntilHashInputStream extends InputStream {

    private final StringBuilder builder = new StringBuilder();
    
    private final InputStream delegate;

    private int buffer = -1;
    
    private boolean closed = false;

    /**
     * @param delegate
     * @throws IOException 
     */
    public UntilHashInputStream(final InputStream delegate) throws IOException {
        this.delegate = delegate;
        this.buffer = delegate.read();
    }

    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        int rv = buffer;
        buffer = delegate.read();
        if (buffer == '#' && rv == '\n') {
            // end of stream
            closed = true;
            return -1;
        } else {
            if (rv >= 0x20 && rv < 0x80) {
                builder.append(((char)rv));
            } else {
                builder.append("0x");
                builder.append(Integer.toHexString(rv));
            }
            return rv;
        }
    }

    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        super.close();
        if (closed) {
            return;
        }
        for (;;) {
            int b = delegate.read();
            if (b == 0x23 || b == -1) {
                break;
            }
        }
        closed = true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return builder.toString();
    }

}
