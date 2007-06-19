package nu.validator.saxtree;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public final class Element extends ParentNode {

    private final String uri;
    private final String localName;
    private final String qName;
    private final Attributes atts;

    public Element(Locator locator, String uri, String localName, String qName, Attributes atts, boolean retainAttributes) {
        super(locator);
        this.uri = uri;
        this.localName = localName;
        this.qName = qName;
        if (retainAttributes) {
            this.atts = atts;
        } else {
            this.atts = new AttributesImpl(atts);
        }
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        treeParser.startElement(uri, localName, qName, atts, this);
    }

    /**
     * @throws SAXException 
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endElement(uri, localName, qName, endLocator);
    }

}
