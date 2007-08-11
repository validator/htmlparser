package nu.validator.htmlparser.xom;

import nu.xom.Element;

public class FormPtrElement extends Element {

    private Element form = null;
    
    /**
     * @param arg0
     */
    public FormPtrElement(Element elt) {
        super(elt);
        if (elt instanceof FormPtrElement) {
            FormPtrElement other = (FormPtrElement) elt;
            this.setForm(other.getForm());
        }
    }

    /**
     * @param arg0
     * @param arg1
     */
    public FormPtrElement(String arg0, String arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * Returns the form.
     * 
     * @return the form
     */
    public Element getForm() {
        return form;
    }

    /**
     * Sets the form.
     * 
     * @param form the form to set
     */
    public void setForm(Element form) {
        this.form = form;
    }

}
