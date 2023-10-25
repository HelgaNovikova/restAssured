import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class BaseCase {

    protected static Properties properties = new Properties();

    protected final String API_KEY = properties.getProperty("apikey");

    @BeforeAll
    public static void setup() {
        InputStream inputStream;
        try {
            String propFileName = "application.properties";
            inputStream = BaseCase.class.getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
