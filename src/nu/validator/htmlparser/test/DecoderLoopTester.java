package nu.validator.htmlparser.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import nu.validator.htmlparser.HtmlInputStreamReader;

import org.xml.sax.SAXException;

public class DecoderLoopTester {
    
    private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);
    
    private void generateData(File tempFile, boolean write, int padding) throws SAXException, IOException {
        // eww. this code turned out to be awfully ugly due to repetitive
        // trial and error. :-( :-( 
        // not useful to clean it up, though.
        Charset utf8 = Charset.forName("UTF-8");
        HtmlInputStreamReader reader = null;
        char[] readArr = null;
        OutputStreamWriter writer = null;
        
        if (write) {
            System.out.println("Will write.");
            writer = new OutputStreamWriter(new FileOutputStream(tempFile), utf8.newEncoder());
        } else {
            System.out.println("Will read.");
            reader = new HtmlInputStreamReader(new FileInputStream(tempFile), null, null, utf8.newDecoder());
            readArr = new char[1 + padding + 24500 * 2];
            char[] buf = new char[2048];
            int currLen = 0;
            int num = 0;
            while ((num = reader.read(buf, 0, buf.length)) != -1) {
                if (num == 0) {
                    throw new RuntimeException("Decoder read 0 chars.");
                }
                currLen += num;
                System.arraycopy(buf, 0, readArr, currLen, num);
            }
            if (currLen != readArr.length) {
                new RuntimeException("Test failed: file length did not match.");
            }
            reader.close();
        }
        int readPtr = 0;
        
        if (write) {
            writer.write(0xFEFF);
        } else {
            if (readArr[readPtr] != 0xFEFF) {
                throw new RuntimeException("Test failed: BOM not read.");
            }
            readPtr++;
        }
        
        for (int i = 0; i < padding; i++) {
            if (write) {
                writer.write('X');                
            } else {
                if (readArr[readPtr] != 'X') {
                    throw new RuntimeException("Test failed: Padding not read.");
                }                
            }
            readPtr++;
        }
        
        char[] astralChar = new char[2];
        for (int i = 0; i < 24500; i++) {
            System.out.println("Padding " + padding + " i " + i);
            int value = 0x10000 + i;
            astralChar[0] = (char) (LEAD_OFFSET + (value >> 10));
            astralChar[1] = (char) (0xDC00 + (value & 0x3FF));
            if (write) {
                writer.write(astralChar);                
            } else {
                if (astralChar[0] != readArr[readPtr]) {
                    throw new RuntimeException("Test failed: Wrong hi surragate. i="+i);                    
                }
                readPtr++;
                if (astralChar[1] != readArr[readPtr]) {
                    throw new RuntimeException("Test failed: Wrong lo surragate. i="+i);                    
                }
                readPtr++;
            }
        }
        
        if (write) {
            writer.flush();
            writer.close();
        }
    }
    
    void runTests(File tempFile) throws SAXException, IOException {
        for (int i = 0; i < 4; i++) {
            generateData(tempFile, true, i);
            System.out.println("Wrote with padding " + i);
            generateData(tempFile, false, i);
            System.out.println("Read with padding " + i);
        }
    }
    
    /**
     * @param args
     * @throws IOException 
     * @throws SAXException 
     */
    public static void main(String[] args) throws IOException, SAXException {
        File tempFile = File.createTempFile("utf-8-test", ".txt");
        new DecoderLoopTester().runTests(tempFile);
    }

}
