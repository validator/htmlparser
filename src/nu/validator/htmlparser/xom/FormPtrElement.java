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
