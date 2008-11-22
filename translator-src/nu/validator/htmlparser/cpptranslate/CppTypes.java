/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is HTML Parser C++ Translator code.
 *
 * The Initial Developer of the Original Code is
 * Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Henri Sivonen <hsivonen@iki.fi>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package nu.validator.htmlparser.cpptranslate;

import java.util.Map;

public class CppTypes {

    private static final String[] INCLUDES = {
        "prtypes",
        "nsIAtom",
        "nsString",
        "nsINameSpaceManager",
        "nsIContent",
        "nsIDocument",
        "nsGkAtoms",
        "jArray",
        "nsHtml5DocumentMode",
        "nsHtml5ArrayCopy",
        "nsHtml5NamedCharacters",
        "nsHtml5Parser",
        "nsHtml5StringLiterals",
    };
    
    private static final String[] NAMED_CHARACTERS_INCLUDES = {
        "prtypes",
        "jArray",
    };

    private static final String[] FORWARD_DECLARATIONS = {
        "nsHtml5Parser",
    };
    
    private final Map<String, String> atomMap;
    
    private final Map<String, String> stringMap;

    public CppTypes(Map<String, String> atomMap, Map<String, String> stringMap) {
        this.atomMap = atomMap;
        this.stringMap = stringMap;
    }

    public String classPrefix() {
        return "nsHtml5";
    }    

    public String booleanType() {
        return "PRBool";
    }

    public String charType() {
        return "PRUnichar";
    }

    public String intType() {
        return "PRInt32";
    }
    
    public String stringType() {
        return "nsString*";
    }

    public String localType() {
        return "nsIAtom*";
    }
    
    public String prefixType() {
        return "nsIAtom*";
    }

    public String nsUriType() {
        return "PRInt32";
    }

    public String falseLiteral() {
        return "PR_FALSE";
    }

    public String trueLiteral() {
        return "PR_TRUE";
    }

    public String nullLiteral() {
        return "nsnull";
    }

    public String encodingDeclarationHandlerType() {
        return "nsHtml5Parser*";
    }

    public String nodeType() {
        return "nsIContent*";
    }

    public String xhtmlNamespaceLiteral() {
        return "kNameSpaceID_XHTML";
    }

    public String svgNamespaceLiteral() {
        return "kNameSpaceID_SVG";
    }

    public String xmlnsNamespaceLiteral() {
        return "kNameSpaceID_XMLNS";
    }

    public String xmlNamespaceLiteral() {
        return "kNameSpaceID_XML";
    }
    
    public String noNamespaceLiteral() {
        return "kNameSpaceID_None";
    }

    public String xlinkNamespaceLiteral() {
        return "kNameSpaceID_XLink";
    }

    public String mathmlNamespaceLiteral() {
        return "kNameSpaceID_MathML";
    }
    
    public String arrayTemplate() {
        return "jArray";
    }
    
    public String localForLiteral(String literal) {
        String atom = atomMap.get(literal);
        if (atom == null) {
            atom = createAtomName(literal);
            atomMap.put(literal, atom);
            System.err.println("MISSING ATOM: GK_ATOM("+ atom + ", \"" + literal + "\")");
        }
        return "nsGkAtoms::" + atom;
    }
    
    private String createAtomName(String literal) {
        String candidate = literal.replaceAll("[^a-zA-Z0-9_]", "_");
        while (atomMap.values().contains(candidate)) {
            candidate = candidate + '_';
        }
        return candidate;
    }

    public String stringForLiteral(String literal) {
        String str = stringMap.get(literal);
        if (str == null) {
            str = createLiteralName(literal);
            stringMap.put(literal, str);
            System.err.println("MISSING STRING:  (" + str + " = new nsString())->Assign(NS_LITERAL_STRING(\"" + literal + "\"));");
        }
        return "nsHtml5StringLiterals::" + str;
    }

    private String createLiteralName(String literal) {
        String candidate = literal.replaceAll("[^a-zA-Z0-9_]", "_");
        while (stringMap.values().contains(candidate)) {
            candidate = candidate + '_';
        }
        return candidate;
    }

    public String staticArrayMacro() {
        return "J_ARRAY_STATIC";
    }
    
    public String[] boilerplateIncludes() {
        return INCLUDES;
    }

    public String[] namedCharactersIncludes() {
        return NAMED_CHARACTERS_INCLUDES;
    }
    
    public String[] boilerplateForwardDeclarations() {
        return FORWARD_DECLARATIONS;
    }
    
    public String treeBuiderHSupplement() {
        return "nsHtml5TreeBuilderHSupplement.h";
    }

    public String treeBuiderCppSupplement() {
        return "nsHtml5TreeBuilderCppSupplement.h";
    }

    public String documentModeHandlerType() {
        return "nsHtml5Parser*";
    }

    public String documentModeType() {
        return "nsHtml5DocumentMode";
    }

    public String arrayCopy() {
        return "nsHtml5ArrayCopy::arraycopy";
    }

    public String maxInteger() {
        return "PR_INT32_MAX";
    }

    public String utf16BufferCppSupplement() {
        return "nsHtml5UTF16BufferCppSupplement.h";
    }

    public String utf16BufferHSupplement() {
        return "nsHtml5UTF16BufferHSupplement.h";
    }
}
