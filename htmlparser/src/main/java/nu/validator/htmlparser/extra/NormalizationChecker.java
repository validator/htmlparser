/*
 * Copyright (c) 2006, 2007 Henri Sivonen
 * Copyright (c) 2007 Mozilla Foundation
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

package nu.validator.htmlparser.extra;

import nu.validator.htmlparser.common.CharacterHandler;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @version $Id$
 * @author hsivonen
 */
public final class NormalizationChecker implements CharacterHandler {

    private ErrorHandler errorHandler;

    private Locator locator;

    /**
     * A set of composing characters as per Charmod Norm.
     * 
     * Generated with ICU4J 67.1 using: new UnicodeSet("[[:nfc_qc=maybe:][:^ccc=0:]]").freeze()
     */
    private static final Set<Integer> COMPOSING_CHARACTERS = new HashSet<Integer>(Arrays.asList(
        768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785,
        786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803,
        804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 821,
        822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839,
        840, 841, 842, 843, 844, 845, 846, 848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858,
        859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876,
        877, 878, 879, 1155, 1156, 1157, 1158, 1159, 1425, 1426, 1427, 1428, 1429, 1430, 1431, 1432,
        1433, 1434, 1435, 1436, 1437, 1438, 1439, 1440, 1441, 1442, 1443, 1444, 1445, 1446, 1447,
        1448, 1449, 1450, 1451, 1452, 1453, 1454, 1455, 1456, 1457, 1458, 1459, 1460, 1461, 1462,
        1463, 1464, 1465, 1466, 1467, 1468, 1469, 1471, 1473, 1474, 1476, 1477, 1479, 1552, 1553,
        1554, 1555, 1556, 1557, 1558, 1559, 1560, 1561, 1562, 1611, 1612, 1613, 1614, 1615, 1616,
        1617, 1618, 1619, 1620, 1621, 1622, 1623, 1624, 1625, 1626, 1627, 1628, 1629, 1630, 1631,
        1648, 1750, 1751, 1752, 1753, 1754, 1755, 1756, 1759, 1760, 1761, 1762, 1763, 1764, 1767,
        1768, 1770, 1771, 1772, 1773, 1809, 1840, 1841, 1842, 1843, 1844, 1845, 1846, 1847, 1848,
        1849, 1850, 1851, 1852, 1853, 1854, 1855, 1856, 1857, 1858, 1859, 1860, 1861, 1862, 1863,
        1864, 1865, 1866, 2027, 2028, 2029, 2030, 2031, 2032, 2033, 2034, 2035, 2045, 2070, 2071,
        2072, 2073, 2075, 2076, 2077, 2078, 2079, 2080, 2081, 2082, 2083, 2085, 2086, 2087, 2089,
        2090, 2091, 2092, 2093, 2137, 2138, 2139, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266,
        2267, 2268, 2269, 2270, 2271, 2272, 2273, 2275, 2276, 2277, 2278, 2279, 2280, 2281, 2282,
        2283, 2284, 2285, 2286, 2287, 2288, 2289, 2290, 2291, 2292, 2293, 2294, 2295, 2296, 2297,
        2298, 2299, 2300, 2301, 2302, 2303, 2364, 2381, 2385, 2386, 2387, 2388, 2492, 2494, 2509,
        2519, 2558, 2620, 2637, 2748, 2765, 2876, 2878, 2893, 2902, 2903, 3006, 3021, 3031, 3149,
        3157, 3158, 3260, 3266, 3277, 3285, 3286, 3387, 3388, 3390, 3405, 3415, 3530, 3535, 3551,
        3640, 3641, 3642, 3656, 3657, 3658, 3659, 3768, 3769, 3770, 3784, 3785, 3786, 3787, 3864,
        3865, 3893, 3895, 3897, 3953, 3954, 3956, 3962, 3963, 3964, 3965, 3968, 3970, 3971, 3972,
        3974, 3975, 4038, 4142, 4151, 4153, 4154, 4237, 4449, 4450, 4451, 4452, 4453, 4454, 4455,
        4456, 4457, 4458, 4459, 4460, 4461, 4462, 4463, 4464, 4465, 4466, 4467, 4468, 4469, 4520,
        4521, 4522, 4523, 4524, 4525, 4526, 4527, 4528, 4529, 4530, 4531, 4532, 4533, 4534, 4535,
        4536, 4537, 4538, 4539, 4540, 4541, 4542, 4543, 4544, 4545, 4546, 4957, 4958, 4959, 5908,
        5940, 6098, 6109, 6313, 6457, 6458, 6459, 6679, 6680, 6752, 6773, 6774, 6775, 6776, 6777,
        6778, 6779, 6780, 6783, 6832, 6833, 6834, 6835, 6836, 6837, 6838, 6839, 6840, 6841, 6842,
        6843, 6844, 6845, 6847, 6848, 6964, 6965, 6980, 7019, 7020, 7021, 7022, 7023, 7024, 7025,
        7026, 7027, 7082, 7083, 7142, 7154, 7155, 7223, 7376, 7377, 7378, 7380, 7381, 7382, 7383,
        7384, 7385, 7386, 7387, 7388, 7389, 7390, 7391, 7392, 7394, 7395, 7396, 7397, 7398, 7399,
        7400, 7405, 7412, 7416, 7417, 7616, 7617, 7618, 7619, 7620, 7621, 7622, 7623, 7624, 7625,
        7626, 7627, 7628, 7629, 7630, 7631, 7632, 7633, 7634, 7635, 7636, 7637, 7638, 7639, 7640,
        7641, 7642, 7643, 7644, 7645, 7646, 7647, 7648, 7649, 7650, 7651, 7652, 7653, 7654, 7655,
        7656, 7657, 7658, 7659, 7660, 7661, 7662, 7663, 7664, 7665, 7666, 7667, 7668, 7669, 7670,
        7671, 7672, 7673, 7675, 7676, 7677, 7678, 7679, 8400, 8401, 8402, 8403, 8404, 8405, 8406,
        8407, 8408, 8409, 8410, 8411, 8412, 8417, 8421, 8422, 8423, 8424, 8425, 8426, 8427, 8428,
        8429, 8430, 8431, 8432, 11503, 11504, 11505, 11647, 11744, 11745, 11746, 11747, 11748, 11749,
        11750, 11751, 11752, 11753, 11754, 11755, 11756, 11757, 11758, 11759, 11760, 11761, 11762,
        11763, 11764, 11765, 11766, 11767, 11768, 11769, 11770, 11771, 11772, 11773, 11774, 11775,
        12330, 12331, 12332, 12333, 12334, 12335, 12441, 12442, 42607, 42612, 42613, 42614, 42615,
        42616, 42617, 42618, 42619, 42620, 42621, 42654, 42655, 42736, 42737, 43014, 43052, 43204,
        43232, 43233, 43234, 43235, 43236, 43237, 43238, 43239, 43240, 43241, 43242, 43243, 43244,
        43245, 43246, 43247, 43248, 43249, 43307, 43308, 43309, 43347, 43443, 43456, 43696, 43698,
        43699, 43700, 43703, 43704, 43710, 43711, 43713, 43766, 44013, 64286, 65056, 65057, 65058,
        65059, 65060, 65061, 65062, 65063, 65064, 65065, 65066, 65067, 65068, 65069, 65070, 65071,
        66045, 66272, 66422, 66423, 66424, 66425, 66426, 68109, 68111, 68152, 68153, 68154, 68159,
        68325, 68326, 68900, 68901, 68902, 68903, 69291, 69292, 69446, 69447, 69448, 69449, 69450,
        69451, 69452, 69453, 69454, 69455, 69456, 69702, 69759, 69817, 69818, 69888, 69889, 69890,
        69927, 69939, 69940, 70003, 70080, 70090, 70197, 70198, 70377, 70378, 70459, 70460, 70462,
        70477, 70487, 70502, 70503, 70504, 70505, 70506, 70507, 70508, 70512, 70513, 70514, 70515,
        70516, 70722, 70726, 70750, 70832, 70842, 70845, 70850, 70851, 71087, 71103, 71104, 71231,
        71350, 71351, 71467, 71737, 71738, 71984, 71997, 71998, 72003, 72160, 72244, 72263, 72345,
        72767, 73026, 73028, 73029, 73111, 92912, 92913, 92914, 92915, 92916, 92976, 92977, 92978,
        92979, 92980, 92981, 92982, 94192, 94193, 113822, 119141, 119142, 119143, 119144, 119145,
        119149, 119150, 119151, 119152, 119153, 119154, 119163, 119164, 119165, 119166, 119167,
        119168, 119169, 119170, 119173, 119174, 119175, 119176, 119177, 119178, 119179, 119210,
        119211, 119212, 119213, 119362, 119363, 119364, 122880, 122881, 122882, 122883, 122884,
        122885, 122886, 122888, 122889, 122890, 122891, 122892, 122893, 122894, 122895, 122896,
        122897, 122898, 122899, 122900, 122901, 122902, 122903, 122904, 122907, 122908, 122909,
        122910, 122911, 122912, 122913, 122915, 122916, 122918, 122919, 122920, 122921, 122922,
        123184, 123185, 123186, 123187, 123188, 123189, 123190, 123628, 123629, 123630, 123631,
        125136, 125137, 125138, 125139, 125140, 125141, 125142, 125252, 125253, 125254, 125255,
        125256, 125257, 125258
    ));

