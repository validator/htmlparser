/*
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

package nu.validator.htmlparser.impl;

class SortStruct implements Comparable<SortStruct> {

    private final ElementName eltName;
    
    private final int magic;
    
    public int compareTo(SortStruct o) {
        if (this.magic == o.magic) {
            return 0;
        } else if (this.magic < o.magic) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SortStruct) {
            SortStruct oth = (SortStruct) obj;
            return this.magic == oth.magic;
        } else {
            return false;
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return magic;
    }

    /**
     * @param eltName
     */
    SortStruct(ElementName eltName) {
        this.eltName = eltName;
        this.magic = Tokenizer.stringToElementMagic(eltName.name);
    }

    /**
     * Returns the string.
     * 
     * @return the string
     */
    ElementName getEltName() {
        return eltName;
    }

    /**
     * Returns the magic.
     * 
     * @return the magic
     */
    int getMagic() {
        return magic;
    }
    
}