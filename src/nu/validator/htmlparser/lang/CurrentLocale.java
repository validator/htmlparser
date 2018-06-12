package nu.validator.htmlparser.lang;

import java.util.Locale;

/**
 * Created by thawankeane on 30/08/17.
 */
public class CurrentLocale {
    private static CurrentLocale ourInstance = new CurrentLocale();

    public static CurrentLocale getInstance() {
        return ourInstance;
    }

    private CurrentLocale() {
        // Construtor
    }

    private static Locale currentLocale;

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setCurrentLocale(Locale currentLocale) {
        CurrentLocale.currentLocale = currentLocale;
    }
}
