package nu.validator.saxtree;

import org.xml.sax.Locator;

public abstract class ParentNode extends Node {

    protected Locator endLocator;
    
    private Node firstChild = null;
    
    private Node lastChild = null;
    
    ParentNode(Locator locator) {
        super(locator);
    }

    /**
     * Sets the endLocator.
     * 
     * @param endLocator the endLocator to set
     */
    public void setEndLocator(Locator endLocator) {
        this.endLocator = new LocatorImpl(endLocator);
    }

    /**
     * Returns the firstChild.
     * 
     * @return the firstChild
     */
    public final Node getFirstChild() {
        return firstChild;
    }

    /**
     * Sets the firstChild.
     * 
     * @param firstChild the firstChild to set
     */
    void setFirstChild(Node firstChild) {
        this.firstChild = firstChild;
    }

    /**
     * Returns the lastChild.
     * 
     * @return the lastChild
     */
    public final Node getLastChild() {
        return lastChild;
    }

    /**
     * Sets the lastChild.
     * 
     * @param lastChild the lastChild to set
     */
    void setLastChild(Node lastChild) {
        this.lastChild = lastChild;
    }

    public void appendChild(Node child) {
        if (firstChild == null) {
            firstChild = child;
            lastChild = child;
        } else {
            lastChild.setNextSibling(child);
            lastChild = child;
        }
    }
}
