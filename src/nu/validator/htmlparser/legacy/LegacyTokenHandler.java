package nu.validator.htmlparser.legacy;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.TokenHandler;
import nu.validator.htmlparser.Tokenizer;

public class LegacyTokenHandler implements TokenHandler {

    private ContentHandler contentHandler;
    
    private Tokenizer tokenizer;
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        contentHandler.characters(buf, start, length);
    }

    public void comment(char[] buf, int length) throws SAXException {

    }

    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean inError) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void endTag(String name, Attributes attributes) throws SAXException {
        contentHandler.endElement("http://www.w3.org/1999/xhtml", name, name);
    }

    public void eof() throws SAXException {
        // TODO flush tag inference
        contentHandler.endDocument();
    }

    public void start(Tokenizer self) throws SAXException {
        tokenizer = self;
        contentHandler.setDocumentLocator(self);
        contentHandler.startDocument();
    }

    public void startTag(String name, Attributes attributes)
            throws SAXException {
        contentHandler.startElement("http://www.w3.org/1999/xhtml", name, name, attributes);
    }

    public boolean wantsComments() throws SAXException {
        return false;
    }

    /**
     * @param contentHandler
     */
    public LegacyTokenHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public void setNonWhiteSpaceAllowed(boolean b) {
        // TODO Auto-generated method stub
        
    }

}
