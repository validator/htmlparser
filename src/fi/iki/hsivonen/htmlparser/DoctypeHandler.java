package fi.iki.hsivonen.htmlparser;

import org.xml.sax.SAXException;

/**
 * @version $Id$
 * @author hsivonen
 */
public interface DoctypeHandler {
    public final static int ANY_DOCTYPE = 0;

    public final static int DOCTYPE_HTML401_TRANSITIONAL = 1;

    public final static int DOCTYPE_HTML401_STRICT = 2;

    public final static int DOCTYPE_HTML5 = 3;

    public void doctype(int doctype) throws SAXException;
}
