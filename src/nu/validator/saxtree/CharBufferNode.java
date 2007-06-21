package nu.validator.saxtree;

import org.xml.sax.Locator;

public abstract class CharBufferNode extends Node {

    protected final char[] buffer;
    
    CharBufferNode(Locator locator, char[] buf, int start, int length) {
        super(locator);
        this.buffer = new char[length];
        System.arraycopy(buf, start, buffer, 0, length);
    }
}
