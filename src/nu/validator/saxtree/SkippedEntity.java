package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class SkippedEntity extends Node {

    private final String name;

    public SkippedEntity(Locator locator, String name) {
        super(locator);
        this.name = name;
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.skippedEntity(name, this);
    }
}
