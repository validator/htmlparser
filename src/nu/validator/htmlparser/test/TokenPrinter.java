package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.Writer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.TokenHandler;
import nu.validator.htmlparser.Tokenizer;

public class TokenPrinter implements TokenHandler {

    private Writer writer;
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        // TODO Auto-generated method stub

    }

    public void comment(String content) throws SAXException {
        try {
            writer.write('!');
            writer.write(content);
            writer.write('\n');
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void doctype(String name, boolean inError) throws SAXException {
        try {
            writer.write('D');
            writer.write(name);
            writer.write(' ');
            writer.write("" + inError);
            writer.write('\n');
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endTag(String name, Attributes attributes) throws SAXException {
        try {
            writer.write(')');
            writer.write(name);
            writer.write('\n');
            for (int i = 0; i < attributes.getLength(); i++) {
                writer.write('A');
                writer.write(attributes.getQName(i));
                writer.write(' ');
                writer.write(attributes.getValue(i));
                writer.write('\n');                
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void eof() throws SAXException {
        // TODO Auto-generated method stub

    }

    public void start(Tokenizer self) throws SAXException {
        // TODO Auto-generated method stub

    }

    public void startTag(String name, Attributes attributes)
            throws SAXException {
        try {
            writer.write('(');
            writer.write(name);
            writer.write('\n');
            for (int i = 0; i < attributes.getLength(); i++) {
                writer.write('A');
                writer.write(attributes.getQName(i));
                writer.write(' ');
                writer.write(attributes.getValue(i));
                writer.write('\n');                
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public boolean wantsComments() throws SAXException {
        return true;
    }

}
