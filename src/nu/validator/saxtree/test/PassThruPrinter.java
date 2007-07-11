package nu.validator.saxtree.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import nu.validator.saxtree.Node;
import nu.validator.saxtree.TreeBuilder;
import nu.validator.saxtree.TreeParser;

import org.apache.xml.serializer.Method;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

public class PassThruPrinter {
    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();

        TreeBuilder treeBuilder = new TreeBuilder();
        reader.setContentHandler(treeBuilder);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", treeBuilder);
        
        File file = new File(args[0]);
        InputSource is = new InputSource(new FileInputStream(file));
        is.setSystemId(file.toURI().toASCIIString());
        reader.parse(is);
        
        Node doc = treeBuilder.getRoot();
        
        Properties props = OutputPropertiesFactory.getDefaultMethodProperties(Method.XML);
        Serializer ser = SerializerFactory.getSerializer(props);
        ser.setOutputStream(System.out);
        ContentHandler xmlSerializer = ser.asContentHandler();
        
        TreeParser treeParser = new TreeParser(xmlSerializer, (LexicalHandler) xmlSerializer);
        treeParser.parse(doc);
    }

}
