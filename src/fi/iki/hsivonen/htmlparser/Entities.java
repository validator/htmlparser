/*
 * Copyright (c) 2005 Henri Sivonen
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
public final class Entities {
    private final static String[] NAMES = { "AElig", "Aacute", "Acirc",
            "Agrave", "Alpha", "Aring", "Atilde", "Auml", "Beta", "Ccedil",
            "Chi", "Dagger", "Delta", "ETH", "Eacute", "Ecirc", "Egrave",
            "Epsilon", "Eta", "Euml", "Gamma", "Iacute", "Icirc", "Igrave",
            "Iota", "Iuml", "Kappa", "Lambda", "Mu", "Ntilde", "Nu", "OElig",
            "Oacute", "Ocirc", "Ograve", "Omega", "Omicron", "Oslash",
            "Otilde", "Ouml", "Phi", "Pi", "Prime", "Psi", "Rho", "Scaron",
            "Sigma", "THORN", "Tau", "Theta", "Uacute", "Ucirc", "Ugrave",
            "Upsilon", "Uuml", "Xi", "Yacute", "Yuml", "Zeta", "aacute",
            "acirc", "acute", "aelig", "agrave", "alefsym", "alpha", "amp",
            "and", "ang", "aring", "asymp", "atilde", "auml", "bdquo", "beta",
            "brvbar", "bull", "cap", "ccedil", "cedil", "cent", "chi", "circ",
            "clubs", "cong", "copy", "crarr", "cup", "curren", "dArr",
            "dagger", "darr", "deg", "delta", "diams", "divide", "eacute",
            "ecirc", "egrave", "empty", "emsp", "ensp", "epsilon", "equiv",
            "eta", "eth", "euml", "euro", "exist", "fnof", "forall", "frac12",
            "frac14", "frac34", "frasl", "gamma", "ge", "gt", "hArr", "harr",
            "hearts", "hellip", "iacute", "icirc", "iexcl", "igrave", "image",
            "infin", "int", "iota", "iquest", "isin", "iuml", "kappa", "lArr",
            "lambda", "lang", "laquo", "larr", "lceil", "ldquo", "le",
            "lfloor", "lowast", "loz", "lrm", "lsaquo", "lsquo", "lt", "macr",
            "mdash", "micro", "middot", "minus", "mu", "nabla", "nbsp",
            "ndash", "ne", "ni", "not", "notin", "nsub", "ntilde", "nu",
            "oacute", "ocirc", "oelig", "ograve", "oline", "omega", "omicron",
            "oplus", "or", "ordf", "ordm", "oslash", "otilde", "otimes",
            "ouml", "para", "part", "permil", "perp", "phi", "pi", "piv",
            "plusmn", "pound", "prime", "prod", "prop", "psi", "quot", "rArr",
            "radic", "rang", "raquo", "rarr", "rceil", "rdquo", "real", "reg",
            "rfloor", "rho", "rlm", "rsaquo", "rsquo", "sbquo", "scaron",
            "sdot", "sect", "shy", "sigma", "sigmaf", "sim", "spades", "sub",
            "sube", "sum", "sup", "sup1", "sup2", "sup3", "supe", "szlig",
            "tau", "there4", "theta", "thetasym", "thinsp", "thorn", "tilde",
            "times", "trade", "uArr", "uacute", "uarr", "ucirc", "ugrave",
            "uml", "upsih", "upsilon", "uuml", "weierp", "xi", "yacute", "yen",
            "yuml", "zeta", "zwj", "zwnj" };

    private final static char[][] VALUES = { { '\u00c6' }, { '\u00c1' },
            { '\u00c2' }, { '\u00c0' }, { '\u0391' }, { '\u00c5' },
            { '\u00c3' }, { '\u00c4' }, { '\u0392' }, { '\u00c7' },
            { '\u03a7' }, { '\u2021' }, { '\u0394' }, { '\u00d0' },
            { '\u00c9' }, { '\u00ca' }, { '\u00c8' }, { '\u0395' },
            { '\u0397' }, { '\u00cb' }, { '\u0393' }, { '\u00cd' },
            { '\u00ce' }, { '\u00cc' }, { '\u0399' }, { '\u00cf' },
            { '\u039a' }, { '\u039b' }, { '\u039c' }, { '\u00d1' },
            { '\u039d' }, { '\u0152' }, { '\u00d3' }, { '\u00d4' },
            { '\u00d2' }, { '\u03a9' }, { '\u039f' }, { '\u00d8' },
            { '\u00d5' }, { '\u00d6' }, { '\u03a6' }, { '\u03a0' },
            { '\u2033' }, { '\u03a8' }, { '\u03a1' }, { '\u0160' },
            { '\u03a3' }, { '\u00de' }, { '\u03a4' }, { '\u0398' },
            { '\u00da' }, { '\u00db' }, { '\u00d9' }, { '\u03a5' },
            { '\u00dc' }, { '\u039e' }, { '\u00dd' }, { '\u0178' },
            { '\u0396' }, { '\u00e1' }, { '\u00e2' }, { '\u00b4' },
            { '\u00e6' }, { '\u00e0' }, { '\u2135' }, { '\u03b1' },
            { '\u0026' }, { '\u2227' }, { '\u2220' }, { '\u00e5' },
            { '\u2248' }, { '\u00e3' }, { '\u00e4' }, { '\u201e' },
            { '\u03b2' }, { '\u00a6' }, { '\u2022' }, { '\u2229' },
            { '\u00e7' }, { '\u00b8' }, { '\u00a2' }, { '\u03c7' },
            { '\u02c6' }, { '\u2663' }, { '\u2245' }, { '\u00a9' },
            { '\u21b5' }, { '\u222a' }, { '\u00a4' }, { '\u21d3' },
            { '\u2020' }, { '\u2193' }, { '\u00b0' }, { '\u03b4' },
            { '\u2666' }, { '\u00f7' }, { '\u00e9' }, { '\u00ea' },
            { '\u00e8' }, { '\u2205' }, { '\u2003' }, { '\u2002' },
            { '\u03b5' }, { '\u2261' }, { '\u03b7' }, { '\u00f0' },
            { '\u00eb' }, { '\u20ac' }, { '\u2203' }, { '\u0192' },
            { '\u2200' }, { '\u00bd' }, { '\u00bc' }, { '\u00be' },
            { '\u2044' }, { '\u03b3' }, { '\u2265' }, { '\u003e' },
            { '\u21d4' }, { '\u2194' }, { '\u2665' }, { '\u2026' },
            { '\u00ed' }, { '\u00ee' }, { '\u00a1' }, { '\u00ec' },
            { '\u2111' }, { '\u221e' }, { '\u222b' }, { '\u03b9' },
            { '\u00bf' }, { '\u2208' }, { '\u00ef' }, { '\u03ba' },
            { '\u21d0' }, { '\u03bb' }, { '\u2329' }, { '\u00ab' },
            { '\u2190' }, { '\u2308' }, { '\u201c' }, { '\u2264' },
            { '\u230a' }, { '\u2217' }, { '\u25ca' }, { '\u200e' },
            { '\u2039' }, { '\u2018' }, { '\u003c' }, { '\u00af' },
            { '\u2014' }, { '\u00b5' }, { '\u00b7' }, { '\u2212' },
            { '\u03bc' }, { '\u2207' }, { '\u00a0' }, { '\u2013' },
            { '\u2260' }, { '\u220b' }, { '\u00ac' }, { '\u2209' },
            { '\u2284' }, { '\u00f1' }, { '\u03bd' }, { '\u00f3' },
            { '\u00f4' }, { '\u0153' }, { '\u00f2' }, { '\u203e' },
            { '\u03c9' }, { '\u03bf' }, { '\u2295' }, { '\u2228' },
            { '\u00aa' }, { '\u00ba' }, { '\u00f8' }, { '\u00f5' },
            { '\u2297' }, { '\u00f6' }, { '\u00b6' }, { '\u2202' },
            { '\u2030' }, { '\u22a5' }, { '\u03c6' }, { '\u03c0' },
            { '\u03d6' }, { '\u00b1' }, { '\u00a3' }, { '\u2032' },
            { '\u220f' }, { '\u221d' }, { '\u03c8' }, { '\u0022' },
            { '\u21d2' }, { '\u221a' }, { '\u232a' }, { '\u00bb' },
            { '\u2192' }, { '\u2309' }, { '\u201d' }, { '\u211c' },
            { '\u00ae' }, { '\u230b' }, { '\u03c1' }, { '\u200f' },
            { '\u203a' }, { '\u2019' }, { '\u201a' }, { '\u0161' },
            { '\u22c5' }, { '\u00a7' }, { '\u00ad' }, { '\u03c3' },
            { '\u03c2' }, { '\u223c' }, { '\u2660' }, { '\u2282' },
            { '\u2286' }, { '\u2211' }, { '\u2283' }, { '\u00b9' },
            { '\u00b2' }, { '\u00b3' }, { '\u2287' }, { '\u00df' },
            { '\u03c4' }, { '\u2234' }, { '\u03b8' }, { '\u03d1' },
            { '\u2009' }, { '\u00fe' }, { '\u02dc' }, { '\u00d7' },
            { '\u2122' }, { '\u21d1' }, { '\u00fa' }, { '\u2191' },
            { '\u00fb' }, { '\u00f9' }, { '\u00a8' }, { '\u03d2' },
            { '\u03c5' }, { '\u00fc' }, { '\u2118' }, { '\u03be' },
            { '\u00fd' }, { '\u00a5' }, { '\u00ff' }, { '\u03b6' },
            { '\u200d' }, { '\u200c' } };

    public static final char[] resolve(String entity) {
        int i = Arrays.binarySearch(NAMES, entity);
        if (i < 0) {
            return null;
        } else {
            return VALUES[i];
        }
    }

    private static final String[] NAMES_5 = { "AElig", "AMP", "Aacute",
            "Acirc", "Agrave", "Alpha", "Aring", "Atilde", "Auml", "Beta",
            "COPY", "Ccedil", "Chi", "Dagger", "Delta", "ETH", "Eacute",
            "Ecirc", "Egrave", "Epsilon", "Eta", "Euml", "GT", "Gamma",
            "Iacute", "Icirc", "Igrave", "Iota", "Iuml", "Kappa", "LT",
            "Lambda", "Mu", "Ntilde", "Nu", "OElig", "Oacute", "Ocirc",
            "Ograve", "Omega", "Omicron", "Oslash", "Otilde", "Ouml", "Phi",
            "Pi", "Prime", "Psi", "QUOT", "REG", "Rho", "Scaron", "Sigma",
            "THORN", "Tau", "Theta", "Uacute", "Ucirc", "Ugrave", "Upsilon",
            "Uuml", "Xi", "Yacute", "Yuml", "Zeta", "aacute", "acirc", "acute",
            "aelig", "agrave", "alefsym", "alpha", "amp", "and", "ang",
            "aring", "asymp", "atilde", "auml", "bdquo", "beta", "brvbar",
            "bull", "cap", "ccedil", "cedil", "cent", "chi", "circ", "clubs",
            "cong", "copy", "crarr", "cup", "curren", "dArr", "dagger", "darr",
            "deg", "delta", "diams", "divide", "eacute", "ecirc", "egrave",
            "empty", "emsp", "ensp", "epsilon", "equiv", "eta", "eth", "euml",
            "euro", "exist", "fnof", "forall", "frac12", "frac14", "frac34",
            "frasl", "gamma", "ge", "gt", "hArr", "harr", "hearts", "hellip",
            "iacute", "icirc", "iexcl", "igrave", "image", "infin", "int",
            "iota", "iquest", "isin", "iuml", "kappa", "lArr", "lambda",
            "lang", "laquo", "larr", "lceil", "ldquo", "le", "lfloor",
            "lowast", "loz", "lrm", "lsaquo", "lsquo", "lt", "macr", "mdash",
            "micro", "middot", "minus", "mu", "nabla", "nbsp", "ndash", "ne",
            "ni", "not", "notin", "nsub", "ntilde", "nu", "oacute", "ocirc",
            "oelig", "ograve", "oline", "omega", "omicron", "oplus", "or",
            "ordf", "ordm", "oslash", "otilde", "otimes", "ouml", "para",
            "part", "permil", "perp", "phi", "pi", "piv", "plusmn", "pound",
            "prime", "prod", "prop", "psi", "quot", "rArr", "radic", "rang",
            "raquo", "rarr", "rceil", "rdquo", "real", "reg", "rfloor", "rho",
            "rlm", "rsaquo", "rsquo", "sbquo", "scaron", "sdot", "sect", "shy",
            "sigma", "sigmaf", "sim", "spades", "sub", "sube", "sum", "sup",
            "sup1", "sup2", "sup3", "supe", "szlig", "tau", "there4", "theta",
            "thetasym", "thinsp", "thorn", "tilde", "times", "trade", "uArr",
            "uacute", "uarr", "ucirc", "ugrave", "uml", "upsih", "upsilon",
            "uuml", "weierp", "xi", "yacute", "yen", "yuml", "zeta", "zwj",
            "zwnj" };

    private final static char[][] VALUES_5 = { { '\u00C6' }, { '\u0026' },
            { '\u00C1' }, { '\u00C2' }, { '\u00C0' }, { '\u0391' },
            { '\u00C5' }, { '\u00C3' }, { '\u00C4' }, { '\u0392' },
            { '\u00A9' }, { '\u00C7' }, { '\u03A7' }, { '\u2021' },
            { '\u0394' }, { '\u00D0' }, { '\u00C9' }, { '\u00CA' },
            { '\u00C8' }, { '\u0395' }, { '\u0397' }, { '\u00CB' },
            { '\u003E' }, { '\u0393' }, { '\u00CD' }, { '\u00CE' },
            { '\u00CC' }, { '\u0399' }, { '\u00CF' }, { '\u039A' },
            { '\u003C' }, { '\u039B' }, { '\u039C' }, { '\u00D1' },
            { '\u039D' }, { '\u0152' }, { '\u00D3' }, { '\u00D4' },
            { '\u00D2' }, { '\u03A9' }, { '\u039F' }, { '\u00D8' },
            { '\u00D5' }, { '\u00D6' }, { '\u03A6' }, { '\u03A0' },
            { '\u2033' }, { '\u03A8' }, { '\u0022' }, { '\u00AE' },
            { '\u03A1' }, { '\u0160' }, { '\u03A3' }, { '\u00DE' },
            { '\u03A4' }, { '\u0398' }, { '\u00DA' }, { '\u00DB' },
            { '\u00D9' }, { '\u03A5' }, { '\u00DC' }, { '\u039E' },
            { '\u00DD' }, { '\u0178' }, { '\u0396' }, { '\u00E1' },
            { '\u00E2' }, { '\u00B4' }, { '\u00E6' }, { '\u00E0' },
            { '\u2135' }, { '\u03B1' }, { '\u0026' }, { '\u2227' },
            { '\u2220' }, { '\u00E5' }, { '\u2248' }, { '\u00E3' },
            { '\u00E4' }, { '\u201E' }, { '\u03B2' }, { '\u00A6' },
            { '\u2022' }, { '\u2229' }, { '\u00E7' }, { '\u00B8' },
            { '\u00A2' }, { '\u03C7' }, { '\u02C6' }, { '\u2663' },
            { '\u2245' }, { '\u00A9' }, { '\u21B5' }, { '\u222A' },
            { '\u00A4' }, { '\u21D3' }, { '\u2020' }, { '\u2193' },
            { '\u00B0' }, { '\u03B4' }, { '\u2666' }, { '\u00F7' },
            { '\u00E9' }, { '\u00EA' }, { '\u00E8' }, { '\u2205' },
            { '\u2003' }, { '\u2002' }, { '\u03B5' }, { '\u2261' },
            { '\u03B7' }, { '\u00F0' }, { '\u00EB' }, { '\u20AC' },
            { '\u2203' }, { '\u0192' }, { '\u2200' }, { '\u00BD' },
            { '\u00BC' }, { '\u00BE' }, { '\u2044' }, { '\u03B3' },
            { '\u2265' }, { '\u003E' }, { '\u21D4' }, { '\u2194' },
            { '\u2665' }, { '\u2026' }, { '\u00ED' }, { '\u00EE' },
            { '\u00A1' }, { '\u00EC' }, { '\u2111' }, { '\u221E' },
            { '\u222B' }, { '\u03B9' }, { '\u00BF' }, { '\u2208' },
            { '\u00EF' }, { '\u03BA' }, { '\u21D0' }, { '\u03BB' },
            { '\u2329' }, { '\u00AB' }, { '\u2190' }, { '\u2308' },
            { '\u201C' }, { '\u2264' }, { '\u230A' }, { '\u2217' },
            { '\u25CA' }, { '\u200E' }, { '\u2039' }, { '\u2018' },
            { '\u003C' }, { '\u00AF' }, { '\u2014' }, { '\u00B5' },
            { '\u00B7' }, { '\u2212' }, { '\u03BC' }, { '\u2207' },
            { '\u00A0' }, { '\u2013' }, { '\u2260' }, { '\u220B' },
            { '\u00AC' }, { '\u2209' }, { '\u2284' }, { '\u00F1' },
            { '\u03BD' }, { '\u00F3' }, { '\u00F4' }, { '\u0153' },
            { '\u00F2' }, { '\u203E' }, { '\u03C9' }, { '\u03BF' },
            { '\u2295' }, { '\u2228' }, { '\u00AA' }, { '\u00BA' },
            { '\u00F8' }, { '\u00F5' }, { '\u2297' }, { '\u00F6' },
            { '\u00B6' }, { '\u2202' }, { '\u2030' }, { '\u22A5' },
            { '\u03C6' }, { '\u03C0' }, { '\u03D6' }, { '\u00B1' },
            { '\u00A3' }, { '\u2032' }, { '\u220F' }, { '\u221D' },
            { '\u03C8' }, { '\u0022' }, { '\u21D2' }, { '\u221A' },
            { '\u232A' }, { '\u00BB' }, { '\u2192' }, { '\u2309' },
            { '\u201D' }, { '\u211C' }, { '\u00AE' }, { '\u230B' },
            { '\u03C1' }, { '\u200F' }, { '\u203A' }, { '\u2019' },
            { '\u201A' }, { '\u0161' }, { '\u22C5' }, { '\u00A7' },
            { '\u00AD' }, { '\u03C3' }, { '\u03C2' }, { '\u223C' },
            { '\u2660' }, { '\u2282' }, { '\u2286' }, { '\u2211' },
            { '\u2283' }, { '\u00B9' }, { '\u00B2' }, { '\u00B3' },
            { '\u2287' }, { '\u00DF' }, { '\u03C4' }, { '\u2234' },
            { '\u03B8' }, { '\u03D1' }, { '\u2009' }, { '\u00FE' },
            { '\u02DC' }, { '\u00D7' }, { '\u2122' }, { '\u21D1' },
            { '\u00FA' }, { '\u2191' }, { '\u00FB' }, { '\u00F9' },
            { '\u00A8' }, { '\u03D2' }, { '\u03C5' }, { '\u00FC' },
            { '\u2118' }, { '\u03BE' }, { '\u00FD' }, { '\u00A5' },
            { '\u00FF' }, { '\u03B6' }, { '\u200D' }, { '\u200C' } };

    public static final char[] resolve5(String entity) {
        int i = Arrays.binarySearch(NAMES_5, entity);
        if (i < 0) {
            return null;
        } else {
            return VALUES_5[i];
        }
    }
}
