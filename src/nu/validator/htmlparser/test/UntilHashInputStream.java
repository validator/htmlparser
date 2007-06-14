package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.InputStream;

public class UntilHashInputStream extends InputStream {

    private final StringBuilder builder = new StringBuilder();
    
    private final InputStream delegate;

    private boolean closed = false;

    /**
     * @param delegate
     */
    public UntilHashInputStream(final InputStream delegate) {
        this.delegate = delegate;
    }

    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        int rv = delegate.read();
        if (rv == 0x23 || rv == -1) {
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
