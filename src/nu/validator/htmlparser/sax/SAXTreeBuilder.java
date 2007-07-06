package nu.validator.htmlparser.sax;

import nu.validator.htmlparser.TreeBuilder;
import nu.validator.saxtree.Characters;
import nu.validator.saxtree.Comment;
import nu.validator.saxtree.Document;
import nu.validator.saxtree.Element;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class SAXTreeBuilder extends TreeBuilder<Element> {

    private LexicalHandler lexicalHandler;
    
    private Element currentNode;
    
    private Element rootElement;
    
    private Document document;
    
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
        assert currentNode.getLocalName() == poppedElemenName;
        currentNode.setEndLocator(tokenizer);
        currentNode = newCurrentNode;
    }

    @Override
    protected void appendCharactersToCurrentNode(char[] buf, int start, int length) {
        currentNode.appendChild(new Characters(tokenizer, buf, start, length));
    }

}
