package nu.validator.htmlparser.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import nu.validator.htmlparser.HtmlInputStreamReader;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import fi.iki.hsivonen.xml.SystemErrErrorHandler;

public class DecoderLoopTester {
    
    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);

    private static final int NUMBER_OR_ASTRAL_CHARS = 24500;
    
    private void runTest(int padding) throws SAXException, IOException {
       Charset utf8 = Charset.forName("UTF-8");
       char[] charArr = new char[1 + padding + 2 * NUMBER_OR_ASTRAL_CHARS];
       byte[] byteArr;
       int i = 0;
       charArr[i++] = '\uFEFF';
       for (int j = 0; j < padding; j++) {
           charArr[i++] = 'x';           
       }
       for (int j = 0; j < NUMBER_OR_ASTRAL_CHARS; j++) {
            int value = 0x10000 + j;
            charArr[i++] = (char) (LEAD_OFFSET + (value >> 10));
            charArr[i++] = (char) (0xDC00 + (value & 0x3FF));
//            charArr[i++] = 'y';
//            charArr[i++] = 'z';

       }
       CharBuffer charBuffer = CharBuffer.wrap(charArr);
       CharsetEncoder enc = utf8.newEncoder();
       enc.onMalformedInput(CodingErrorAction.REPORT);
       enc.onUnmappableCharacter(CodingErrorAction.REPORT);
       ByteBuffer byteBuffer = enc.encode(charBuffer);
       byteArr = new byte[byteBuffer.limit()];
       byteBuffer.get(byteArr);
       
       ErrorHandler eh = new SystemErrErrorHandler();
       compare(new HtmlInputStreamReader(new ByteArrayInputStream(byteArr), eh, null, null), padding, charArr, byteArr);
       compare(new HtmlInputStreamReader(new ByteArrayInputStream(byteArr), eh, null, null, utf8.newDecoder()), padding, charArr, byteArr);
    }

    /**
     * @param padding
     * @param charArr
     * @param byteArr
     * @throws SAXException
     * @throws IOException
     */
    private void compare(HtmlInputStreamReader reader, int padding, char[] charArr, byte[] byteArr) throws SAXException, IOException {
           char[] readBuffer = new char[2048];
           int offset = 0;
           int num = 0;
           int readNum = 0;
           while ((num = reader.read(readBuffer)) != -1) {
               for (int j = 0; j < num; j++) {
                   System.out.println(offset + j);
                   if (readBuffer[j] != charArr[offset + j]) {
                       throw new RuntimeException("Test failed. Char: " + Integer.toHexString(readBuffer[j]) + " j: " + j + " readNum: " + readNum);
                   }
               }
               offset += num;
               readNum++;
           }
    }
    
    void runTests() throws SAXException, IOException {
        for (int i = 0; i < 4; i++) {
            runTest(i);
        }
    }
    
    /**
     * @param args
     * @throws IOException 
     * @throws SAXException 
     */
    public static void main(String[] args) throws IOException, SAXException {
        new DecoderLoopTester().runTests();
    }

}
