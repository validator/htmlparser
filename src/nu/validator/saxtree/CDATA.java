package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class CDATA extends ParentNode {

    public CDATA(Locator locator) {
        super(locator);
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.startCDATA(this);
    }

    /**
     * @throws SAXException 
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endCDATA(endLocator);
    }

}
