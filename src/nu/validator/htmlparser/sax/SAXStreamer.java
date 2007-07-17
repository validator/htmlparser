package nu.validator.htmlparser.sax;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;

public class SAXStreamer extends TreeBuilder<Attributes>{

    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;

    SAXStreamer() {
        super(XmlViolationPolicy.FATAL, false);
    }
    
    @Override
    protected void addAttributesToElement(Attributes element, Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void appendCharacters(Attributes parent, char[] buf, int start, int length) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void appendChildrenToNewParent(Attributes oldParent, Attributes newParent) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void appendComment(Attributes parent, char[] buf, int start, int length) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void appendCommentToDocument(char[] buf, int start, int length) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected Attributes createElement(String name, Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Attributes createHtmlElementSetAsRoot(Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void detachFromParent(Attributes element) throws SAXException {
        fatal();
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(Attributes child, Attributes newParent) throws SAXException {
        fatal();
    }

    @Override
    protected boolean hasChildren(Attributes element) throws SAXException {
        return false;
    }

    @Override
    protected void insertBefore(Attributes child, Attributes sibling, Attributes parent) throws SAXException {
        fatal();
    }

    @Override
    protected void insertCharactersBefore(char[] buf, int start, int length, Attributes sibling, Attributes parent) throws SAXException {
        fatal();
    }

    @Override
    protected Attributes parentElementFor(Attributes child) throws SAXException {
        fatal();
        return null;
    }

    @Override
    protected Attributes shallowClone(Attributes element) throws SAXException {
        return element;
    }
    
    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
    }

    public void setLexicalHandler(LexicalHandler handler) {
        lexicalHandler = handler;
    }

}
