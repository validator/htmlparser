package nu.validator.saxtree;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class TreeParser implements Locator {
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;
    private Locator locator;
    
    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length, Locator locator) throws SAXException {
        contentHandler.characters(ch, start, length);
    }
    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument(Locator locator) throws SAXException {
        contentHandler.endDocument();
    }
    /**
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName, Locator locator) throws SAXException {
        contentHandler.endElement(uri, localName, qName);
    }
    /**
     * @param prefix
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix, Locator locator) throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }
    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length, Locator locator) throws SAXException {
        contentHandler.ignorableWhitespace(ch, start, length);
    }
    /**
     * @param target
     * @param data
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data, Locator locator) throws SAXException {
        contentHandler.processingInstruction(target, data);
    }
    /**
     * @param locator
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }
    /**
     * @param name
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name, Locator locator) throws SAXException {
        contentHandler.skippedEntity(name);
    }
    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument(Locator locator) throws SAXException {
        contentHandler.startDocument();
    }
    /**
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes atts, Locator locator) throws SAXException {
        contentHandler.startElement(uri, localName, qName, atts);
    }
    /**
     * @param prefix
     * @param uri
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri, Locator locator) throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }
    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
     */
    public void comment(char[] ch, int start, int length, Locator locator) throws SAXException {
        lexicalHandler.comment(ch, start, length);
    }
    /**
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#endCDATA()
     */
    public void endCDATA(Locator locator) throws SAXException {
        lexicalHandler.endCDATA();
    }
    /**
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD(Locator locator) throws SAXException {
        lexicalHandler.endDTD();
    }
    /**
     * @param name
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
     */
    public void endEntity(String name, Locator locator) throws SAXException {
        lexicalHandler.endEntity(name);
    }
    /**
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#startCDATA()
     */
    public void startCDATA(Locator locator) throws SAXException {
        lexicalHandler.startCDATA();
    }
    /**
     * @param name
     * @param publicId
     * @param systemId
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
     */
    public void startDTD(String name, String publicId, String systemId, Locator locator) throws SAXException {
        lexicalHandler.startDTD(name, publicId, systemId);
    }
    /**
     * @param name
     * @throws SAXException
     * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
     */
    public void startEntity(String name, Locator locator) throws SAXException {
        lexicalHandler.startEntity(name);
    }
    /**
     * @return
     * @see org.xml.sax.Locator#getColumnNumber()
     */
    public int getColumnNumber() {
        return locator.getColumnNumber();
    }
    /**
     * @return
     * @see org.xml.sax.Locator#getLineNumber()
     */
    public int getLineNumber() {
        return locator.getLineNumber();
    }
    /**
     * @return
     * @see org.xml.sax.Locator#getPublicId()
     */
    public String getPublicId() {
        return locator.getPublicId();
    }
    /**
     * @return
     * @see org.xml.sax.Locator#getSystemId()
     */
    public String getSystemId() {
        return locator.getSystemId();
    }
}