    // see http://sourceforge.net/mailarchive/message.php?msg_id=37279908

    /**
     * A buffer for holding sequences overlap the SAX buffer boundary.
     */
    private char[] buf = new char[128];

    /**
     * A holder for the original buffer (for the memory leak prevention 
     * mechanism).
     */
    private char[] bufHolder = null;

    /**
     * The current used length of the buffer, i.e. the index of the first slot 
     * that does not hold current data.
     */
    private int pos;

    /**
     * Indicates whether the checker the next call to <code>characters()</code> 
     * is the first call in a run.
     */
    private boolean atStartOfRun;

    /**
     * Indicates whether the current run has already caused an error.
     */
    private boolean alreadyComplainedAboutThisRun;

    /**
     * Emit an error. The locator is used.
     * 
     * @param message the error message
     * @throws SAXException if something goes wrong
     */
    public void err(String message) throws SAXException {
        if (errorHandler != null) {
            SAXParseException spe = new SAXParseException(message, locator);
            errorHandler.error(spe);
        }
    }

    /**
     * Returns <code>true</code> if the argument is a composing BMP character 
     * or a surrogate and <code>false</code> otherwise.
     * 
     * @param c a UTF-16 code unit
     * @return <code>true</code> if the argument is a composing BMP character 
     * or a surrogate and <code>false</code> otherwise
     */
    private static boolean isComposingCharOrSurrogate(char c) {
        if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
            return true;
        }
        return isComposingChar(c);
    }

    /**
     * Returns <code>true</code> if the argument is a composing character 
     * and <code>false</code> otherwise.
     * 
     * @param c a Unicode code point
     * @return <code>true</code> if the argument is a composing character 
     * <code>false</code> otherwise
     */
    private static boolean isComposingChar(int c) {
        return COMPOSING_CHARACTERS.contains(c);
    }

    /**
     * Constructor with locator.
     * 
     * @param locator
     */
    public NormalizationChecker(Locator locator) {
        super();
        start();
    }

    /**
     * @see nu.validator.htmlparser.common.CharacterHandler#start()
     */
    public void start() {
        atStartOfRun = true;
        alreadyComplainedAboutThisRun = false;
        pos = 0;
    }

    /**
     * @see nu.validator.htmlparser.common.CharacterHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (alreadyComplainedAboutThisRun) {
            return;
        }
        if (atStartOfRun) {
            char c = ch[start];
            if (pos == 1) {
                // there's a single high surrogate in buf
                if (isComposingChar(getCodePoint(buf[0], c))) {
                    err("Text run starts with a composing character.");
                }
                atStartOfRun = false;
            } else {
                if (length == 1 && Character.isHighSurrogate(c)) {
                    buf[0] = c;
                    pos = 1;
                    return;
                } else {
                    if (Character.isHighSurrogate(c)) {
                        if (isComposingChar(getCodePoint(c, ch[start + 1]))) {
                            err("Text run starts with a composing character.");
                        }
                    } else {
                        if (isComposingCharOrSurrogate(c)) {
                            err("Text run starts with a composing character.");
                        }
                    }
                    atStartOfRun = false;
                }
            }
        }
        int i = start;
        int stop = start + length;
        if (pos > 0) {
            // there's stuff in buf
            while (i < stop && isComposingCharOrSurrogate(ch[i])) {
                i++;
            }
            appendToBuf(ch, start, i);
            if (i == stop) {
                return;
            } else {
                if (!Normalizer.isNormalized(new String(buf, 0, pos), Normalizer.Form.NFC)) {
                    errAboutTextRun();
                }
                pos = 0;
            }
        }
        if (i < stop) {
            start = i;
            i = stop - 1;
            while (i > start && isComposingCharOrSurrogate(ch[i])) {
                i--;
            }
            if (i > start) {
                if (!Normalizer.isNormalized(new String(ch, start, i), Normalizer.Form.NFC)) {
                    errAboutTextRun();
                }
            }
            appendToBuf(ch, i, stop);
        }
    }

    private static int getCodePoint(char lead, char trail) {
        if (Character.isSurrogatePair(lead, trail)) {
            return Character.toCodePoint(lead, trail);
        }
        throw new IllegalArgumentException("Illegal surrogate characters");
    }

    /**
     * Emits an error stating that the current text run or the source 
     * text is not in NFC.
     * 
     * @throws SAXException if the <code>ErrorHandler</code> throws
     */
    private void errAboutTextRun() throws SAXException {
        err("Source text is not in Unicode Normalization Form C.");
        alreadyComplainedAboutThisRun = true;
    }

    /**
     * Appends a slice of an UTF-16 code unit array to the internal 
     * buffer.
     * 
     * @param ch the array from which to copy
     * @param start the index of the first element that is copied
     * @param end the index of the first element that is not copied
     */
    private void appendToBuf(char[] ch, int start, int end) {
        if (start == end) {
            return;
        }
        int neededBufLen = pos + (end - start);
        if (neededBufLen > buf.length) {
            char[] newBuf = new char[neededBufLen];
            System.arraycopy(buf, 0, newBuf, 0, pos);
            if (bufHolder == null) {
                bufHolder = buf; // keep the original around
            }
            buf = newBuf;
        }
        System.arraycopy(ch, start, buf, pos, end - start);
        pos += (end - start);
    }

    /**
     * @see nu.validator.htmlparser.common.CharacterHandler#end()
     */
    public void end() throws SAXException {
        if (!alreadyComplainedAboutThisRun
                && !Normalizer.isNormalized(new String(buf, 0, pos), Normalizer.Form.NFC)) {
            errAboutTextRun();
        }
        if (bufHolder != null) {
            // restore the original small buffer to avoid leaking
            // memory if this checker is recycled
            buf = bufHolder;
            bufHolder = null;
        }
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

}
