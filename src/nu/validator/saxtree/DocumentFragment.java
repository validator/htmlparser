package nu.validator.saxtree;

import org.xml.sax.helpers.LocatorImpl;

public final class DocumentFragment extends ParentNode {

    public DocumentFragment() {
        super(new LocatorImpl());
    }

    @Override
    void visit(TreeParser treeParser) {
        // nothing
    }
}
