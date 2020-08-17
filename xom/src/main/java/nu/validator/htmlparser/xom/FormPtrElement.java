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

/**
 * Element with an associated form.
 * 
 * @version $Id$
 * @author hsivonen
 */
public class FormPtrElement extends Element implements FormPointer {

    private Element form = null;
    
    /**
     * Copy constructor (<code>FormPointer</code>-aware).
     * @param elt
     */
    public FormPtrElement(Element elt) {
        super(elt);
        if (elt instanceof FormPointer) {
            FormPointer other = (FormPointer) elt;
            this.setForm(other.getForm());
        }
    }

    /**
     * Null form.
     * 
     * @param name
     * @param uri
     */
    public FormPtrElement(String name, String uri) {
        super(name, uri);
    }

    /**
     * Full constructor.
     * 
     * @param name
     * @param uri
     * @param form
     */
    public FormPtrElement(String name, String uri, Element form) {
        super(name, uri);
        this.form = form;
    }
    
    /**
     * Gets the form.
     * @see nu.validator.htmlparser.xom.FormPointer#getForm()
     */
    public Element getForm() {
        return form;
    }

    /**
     * Sets the form.
     * @see nu.validator.htmlparser.xom.FormPointer#setForm(nu.xom.Element)
     */
    public void setForm(Element form) {
        this.form = form;
    }

}
