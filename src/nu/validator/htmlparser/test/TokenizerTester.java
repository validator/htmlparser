package nu.validator.htmlparser.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
    
    private TokenizerTester(InputStream stream) throws TokenStreamException, RecognitionException {
        JSONParser jsonParser = new JSONParser(stream);
        tests = (JSONArray) ((JSONObject)jsonParser.nextValue()).get("tests");
        tokenHandler = new JSONArrayTokenHandler();
        tokenizer = new Tokenizer(tokenHandler);
    }
    
    private void runTests() throws SAXException, IOException {
        for (JSONValue val : tests.getValue()) {
            runTest((JSONObject) val);
        }
    }
    
    private void runTest(JSONObject test) throws SAXException, IOException {
        JSONString inputString = (JSONString) test.get("input");
        InputSource is = new InputSource(new StringReader(inputString.getValue()));
        tokenizer.tokenize(is);
        JSONArray actualTokens = tokenHandler.getArray();
        JSONArray expectedTokens = (JSONArray) test.get("output");
        if (jsonDeepEquals(actualTokens, expectedTokens)) {
            System.out.println("Success");
        } else {
            System.out.println("Failure");
            System.out.println(((JSONString)test.get("description")).getValue());
            System.out.println("Expected tokens:");
            System.out.println(expectedTokens.render(false));
            System.out.println("Actual tokens:");
            System.out.println(actualTokens.render(false));
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
