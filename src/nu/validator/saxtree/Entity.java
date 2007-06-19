package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class Entity extends ParentNode {

    private final String name;

    public Entity(Locator locator, String name) {
        super(locator);
        this.name = name;
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.startEntity(name, this);
    }

    /**
     * @throws SAXException 
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endEntity(name, endLocator);
    }

}
