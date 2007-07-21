package nu.validator.htmlparser.sax;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import nu.validator.htmlparser.AttributesImpl;
import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;

public class SAXStreamer extends TreeBuilder<Attributes>{

    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;
    private int depth;
    
    SAXStreamer() {
        super(XmlViolationPolicy.FATAL, false);
    }
    
    @Override
    protected void addAttributesToElement(Attributes element, Attributes attributes) throws SAXException {
        Attributes existingAttrs = element;
        for (int i = 0; i < attributes.getLength(); i++) {
            String qName = attributes.getQName(i);
            if (existingAttrs.getIndex(qName) < 0) {
                fatal();
            }
        }
    }

    @Override
    protected void appendCharacters(Attributes parent, char[] buf, int start, int length) throws SAXException {
        contentHandler.characters(buf, start, length);
    }

    @Override
    protected void appendChildrenToNewParent(Attributes oldParent, Attributes newParent) throws SAXException {
        fatal();
    }

    @Override
    protected void appendComment(Attributes parent, char[] buf, int start, int length) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.comment(buf, start, length);
        }
    }

    @Override
    protected void appendCommentToDocument(char[] buf, int start, int length)
            throws SAXException {
        if (lexicalHandler != null) {
            if (depth == 0) {
                lexicalHandler.comment(buf, start, length);
            } else {
                fatal();
            }
        }
    }

    @Override
    protected Attributes createElement(String name, Attributes attributes) throws SAXException {
        return attributes;
    }

    @Override
    protected Attributes createHtmlElementSetAsRoot(Attributes attributes) throws SAXException {
        return attributes;
    }

    @Override
    protected void detachFromParent(Attributes element) throws SAXException {
        fatal();
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(Attributes child, Attributes newParent) throws SAXException {
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
        throw new RuntimeException("Unreachable");
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

    /**
     * @see nu.validator.htmlparser.TreeBuilder#appendDoctypeToDocument(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    protected void appendDoctypeToDocument(String name, String publicIdentifier, String systemIdentifier) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startDTD(name, publicIdentifier, systemIdentifier);
            lexicalHandler.endDTD();
        }
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#bodyClosed(java.lang.Object)
     */
    @Override
    protected void bodyClosed(Attributes body) throws SAXException {
        contentHandler.endElement("http://www.w3.org/1999/xhtml", "body", "body");
        depth--;
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#elementPopped(java.lang.String, java.lang.Object)
     */
    @Override
    protected void elementPopped(String name, Attributes node) throws SAXException {
        contentHandler.endElement("http://www.w3.org/1999/xhtml", name, name);
        depth--;        
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#elementPushed(java.lang.String, java.lang.Object)
     */
    @Override
    protected void elementPushed(String name, Attributes node) throws SAXException {
        if (depth == 0) {
            contentHandler.startPrefixMapping("", "http://www.w3.org/1999/xhtml");
        }
        contentHandler.startElement("http://www.w3.org/1999/xhtml", name, name, node);
        depth++;
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#end()
     */
    @Override
    protected void end() throws SAXException {
        contentHandler.endDocument();
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#htmlClosed(java.lang.Object)
     */
    @Override
    protected void htmlClosed(Attributes html) throws SAXException {
        contentHandler.endElement("http://www.w3.org/1999/xhtml", "html", "html");
        contentHandler.endPrefixMapping("");
        depth--;
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#start()
     */
    @Override
    protected void start(boolean fragment) throws SAXException {
        contentHandler.setDocumentLocator(tokenizer);
        if (fragment) {
            depth = 1;
        } else {
            depth = 0;
            contentHandler.startDocument();
        }
    }

}
