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

import java.util.HashSet;
import java.util.Set;

public class ElementName {
    
    public static final ElementName IMG = new ElementName("img", TreeBuilder.EMBED_OR_IMG, true, false, false);
    public static final ElementName HEAD = new ElementName("head", TreeBuilder.HEAD, true, false, false);
    public static final ElementName HTML = new ElementName("html", TreeBuilder.HTML, false, true, false);
    public static final ElementName FORM = new ElementName("form", TreeBuilder.FORM, true, false, false);
    public static final ElementName BODY = new ElementName("body", TreeBuilder.BODY, true, false, false);
    public static final ElementName P = new ElementName("p", TreeBuilder.P, true, false, false);
    public static final ElementName TR = new ElementName("tr", TreeBuilder.TR, true, false, true);
    public static final ElementName COLGROUP = new ElementName("colgroup", TreeBuilder.COLGROUP, true, false, false);
    public static final ElementName TBODY = new ElementName("tbody", TreeBuilder.TBODY_OR_THEAD_OR_TFOOT, true, false, true);
    public static final ElementName LABEL = new ElementName("label", TreeBuilder.OTHER, false, false, false);
    public final String name;   
    public final String camelCaseName;
    public final int group;
    public final boolean special;
    public final boolean scoping;
    public final boolean fosterParenting;
    public final boolean custom;
    
    private ElementName(String camelCaseName, int group, boolean special, boolean scoping, boolean fosterParenting) {
        this.name = camelCaseName.toLowerCase().intern();
        this.camelCaseName = camelCaseName.intern();
        this.group = group;
        this.special = special;
        this.scoping = scoping;
        this.fosterParenting = fosterParenting;
        this.custom = false;
    }
    
    ElementName(String name) {
        this.name = name;
        this.camelCaseName = name;
        this.group = TreeBuilder.OTHER;
        this.special = false;
        this.scoping = false;
        this.fosterParenting = false;
        this.custom = true;
    }
    
