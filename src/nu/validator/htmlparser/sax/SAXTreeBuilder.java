/*
 * Copyright (c) 2007 Henri Sivonen
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

import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;
import nu.validator.saxtree.Characters;
import nu.validator.saxtree.Comment;
import nu.validator.saxtree.DTD;
import nu.validator.saxtree.Document;
import nu.validator.saxtree.Element;
import nu.validator.saxtree.NodeType;
import nu.validator.saxtree.ParentNode;
import nu.validator.saxtree.TreeParser;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class SAXTreeBuilder extends TreeBuilder<Element> {

    private ContentHandler contentHandler;
    
    private LexicalHandler lexicalHandler;
    
    private Element rootElement;
    
    private Document document;
    
    public SAXTreeBuilder() {
        super(XmlViolationPolicy.ALLOW, false);
    }
    
    @Override
    protected void appendCommentToCurrentNode(char[] buf, int length) {
        currentNode().appendChild(new Comment(tokenizer, buf, 0, length));
    }

    @Override
    protected void appendCommentToDocument(char[] buf, int length) {
        document.appendChild(new Comment(tokenizer, buf, 0, length));
    }

    @Override
    protected void appendCommentToRootElement(char[] buf, int length) {
        rootElement.appendChild(new Comment(tokenizer, buf, 0, length));
    }

    @Override
    public boolean wantsComments() throws SAXException {
        return lexicalHandler != null;
    }

    @Override
    protected Element createElementAppendToCurrentAndPush(String name,
            Attributes attributes) {
        Element newElt = new Element(tokenizer, "http://www.w3.org/1999/xhtml", name, name, attributes, true, null);
        currentNode().appendChild(newElt);
        return newElt;
    }

    @Override
    protected void appendCharactersToCurrentNode(char[] buf, int start, int length) {
        currentNode().appendChild(new Characters(tokenizer, buf, start, length));
    }

    @Override
    protected void detachFromParent(Element element) {
        element.detach();
    }

    @Override
    protected boolean hasChildren(Element element) {
        return element.getFirstChild() != null;
    }

    @Override
    protected Element shallowClone(Element element) {
        Element newElt =  new Element(element, element.getUri(), element.getLocalName(), element.getQName(), element.getAttributes(), true, element.getPrefixMappings());
        newElt.copyEndLocator(element);
        return newElt;
    }

    @Override
    protected void detachFromParentAndAppendToNewParent(Element child, Element newParent) {
        newParent.appendChild(child);
    }

    @Override
    protected Element createHtmlElementSetAsRoot(Attributes attributes) {
        Element newElt = new Element(tokenizer, "http://www.w3.org/1999/xhtml", "html", "html", attributes, true, null);
        document.appendChild(newElt);
        rootElement = newElt;
        return newElt;
    }

    @Override
    protected void insertBefore(Element child, Element sibling, Element parent) {
        parent.insertBefore(child, sibling);
    }

    @Override
    protected Element parentElementFor(Element child) {
        ParentNode parent = child.getParentNode();
        if (parent == null) {
            return null;
        }
        if (parent.getNodeType() == NodeType.ELEMENT) {
            return (Element) parent;
        }
        return null;
    }

    @Override
    protected void addAttributesToElement(Element element, Attributes attributes) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#appendDoctypeToDocument(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    protected void appendDoctypeToDocument(String name, String publicIdentifier, String systemIdentifier) {
         DTD dtd = new DTD(tokenizer, name, publicIdentifier, systemIdentifier);
         dtd.setEndLocator(tokenizer);
         document.appendChild(dtd);
    }
    
    /**
     * Returns the document.
     * 
     * @return the document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * @throws SAXException 
     * @see nu.validator.htmlparser.TreeBuilder#end()
     */
    @Override
    protected void end() throws SAXException {
        document.setEndLocator(tokenizer);
        new TreeParser(contentHandler, lexicalHandler).parse(document);
    }

    /**
     * @see nu.validator.htmlparser.TreeBuilder#start()
     */
    @Override
    protected void start() {
        document = new Document(tokenizer);
    }

    /**
     * Sets the contentHandler.
     * 
     * @param contentHandler the contentHandler to set
     */
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    /**
     * Sets the lexicalHandler.
     * 
     * @param lexicalHandler the lexicalHandler to set
     */
    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        this.lexicalHandler = lexicalHandler;
    }

    @Override
    protected void appendChildrenToNewParent(Element oldParent, Element newParent) throws SAXException {
        newParent.appendChildren(oldParent);
    }

}
