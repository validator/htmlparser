package nu.validator.saxtree;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public abstract class Node implements Locator {

    private final String systemId;
    private final String publicId;
    private final int column;
    private final int line;
    
    private Node nextSibling = null;
    
    private ParentNode parentNode = null;

    Node(Locator locator) {
        this.systemId = locator.getSystemId();
        this.publicId = locator.getPublicId();
        this.column = locator.getColumnNumber();
        this.line = locator.getLineNumber();
    }
    
    public int getColumnNumber() {
        return column;
    }

    public int getLineNumber() {
        return line;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    abstract void visit(TreeParser treeParser) throws SAXException;
    
    void revisit(TreeParser treeParser) throws SAXException {
        return;
    }
    
    public Node getFirstChild() {
        return null;
    }

    /**
     * Returns the nextSibling.
     * 
     * @return the nextSibling
     */
    public final Node getNextSibling() {
        return nextSibling;
    }

    /**
     * Sets the nextSibling.
     * 
     * @param nextSibling the nextSibling to set
     */
    void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }
    
    
    /**
     * Returns the parentNode.
     * 
     * @return the parentNode
     */
    public final ParentNode getParentNode() {
        return parentNode;
    }

    /**
     * Sets the parentNode.
     * 
     * @param parentNode the parentNode to set
     */
    void setParentNode(ParentNode parentNode) {
        this.parentNode = parentNode;
    }
}
