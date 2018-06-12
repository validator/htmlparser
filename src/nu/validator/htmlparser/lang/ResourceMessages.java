package nu.validator.htmlparser.lang;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceMessages {

    private static final String BUNDLE_NAME = "nu.validator.htmlparser.lang.messages";

    public static String getMessage(MessageEnum messageEnum, Locale locale, Object... args ){
        ResourceBundle resourceBundle = null;
        if(locale!=null)
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, new UTF8Control());
        else
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());
            String message = null;
            if(resourceBundle!=null){
               message = resourceBundle.getString(messageEnum.getProp());
               if(message!=null && args.length > 0)
                message = MessageFormat.format(message, args);
            }
            return message;
    }
}
