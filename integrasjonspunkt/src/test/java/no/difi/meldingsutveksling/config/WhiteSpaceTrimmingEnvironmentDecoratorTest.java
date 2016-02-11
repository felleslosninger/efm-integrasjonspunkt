package no.difi.meldingsutveksling.config;


import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WhiteSpaceTrimmingEnvironmentDecoratorTest {

    public static final String LEADING = "leading";
    public static final String TRAILING = "trailing";
    public static final String NULL = "null";
    private Map<String, String> sampleValues = new HashMap<>();
    private Environment environment;

    {
        sampleValues.put(LEADING, "              leading");
        sampleValues.put(TRAILING, "trailing          ");
        sampleValues.put(NULL, null);
    }

    @Test
    public void testWhitespaceTrim() {
        WhiteSpaceTrimmingEnvironmentDecorator wst = new WhiteSpaceTrimmingEnvironmentDecorator(environment);
        assertEquals(LEADING, wst.getProperty(LEADING));
        assertEquals(TRAILING, wst.getProperty(TRAILING));
        assertNull(wst.getProperty(NULL));
    }

    @Before
    public void setup() {
        environment = new Environment() {

            @Override
            public boolean containsProperty(String s) {
                return false;
            }

            @Override
            public String getProperty(String s) {
                return sampleValues.get(s);
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return sampleValues.get(key) != null ? sampleValues.get(key) : defaultValue;
            }

            @Override
            public <T> T getProperty(String s, Class<T> aClass) {
                return null;
            }

            @Override
            public <T> T getProperty(String s, Class<T> aClass, T t) {
                return null;
            }

            @Override
            public <T> Class<T> getPropertyAsClass(String s, Class<T> aClass) {
                return null;
            }

            @Override
            public String getRequiredProperty(String s) throws IllegalStateException {
                return null;
            }

            @Override
            public <T> T getRequiredProperty(String s, Class<T> aClass) throws IllegalStateException {
                return null;
            }

            @Override
            public String resolvePlaceholders(String s) {
                return null;
            }

            @Override
            public String resolveRequiredPlaceholders(String s) throws IllegalArgumentException {
                return null;
            }

            @Override
            public String[] getActiveProfiles() {
                return new String[0];
            }

            @Override
            public String[] getDefaultProfiles() {
                return new String[0];
            }

            @Override
            public boolean acceptsProfiles(String... strings) {
                return false;
            }
        };
    }


}