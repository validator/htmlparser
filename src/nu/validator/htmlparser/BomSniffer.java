package nu.validator.htmlparser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class BomSniffer {
    
    private final ByteReadable source;

    /**
     * @param source
     */
    public BomSniffer(final ByteReadable source) {
        this.source = source;
    }
    
    CharsetDecoder sniff() throws IOException {
        int b = source.readByte();
        if (b == 0xEF) { // UTF-8
            b = source.readByte();
            if (b == 0xBB) {
                b = source.readByte();
                if (b == 0xBF) {
                    return Charset.forName("UTF-8").newDecoder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else if (b == 0xFF) { // little-endian
            b = source.readByte();
            if (b == 0xFE) {
                b = source.readByte();
                if (b == 0x00) {
                    b = source.readByte();
                    if (b == 0x00) {
                        // XXX What if an UTF-32 decoder is unavailable?
                        return Charset.forName("UTF-32LE").newDecoder();                   
                    } else {
                        return Charset.forName("UTF-16LE").newDecoder();
                    }                    
                } else {
                    return Charset.forName("UTF-16LE").newDecoder();
                }
            } else {
                return null;
            }
        } else if (b == 0xFE) { // big-endian UTF-16
            b = source.readByte();
            if (b == 0xFF) {
                return Charset.forName("UTF-16BE").newDecoder();        
            } else {
                return null;
            }
        } else if (b == 0x00) { // big-endian UTF-32
            b = source.readByte();
            if (b == 0x00) {
                b = source.readByte();
                if (b == 0xFE) {
                    b = source.readByte();
                    if (b == 0xFF) {
                        // XXX What if an UTF-32 decoder is unavailable?
                        return Charset.forName("UTF-32BE").newDecoder();                                           
                    } else {
                        return null;
                    }                    
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;            
        }
    }
    
}
