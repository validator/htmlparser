package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class Characters extends CharBufferNode {

    public Characters(Locator locator, char[] buf, int start, int length) {
        super(locator, buf, start, length);
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.characters(buffer, 0, buffer.length, this);
    }

}
