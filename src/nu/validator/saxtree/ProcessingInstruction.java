package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class ProcessingInstruction extends Node {

    private final String target;
    private final String data;

    public ProcessingInstruction(Locator locator, String target, String data) {
        super(locator);
        this.target = target;
        this.data = data;
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.processingInstruction(target, data, this);
    }

}
