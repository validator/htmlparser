package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.xml.sax.SAXException;

import nu.validator.htmlparser.HtmlInputStreamReader;
import nu.validator.htmlparser.MetaSniffer;

public class EncodingTester {

    private final InputStream aggregateStream;

    /**
     * @param aggregateStream
     */
    public EncodingTester(InputStream aggregateStream) {
        this.aggregateStream = aggregateStream;
    }

    private void runTests() throws IOException, SAXException {
        while (runTest())
            ;
    }

    private boolean runTest() throws IOException, SAXException {
        if (skipLabel()) {
            return false;
        }
        HtmlInputStreamReader reader = new HtmlInputStreamReader(new UntilHashInputStream(aggregateStream), null, null);
        Charset charset = reader.getCharset();
        if (skipLabel()) {
            return false;
        }
        
        return true;
    }

    private boolean skipLabel() throws IOException {
        int b = aggregateStream.read();
        if (b == -1) {
            return true;
        }
        for (;;) {
            b = aggregateStream.read();
            if (b == -1) {
                return true;
            } else if (b == 0x0A) {
                return false;
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
