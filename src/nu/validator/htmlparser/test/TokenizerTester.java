package nu.validator.htmlparser.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.ContentModelFlag;
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

    private static JSONString PLAINTEXT = new JSONString("PLAINTEXT");

    private static JSONString PCDATA = new JSONString("PCDATA");

    private static JSONString RCDATA = new JSONString("RCDATA");

    private static JSONString CDATA = new JSONString("CDATA");

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

    private TokenizerTester(InputStream stream) throws TokenStreamException,
            RecognitionException, UnsupportedEncodingException {
        JSONParser jsonParser = new JSONParser(new InputStreamReader(stream,
                "UTF-8"));
        tests = (JSONArray) ((JSONObject) jsonParser.nextValue()).get("tests");
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
    }

    private void runTest(JSONObject test) throws SAXException, IOException {
        String inputString = ((JSONString) test.get("input")).getValue();
        JSONArray expectedTokens = (JSONArray) test.get("output");
        String description = ((JSONString) test.get("description")).getValue();
        JSONString lastStartTagJSON = ((JSONString) test.get("lastStartTag"));
        String lastStartTag = lastStartTagJSON == null ? null
                : lastStartTagJSON.getValue();
        JSONArray contentModelFlags = (JSONArray) test.get("contentModelFlags");
        if (contentModelFlags == null) {
            runTestInner(inputString, expectedTokens, description,
                    ContentModelFlag.PCDATA, null);
        } else {
            for (JSONValue value : contentModelFlags.getValue()) {
                if (PCDATA.equals(value)) {
                    runTestInner(inputString, expectedTokens, description,
                            ContentModelFlag.PCDATA, lastStartTag);
                } else if (CDATA.equals(value)) {
                    runTestInner(inputString, expectedTokens, description,
                            ContentModelFlag.CDATA, lastStartTag);
                } else if (RCDATA.equals(value)) {
                    runTestInner(inputString, expectedTokens, description,
                            ContentModelFlag.RCDATA, lastStartTag);
                } else if (PLAINTEXT.equals(value)) {
                    runTestInner(inputString, expectedTokens, description,
                            ContentModelFlag.PLAINTEXT, lastStartTag);
                } else {
                    throw new RuntimeException("Broken test data.");
                }
            }
        }
    }

    /**
     * @param contentModelElement
     * @param contentModelFlag
     * @param test
     * @throws SAXException
     * @throws IOException
     */
    private void runTestInner(String inputString, JSONArray expectedTokens,
            String description, ContentModelFlag contentModelFlag,
            String contentModelElement) throws SAXException, IOException {
        tokenHandler.setContentModelFlag(contentModelFlag, contentModelElement);
        InputSource is = new InputSource(new StringReader(inputString));
        try {
            tokenizer.tokenize(is);
            JSONArray actualTokens = tokenHandler.getArray();
            if (jsonDeepEquals(actualTokens, expectedTokens)) {
                writer.write("Success\n");
            } else {
                writer.write("Failure\n");
                writer.write(description);
                writer.write("\nInput:\n");
                writer.write(inputString);
                writer.write("\nExpected tokens:\n");
                writer.write(expectedTokens.render(false));
                writer.write("\nActual tokens:\n");
                writer.write(actualTokens.render(false));
                writer.write("\n");
            }
        } catch (Exception e) {
            writer.write("Failure\n");
            writer.write(description);
            writer.write("\nInput:\n");
            writer.write(inputString);
            writer.write("\n");
            e.printStackTrace(new PrintWriter(writer, false));
        }
    }

    /**
     * @param args
     * @throws RecognitionException
     * @throws TokenStreamException
     * @throws IOException
     * @throws SAXException
     */
    public static void main(String[] args) throws TokenStreamException,
            RecognitionException, SAXException, IOException {
        for (int i = 0; i < args.length; i++) {
            TokenizerTester tester = new TokenizerTester(new FileInputStream(
                    args[i]));
            tester.runTests();
        }
    }

}
