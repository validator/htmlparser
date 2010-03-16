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

package nu.validator.htmlparser.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.validator.htmlparser.cpptranslate.CppTypes;

public class GenerateNamedCharactersCpp {

    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);

    private static final Pattern LINE_PATTERN = Pattern.compile("<td> <code title=\"\">([^<]*)</code> </td> <td> U\\+(\\S*) </td>");

    private static String toHexString(int c) {
        String hexString = Integer.toHexString(c);
        switch (hexString.length()) {
            case 1:
                return "0x000" + hexString;
            case 2:
                return "0x00" + hexString;
            case 3:
                return "0x0" + hexString;
            case 4:
                return "0x" + hexString;
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(args[0]), "utf-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = LINE_PATTERN.matcher(line);
            while (m.find()) {
                entities.put(m.group(1), m.group(2));
            }
        }

        CppTypes cppTypes = new CppTypes(null);
        File targetDirectory = new File(args[1]);

        generateH(targetDirectory, cppTypes, entities);
        generateInclude(targetDirectory, cppTypes, entities);
        generateCpp(targetDirectory, cppTypes, entities);
    }

    private static void generateH(File targetDirectory, CppTypes cppTypes,
            Map<String, String> entities) throws IOException {
        File hFile = new File(targetDirectory, cppTypes.classPrefix()
                + "NamedCharacters.h");
        Writer out = new OutputStreamWriter(new FileOutputStream(hFile),
                "utf-8");
        out.write("#ifndef " + cppTypes.classPrefix() + "NamedCharacters_h__\n");
        out.write("#define " + cppTypes.classPrefix() + "NamedCharacters_h__\n");
        out.write('\n');

        String[] includes = cppTypes.namedCharactersIncludes();
        for (int i = 0; i < includes.length; i++) {
            String include = includes[i];
            out.write("#include \"" + include + ".h\"\n");
        }

        out.write('\n');

        out.write("class " + cppTypes.classPrefix() + "NamedCharacters\n");
        out.write("{\n");
        out.write("  public:\n");
        out.write("    static " + cppTypes.arrayTemplate() + "<"
                + cppTypes.arrayTemplate() + "<" + cppTypes.byteType() + ","
                + cppTypes.intType() + ">," + cppTypes.intType() + "> NAMES;\n");
        out.write("    static const " + cppTypes.charType() + " VALUES[][2];\n");
        out.write("    static " + cppTypes.charType() + "** WINDOWS_1252;\n");
        out.write("    static const " + cppTypes.intType()
                + "* const HILO_ACCEL[];\n");
        out.write("    static void initializeStatics();\n");
        out.write("    static void releaseStatics();\n");
        out.write("};\n");

        out.write("\n#endif // " + cppTypes.classPrefix()
                + "NamedCharacters_h__\n");
        out.flush();
        out.close();
    }

    private static void generateInclude(File targetDirectory,
            CppTypes cppTypes, Map<String, String> entities) throws IOException {
        File includeFile = new File(targetDirectory, cppTypes.classPrefix()
                + "NamedCharactersInclude.h");
        Writer out = new OutputStreamWriter(new FileOutputStream(includeFile),
                "utf-8");

        out.write("/* Data generated from the table of named character references found at\n");
        out.write(" *\n");
        out.write(" *   http://www.w3.org/TR/html5/named-character-references.html\n");
        out.write(" *\n");
        out.write(" * Files that #include this file must #define NAMED_CHARACTER_REFERENCE as a\n");
        out.write(" * macro of four parameters:\n");
        out.write(" *\n");
        out.write(" *   1.  a unique integer N identifying the Nth [0,1,..] macro expansion in this file,\n");
        out.write(" *   2.  a comma-separated sequence of characters comprising the character name,\n");
        out.write(" *       without the first two letters. See Tokenizer.java.\n");
        out.write(" *   3.  the length of this sequence of characters,\n");
        out.write(" *   4.  a comma-separated sequence of PRUnichar literals (high to low) corresponding\n");
        out.write(" *       to the code-point of the named character.\n");
        out.write(" *\n");
        out.write(" * The macro expansion doesn't have to refer to all or any of these parameters,\n");
        out.write(" * but common sense dictates that it should involve at least one of them.\n");
        out.write(" */\n");
        out.write("\n");
        out.write("// This #define allows the NAMED_CHARACTER_REFERENCE macro to accept comma-\n");
        out.write("// separated sequences as single macro arguments.  Using commas directly would\n");
        out.write("// split the sequence into multiple macro arguments.\n");
        out.write("#define _ ,\n");
        out.write("\n");

        int i = 0;
        for (Map.Entry<String, String> entity : entities.entrySet()) {
            out.write("NAMED_CHARACTER_REFERENCE(" + i++ + ", ");
            String name = entity.getKey();
            writeNameInitializer(out, name, " _ ");
            out.write(", " + (name.length() - 2) + ", ");
            writeValueInitializer(out, Integer.parseInt(entity.getValue(), 16), " _ ");
            out.write(")\n");
        }

        out.write("\n");
        out.write("#undef _\n");

        out.flush();
        out.close();
    }

    private static void writeNameInitializer(Writer out,
            String name, String separator)
            throws IOException {
        out.write("/* " + name.charAt(0) + " " + name.charAt(1) + " */ ");
        if (name.length() == 2) {
            out.write("0");
        } else {
            for (int i = 2; i < name.length(); i++) {
                out.write("'" + name.charAt(i) + "'");
                if (i < name.length() - 1)
                    out.write(separator);
            }            
        }
    }

    private static void writeValueInitializer(Writer out,
            int value, String separator)
            throws IOException {
        if (value <= 0xFFFF) {
            out.write(toHexString(value));
            out.write(separator);
            out.write("0");
        } else {
            int hi = (LEAD_OFFSET + (value >> 10));
            int lo = (0xDC00 + (value & 0x3FF));
            out.write(toHexString(hi));
            out.write(separator);
            out.write(toHexString(lo));
        }
    }

    private static void defineMacroAndInclude(Writer out, String expansion,
            String includeFile) throws IOException {
        out.write("\n#define NAMED_CHARACTER_REFERENCE(N, CHARS, LEN, VALUE) \\\n"
                + expansion + "\n");
        out.write("#include \"" + includeFile + "\"\n");
        out.write("#undef NAMED_CHARACTER_REFERENCE\n");
    }

    private static void writeStaticMemberDeclaration(Writer out,
            CppTypes cppTypes, String type, String name) throws IOException {
        out.write(type + " " + cppTypes.classPrefix() + "NamedCharacters::"
                + name + ";\n");
    }

    private static int charToIndex(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        throw new IllegalArgumentException("Bad char in named character name: "
                + c);
    }

    private static boolean allZero(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static void generateCpp(File targetDirectory, CppTypes cppTypes,
            Map<String, String> entities) throws IOException {
        String includeFile = cppTypes.classPrefix()
                + "NamedCharactersInclude.h";
        File hFile = new File(targetDirectory, cppTypes.classPrefix()
                + "NamedCharacters.cpp");
        Writer out = new OutputStreamWriter(new FileOutputStream(hFile),
                "utf-8");

        out.write("#define " + cppTypes.classPrefix()
                + "NamedCharacters_cpp__\n");

        String[] includes = cppTypes.namedCharactersIncludes();
        for (int i = 0; i < includes.length; i++) {
            String include = includes[i];
            out.write("#include \"" + include + ".h\"\n");
        }

        out.write('\n');
        out.write("#include \"" + cppTypes.classPrefix()
                + "NamedCharacters.h\"\n");
        out.write("\n");

        String staticMemberType = cppTypes.arrayTemplate() + "<"
                + cppTypes.arrayTemplate() + "<" + cppTypes.byteType() + ","
                + cppTypes.intType() + ">," + cppTypes.intType() + ">";
        writeStaticMemberDeclaration(out, cppTypes, staticMemberType, "NAMES");

        out.write("\nconst PRUnichar nsHtml5NamedCharacters::VALUES[][2] = {\n");
        defineMacroAndInclude(out, "{ VALUE },", includeFile);
        // The useless terminator entry makes the above macro simpler with
        // compilers that whine about a comma after the last item
        out.write("{0, 0} };\n\n");

        staticMemberType = cppTypes.charType() + "**";
        writeStaticMemberDeclaration(out, cppTypes, staticMemberType,
                "WINDOWS_1252");

        out.write("static " + cppTypes.charType()
                + " const WINDOWS_1252_DATA[] = {\n");
        out.write("  0x20AC,\n");
        out.write("  0x0081,\n");
        out.write("  0x201A,\n");
        out.write("  0x0192,\n");
        out.write("  0x201E,\n");
        out.write("  0x2026,\n");
        out.write("  0x2020,\n");
        out.write("  0x2021,\n");
        out.write("  0x02C6,\n");
        out.write("  0x2030,\n");
        out.write("  0x0160,\n");
        out.write("  0x2039,\n");
        out.write("  0x0152,\n");
        out.write("  0x008D,\n");
        out.write("  0x017D,\n");
        out.write("  0x008F,\n");
        out.write("  0x0090,\n");
        out.write("  0x2018,\n");
        out.write("  0x2019,\n");
        out.write("  0x201C,\n");
        out.write("  0x201D,\n");
        out.write("  0x2022,\n");
        out.write("  0x2013,\n");
        out.write("  0x2014,\n");
        out.write("  0x02DC,\n");
        out.write("  0x2122,\n");
        out.write("  0x0161,\n");
        out.write("  0x203A,\n");
        out.write("  0x0153,\n");
        out.write("  0x009D,\n");
        out.write("  0x017E,\n");
        out.write("  0x0178\n");
        out.write("};\n\n");

        // start hilo

        // Java initializes arrays to zero. Zero is our magic value for no hilo
        // value.
        int[][] hiLoTable = new int['z' + 1]['Z' - 'A' + 1 + 'z' - 'a' + 1];

        String firstName = entities.entrySet().iterator().next().getKey();
        int firstKey = charToIndex(firstName.charAt(0));
        int secondKey = firstName.charAt(1);
        int row = 0;
        int lo = 0;

        for (Map.Entry<String, String> entity : entities.entrySet()) {
            String name = entity.getKey();
            int newFirst = charToIndex(name.charAt(0));
            int newSecond = name.charAt(1);
            assert !(newFirst == 0 && newSecond == 0) : "Not prepared for name starting with AA";
            if (firstKey != newFirst || secondKey != newSecond) {
                hiLoTable[secondKey][firstKey] = ((row - 1) << 16) | lo;
                lo = row;
                firstKey = newFirst;
                secondKey = newSecond;
            }
            row++;
        }

        hiLoTable[secondKey][firstKey] = ((entities.size() - 1) << 16) | lo;

        for (int i = 0; i < hiLoTable.length; i++) {
            if (!allZero(hiLoTable[i])) {
                out.write("static " + cppTypes.intType() + " const HILO_ACCEL_"
                        + i + "[] = {\n");
                for (int j = 0; j < hiLoTable[i].length; j++) {
                    if (j != 0) {
                        out.write(", ");
                    }
                    out.write("" + hiLoTable[i][j]);
                }
                out.write("\n};\n\n");
            }
        }

        out.write("const PRInt32* const " + cppTypes.classPrefix()
                + "NamedCharacters::HILO_ACCEL[] = {\n");
        for (int i = 0; i < hiLoTable.length; i++) {
            if (i != 0) {
                out.write(",\n");
            }
            if (allZero(hiLoTable[i])) {
                out.write("  0");
            } else {
                out.write("  HILO_ACCEL_" + i);
            }
        }
        out.write("\n};\n\n");

        // end hilo

        defineMacroAndInclude(out,
                "static PRInt8 const NAME_##N[] = { CHARS };", includeFile);
        defineMacroAndInclude(out,
                "static PRUnichar const VALUE_##N[] = { VALUE };", includeFile);

        out.write("\n// XXX bug 501082: for some reason, msvc takes forever to optimize this function\n");
        out.write("#ifdef _MSC_VER\n");
        out.write("#pragma optimize(\"\", off)\n");
        out.write("#endif\n\n");

        out.write("void\n");
        out.write(cppTypes.classPrefix()
                + "NamedCharacters::initializeStatics()\n");
        out.write("{\n");
        out.write("  NAMES = " + cppTypes.arrayTemplate() + "<"
                + cppTypes.arrayTemplate() + "<" + cppTypes.byteType() + ","
                + cppTypes.intType() + ">," + cppTypes.intType() + ">("
                + entities.size() + ");\n");
        defineMacroAndInclude(out,
                "  NAMES[N] = jArray<PRInt8,PRInt32>((PRInt8*)NAME_##N, LEN);",
                includeFile);

        out.write("\n");
        out.write("  WINDOWS_1252 = new " + cppTypes.charType() + "*[32];\n");
        out.write("  for (" + cppTypes.intType() + " i = 0; i < 32; ++i) {\n");
        out.write("    WINDOWS_1252[i] = (" + cppTypes.charType()
                + "*)&(WINDOWS_1252_DATA[i]);\n");
        out.write("  }\n");
        out.write("}\n");
        out.write("\n");

        out.write("#ifdef _MSC_VER\n");
        out.write("#pragma optimize(\"\", on)\n");
        out.write("#endif\n\n");

        out.write("void\n");
        out.write(cppTypes.classPrefix()
                + "NamedCharacters::releaseStatics()\n");
        out.write("{\n");
        out.write("  NAMES.release();\n");
        out.write("  delete[] WINDOWS_1252;\n");
        out.write("}\n");
        out.flush();
        out.close();
    }
}
