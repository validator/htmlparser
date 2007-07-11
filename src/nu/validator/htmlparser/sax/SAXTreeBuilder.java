package nu.validator.htmlparser.sax;

import nu.validator.htmlparser.TreeBuilder;
import nu.validator.htmlparser.XmlViolationPolicy;
import nu.validator.saxtree.Characters;
import nu.validator.saxtree.Comment;
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
    
    private Element currentNode;
    
    private Element rootElement;
    
    private Document document;
    
    public SAXTreeBuilder() {
        super(XmlViolationPolicy.ALLOW, false);
    }
    
    @Override
    protected void appendCommentToCurrentNode(char[] buf, int length) {
        currentNode.appendChild(new Comment(tokenizer, buf, 0, length));
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
        currentNode.appendChild(newElt);
        currentNode = newElt;
        return newElt;
    }

    @Override
    protected void elementPopped(String poppedElemenName, Element newCurrentNode) {
        assert poppedElemenName == null || currentNode.getLocalName() == poppedElemenName;
        currentNode.setEndLocator(tokenizer);
        currentNode = newCurrentNode;
    }

    @Override
    protected void appendCharactersToCurrentNode(char[] buf, int start, int length) {
        currentNode.appendChild(new Characters(tokenizer, buf, start, length));
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
    protected Element createHtmlElementSetAsRootAndPush(Attributes attributes) {
        Element newElt = new Element(tokenizer, "http://www.w3.org/1999/xhtml", "html", "html", attributes, true, null);
        document.appendChild(newElt);
        currentNode = newElt;
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

}
