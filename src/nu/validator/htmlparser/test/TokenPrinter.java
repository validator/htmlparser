package nu.validator.htmlparser.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fi.iki.hsivonen.xml.SystemErrErrorHandler;

import nu.validator.htmlparser.TokenHandler;
import nu.validator.htmlparser.Tokenizer;

public class TokenPrinter implements TokenHandler {

    private final Writer writer;
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        try {
        boolean lineStarted = true;
        writer.write('-');
        for (int i = start; i < start + length; i++) {
            if (!lineStarted) {
                writer.write("\n-");                
                lineStarted = true;
            }
            char c = buf[i];
            if (c == '\n') {
                writer.write("\\n");                                
                lineStarted = false;                
            } else {
                writer.write(c);                
            }
        }
        writer.write('\n');
        } catch (IOException e) {
            throw new SAXException(e);
        }
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

    public void doctype(String name, String publicIdentifier, String systemIdentifier, boolean inError) throws SAXException {
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
    try {
        writer.write("E\n");
        writer.flush();
        writer.close();
    } catch (IOException e) {
        throw new SAXException(e);
    }        
    }

    public void start(Tokenizer self) throws SAXException {

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

    public static void main(String[] args) throws SAXException, IOException {
        Tokenizer tokenizer = new Tokenizer(new TokenPrinter(new OutputStreamWriter(System.out, "UTF-8")));
        tokenizer.setErrorHandler(new SystemErrErrorHandler());
        File file = new File(args[0]);
        InputSource is = new InputSource(new FileInputStream(file));
        is.setSystemId(file.toURI().toASCIIString());
        tokenizer.tokenize(is);
    }

    /**
     * @param writer
     */
    public TokenPrinter(final Writer writer) {
        this.writer = writer;
    }
    
}
