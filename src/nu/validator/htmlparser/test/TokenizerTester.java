package nu.validator.htmlparser.test;

import java.io.InputStream;
import java.util.Iterator;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONObject;
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
    
    private TokenizerTester(InputStream stream) throws TokenStreamException, RecognitionException {
        JSONParser jsonParser = new JSONParser(stream);
        tests = (JSONArray) ((JSONObject)jsonParser.nextValue()).get("tests");
        
    }
    
    private void runTests() {
        for (JSONValue val : tests.getValue()) {
            runTest((JSONObject) val);
        }
    }
    
    private void runTest(JSONObject test) {
        
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
