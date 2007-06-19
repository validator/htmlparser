package nu.validator.htmlparser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface TokenHandler {
    public void start(Tokenizer self) throws SAXException;
    
    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean correct) throws SAXException;
    
    public void startTag(String name, Attributes attributes) throws SAXException;
    
    public void endTag(String name, Attributes attributes) throws SAXException;
    
    public void comment(String content) throws SAXException;
    
    public void characters(char[] buf, int start, int length) throws SAXException;
    
    public void eof() throws SAXException;
    
    public boolean wantsComments() throws SAXException;
}
