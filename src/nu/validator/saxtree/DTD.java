package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class DTD extends ParentNode {

    private final String name;
    private final String publicIdentifier;
    private final String systemIdentifier;

    public DTD(Locator locator, String name, String publicIdentifier, String systemIdentifier) {
        super(locator);
        this.name = name;
        this.publicIdentifier = publicIdentifier;
        this.systemIdentifier = systemIdentifier;
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.startDTD(name, publicIdentifier, systemIdentifier, this);
    }

    /**
     * @throws SAXException 
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endDTD(endLocator);
    }

}
