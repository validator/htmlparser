/*
 * Copyright (c) 2005, 2006 Henri Sivonen
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

package fi.iki.hsivonen.htmlparser;

import java.util.Arrays;

/**
 * @version $Id$
 * @author hsivonen
 */
public class AttributeInfo {
    private static final String[] BOOLEAN_ATTRIBUTES = { "active", "async",
            "autofocus", "autosubmit", "checked", "compact", "declare",
            "default", "defer", "disabled", "ismap", "multiple", "nohref",
            "noresize", "noshade", "nowrap", "readonly", "required", "selected" };

    private static final String[] CASE_FOLDED = { "active", "align", "async",
            "autocomplete", "autofocus", "autosubmit", "checked", "clear",
            "compact", "dataformatas", /* sic */
            "declare", "default", "defer", "dir", "disabled", "enctype",
            "frame", "ismap", "method", "multiple", "nohref", "noresize",
            "noshade", "nowrap", "readonly", "replace", "required", "rules",
            "scope", "scrolling", "selected", "shape", "step", "type",
            "valign", "valuetype" };

    public static boolean isBoolean(String name) {
        return Arrays.binarySearch(BOOLEAN_ATTRIBUTES, name) > -1;
    }

    public static boolean isCaseFolded(String name) {
        return Arrays.binarySearch(CASE_FOLDED, name) > -1;
    }

}