    static Set<ElementName> buildSet() {
        Set<ElementName> set = new HashSet<ElementName>();
        set.add(new ElementName("a", TreeBuilder.A, false, false, false));
        set.add(new ElementName("abbr", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("abs", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("acronym", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("address", TreeBuilder.FIELDSET_OR_ADDRESS_OR_DIR, true, false, false));
        set.add(new ElementName("altGlyph", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("altGlyphDef", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("altGlyphItem", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("and", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("animate", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("animateColor", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("animateMotion", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("animateTransform", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("animation", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("annotation", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("annotation-xml", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("applet", TreeBuilder.OBJECT_OR_MARQUEE_OR_APPLET, false, true, false));
        set.add(new ElementName("apply", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("approx", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccos", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccosh", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccot", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccoth", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccsc", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arccsch", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arcsec", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arcsech", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arcsin", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arcsinh", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arctan", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("arctanh", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("area", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("arg", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("article", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("aside", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("audio", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("b", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("base", TreeBuilder.BASE, true, false, false));
        set.add(new ElementName("basefont", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("bgsound", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("bdo", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("big", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("blockquote", TreeBuilder.DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU, true, false, false));
        set.add(BODY);
        set.add(new ElementName("br", TreeBuilder.BR, true, false, false));
        set.add(new ElementName("button", TreeBuilder.BUTTON, false, true, false));
        set.add(new ElementName("bvar", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("canvas", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("caption", TreeBuilder.CAPTION, false, true, false));
        set.add(new ElementName("card", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cartesianproduct", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("ceiling", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("center", TreeBuilder.DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU, true, false, false));
        set.add(new ElementName("ci", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("circle", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cite", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("clipPath", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cn", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("code", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("codomain", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("col", TreeBuilder.COL, true, false, false));
        set.add(COLGROUP);
        set.add(new ElementName("color-profile", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("command", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("complexes", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("compose", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("condition", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("conjugate", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cos", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cosh", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cot", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("coth", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("csc", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("csch", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("csymbol", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("curl", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("cursor", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("datagrid", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("datatemplate", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dd", TreeBuilder.DD_OR_DT, true, false, false));
        set.add(new ElementName("declare", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("definition-src", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("defs", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("degree", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("del", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("desc", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("details", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("determinant", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dfn", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dialog", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("diff", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dir", TreeBuilder.FIELDSET_OR_ADDRESS_OR_DIR, true, false, false));
        set.add(new ElementName("discard", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("div", TreeBuilder.DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU, true, false, false));
        set.add(new ElementName("divergence", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("divide", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dl", TreeBuilder.UL_OR_OL_OR_DL, true, false, false));
        set.add(new ElementName("domain", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("domainofapplication", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("dt", TreeBuilder.DD_OR_DT, true, false, false));
        set.add(new ElementName("ellipse", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("em", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("embed", TreeBuilder.EMBED_OR_IMG, true, false, false));
        set.add(new ElementName("emptyset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("eq", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("equivalent", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("eulergamma", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("event-source", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("exists", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("exp", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("exponentiale", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("factorial", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("factorof", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("false", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feBlend", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feColorMatrix", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feComponentTransfer", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feComposite", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feConvolveMatrix", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feDiffuseLighting", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feDisplacementMap", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feDistantLight", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feFlood", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feFuncA", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feFuncB", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feFuncG", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feFuncR", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feGaussianBlur", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feImage", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feMerge", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feMergeNode", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feMorphology", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feOffset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("fePointLight", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feSpecularLighting", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feSpotLight", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feTile", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("feTurbulence", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("fieldset", TreeBuilder.FIELDSET_OR_ADDRESS_OR_DIR, true, false, false));
        set.add(new ElementName("figure", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("filter", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("floor", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("fn", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("font", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("font-face", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("font-face-format", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("font-face-name", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("font-face-src", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("font-face-uri", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("footer", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("forall", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("foreignObject", TreeBuilder.OTHER, false, false, false));
        set.add(FORM);
        set.add(new ElementName("frame", TreeBuilder.FRAME, true, false, false));
        set.add(new ElementName("frameset", TreeBuilder.FRAMESET, true, false, false));
        set.add(new ElementName("g", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("gcd", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("geq", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("glyph", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("glyphRef", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("grad", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("gt", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("h1", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("h2", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("h3", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("h4", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("h5", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("h6", TreeBuilder.H1_OR_H2_OR_H3_OR_H4_OR_H5_OR_H6, true, false, false));
        set.add(new ElementName("handler", TreeBuilder.OTHER, false, false, false));
        set.add(HEAD);
        set.add(new ElementName("header", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("hkern", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("hr", TreeBuilder.HR, true, false, false));
        set.add(HTML);
        set.add(new ElementName("i", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("ident", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("iframe", TreeBuilder.IFRAME_OR_NOEMBED, true, false, false));
        set.add(new ElementName("image", TreeBuilder.IMAGE, true, false, false));
        set.add(new ElementName("imaginary", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("imaginaryi", TreeBuilder.OTHER, false, false, false));
        set.add(IMG);
        set.add(new ElementName("implies", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("in", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("infinity", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("input", TreeBuilder.INPUT, true, false, false));
        set.add(new ElementName("ins", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("int", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("integers", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("intersect", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("interval", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("inverse", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("isindex", TreeBuilder.ISINDEX, true, false, false));
        set.add(new ElementName("kbd", TreeBuilder.OTHER, false, false, false));
        set.add(LABEL);
        set.add(new ElementName("lambda", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("laplacian", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("lcm", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("legend", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("leq", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("li", TreeBuilder.LI, true, false, false));
        set.add(new ElementName("limit", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("line", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("linearGradient", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("link", TreeBuilder.LINK, true, false, false));
        set.add(new ElementName("list", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("listener", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("listing", TreeBuilder.PRE_OR_LISTING, true, false, false));
        set.add(new ElementName("ln", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("log", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("logbase", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("lowlimit", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("lt", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("maction", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("maligngroup", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("malignmark", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("map", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mark", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("marker", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("marquee", TreeBuilder.OBJECT_OR_MARQUEE_OR_APPLET, false, true, false));
        set.add(new ElementName("mask", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("math", TreeBuilder.MATH, false, false, false));
        set.add(new ElementName("matrix", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("matrixrow", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("max", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mean", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("median", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("menclose", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("menu", TreeBuilder.DIV_OR_BLOCKQUOTE_OR_CENTER_OR_MENU, true, false, false));
        set.add(new ElementName("merror", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("meta", TreeBuilder.META, true, false, false));
        set.add(new ElementName("metadata", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("meter", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mfenced", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mfrac", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mglyph", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mi", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("min", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("minus", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("missing-glyph", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mlabeledtr", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mmultiscripts", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mn", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mo", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mode", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("moment", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("momentabout", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mover", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mpadded", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mpath", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mphantom", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mprescripts", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mroot", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mrow", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("ms", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mspace", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("msqrt", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mstyle", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("msub", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("msubsup", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("msup", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mtable", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mtd", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mtext", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("mtr", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("munder", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("munderover", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("naturalnumbers", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("nav", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("neq", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("nest", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("nobr", TreeBuilder.NOBR, false, false, false));
        set.add(new ElementName("noembed", TreeBuilder.IFRAME_OR_NOEMBED, true, false, false));
        set.add(new ElementName("noframes", TreeBuilder.NOFRAMES, true, false, false));
        set.add(new ElementName("none", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("noscript", TreeBuilder.NOSCRIPT, true, false, false));
        set.add(new ElementName("not", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("notanumber", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("notin", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("notprsubset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("notsubset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("object", TreeBuilder.OBJECT_OR_MARQUEE_OR_APPLET, false, true, false));
        set.add(new ElementName("ol", TreeBuilder.UL_OR_OL_OR_DL, true, false, false));
        set.add(new ElementName("optgroup", TreeBuilder.OPTGROUP, true, false, false));
        set.add(new ElementName("option", TreeBuilder.OPTION, true, false, false));
        set.add(new ElementName("or", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("otherwise", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("outerproduct", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("output", TreeBuilder.OTHER, false, false, false));
        set.add(P);
        set.add(new ElementName("param", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("partialdiff", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("path", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("pattern", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("pi", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("piece", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("piecewise", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("plaintext", TreeBuilder.PLAINTEXT, true, false, false));
        set.add(new ElementName("plus", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("polygon", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("polyline", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("power", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("pre", TreeBuilder.PRE_OR_LISTING, true, false, false));
        set.add(new ElementName("prefetch", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("primes", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("product", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("progress", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("prsubset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("q", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("quotient", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("radialGradient", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("rationals", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("real", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("reals", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("rect", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("reln", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("rem", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("root", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("ruby", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("rule", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("s", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("samp", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("scalarproduct", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("script", TreeBuilder.SCRIPT, true, false, false));
        set.add(new ElementName("sdev", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sec", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sech", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("section", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("select", TreeBuilder.SELECT, true, false, false));
        set.add(new ElementName("selector", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("semantics", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sep", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("set", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("setdiff", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sin", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sinh", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("small", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("solidColor", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("source", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("spacer", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("span", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("stop", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("strike", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("strong", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("style", TreeBuilder.STYLE, true, false, false));
        set.add(new ElementName("sub", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("subset", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sum", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("sup", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("svg", TreeBuilder.SVG, false, false, false));
        set.add(new ElementName("switch", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("symbol", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("table", TreeBuilder.TABLE, false, true, true));
        set.add(new ElementName("tan", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("tanh", TreeBuilder.OTHER, false, false, false));
        set.add(TBODY);
        set.add(new ElementName("tbreak", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("td", TreeBuilder.TD_OR_TH, false, true, false));
        set.add(new ElementName("tendsto", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("text", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("textPath", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("textarea", TreeBuilder.TEXTAREA, true, false, false));
        set.add(new ElementName("tfoot", TreeBuilder.TBODY_OR_THEAD_OR_TFOOT, true, false, true));
        set.add(new ElementName("th", TreeBuilder.TD_OR_TH, false, true, false));
        set.add(new ElementName("thead", TreeBuilder.TBODY_OR_THEAD_OR_TFOOT, true, false, true));
        set.add(new ElementName("time", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("times", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("title", TreeBuilder.TITLE, true, false, false));
        set.add(TR);
        set.add(new ElementName("transpose", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("tref", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("true", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("tspan", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("tt", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("u", TreeBuilder.B_OR_BIG_OR_EM_OR_FONT_OR_I_OR_S_OR_SMALL_OR_STRIKE_OR_STRONG_OR_TT_OR_U, false, false, false));
        set.add(new ElementName("ul", TreeBuilder.UL_OR_OL_OR_DL, true, false, false));
        set.add(new ElementName("union", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("uplimit", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("use", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("var", TreeBuilder.CODE_OR_RUBY_OR_SPAN_OR_SUB_OR_SUP_OR_VAR, false, false, false));
        set.add(new ElementName("variance", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("vector", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("vectorproduct", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("video", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("view", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("vkern", TreeBuilder.OTHER, false, false, false));
        set.add(new ElementName("wbr", TreeBuilder.AREA_OR_BASEFONT_OR_BGSOUND_OR_PARAM_OR_SPACER_OR_WBR, true, false, false));
        set.add(new ElementName("xmp", TreeBuilder.XMP, false, false, false));
        set.add(new ElementName("xor", TreeBuilder.OTHER, false, false, false));
        return set;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "new ElementName(\"" + name + "\", \"" + camelCaseName + "\", " + treeBuilderGroupToName();
    }
    
    private String treeBuilderGroupToName() {
        switch (group) {
            
        }
        return null;
    }

    /**
     * Regenerate self
     * @param args
     */
    public static void main(String[] args) {
        
    }
}
