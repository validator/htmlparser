/*
 * Copyright (c) 2007 Henri Sivonen
 * Copyright (c) 2008 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.htmlparser.sax;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XmlSerializer implements ContentHandler, LexicalHandler {   
    
    private class PrefixMapping {
        public final String uri;
        public final String prefix;
        /**
         * @param uri
         * @param prefix
         */
        public PrefixMapping(String uri, String prefix) {
            this.uri = uri;
            this.prefix = prefix;
        }
        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override public boolean equals(Object obj) {
            if (obj instanceof PrefixMapping) {
                PrefixMapping other = (PrefixMapping) obj;
                return this.prefix.equals(other.prefix);
            } else {
                return false;
            }
        }
        /**
         * @see java.lang.Object#hashCode()
         */
        @Override public int hashCode() {
            return  prefix.hashCode();
        }
        
    }
    
    private class StackNode {
        public final String uri;
        public final String prefix;
        public final String qName;
        public final Set<PrefixMapping> mappings = new HashSet<PrefixMapping>();
        /**
         * @param uri
         * @param qName
         */
        public StackNode(String uri, String qName, String prefix) {
            this.uri = uri;
            this.qName = qName;
            this.prefix = prefix;
        } 
    }
    
    private final static Map<String, String> WELL_KNOWN_ATTRIBUTE_PREFIXES = new HashMap<String, String>();
    
    static {
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://www.w3.org/1999/xlink", "xlink");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://www.inkscape.org/namespaces/inkscape", "inkscape");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd", "sodipodi");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/AdobeSVGViewerExtensions/3.0/", "a");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/AdobeIllustrator/10.0/", "i");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("adobe:ns:meta/", "x");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/xap/1.0/", "xap");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/pdf/1.3/", "pdf");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/tiff/1.0/", "tiff");  
    }

    private final static Map<String, String> WELL_KNOWN_ELEMENT_PREFIXES = new HashMap<String, String>();
    
    static {
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://www.w3.org/1999/XSL/Transform", "xsl");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://purl.org/dc/elements/1.1/", "dc");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://www.ascc.net/xml/schematron", "sch");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://purl.oclc.org/dsdl/schematron", "sch");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://www.inkscape.org/namespaces/inkscape", "inkscape");
        WELL_KNOWN_ELEMENT_PREFIXES.put("http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd", "sodipodi");
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/AdobeSVGViewerExtensions/3.0/", "a");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/AdobeIllustrator/10.0/", "i");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("adobe:ns:meta/", "x");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/xap/1.0/", "xap");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/pdf/1.3/", "pdf");  
        WELL_KNOWN_ATTRIBUTE_PREFIXES.put("http://ns.adobe.com/tiff/1.0/", "tiff");  
    }
    
    private static Writer wrap(OutputStream out) {
        try {
            return new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // grows from head
    private final LinkedList<StackNode> stack = new LinkedList<StackNode>();
    
    private final Writer writer;

    private int counter = 0;

    public XmlSerializer(OutputStream out) {
        this(wrap(out));
    }

    public XmlSerializer(Writer out) {
        this.writer = out;
    }
    
    private void push(String uri, String local, String prefix) {
        stack.addFirst(new StackNode(uri, local, prefix));
    }

    private String pop() {
        String rv = stack.removeFirst().qName;
        stack.getFirst().mappings.clear();
        return rv;
    }

    private String lookupPrefixAttribute(String ns) {
        if ("http://www.w3.org/XML/1998/namespace".equals(ns)) {
            return "xml";
        }
        Set<String> hidden = new HashSet<String>();        
        for (StackNode node : stack) {
            for (PrefixMapping mapping : node.mappings) {
                if (mapping.prefix.length() != 0 && mapping.uri.equals(ns) && !hidden.contains(mapping.prefix)) {
                    return mapping.prefix;
                }
                hidden.add(mapping.prefix);
            }
        }
        return null;
    }
    
    private String lookupUri(String prefix) {
        for (StackNode node : stack) {
            for (PrefixMapping mapping : node.mappings) {
                if (mapping.prefix.equals(prefix)) {
                    return mapping.uri;
                }
            }
        }
        return null;
    }
    
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        try {
            for (int i = start; i < start + length; i++) {
                char c = ch[i];
                switch (c) {
                    case '<':
                        writer.write("&lt;");
                        break;
                    case '>':
                        writer.write("&gt;");
                        break;
                    case '&':
                        writer.write("&amp;");
                        break;
                    case '\r':
                        writer.write("&#xD;");
                        break;
                    case '\t':
                        writer.write('\t');
                        break;                        
                    case '\n':
                        writer.write('\n');
                        break;              
                    case '\uFFFE':
                        writer.write('\uFFFD');
                        break;              
                    case '\uFFFF':
                        writer.write('\uFFFD');
                        break;              
                    default:
                        if (c < ' ') {
                            writer.write('\uFFFD');                            
                        } else {
                            writer.write(c);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endDocument() throws SAXException {
        try {
            stack.clear();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
            try {
                writer.write('<');
                writer.write('/');
                writer.write(pop());
                writer.write('>');
            } catch (IOException e) {
                throw new SAXException(e);
            }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        characters(ch, start, length);
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        try {
            writer.write("<?");
            writer.write(target);
            writer.write(' ');
            boolean prevWasQuestionmark = false;
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                switch (c) {
                    case '?':
                        writer.write('?');
                        prevWasQuestionmark = true;
                        break;
                    case '>':
                        if (prevWasQuestionmark) {
                            writer.write(" >");                            
                        } else {
                            writer.write('>');
                        }
                        prevWasQuestionmark = false;
                        break;
                    case '\r':
                        writer.write("&#xD;");
                        prevWasQuestionmark = false;
                        break;
                    case '\t':
                        writer.write('\t');
                        prevWasQuestionmark = false;
                        break;                        
                    case '\n':
                        writer.write('\n');
                        prevWasQuestionmark = false;
                        break;              
                    case '\uFFFE':
                        writer.write('\uFFFD');
                        prevWasQuestionmark = false;
                        break;              
                    case '\uFFFF':
                        writer.write('\uFFFD');
                        prevWasQuestionmark = false;
                        break;              
                    default:
                        if (c < ' ') {
                            writer.write('\uFFFD');                            
                        } else {
                            writer.write(c);
                        }
                    prevWasQuestionmark = false;
                        break;
                }
            }
            if (prevWasQuestionmark) {
                writer.write(' ');                
            }
            writer.write("?>");
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
        try {
            writer.write("<?xml version='1.0' encoding='utf-8'?>\n");
        } catch (IOException e) {
            throw new SAXException(e);
        }
        stack.clear();
        counter = 0;
        push(null, null, null);
    }

    public void startElement(String uri, String localName, String q,
            Attributes atts) throws SAXException {
        String prefix;
        String qName;
        if (uri.length() == 0) {
            prefix = "";
            qName = localName;
            // generate xmlns
            startPrefixMappingPrivate(prefix, uri);
        } else {
            prefix = WELL_KNOWN_ELEMENT_PREFIXES.get(uri);
            if (prefix == null) {
                prefix = "";
            }
            String lookup = lookupUri(prefix);
            if (lookup != null && !lookup.equals(uri)) {
                prefix = "";
            }
            startPrefixMappingPrivate(prefix, uri);
            if (prefix.length() == 0) {
                qName = localName;
            } else {
                qName = prefix + ':' + localName;
            }
        }
        
        int attLen = atts.getLength();
        for (int i = 0; i < attLen; i++) {
            String attUri = atts.getURI(i);
            if (attUri.length() == 0 || "http://www.w3.org/XML/1998/namespace".equals(attUri)) {
                continue;
            }
            if (lookupPrefixAttribute(attUri) == null) {
                generatePrefix(attUri);
            }
        }
        
        try {
            writer.write('<');
            writer.write(qName);
            for (PrefixMapping mapping : stack.getFirst().mappings) {
                writer.write(' ');
                if (mapping.prefix.length() == 0) {
                    writer.write("xmlns");                    
                } else {
                    writer.write("xmlns:");
                    writer.write(mapping.prefix);                    
                }
                writer.write('=');
                writer.write('"');
                writer.write(mapping.uri);
                writer.write('"');
            }
            
            for (int i = 0; i < attLen; i++) {
                writer.write(' ');
                String attUri = atts.getURI(i);
                if (attUri.length() != 0) {
                    writer.write(lookupPrefixAttribute(attUri));
                    writer.write(':');
                }
                writer.write(atts.getLocalName(i));
                writer.write('=');
                writer.write('"');
                String val = atts.getValue(i);
                for (int j = 0; j < val.length(); j++) {
                    char c = val.charAt(j);
                    switch (c) {
                        case '<':
                            writer.write("&lt;");
                            break;
                        case '>':
                            writer.write("&gt;");
                            break;
                        case '&':
                            writer.write("&amp;");
                            break;
                        case '"':
                            writer.write("&quot;");
                            break;
                        case '\r':
                            writer.write("&#xD;");
                            break;
                        case '\t':
                            writer.write('\t');
                            break;                        
                        case '\n':
                            writer.write('\n');
                            break;              
                        case '\uFFFE':
                            writer.write('\uFFFD');
                            break;              
                        case '\uFFFF':
                            writer.write('\uFFFD');
                            break;              
                        default:
                            if (c < ' ') {
                                writer.write('\uFFFD');                            
                            } else {
                                writer.write(c);
                            }
                            break;
                    }
                }
                writer.write('"');
            }
            writer.write('>');
        } catch (IOException e) {
            throw new SAXException(e);
        }
        push(uri, qName, prefix);
    }

    private void generatePrefix(String uri) throws SAXException {
        String candidate = WELL_KNOWN_ATTRIBUTE_PREFIXES.get(uri);
        if (candidate == null) {
            candidate = "p" + (counter++);
        }
        while (lookupUri(candidate) != null) {
            candidate = "p" + (counter++);            
        }
        startPrefixMappingPrivate(candidate, uri);
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        try {
            boolean prevWasHyphen = false;
            writer.write("<!--");
            for (int i = start; i < start + length; i++) {
                char c = ch[i];
                switch (c) {
                    case '-':
                        if (prevWasHyphen) {
                            writer.write(" -");                            
                        } else {
                            writer.write('-');
                            prevWasHyphen = true;
                        }
                        break;
                    case '\r':
                        writer.write("&#xD;");
                        prevWasHyphen = false;
                        break;
                    case '\t':
                        writer.write('\t');
                        prevWasHyphen = false;
                        break;                        
                    case '\n':
                        writer.write('\n');
                        prevWasHyphen = false;
                        break;              
                    case '\uFFFE':
                        writer.write('\uFFFD');
                        prevWasHyphen = false;
                        break;              
                    case '\uFFFF':
                        writer.write('\uFFFD');
                        prevWasHyphen = false;
                        break;              
                    default:
                        if (c < ' ') {
                            writer.write('\uFFFD');                            
                        } else {
                            writer.write(c);
                        }
                    prevWasHyphen = false;
                        break;
                }
            }
            if (prevWasHyphen) {
                writer.write(' ');                
            }
            writer.write("-->");
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endCDATA() throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        if (prefix.length() == 0 || uri.equals(lookupUri(prefix))) {
            return;
        }
        stack.getFirst().mappings.add(new PrefixMapping(uri, prefix));
    }

    public void startPrefixMappingPrivate(String prefix, String uri)
            throws SAXException {
        if (uri.equals(lookupUri(prefix))) {
            return;
        }
        stack.getFirst().mappings.add(new PrefixMapping(uri, prefix));
    }
    
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

}
