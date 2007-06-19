package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class Document extends ParentNode {

    public Document(Locator locator) {
        super(locator);
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.startDocument(this);
    }

    /**
     * @throws SAXException 
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endDocument(endLocator);
    }

}
