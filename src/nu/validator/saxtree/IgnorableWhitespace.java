package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class IgnorableWhitespace extends CharBufferNode {

    public IgnorableWhitespace(Locator locator, char[] buf, int start, int length) {
        super(locator, buf, start, length);
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.ignorableWhitespace(buffer, 0, buffer.length, this);
    }

}
