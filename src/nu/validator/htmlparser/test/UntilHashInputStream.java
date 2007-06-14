package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.InputStream;

public class UntilHashInputStream extends InputStream {

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
        if (rv == 0x23) {
            closed = true;
            return -1;
        } else {
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

}
