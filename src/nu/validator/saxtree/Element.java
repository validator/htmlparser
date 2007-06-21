package nu.validator.saxtree;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public final class Element extends ParentNode {

    private final String uri;

    private final String localName;

    private final String qName;

    private final Attributes atts;

    private final List<PrefixMapping> prefixMappings;

    public Element(Locator locator, String uri, String localName, String qName,
            Attributes atts, boolean retainAttributes,
            List<PrefixMapping> prefixMappings) {
        super(locator);
        this.uri = uri;
        this.localName = localName;
        this.qName = qName;
        if (retainAttributes) {
            this.atts = atts;
        } else {
            this.atts = new AttributesImpl(atts);
        }
        this.prefixMappings = prefixMappings;
    }

    @Override
    void visit(TreeParser treeParser) throws SAXException {
        if (prefixMappings != null) {
            for (PrefixMapping mapping : prefixMappings) {
                treeParser.startPrefixMapping(mapping.getPrefix(),
                        mapping.getUri(), this);
            }
        }
        treeParser.startElement(uri, localName, qName, atts, this);
    }

    /**
     * @throws SAXException
     * @see nu.validator.saxtree.Node#revisit(nu.validator.saxtree.TreeParser)
     */
    @Override
    void revisit(TreeParser treeParser) throws SAXException {
        treeParser.endElement(uri, localName, qName, endLocator);
        if (prefixMappings != null) {
            for (PrefixMapping mapping : prefixMappings) {
                treeParser.endPrefixMapping(mapping.getPrefix(), endLocator);
            }
        }
    }

}
