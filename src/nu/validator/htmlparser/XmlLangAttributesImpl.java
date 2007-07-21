package nu.validator.htmlparser;

public class XmlLangAttributesImpl extends AttributesImpl {

    /**
     * @see nu.validator.htmlparser.AttributesImpl#getIndex(java.lang.String, java.lang.String)
     */
    @Override
    public int getIndex(String uri, String localName) {
        if (("".equals(uri) && !"lang".equals(localName)) || ("http://www.w3.org/XML/1998/namespace".equals(uri) && "lang".equals(localName))) {
            return getIndex(localName);
        } else {
            return -1;
        }
    }

    /**
     * @see nu.validator.htmlparser.AttributesImpl#getURI(int)
     */
    @Override
    public String getURI(int index) {
        String localName = getQName(index);
        if (localName == null) {
            return null;
        } else if ("lang".equals(localName)) {
            return "http://www.w3.org/XML/1998/namespace";
        } else {
            return "";
        }
    }

    /**
     * @see nu.validator.htmlparser.AttributesImpl#getValue(java.lang.String, java.lang.String)
     */
    @Override
    public String getValue(String uri, String localName) {
        if (("".equals(uri) && !"lang".equals(localName)) || ("http://www.w3.org/XML/1998/namespace".equals(uri) && "lang".equals(localName))) {
            return getValue(localName);
        } else {
            return null;
        }
    }

    /**
     * @see nu.validator.htmlparser.AttributesImpl#getType(java.lang.String, java.lang.String)
     */
    @Override
    public String getType(String uri, String localName) {
        if (("".equals(uri) && !"lang".equals(localName)) || ("http://www.w3.org/XML/1998/namespace".equals(uri) && "lang".equals(localName))) {
            return getType(localName);
        } else {
            return null;
        }
    }

}
