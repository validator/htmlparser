package nu.validator.htmlparser.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.TokenHandler;
import nu.validator.htmlparser.Tokenizer;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;
import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;

import antlr.TokenStreamException;
import antlr.RecognitionException;

public class TokenizerTester {

    private static boolean jsonDeepEquals(JSONValue one, JSONValue other) {
        if (one.isSimple()) {
            return one.equals(other);
        } else if (one.isArray()) {
            if (other.isArray()) {
                JSONArray oneArr = (JSONArray) one;
                JSONArray otherArr = (JSONArray) other;
                return oneArr.getValue().equals(otherArr.getValue());
            } else {
                return false;
            }
        } else if (one.isObject()) {
            if (other.isObject()) {
                JSONObject oneObject = (JSONObject) one;
                JSONObject otherObject = (JSONObject) other;
                return oneObject.getValue().equals(otherObject.getValue());
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Should never happen.");
        }
    }
    
    private JSONArray tests;
    private final JSONArrayTokenHandler tokenHandler;
    private final Tokenizer tokenizer;
    private final Writer writer;
    
    private TokenizerTester(InputStream stream) throws TokenStreamException, RecognitionException, UnsupportedEncodingException {
        JSONParser jsonParser = new JSONParser(new InputStreamReader(stream, "UTF-8"));
        tests = (JSONArray) ((JSONObject)jsonParser.nextValue()).get("tests");
        tokenHandler = new JSONArrayTokenHandler();
        tokenizer = new Tokenizer(tokenHandler);
        tokenizer.setErrorHandler(tokenHandler);
        writer = new OutputStreamWriter(System.out, "UTF-8");
    }
    
    private void runTests() throws SAXException, IOException {
        for (JSONValue val : tests.getValue()) {
            runTest((JSONObject) val);
        }
        writer.flush();
        writer.close();
    }
    
    private void runTest(JSONObject test) throws SAXException, IOException {
        String inputString = ((JSONString) test.get("input")).getValue();
        if ("I'm &notit".equals(inputString)) {
            int i = 1;
        }
        InputSource is = new InputSource(new StringReader(inputString));
        tokenizer.tokenize(is);
        JSONArray actualTokens = tokenHandler.getArray();
        JSONArray expectedTokens = (JSONArray) test.get("output");
        if (jsonDeepEquals(actualTokens, expectedTokens)) {
            writer.write("Success\n");
        } else {
            writer.write("Failure\n");
            writer.write(((JSONString)test.get("description")).getValue());
            writer.write("\nInput:\n");
            writer.write(inputString);            
            writer.write("\nExpected tokens:\n");
            writer.write(expectedTokens.render(false));
            writer.write("\nActual tokens:\n");
            writer.write(actualTokens.render(false));
            writer.write("\n");
        }
    }
    
    /**
     * @param args
     * @throws RecognitionException 
     * @throws TokenStreamException 
     * @throws IOException 
     * @throws SAXException 
     */
    public static void main(String[] args) throws TokenStreamException, RecognitionException, SAXException, IOException {
        for (int i = 0; i < args.length; i++) {
            TokenizerTester tester = new TokenizerTester(new FileInputStream(
                    args[i]));
            tester.runTests();
        }
    }

}
