package nu.validator.htmlparser.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import nu.validator.htmlparser.sax.HtmlParser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TreePrinter {

    public static void main(String[] args) throws SAXException, IOException {
        TreeDumpContentHandler treeDumpContentHandler = new TreeDumpContentHandler(new OutputStreamWriter(System.out, "UTF-8")); 
        HtmlParser htmlParser = new HtmlParser();
        htmlParser.setContentHandler(treeDumpContentHandler);
        File file = new File(args[0]);
        InputSource is = new InputSource(new FileInputStream(file));
        is.setSystemId(file.toURI().toASCIIString());
        htmlParser.parse(is);
    }
}
