package nu.validator.htmlparser.sax;

import java.io.IOException;

import nu.validator.htmlparser.Tokenizer;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class HtmlParser implements XMLReader {

    private Tokenizer tokenizer;
    
    private SAXTreeBuilder treeBuilder;
    
    public HtmlParser() {
        this.treeBuilder = new SAXTreeBuilder();
        this.tokenizer = new Tokenizer(treeBuilder);
    }
    
    public ContentHandler getContentHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public DTDHandler getDTDHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public EntityResolver getEntityResolver() {
        // TODO Auto-generated method stub
        return null;
    }

    public ErrorHandler getErrorHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    public void parse(InputSource input) throws IOException, SAXException {
        // TODO Auto-generated method stub
        
    }

    public void parse(String systemId) throws IOException, SAXException {
        // TODO Auto-generated method stub
        
    }

    public void setContentHandler(ContentHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void setDTDHandler(DTDHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void setEntityResolver(EntityResolver resolver) {
        // TODO Auto-generated method stub
        
    }

    public void setErrorHandler(ErrorHandler handler) {
        tokenizer.setErrorHandler(handler);
        treeBuilder.setErrorHandler(handler);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
    }

}
