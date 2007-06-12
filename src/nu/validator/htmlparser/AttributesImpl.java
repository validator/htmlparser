package nu.validator.htmlparser;

import org.xml.sax.Attributes;

public class AttributesImpl implements Attributes {
    
    private int length = 0;
    private int limit = 0;
    
    private String[] array = new String[6];

    public final int getIndex(String qName) {
        for (int i = 0; i < limit; i += 2) {
            if (array[i].equals(qName)) {
                return i / 2;
            }
        }
        return -1;
    }

    public int getIndex(String uri, String localName) {
        if ("".equals(uri)) {
            return getIndex(localName);
        } else {
            return -1;
        }
    }

    public final int getLength() {
        return length / 2;
    }

    public String getLocalName(int index) {
        return getQName(index);
    }

    public final String getQName(int index) {
        return index < length ? array[index * 2] : null;
    }

    public final String getType(int index) {
        if (index < length) {
            if ("id".equals(getQName(index))) {
                return "ID";
            } else {
                return "CDATA";
            }
        } else {
            return null;
        }
    }

    public final String getType(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getType(index);
        }
    }

    public String getType(String uri, String localName) {
        if ("".equals(uri)) {
            return getType(localName);
        } else {
            return null;
        }
    }

    public String getURI(int index) {
        return index < length ? "" : null;
    }

    public final String getValue(int index) {
        return index < length ? array[index * 2 + 1] : null;
    }

    public String getValue(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        } else {
            return getValue(index);
        }
    }

    public String getValue(String uri, String localName) {
        if ("".equals(uri)) {
            return getValue(localName);
        } else {
            return null;
        }
    }

    final void addAttribute(String name, String value) {
        if (array.length == limit) {
            String[] newArray = new String[array.length + 4];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
        array[limit] = name;
        array[limit + 1] = value;
        length++;
        limit += 2;
    }
    
    final void addAttribute(String name) {
        addAttribute(name, "");
    }
    
}
