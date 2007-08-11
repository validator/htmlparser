package nu.validator.htmlparser.xom;

import nu.validator.htmlparser.DocumentMode;
import nu.xom.Document;
import nu.xom.Element;

public class ModalDocument extends Document {

    private DocumentMode mode = null;
    
    public ModalDocument(Document doc) {
        super(doc);
    }

    public ModalDocument(Element elt) {
        super(elt);
    }
    
}
