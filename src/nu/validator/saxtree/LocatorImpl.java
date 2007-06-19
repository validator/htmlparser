package nu.validator.saxtree;

import org.xml.sax.Locator;

public final class LocatorImpl implements Locator {

    private final String systemId;
    private final String publicId;
    private final int column;
    private final int line;
    
    public LocatorImpl(Locator locator) {
        this.systemId = locator.getSystemId();
        this.publicId = locator.getPublicId();
        this.column = locator.getColumnNumber();
        this.line = locator.getLineNumber();
    }
    
    public int getColumnNumber() {
        return column;
    }

    public int getLineNumber() {
        return line;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }
}
