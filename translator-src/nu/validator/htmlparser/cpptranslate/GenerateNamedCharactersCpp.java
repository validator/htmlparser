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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateNamedCharactersCpp {

    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);
    
    private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*<tr> <td> <code title=\"\">([^<]*)</code> </td> <td> U\\+(\\S*) </td> </tr>.*$");
    
    private static String toUString(int c) {
        String hexString = Integer.toHexString(c);
        switch (hexString.length()) {
            case 1:
                return "\\u000" + hexString;
            case 2:
                return "\\u00" + hexString;
            case 3:
                return "\\u0" + hexString;
            case 4:
                return "\\u" + hexString;
            default:
                throw new RuntimeException("Unreachable.");
        }
    }

    
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        TreeMap<String, String> entities = new TreeMap<String, String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = LINE_PATTERN.matcher(line);
            if (m.matches()) {
                entities.put(m.group(1), m.group(2));
            }
        }
        System.out.println("static final char[][] NAMES = {");
        for (Map.Entry<String, String> entity : entities.entrySet()) {
            String name = entity.getKey();
            System.out.print("\"");
            System.out.print(name);
            System.out.println("\".toCharArray(),");
        }
        System.out.println("};");

        System.out.println("static final @NoLength char[][] VALUES = {");
        for (Map.Entry<String, String> entity : entities.entrySet()) {
            String value = entity.getValue();
            int intVal = Integer.parseInt(value, 16);
            System.out.print("{");
            if (intVal == '\'') {
                System.out.print("\'\\\'\'");                
            } else if (intVal == '\n') {
                System.out.print("\'\\n\'");                
            } else if (intVal == '\\') {
                System.out.print("\'\\\\\'");                
            } else if (intVal <= 0xFFFF) {
                System.out.print("\'");                
                System.out.print(toUString(intVal));                                
                System.out.print("\'");                
            } else {
                int hi = (LEAD_OFFSET + (intVal >> 10));
                int lo = (0xDC00 + (intVal & 0x3FF));
                System.out.print("\'");                
                System.out.print(toUString(hi));                                
                System.out.print("\', \'");                
                System.out.print(toUString(lo));                                
                System.out.print("\'");                
            }
            System.out.println("},");
        }
        System.out.println("};");

    }

}
