package nu.validator.htmlparser.test;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class TreeDumpContentHandler implements ContentHandler, LexicalHandler {

    private final Writer writer;
    
    private int level = 0;
    
    private boolean inCharacters = false;
    
    /**
     * @param writer
     */
    public TreeDumpContentHandler(final Writer writer) {
        this.writer = writer;
    }
    
    private void printLead() throws IOException {
        if (inCharacters) {
            writer.write("\"\n");
        }
        writer.write("| ");
        for (int i = 0; i < level; i++) {
            writer.write("  ");            
        }
    }
    
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        try {
            if (!inCharacters) {
                printLead();
                writer.write('"');
                inCharacters = true;
            }
            writer.write(ch, start, length);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }


    public void endElement(String uri, String localName, String qName) throws SAXException {
        level--;
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        try {
            printLead();
            writer.write('<');
            writer.write(qName);
            writer.write(">\n");
            level++;
            
            TreeMap<String, String> map = new TreeMap<String, String>();
            for (int i = 0; i < atts.getLength(); i++) {
                map.put(atts.getQName(i), atts.getValue(i));
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                printLead();
                writer.write(entry.getKey());
                writer.write("=\"");
                writer.write(entry.getValue());
                writer.write("\n\"");
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void comment(char[] ch, int offset, int len) throws SAXException {
        try {
            printLead();
            writer.write("<!--");
            writer.write(ch, offset, len);
            writer.write("-->");            
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    public void startDTD(String name, String publicIdentifier, String systemIdentifier) throws SAXException {
        try {
            printLead();
            writer.write("<!DOCTYPE ");
            writer.write(name);
            writer.write(">");            
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endDocument() throws SAXException {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void startEntity(String arg0) throws SAXException {
    }

    public void endCDATA() throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void endEntity(String arg0) throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public void startDocument() throws SAXException {
    }

}
