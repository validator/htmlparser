package nu.validator.htmlparser.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.PropertyResourceBundle;

/**
 * ResourceBundle control which loads UTF-8 encoded properties files.
 * Usage: {@code ResourceBundle bundle = ResourceBundle.getBundle("com.example.i18n.text", new UTF8Control());}.
 * Adapted from <a href="http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle">this Stack Overflow article</a>.
 *
 */
public class UTF8Control extends Control {
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale,
                                    String format, ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
        // The below is a copy of the default implementation.
        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, "properties");
        ResourceBundle bundle = null;
        InputStream stream = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (stream != null) {
            try {
                // Only this line is changed to make it to read properties files
                // as UTF-8.
                bundle = new PropertyResourceBundle(new InputStreamReader(
                    stream, "UTF-8"));
            } finally {
                stream.close();
            }
        }
        return bundle;
    }
}
